/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.run.rewire;

import io.codelaser.maddi.callgraph.ComputeCallGraph;
import io.codelaser.maddi.callgraph.PrimaryTypeUseGraph;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;

/**
 * The rewire driver: edit a randomly chosen source file on disk, ask the inspector what changed, re-parse with the
 * changed types INVALID and their dependents REWIRE, prep, restore the file, and re-parse again — over and over.
 * <p>
 * Run as {@code --analysis-steps rewire-tests}. With {@link Options} it is also the corpus measurement of
 * {@code docs/design/incremental-and-overlay.md} §8: one {@code REWIRE STATS} line per re-parse (how many types were
 * changed, rewired and rebuilt, and the time of each step), a summary at the end, and, every
 * {@link Options#oracleEvery()} iterations, the {@link ReparseOracle} against a cold parse of the same files. An
 * oracle difference is a defect of the incremental path; {@link #go()} reports it in its {@link Result}.
 */
public class RunRewireTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunRewireTests.class);
    private static final int ITERATIONS = 200;
    public static final long SEED = 15;

    /**
     * @param iterations   how many files to edit (each edit is two re-parses: the edit and its restore)
     * @param oracleEvery  run the oracle after every n-th restore; 0 never. A cold parse per check: on a corpus, keep
     *                     it rare
     * @param parseOptions the options of the cold parse the run started from; every re-parse takes them, with its own
     *                     {@code Invalidated}. A re-parse with other options than the parse it updates is not the
     *                     same parse
     * @param prep         prep over a parse result, returning its call graph; the driver measures it per re-parse
     * @param coldInspector a fresh, initialized inspector on the same input configuration, for the oracle; may be null
     *                     when {@code oracleEvery} is 0
     */
    public record Options(int iterations,
                          int oracleEvery,
                          JavaInspector.ParseOptions parseOptions,
                          BiFunction<JavaInspector, ParseResult, G<Info>> prep,
                          Supplier<JavaInspector> coldInspector) {
    }

    /**
     * @param reparses         re-parses done
     * @param oracleChecks     oracle checks done
     * @param oracleDifferences every difference an oracle check found; empty is the expected outcome
     */
    public record Result(int reparses, int oracleChecks, List<String> oracleDifferences) {
    }

    private final InputConfiguration inputConfiguration;
    private final JavaInspector javaInspector;
    private final ParseResult parseResult;
    private final G<Info> initialGraph;
    private final Options options;
    private final List<Stats> stats = new ArrayList<>();
    private final List<String> oracleDifferences = new ArrayList<>();
    private int oracleChecks;
    private G<Info> lastGraph;
    private ParseResult lastParseResult;

    /** The stress loop as it always ran: 200 iterations, the call graph as the only prep, no oracle. */
    public RunRewireTests(InputConfiguration inputConfiguration,
                          JavaInspector javaInspector,
                          ParseResult parseResult,
                          G<Info> graph) {
        this(inputConfiguration, javaInspector, parseResult, graph, new Options(ITERATIONS, 0, null,
                (inspector, pr) -> new ComputeCallGraph(inspector.runtime(), pr, _ -> false).go().graph(), null));
    }

    public RunRewireTests(InputConfiguration inputConfiguration,
                          JavaInspector javaInspector,
                          ParseResult parseResult,
                          G<Info> graph,
                          Options options) {
        this.inputConfiguration = inputConfiguration;
        this.javaInspector = javaInspector;
        this.parseResult = parseResult;
        this.initialGraph = graph;
        this.options = options;
    }

    /** One re-parse: what it touched and what each step took. */
    private record Stats(int iteration, boolean restore, int changed, int cone, int rewiredTypes, int primaryTypes,
                         long reloadMs, long reparseMs, long prepMs, long callGraphMs, int vertices, long edges,
                         long heapMb) {
        @Override
        public String toString() {
            return String.format("iteration %d%s: %d changed, %d in the REWIRE cone, %d type(s) built by the rewire, of"
                                 + " %d primary types; reload %d ms, re-parse %d ms, prep %d ms (call graph alone %d"
                                 + " ms); call graph %d vertices, %d edges; heap after GC %d MB", iteration,
                    restore ? " (restore)" : "", changed, cone, rewiredTypes, primaryTypes, reloadMs, reparseMs,
                    prepMs, callGraphMs, vertices, edges, heapMb);
        }
    }

    /*
     Heap in use after a collection: a run whose re-parses keep what they replaced shows it growing step by step. The
     collection is outside every timed step.
     */
    private static long heapAfterGcMb() {
        System.gc();
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    /*
     -Dmaddi.rewire.histogram=N: the N largest classes of the live heap after each re-parse (a full collection first,
     as the histogram's own), to see what a growing heap keeps. Off by default: it costs a second or so each time.
     */
    private static final int HISTOGRAM = Integer.getInteger("maddi.rewire.histogram", 0);

    private static void logHistogram(int iteration, boolean restore) {
        if (HISTOGRAM <= 0) return;
        try {
            Object out = java.lang.management.ManagementFactory.getPlatformMBeanServer().invoke(
                    new javax.management.ObjectName("com.sun.management:type=DiagnosticCommand"),
                    "gcClassHistogram", new Object[]{new String[0]}, new String[]{String[].class.getName()});
            String top = ((String) out).lines().limit(HISTOGRAM + 3L).collect(java.util.stream.Collectors.joining("\n"));
            LOGGER.info("REWIRE HISTOGRAM iteration {}{}:\n{}", iteration, restore ? " (restore)" : "", top);
        } catch (Exception e) {
            LOGGER.warn("REWIRE HISTOGRAM not available: {}", e.toString());
        }
    }

    /*
     -Dmaddi.rewire.leakCheck=true: hold a weak reference to a type the rewire built at the second re-parse, and a few
     re-parses later, when the type should long be garbage, print the chain of references by which the driver still
     reaches it (LeakPath). "not found" means a root outside the driver: a static, a thread-local, a cache.
     */
    private static final boolean LEAK_CHECK = Boolean.getBoolean("maddi.rewire.leakCheck");
    private java.lang.ref.WeakReference<TypeInfo> watched;
    private int watchedAt;

    private void leakCheck(int iteration, InfoMapView view) {
        if (!LEAK_CHECK || view == null) return;
        int n = stats.size();
        if (watched == null && n == 2 && !view.rewiredTypes().isEmpty()) {
            TypeInfo some = view.rewiredTypes().stream().filter(TypeInfo::isPrimaryType)
                    .min(Comparator.comparing(TypeInfo::fullyQualifiedName)).orElse(null);
            if (some != null) {
                watched = new java.lang.ref.WeakReference<>(some);
                watchedAt = n;
                LOGGER.info("REWIRE LEAK watching {} built at re-parse {}", some, n);
            }
        } else if (watched != null && n == watchedAt + 4) {
            for (int i = 0; i < 3; i++) System.gc();
            TypeInfo still = watched.get();
            if (still == null) {
                LOGGER.info("REWIRE LEAK the type built at re-parse {} was collected", watchedAt);
            } else {
                LOGGER.info("REWIRE LEAK {} built at re-parse {} is still reachable; from the driver:{}", still,
                        watchedAt, LeakPath.path(this, still));
            }
        }
    }

    public Result go() {
        Random random = new Random(SEED);
        List<TypeInfo> list = parseResult.primaryTypes().stream().sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
        G<Info> infoGraph = initialGraph;
        lastParseResult = parseResult;
        for (int i = 0; i < options.iterations(); ++i) {
            LOGGER.info("Compute primary type use graph, iteration {}", i);
            TypeInfo typeToRewire = list.get(random.nextInt(list.size()));
            Path path = Path.of(typeToRewire.compilationUnit().uri().getSchemeSpecificPart());
            Path absolutePath = path.toAbsolutePath();
            boolean stop = false;
            try {
                String content = Files.readString(path);
                boolean restored = false;
                try {
                    LOGGER.info("Modifying {}", absolutePath);
                    String updatedContent = "// some comment\n\n" + content;
                    // this will trigger a change of fingerprint
                    stop |= write(path, updatedContent);
                    if (!stop) {
                        infoGraph = reloadSources(typeToRewire, infoGraph, i, false);
                        LOGGER.info("Restoring {}", absolutePath);
                        stop |= write(path, content);
                        restored = true;
                        /*
                        The analyzer must follow the file BACK, or its fingerprints stop describing the disk: after
                        the re-parse above it holds the fingerprint of the EDITED file, which is what the next
                        reloadSources compares against. Two things go wrong if we leave it there. The file stays
                        "changed" for ever, so every later iteration invalidates it along with the one it meant to
                        pick. And when this file is picked again, the very same edit produces the very same content
                        and hence the very same fingerprint the analyzer already holds -- so it is told, correctly,
                        that nothing changed, and the assertion in reloadSources fires.
                        Reverting is a rewire in its own right, so this is a second exercise, not overhead.
                         */
                        if (!stop) {
                            infoGraph = reloadSources(typeToRewire, infoGraph, i, true);
                            if (options.oracleEvery() > 0 && (i + 1) % options.oracleEvery() == 0) oracle(i);
                        }
                    }
                } finally {
                    if (!restored) {
                        // whatever went wrong, never leave the file edited
                        LOGGER.info("Restoring {}", absolutePath);
                        stop |= write(path, content);
                    }
                }
            } catch (IOException ioe) {
                LOGGER.error("Could not read {}", absolutePath, ioe);
                stop = true;
            }
            if (stop) break;
        }
        logSummary();
        return new Result(stats.size(), oracleChecks, List.copyOf(oracleDifferences));
    }

    private G<Info> reloadSources(TypeInfo pt, G<Info> infoGraph, int iteration, boolean restore) throws IOException {
        long t0 = System.nanoTime();
        LOGGER.info("Calling javaInspector.reloadSources");
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration, Map.of());
        assert rr.sourceHasChanged().contains(pt);
        Set<TypeInfo> dependentPrimaryTypes = new PrimaryTypeUseGraph(infoGraph).dependentsOf(rr.sourceHasChanged());
        assert Collections.disjoint(dependentPrimaryTypes, rr.sourceHasChanged());
        LOGGER.info("{} has {} dependent types", pt, dependentPrimaryTypes.size());
        JavaInspector.Invalidated invalidated = ti -> {
            if (rr.sourceHasChanged().contains(ti)) return INVALID;
            if (dependentPrimaryTypes.contains(ti)) return REWIRE;
            return UNCHANGED;
        };
        long t1 = System.nanoTime();
        LOGGER.info("Reparse");
        ParseResult parseResult1 = javaInspector.parse(reparseOptions(invalidated)).parseResult();
        long t2 = System.nanoTime();
        LOGGER.info("Prep");
        G<Info> graph = options.prep().apply(javaInspector, parseResult1);
        long t3 = System.nanoTime();
        // the call graph on its own, to tell prep's two halves apart: per-type work against the whole-graph passes
        new ComputeCallGraph(javaInspector.runtime(), parseResult1, _ -> false).go();
        long t4 = System.nanoTime();
        InfoMapView view = javaInspector.lastRewireInfoMap();
        Stats s = new Stats(iteration, restore, rr.sourceHasChanged().size(), dependentPrimaryTypes.size(),
                view == null ? 0 : view.rewiredTypes().size(), parseResult1.primaryTypes().size(),
                ms(t1 - t0), ms(t2 - t1), ms(t3 - t2), ms(t4 - t3), graph.vertices().size(), graph.edgeStream().count(),
                heapAfterGcMb());
        stats.add(s);
        LOGGER.info("REWIRE STATS {}", s);
        logHistogram(iteration, restore);
        leakCheck(iteration, view);
        lastGraph = graph;
        lastParseResult = parseResult1;
        return graph;
    }

    private JavaInspector.ParseOptions reparseOptions(JavaInspector.Invalidated invalidated) {
        JavaInspector.ParseOptions base = options.parseOptions();
        if (base == null) {
            return new JavaInspector.ParseOptions.Builder()
                    .setLombok(inputConfiguration.containsLombok())
                    .setDetailedSources(true)
                    .setFailFast(true)
                    .setInvalidated(invalidated)
                    .setParallel(true)
                    .build();
        }
        return new JavaInspector.ParseOptions(base.failFast(), base.detailedSources(), invalidated, base.parallel(),
                base.lombok(), base.ignoreModule(), base.parameterNames(), base.syntheticListField());
    }

    /** The files are back as the run started; compare the warm state with a cold parse and prep of them. */
    private void oracle(int iteration) {
        long t0 = System.nanoTime();
        JavaInspector cold = options.coldInspector().get();
        ParseResult coldResult = cold.parse(options.parseOptions()).parseResult();
        G<Info> coldGraph = options.prep().apply(cold, coldResult);
        List<String> differences = ReparseOracle.differences(javaInspector, lastParseResult, lastGraph, cold,
                coldResult, coldGraph);
        ++oracleChecks;
        if (differences.isEmpty()) {
            LOGGER.info("REWIRE ORACLE iteration {}: as a cold parse ({} ms)", iteration, ms(System.nanoTime() - t0));
        } else {
            LOGGER.error("REWIRE ORACLE iteration {}: {} difference(s) with a cold parse", iteration,
                    differences.size());
            differences.stream().limit(50).forEach(d -> LOGGER.error("REWIRE ORACLE DIFFERENCE {}", d));
            differences.forEach(d -> oracleDifferences.add("iteration " + iteration + ": " + d));
        }
    }

    private void logSummary() {
        if (stats.isEmpty()) return;
        LOGGER.info("REWIRE SUMMARY {} re-parse(s) over {} primary types; median cone {}, median rewire-built types {};"
                    + " median ms: reload {}, re-parse {}, prep {}, call graph alone {}; oracle: {} check(s), {}"
                    + " difference(s)", stats.size(), stats.getLast().primaryTypes(),
                median(stats.stream().mapToLong(Stats::cone)), median(stats.stream().mapToLong(Stats::rewiredTypes)),
                median(stats.stream().mapToLong(Stats::reloadMs)), median(stats.stream().mapToLong(Stats::reparseMs)),
                median(stats.stream().mapToLong(Stats::prepMs)), median(stats.stream().mapToLong(Stats::callGraphMs)),
                oracleChecks, oracleDifferences.size());
    }

    private static long median(java.util.stream.LongStream values) {
        long[] sorted = values.sorted().toArray();
        return sorted.length == 0 ? 0 : sorted[sorted.length / 2];
    }

    private static long ms(long nanos) {
        return nanos / 1_000_000;
    }

    private static boolean write(Path path, String content) {
        try {
            Files.writeString(path, content);
            return false;
        } catch (IOException ioe) {
            LOGGER.error("Could not write {}", path.toAbsolutePath(), ioe);
            return true;
        }
    }
}
