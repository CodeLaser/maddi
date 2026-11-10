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

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.codelaser.maddi.callgraph.ComputeCallGraph;
import io.codelaser.maddi.callgraph.PrimaryTypeUseGraph;
import io.codelaser.maddi.callgraph.ReparseWithDependents;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;

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
    // the rewired cone's analyzer output carries, and the seed is the re-parsed types alone; false: the cone is seeded
    private static final boolean CARRY = !"false".equals(System.getProperty("maddi.rewire.carry"));

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
                          Supplier<JavaInspector> coldInspector,
                          WarmAnalysis analysis) {

        /** Without the analysis. */
        public Options(int iterations, int oracleEvery, JavaInspector.ParseOptions parseOptions,
                       BiFunction<JavaInspector, ParseResult, G<Info>> prep, Supplier<JavaInspector> coldInspector) {
            this(iterations, oracleEvery, parseOptions, prep, coldInspector, null);
        }
    }

    /**
     * The modification analysis after prep: once in full on the parse the run starts from, then seeded after every
     * re-parse, and in full on the oracle's cold parse, whose output it is compared with.
     */
    public interface WarmAnalysis {
        /**
         * @param seedTypes null: analyse everything; otherwise the primary types to analyse again, on the state the
         *                  previous analysis left
         * @param carryFrom null, or the previous generation's primary types whose analyzer output carries onto their
         *                  rewired copies before the analysis (the REWIRE cone; {@code ModificationRequest.carryFrom})
         * @param seedPrevious null, or the previous generation's objects of the seed types, for the early cutoff
         *                  ({@code ModificationRequest.seedPrevious})
         * @return for a seeded analysis ({@code seedTypes} not null): whether it stayed incremental, false when it fell
         *         back to the cold analysis of everything (the early cutoff found a changed element whose change reaches
         *         outside the seed). Ignored for a full analysis
         */
        boolean analyze(JavaInspector inspector, ParseResult parseResult, G<Info> graph, Set<TypeInfo> seedTypes,
                     Set<TypeInfo> carryFrom, Set<TypeInfo> seedPrevious);

        /** The output of one primary type, comparable between two analyses. */
        String dump(JavaInspector inspector, TypeInfo primaryType);
    }

    /**
     * @param reparses         re-parses done
     * @param oracleChecks     oracle checks done
     * @param oracleDifferences every difference an oracle check found; empty is the expected outcome
     * @param coldFallbacks    re-parses whose seeded analysis fell back to the cold analysis; 0 without the analysis
     */
    public record Result(int reparses, int oracleChecks, List<String> oracleDifferences, int coldFallbacks) {
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
                         long reloadMs, long reparseMs, long prepMs, long callGraphMs, long analysisMs,
                         boolean coldFallback, int vertices, long edges, long heapMb) {
        @Override
        public String toString() {
            return String.format("iteration %d%s: %d changed, %d in the REWIRE cone, %d type(s) built by the rewire, of"
                                 + " %d primary types; reload %d ms, re-parse %d ms, prep %d ms (call graph alone %d"
                                 + " ms)%s; call graph %d vertices, %d edges; heap after GC %d MB", iteration,
                    restore ? " (restore)" : "", changed, cone, rewiredTypes, primaryTypes, reloadMs, reparseMs,
                    prepMs, callGraphMs, analysisMs < 0 ? "" : ", analysis " + analysisMs + " ms"
                                                              + (coldFallback ? " (fell back to cold)" : ""), vertices, edges,
                    heapMb);
        }
    }

    /*
     Heap in use after a collection: a run whose re-parses keep what they replaced shows it growing step by step. The
     collection is outside every timed step.
     */
    private static long heapAfterGcMb() {
        System.gc();
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    /*
     -Dmaddi.rewire.histogram=N: the N largest classes of the live heap after each re-parse (a full collection first,
     as the histogram's own), to see what a growing heap keeps. Off by default: it costs a second or so each time.
     */
    private static final int HISTOGRAM = Integer.getInteger("maddi.rewire.histogram", 0);

    /*
     -Dmaddi.rewire.types=A,B: edit these primary types, in this order and round, instead of drawing them at random;
     each is the first primary type whose fully qualified name ends in the given text. Edit a type first to compare
     its warm summary with the cold run's own values rather than with values an earlier rewire carried.
     */
    private static final String TYPES = System.getProperty("maddi.rewire.types");

    /*
     DIAGNOSTIC -Dmaddi.rewire.oracleAtStart=true: run the oracle before the first edit as well (logged as iteration
     -1). Nothing has been re-parsed or carried yet, so it compares two cold runs in one JVM: a difference there is
     the analysis not being deterministic, not the incremental path. Each oracle call also logs a digest of both
     sides' analysis output, to tell which of the runs is the odd one.
     */
    private static final boolean ORACLE_AT_START = Boolean.getBoolean("maddi.rewire.oracleAtStart");
    // DIAGNOSTIC -Dmaddi.rewire.dumpTo=<directory>: each oracle call writes both sides' analysis output there
    // (cold-N.txt, warm-N.txt, one element per line under its primary type), to compare runs of different JVMs
    private static final String DUMP_TO = System.getProperty("maddi.rewire.dumpTo");

    private static void logHistogram(int iteration, boolean restore) {
        if (HISTOGRAM <= 0) return;
        try {
            Object out = ManagementFactory.getPlatformMBeanServer().invoke(
                    new ObjectName("com.sun.management:type=DiagnosticCommand"),
                    "gcClassHistogram", new Object[]{new String[0]}, new String[]{String[].class.getName()});
            String top = ((String) out).lines().limit(HISTOGRAM + 3L).collect(Collectors.joining("\n"));
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
    private final List<WeakReference<TypeInfo>> watched = new ArrayList<>();
    private int watchedAt;

    private void leakCheck(int iteration, InfoMapView view) {
        if (!LEAK_CHECK || view == null) return;
        int n = stats.size();
        if (watched.isEmpty() && n == 2 && !view.rewiredTypes().isEmpty()) {
            // every type the rewire built: whether a generation survives is a count, and one survivor's chain is enough
            view.rewiredTypes().forEach(t -> watched.add(new WeakReference<>(t)));
            LOGGER.info("REWIRE LEAK watching the {} type(s) built at re-parse {}", watched.size(), n);
            watchedAt = n;
        } else if (!watched.isEmpty() && n == watchedAt + 4) {
            for (int i = 0; i < 3; i++) System.gc();
            List<TypeInfo> alive = watched.stream().map(WeakReference::get).filter(Objects::nonNull)
                    .toList();
            LOGGER.info("REWIRE LEAK of the {} type(s) built at re-parse {}, {} are still reachable", watched.size(),
                    watchedAt, alive.size());
            // the types reachable from the driver by name: more than one object for a name is a generation kept
            List<TypeInfo> all = LeakPath.reachable(this, TypeInfo.class, 40_000_000);
            Map<String, List<TypeInfo>> byName = all.stream().collect(Collectors.groupingBy(
                    TypeInfo::fullyQualifiedName));
            List<Map.Entry<String, List<TypeInfo>>> doubled = byName.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1).sorted(Map.Entry.comparingByKey()).toList();
            LOGGER.info("REWIRE LEAK {} type object(s) reachable from the driver, {} name(s) with more than one: {}",
                    all.size(), doubled.size(), doubled.stream().limit(20)
                            .map(e -> e.getKey() + " x" + e.getValue().size()).toList());
            // the name with the most objects: the one whose generations accumulate
            for (Map.Entry<String, List<TypeInfo>> e : doubled.stream()
                    .max(Comparator.comparingInt(en -> en.getValue().size())).stream().toList()) {
                // every instance of one name: which is live, which is generation 0, and how each is reached
                String simple = e.getKey().substring(e.getKey().lastIndexOf('.') + 1) + ".java";
                javaInspector.sourceFiles().stream().filter(sf -> sf.uri().toString().endsWith("/" + simple))
                        .forEach(sf -> LOGGER.info("REWIRE LEAK source file {} of {} (set object {})", sf.uri(),
                                sf.sourceSet(), System.identityHashCode(sf.sourceSet())));
                TypeInfo live = lastParseResult.findType(e.getKey());
                TypeInfo first = parseResult.findType(e.getKey());
                for (TypeInfo t : e.getValue()) {
                    String role = t == live ? "live" : t == first ? "generation 0" : "other";
                    LOGGER.info("REWIRE LEAK instance {} of {} ({}; in the last parse result's primary types: {}), "
                                + "from the driver:{}", System.identityHashCode(t), e.getKey(), role,
                            lastParseResult.primaryTypes().stream().anyMatch(pt -> pt == t), LeakPath.path(this, t));
                }
            }
            if (!alive.isEmpty()) {
                TypeInfo one = alive.stream().filter(TypeInfo::isPrimaryType).findFirst().orElse(alive.getFirst());
                LOGGER.info("REWIRE LEAK {} (source set {}), from the driver:{}", one,
                        one.compilationUnit() == null ? null : one.compilationUnit().sourceSet(),
                        LeakPath.path(this, one));
            }
        }
    }

    /*
     -Dmaddi.rewire.leakFields=A,B: at re-parse A, hold weak references to the analysis-made fields the driver reaches
     (the link engine's virtual fields and translations: owned by a type, not among its fields()); at re-parse B, after
     re-analyses that replaced them, print how many are still reachable and how a few of them are reached. An analysis
     that clears an element and computes it again should leave the old ones to the collector.
     */
    private static final String LEAK_FIELDS = System.getProperty("maddi.rewire.leakFields");
    private final List<java.lang.ref.WeakReference<io.codelaser.maddi.cst.api.info.FieldInfo>> watchedFields =
            new ArrayList<>();

    private void leakFields() {
        if (LEAK_FIELDS == null) return;
        String[] ab = LEAK_FIELDS.split(",");
        int a = Integer.parseInt(ab[0].strip());
        int b = Integer.parseInt(ab[1].strip());
        int n = stats.size();
        if (n == a) {
            List<io.codelaser.maddi.cst.api.info.FieldInfo> all = LeakPath.reachable(this,
                    io.codelaser.maddi.cst.api.info.FieldInfo.class, 40_000_000);
            List<io.codelaser.maddi.cst.api.info.FieldInfo> made = all.stream()
                    .filter(f -> f.owner() != null && f.owner().fields().stream().noneMatch(g -> g == f)).toList();
            int step = Math.max(1, made.size() / 50_000);
            for (int i = 0; i < made.size(); i += step) watchedFields.add(new java.lang.ref.WeakReference<>(made.get(i)));
            LOGGER.info("REWIRE LEAKFIELDS at re-parse {}: {} field(s) reachable, {} analysis-made, watching {}", n,
                    all.size(), made.size(), watchedFields.size());
        } else if (n == b && !watchedFields.isEmpty()) {
            for (int i = 0; i < 3; i++) System.gc();
            List<io.codelaser.maddi.cst.api.info.FieldInfo> alive = watchedFields.stream()
                    .map(java.lang.ref.WeakReference::get).filter(Objects::nonNull).toList();
            LOGGER.info("REWIRE LEAKFIELDS at re-parse {}: of the {} watched since re-parse {}, {} still reachable", n,
                    watchedFields.size(), a, alive.size());
            for (int i = 0; i < Math.min(5, alive.size()); i++) {
                io.codelaser.maddi.cst.api.info.FieldInfo f = alive.get(i * alive.size() / Math.min(5, alive.size()));
                LOGGER.info("REWIRE LEAKFIELDS {} of {}, from the driver:{}", f.name(), f.owner(),
                        LeakPath.path(this, f));
            }
        }
    }

    public Result go() {
        Random random = new Random(SEED);
        List<TypeInfo> list = parseResult.primaryTypes().stream().sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
        List<TypeInfo> chosen = TYPES == null ? null : Arrays.stream(TYPES.split(","))
                .map(name -> list.stream().filter(t -> t.fullyQualifiedName().endsWith(name.strip())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("maddi.rewire.types: no type " + name)))
                .toList();
        G<Info> infoGraph = initialGraph;
        lastParseResult = parseResult;
        if (options.analysis() != null) {
            long t0 = System.nanoTime();
            options.analysis().analyze(javaInspector, parseResult, initialGraph, null, null, null);
            LOGGER.info("REWIRE ANALYSIS of the parse the run starts from: {} ms", ms(System.nanoTime() - t0));
        }
        if (ORACLE_AT_START) {
            lastGraph = initialGraph;
            oracle(-1);
        }
        for (int i = 0; i < options.iterations(); ++i) {
            LOGGER.info("Compute primary type use graph, iteration {}", i);
            TypeInfo typeToRewire = chosen != null ? chosen.get(i % chosen.size())
                    : list.get(random.nextInt(list.size()));
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
        return new Result(stats.size(), oracleChecks, List.copyOf(oracleDifferences), coldFallbacks());
    }

    private G<Info> reloadSources(TypeInfo pt, G<Info> infoGraph, int iteration, boolean restore) throws IOException {
        long t0 = System.nanoTime();
        ParseResult previousParseResult = lastParseResult;
        LOGGER.info("Calling javaInspector.reloadSources");
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration, Map.of());
        assert rr.sourceHasChanged().contains(pt);
        Set<TypeInfo> dependentPrimaryTypes = new PrimaryTypeUseGraph(infoGraph).dependentsOf(rr.sourceHasChanged());
        assert Collections.disjoint(dependentPrimaryTypes, rr.sourceHasChanged());
        LOGGER.info("{} has {} dependent types", pt, dependentPrimaryTypes.size());
        long t1 = System.nanoTime();
        LOGGER.info("Reparse");
        // the changed types INVALID, their dependents REWIRE; when a changed type's declarations moved, the
        // dependents are parsed again instead, and have no analysis to carry
        ReparseWithDependents.Outcome reparse = ReparseWithDependents.go(javaInspector, rr.sourceHasChanged(),
                dependentPrimaryTypes, this::reparseOptions);
        ParseResult parseResult1 = reparse.parseResult();
        long t2 = System.nanoTime();
        LOGGER.info("Prep");
        G<Info> graph = options.prep().apply(javaInspector, parseResult1);
        long t3 = System.nanoTime();
        // the call graph on its own, to tell prep's two halves apart: per-type work against the whole-graph passes
        new ComputeCallGraph(javaInspector.runtime(), parseResult1, _ -> false).go();
        long t4 = System.nanoTime();
        long analysisMs = -1;
        boolean coldFallback = false;
        if (options.analysis() != null) {
            // seeded: the re-parsed types, with the rewired cone's analyzer output carried onto the copies, so the
            // worklist wakes a cone type only when an input it read changed; the kept ones keep their values. Without
            // the carry (maddi.rewire.carry=false) the cone has no derived values and is seeded with the re-parsed ones
            Set<TypeInfo> seedTypes = new HashSet<>();
            for (TypeInfo t : rr.sourceHasChanged()) seedTypes.add(parseResult1.findType(t.fullyQualifiedName()));
            Set<TypeInfo> carryFrom = null;
            Set<TypeInfo> seedPrevious = null;
            if (CARRY && !reparse.dependentsReparsed()) {
                // every previous primary type but the changed ones: the REWIRE cone, and what else the re-parse
                // rebuilt (a source set re-scanned in full); a kept type maps onto itself and is skipped
                Set<TypeInfo> previous = new HashSet<>(previousParseResult.primaryTypes());
                previous.removeAll(rr.sourceHasChanged());
                carryFrom = Set.copyOf(previous);
                seedPrevious = Set.copyOf(rr.sourceHasChanged());
            } else {
                for (TypeInfo t : dependentPrimaryTypes) seedTypes.add(parseResult1.findType(t.fullyQualifiedName()));
            }
            seedTypes.remove(null);
            coldFallback = !options.analysis().analyze(javaInspector, parseResult1, graph, seedTypes, carryFrom,
                    seedPrevious);
            analysisMs = ms(System.nanoTime() - t4);
        }
        InfoMapView view = javaInspector.lastRewireInfoMap();
        Stats s = new Stats(iteration, restore, rr.sourceHasChanged().size(), dependentPrimaryTypes.size(),
                view == null ? 0 : view.rewiredTypes().size(), parseResult1.primaryTypes().size(),
                ms(t1 - t0), ms(t2 - t1), ms(t3 - t2), ms(t4 - t3), analysisMs, coldFallback, graph.vertices().size(),
                graph.edgeStream().count(),
                heapAfterGcMb());
        stats.add(s);
        LOGGER.info("REWIRE STATS {}", s);
        logHistogram(iteration, restore);
        lastGraph = graph;
        lastParseResult = parseResult1;
        leakCheck(iteration, view); // after: it tells the live types by lastParseResult
        leakFields();
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
        // the analysis first: the warm side's prep data has been through it too, and it writes carried-tier values
        // of its own (the constants of an interface have none before it): compared before, every one of them showed
        // as a prep difference
        if (options.analysis() != null) options.analysis().analyze(cold, coldResult, coldGraph, null, null, null);
        List<String> differences = new ArrayList<>(ReparseOracle.differences(javaInspector, lastParseResult, lastGraph,
                cold, coldResult, coldGraph));
        if (options.analysis() != null) differences.addAll(analysisDifferences(cold, coldResult));
        ++oracleChecks;
        if (differences.isEmpty()) {
            LOGGER.info("REWIRE ORACLE iteration {}: as a cold parse ({} ms)", iteration, ms(System.nanoTime() - t0));
        } else {
            LOGGER.error("REWIRE ORACLE iteration {}: {} difference(s) with a cold parse", iteration,
                    differences.size());
            // up to 50 of each kind ("prep of", "analysis of", ...): one kind must not hide another
            differences.stream().collect(Collectors.groupingBy(
                            d -> d.substring(0, Math.max(0, d.indexOf(" of "))), LinkedHashMap::new,
                            Collectors.toList()))
                    .forEach((kind, list) -> {
                        LOGGER.error("REWIRE ORACLE iteration {}: {} {} difference(s)", iteration, list.size(),
                                kind.isEmpty() ? "other" : kind);
                        list.stream().limit(50).forEach(d -> LOGGER.error("REWIRE ORACLE DIFFERENCE {}", d));
                    });
            differences.forEach(d -> oracleDifferences.add("iteration " + iteration + ": " + d));
        }
    }

    /* per primary type, the element lines of the analysis output that differ: "-" cold only, "+" warm only */
    private List<String> analysisDifferences(JavaInspector cold, ParseResult coldResult) {
        List<String> differences = new ArrayList<>();
        int coldDigest = 0;
        int warmDigest = 0;
        StringBuilder coldDump = new StringBuilder();
        StringBuilder warmDump = new StringBuilder();
        for (TypeInfo coldType : coldResult.primaryTypes().stream()
                .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList()) {
            TypeInfo warmType = lastParseResult.findType(coldType.fullyQualifiedName());
            if (warmType == null) continue; // ReparseOracle reports it
            List<String> c = options.analysis().dump(cold, coldType).lines().map(String::strip)
                    .map(RunRewireTests::linksInOrder).toList();
            List<String> w = options.analysis().dump(javaInspector, warmType).lines().map(String::strip)
                    .map(RunRewireTests::linksInOrder).toList();
            if (DUMP_TO != null) {
                coldDump.append("== ").append(coldType.fullyQualifiedName()).append('\n');
                c.forEach(l -> coldDump.append(l).append('\n'));
                warmDump.append("== ").append(coldType.fullyQualifiedName()).append('\n');
                w.forEach(l -> warmDump.append(l).append('\n'));
            }
            coldDigest = 31 * coldDigest + c.hashCode();
            warmDigest = 31 * warmDigest + w.hashCode();
            if (c.equals(w)) continue;
            StringBuilder sb = new StringBuilder("analysis of " + coldType.fullyQualifiedName() + ":");
            c.stream().filter(l -> !w.contains(l)).forEach(l -> sb.append("\n  - ").append(l));
            w.stream().filter(l -> !c.contains(l)).forEach(l -> sb.append("\n  + ").append(l));
            differences.add(sb.toString());
        }
        LOGGER.info("REWIRE ORACLE analysis digest: cold {}, warm {}", Integer.toHexString(coldDigest),
                Integer.toHexString(warmDigest));
        if (DUMP_TO != null) {
            write(Path.of(DUMP_TO, "cold-" + oracleChecks + ".txt"), coldDump.toString());
            write(Path.of(DUMP_TO, "warm-" + oracleChecks + ".txt"), warmDump.toString());
        }
        return differences;
    }

    private static final String LINKS = "\"links\":[";

    /*
     The links of an element are a set; the dump lists them in the order the analysis added them, which follows the
     order in which it visited the code. A warm re-analysis visits less, in another order: the same links, listed
     differently (fernflower's Statement.type, 2026-11-09). Sort the top-level entries of each "links" array before
     comparing. Quote-aware, because names hold brackets (int[], C<init>).
     */
    static String linksInOrder(String line) {
        int from = 0;
        StringBuilder sb = new StringBuilder();
        int at;
        while ((at = line.indexOf(LINKS, from)) >= 0) {
            int start = at + LINKS.length();
            List<String> entries = new ArrayList<>();
            int depth = 0;
            boolean inString = false;
            int entryStart = start;
            int i = start;
            for (; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (inString) {
                    if (ch == '\\') i++;
                    else if (ch == '"') inString = false;
                } else if (ch == '"') {
                    inString = true;
                } else if (ch == '[' || ch == '{') {
                    depth++;
                } else if (ch == ']' || ch == '}') {
                    if (depth == 0) break; // the end of the links array
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    entries.add(line.substring(entryStart, i));
                    entryStart = i + 1;
                }
            }
            if (i >= line.length()) return line; // not what we expected: compare as is
            if (i > entryStart) entries.add(line.substring(entryStart, i));
            entries.sort(null);
            sb.append(line, from, start).append(String.join(",", entries));
            from = i;
        }
        return sb.append(line, from, line.length()).toString();
    }

    private void logSummary() {
        if (stats.isEmpty()) return;
        LOGGER.info("REWIRE SUMMARY {} re-parse(s) over {} primary types; median cone {}, median rewire-built types {};"
                    + " median ms: reload {}, re-parse {}, prep {}, call graph alone {}, analysis {}; oracle: {}"
                    + " check(s), {} difference(s); fell back to cold: {}", stats.size(), stats.getLast().primaryTypes(),
                median(stats.stream().mapToLong(Stats::cone)), median(stats.stream().mapToLong(Stats::rewiredTypes)),
                median(stats.stream().mapToLong(Stats::reloadMs)), median(stats.stream().mapToLong(Stats::reparseMs)),
                median(stats.stream().mapToLong(Stats::prepMs)), median(stats.stream().mapToLong(Stats::callGraphMs)),
                median(stats.stream().mapToLong(Stats::analysisMs)), oracleChecks, oracleDifferences.size(),
                coldFallbacks());
    }

    private int coldFallbacks() {
        return (int) stats.stream().filter(Stats::coldFallback).count();
    }

    private static long median(LongStream values) {
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
