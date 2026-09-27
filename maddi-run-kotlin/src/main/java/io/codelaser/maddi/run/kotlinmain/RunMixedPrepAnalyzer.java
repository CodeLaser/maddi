/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.common.AnalyzerException;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import java.io.File;
import io.codelaser.maddi.util.Trie;
import io.codelaser.maddi.modification.prepwork.io.WriteAnalysisResults;
import io.codelaser.maddi.modification.link.io.LinkCodec;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.modification.common.defaults.ShallowMethodAnalyzer;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.kotlin.realm.K2Realm;
import io.codelaser.maddi.graph.G;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prep-only runner for a mixed Java+Kotlin project. Parses the input configuration with
 * {@link MixedProjectInspector} — the openjdk and K2 front-ends share one core (so a cross-language reference
 * resolves to a single {@link TypeInfo}), each type keeps its own source set, and the configuration's library
 * class-path parts are honoured — then runs the prep analysis (call graph + analysis order) over the combined
 * primary types.
 * <p>
 * It deliberately stops after prep: no modification analysis is run and no results are written (the modification
 * analysis has open issues on real code, handled elsewhere). It inherits {@link MixedProjectInspector}'s current
 * scope (Java↔Java across rebuilt source sets, and a project mixing both cross-language directions in one module,
 * are follow-ups).
 * <p>
 * The running JVM must be started with the openjdk {@code --add-exports jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED}.
 */
public class RunMixedPrepAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunMixedPrepAnalyzer.class);

    /**
     * A small summary of a run: the number of Kotlin/Java/primary types, the analysis-order size, and how many
     * elements prep had to isolate (0 when it ran clean), and how many primary types were concluded immutable
     * (0 when no modification analysis ran — and a telltale that the annotated APIs were not loaded when one
     * did).
     */
    public record Summary(int kotlinTypes, int javaTypes, int primaryTypes, int analysisOrderSize,
                          int prepErrors, int immutableTypes, int placeholders) {
    }

    /**
     * What the CLI can vary, beyond the input configuration. A record rather than more {@code go} overloads:
     * the mixed runner now serves a command line with the SAME flags as the Java one (see
     * {@code kotlinmain.Main}), so this list grows with the options the mixed pipeline learns to honour —
     * and an option it does NOT honour is refused there by name, never quietly dropped.
     */
    public record Options(boolean modification, List<String> analysisResultsDirs, boolean parallel,
                          boolean warnNearMisses, String analysisResultsTargetDir) {
        public Options(boolean modification, List<String> analysisResultsDirs) {
            this(modification, analysisResultsDirs, false, false, null);
        }

        public Options(boolean modification, List<String> analysisResultsDirs, boolean parallel,
                       boolean warnNearMisses) {
            this(modification, analysisResultsDirs, parallel, warnNearMisses, null);
        }
    }

    public Summary go(InputConfiguration inputConfiguration) throws IOException {
        return go(inputConfiguration, false, List.of());
    }

    public Summary go(InputConfiguration inputConfiguration, boolean modification) throws IOException {
        return go(inputConfiguration, modification, List.of());
    }

    /**
     * @param modification            also run the iterating modification/immutability analysis over the prep
     *                                result. The analyzer takes a {@code JavaInspector}; a Kotlin-only project
     *                                has one anyway, since the mixed driver's openjdk inspector owns the
     *                                shared core.
     * @param analysisResultsDirs     pre-analyzed annotations for library types (the AAPI archive). Without
     *                                them every library type is an unknown, so nothing built on one can be
     *                                concluded immutable and the run reports only {@code @FinalFields} and
     *                                {@code @Mutable} — which is exactly what detekt did before this.
     */
    public Summary go(InputConfiguration inputConfiguration, boolean modification,
                      List<String> analysisResultsDirs) throws IOException {
        return go(inputConfiguration, new Options(modification, analysisResultsDirs));
    }

    public Summary go(InputConfiguration inputConfiguration, Options options) throws IOException {
        // idempotent: the CLI installs the realm before it gets here; a test or embedder that calls this
        // runner directly gets it installed on the way in, from -Dmaddi.k2.classpath / -Dmaddi.k2.home
        K2Realm.installIfAbsent();
        boolean modification = options.modification();
        List<String> analysisResultsDirs = options.analysisResultsDirs();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(inputConfiguration);
        Runtime runtime = parsed.getRuntime();

        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Mixed parse produced {} Kotlin and {} Java type(s), {} primary; running prep analyzer",
                parsed.getKotlinTypes().size(), parsed.getJavaTypes().size(), primaryTypes.size());

        // ⭐ BEFORE anything is concluded: how much of the Kotlin the front end could not read. A placeholder is
        // EMPTY to every consumer downstream, so a hole in a body is indistinguishable from a body with nothing
        // to say — unless a run says how many there are. Disclosed like the by-name lane's unresolvedSinkCalls.
        PlaceholderCensus placeholderCensus = PlaceholderCensus.of(parsed.getKotlinTypes());
        if (placeholderCensus.getTotal() > 0) {
            LOGGER.warn("{}", placeholderCensus.report());
        } else {
            LOGGER.info("{}", placeholderCensus.report());
        }
        writePlaceholderDump(placeholderCensus);

        // AFTER the parse, as in run-openjdk's RunAnalyzer: only by now is the compiled-types manager
        // populated, and loading earlier resolves none of the hint types. The source set of request is a
        // Kotlin one here — that is where the lookups originate, and it is what the distance-based resolution
        // in InfoByFqn measures from.
        if (!analysisResultsDirs.isEmpty()) {
            SourceSet sourceSetOfRequest = parsed.getKotlinBySourceSet().keySet().stream().findFirst()
                    .orElseGet(() -> inputConfiguration.sourceSets().stream().findAny().orElse(null));
            LOGGER.info("Loading analyzed analysis hints from {} (source set of request {})",
                    analysisResultsDirs, sourceSetOfRequest);
            new LoadAnalysisResults(runtime, sourceSetOfRequest).go(analysisResultsDirs);
        }
        // after the archive is loaded, before anything is concluded: which library members the source calls, and
        // whether a contract reached each (absent -Dmaddi.libraryCallDump: no walk, no cost)
        writeLibraryCallDump(runtime, Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream()).toList());

        // Fault-tolerant, as in run-openjdk's RunAnalyzer: one failing method must not deny analysis to a whole
        // corpus. The Kotlin front end has more rough edges than the Java one, so this matters more here, not
        // less — prep aborted detekt outright at 652 of 1,202 types before this.
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnComputeCallGraph(primaryTypes, List.of(),
                _ -> false, options.parallel()).graph();
        int prepErrors = report("Prep", prepAnalyzer.exceptions());
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        LOGGER.info("Prep analysis order has size {}", order.size());

        int immutableTypes = 0;
        if (modification) {
            LOGGER.info("Starting modification analysis over {} element(s)", order.size());
            IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder()
                    .setMaxIterations(30) // safety net; the loop exits on convergence/certification/plateau
                    .setStopWhenCycleDetectedAndNoImprovements(true)
                    .setFaultTolerant(true) // isolate a crash on one element rather than abort the run
                    .setWarnNearMisses(options.warnNearMisses())
                    .build();
            IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(parsed.getJavaInspector(), configuration);
            analyzer.analyze(order, callGraph); // the graph enables worklist narrowing
            LOGGER.info("Modification analysis finished");
            immutableTypes = (int) primaryTypes.stream().filter(RunMixedPrepAnalyzer::isImmutable).count();
            // every SOURCE type, not the primaries: a verdict that moves between runs may well be a nested
            // type's, and a dump that cannot show it cannot rule it out either (#34)
            writeVerdicts(Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream()).toList());
            writeMemberVerdicts(Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream()).toList());
        }
        writeAnalysisResults(options.analysisResultsTargetDir(), runtime, parsed, primaryTypes,
                inputConfiguration);
        return new Summary(parsed.getKotlinTypes().size(), parsed.getJavaTypes().size(),
                primaryTypes.size(), order.size(), prepErrors, immutableTypes, placeholderCensus.getTotal());
    }

    /**
     * ⭐ The encode half of the round trip (`TestKotlinAnalysisRoundTrip`): write what was concluded, so a
     * later run — incremental analysis, the IDE daemon, or a consumer of this project's results — can read it
     * back instead of recomputing it.
     *
     * <p>⛔ The codec is not interchangeable. {@code WriteAnalysisResults}' two-argument overload builds a
     * prep-work codec, whose property provider cannot know {@code methodLinks} — that Property is declared in
     * maddi-modification-link, which maddi-modification-prepwork does not and must not depend on. Written with
     * the wrong codec the file is unreadable, and the reader does not degrade: it asserts, and the WHOLE file
     * is lost. {@link LinkCodec} is the matching pair, and {@code restoreCodec()} its read side.
     *
     * <p>⚠ Without {@code --analysis-steps=modification} the results carry only what prep concluded. That is a
     * legitimate thing to write, but it is not a full analysis, and a reader cannot tell the two apart from
     * the file alone — so the log says which it was.
     */
    private void writeAnalysisResults(String targetDir, Runtime runtime, MixedProjectInspector.Result parsed,
                                      Set<TypeInfo> primaryTypes, InputConfiguration inputConfiguration)
            throws IOException {
        if (targetDir == null || targetDir.isBlank() || "none".equalsIgnoreCase(targetDir)) return;
        SourceSet sourceSetOfRequest = parsed.getKotlinBySourceSet().keySet().stream().findFirst()
                .orElseGet(() -> inputConfiguration.sourceSets().stream().findAny().orElse(null));
        Trie<TypeInfo> trie = new Trie<>();
        primaryTypes.forEach(ti -> trie.add(ti.packageName().split("\\."), ti));
        new WriteAnalysisResults(runtime).write(new File(targetDir), trie,
                new LinkCodec(parsed.getJavaInspector(), sourceSetOfRequest).codec());
        LOGGER.info("Wrote analysis results for {} primary type(s) to {}", primaryTypes.size(), targetDir);
    }

    /**
     * Every placeholder as {@code <kind> <owner> <line>:<pos>}, to the file named by
     * {@code -Dmaddi.placeholderDump} (absent: no file, no cost). ⭐ The count says how big the front end's
     * blind spot is; only this says WHERE, and the two questions have different answers — detekt's biggest
     * kind is {@code k2-unresolved-call:add}, which a four-line fixture of `mutableListOf().add(...)`
     * converts perfectly. A worklist needs the sites.
     */
    private static void writePlaceholderDump(PlaceholderCensus census) throws IOException {
        String target = System.getProperty("maddi.placeholderDump");
        if (target == null || target.isBlank()) return;
        Path path = Path.of(target);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.write(path, census.dumpLines());
        LOGGER.info("Wrote {} placeholder site(s) to {}", census.getSites().size(), path);
    }

    /**
     * Every library member the source calls under a {@code kotlin.} package, one
     * {@code <calls> <contracted|DEFAULT> <member> <parameter types>} line, most-called first, to the file named by
     * {@code -Dmaddi.libraryCallDump} (absent: no file, no cost). ⭐ An uncontracted library method is a MODIFYING
     * one — it modifies its receiver and every non-trivial argument (ShallowMethodAnalyzer) — so this is the
     * worklist for the Kotlin archive: the calls the analysis currently reads as writes. "contracted" means the
     * loaded archive marked the callee ANNOTATED_API (not NON_MODIFYING_METHOD: a static has no receiver, so a
     * contracted extension function never carries one); written after the load and before prep, so nothing
     * computed can pass for a contract. A constructor counts as its type's {@code <init>}.
     */
    private static void writeLibraryCallDump(Runtime runtime, List<TypeInfo> sourceTypes) throws IOException {
        String target = System.getProperty("maddi.libraryCallDump");
        if (target == null || target.isBlank()) return;
        Map<MethodInfo, Integer> calls = new HashMap<>();
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        for (TypeInfo type : sourceTypes) countLibraryCalls(type, calls, seen);
        // The 5th column is what the analysis WILL read: "this" for a modifying instance method, the index of each
        // parameter it takes as modified, "-" for none. A member the archive does not list gets its defaults here,
        // from the same ShallowMethodAnalyzer the link computer would run on it later with the same (loaded) jdk
        // data -- so the values are the ones the analysis uses, only computed earlier. ⛔ Guessing harm from a type
        // NAME overcounts: jdk/JavaLang makes Iterable and CharSequence @Immutable(hc=true), unmodified by default.
        ShallowMethodAnalyzer shallow = new ShallowMethodAnalyzer(runtime, Element::annotations);
        calls.keySet().forEach(shallow::analyze);
        List<String> lines = calls.entrySet().stream()
                .sorted(Map.Entry.<MethodInfo, Integer>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().fullyQualifiedName()))
                .map(e -> e.getValue() + "\t"
                          + (e.getKey().analysis().haveAnalyzedValueFor(PropertyImpl.ANNOTATED_API)
                        ? "contracted" : "DEFAULT") + "\t" + e.getKey().fullyQualifiedName()
                          // the UNERASED parameter types: an erased Object is a bare T (unmodified by default) or a
                          // real Object (modified), and only these tell them apart
                          + "\t" + e.getKey().parameters().stream()
                                  .map(p -> p.parameterizedType().toString().replaceFirst("^Type ", ""))
                                  .collect(Collectors.joining(", "))
                          + "\t" + modifies(e.getKey()))
                .toList();
        Path path = Path.of(target);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.write(path, lines);
        LOGGER.info("Wrote {} library member(s), {} call(s), to {}", lines.size(),
                calls.values().stream().mapToInt(Integer::intValue).sum(), path);
    }

    private static String modifies(MethodInfo m) {
        List<String> out = new ArrayList<>();
        if (!m.isStatic() && !m.isConstructor()
            && !m.analysis().getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue()) {
            out.add("this");
        }
        m.parameters().stream()
                .filter(p -> !p.analysis().getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue())
                .forEach(p -> out.add(String.valueOf(p.index())));
        return out.isEmpty() ? "-" : String.join(",", out);
    }

    private static void countLibraryCalls(TypeInfo type, Map<MethodInfo, Integer> calls, Map<Object, Boolean> seen) {
        if (seen.put(type, true) != null) return;
        type.subTypes().forEach(sub -> countLibraryCalls(sub, calls, seen));
        Stream.concat(type.constructors().stream(), type.methodStream()).forEach(m -> {
            if (seen.put(m, true) != null || m.methodBody() == null) return;
            m.methodBody().visit(e -> {
                MethodInfo callee = e instanceof MethodCall mc ? mc.methodInfo()
                        : e instanceof ConstructorCall cc ? cc.constructor() : null;
                // the PRIMARY type's package: a nested or local type has none of its own
                String pkg = callee == null ? null : callee.typeInfo().primaryType().packageName();
                if (pkg != null && pkg.startsWith("kotlin") && callee.typeInfo().compilationUnit().externalLibrary()) {
                    calls.merge(callee, 1, Integer::sum);
                }
                return true;
            });
        });
    }

    /**
     * The immutability verdict of every primary type, one {@code <verdict> <fqn>} line, sorted by name, to the
     * file named by {@code -Dmaddi.verdictDump} (absent: no file, no cost). A count is not enough to debug a run
     * that disagrees with the previous one over the same tree (#34): two dumps diff to the types that moved,
     * which is where a cause can be looked for. Never assert on this — it is an instrument, not a result.
     */
    private static void writeVerdicts(List<TypeInfo> types) throws IOException {
        String target = System.getProperty("maddi.verdictDump");
        if (target == null || target.isBlank()) return;
        List<String> lines = types.stream()
                .map(t -> verdict(t) + " " + t.fullyQualifiedName())
                .sorted(Comparator.comparing(s -> s.substring(s.indexOf(' ') + 1)))
                .toList();
        Path path = Path.of(target);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.write(path, lines);
        LOGGER.info("Wrote {} type verdict(s) to {}", lines.size(), path);
    }

    /**
     * The member-level verdicts, to the file named by {@code -Dmaddi.memberVerdictDump} (absent: no file, no cost):
     * {@code F <unmodified> <field>}, {@code M <non-modifying> <method>} and {@code P <unmodified> <method>#<i>}, one per
     * line, sorted. ⭐ The type-level {@link #writeVerdicts} dump did not move when library contracts turned 20 field
     * reads from modified to unmodified in a fixture: a detekt type blocked by something else keeps its level, so
     * the improvement is visible only here. An instrument, never asserted.
     */
    private static void writeMemberVerdicts(List<TypeInfo> types) throws IOException {
        String target = System.getProperty("maddi.memberVerdictDump");
        if (target == null || target.isBlank()) return;
        List<String> lines = new java.util.ArrayList<>();
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        for (TypeInfo type : types) memberVerdicts(type, lines, seen);
        java.util.Collections.sort(lines);
        Path path = Path.of(target);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.write(path, lines);
        LOGGER.info("Wrote {} member verdict(s) to {}", lines.size(), path);
    }

    private static void memberVerdicts(TypeInfo type, List<String> lines, Map<Object, Boolean> seen) {
        if (seen.put(type, true) != null) return;
        type.subTypes().forEach(sub -> memberVerdicts(sub, lines, seen));
        type.fields().forEach(f -> lines.add("F " + bool(f.analysis().getOrNull(PropertyImpl.UNMODIFIED_FIELD,
                ValueImpl.BoolImpl.class)) + " " + f.fullyQualifiedName()));
        type.methodStream().forEach(m -> {
            lines.add("M " + bool(m.analysis().getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class))
                      + " " + m.fullyQualifiedName());
            m.parameters().forEach(p -> lines.add("P " + bool(p.analysis().getOrNull(PropertyImpl.UNMODIFIED_PARAMETER,
                    ValueImpl.BoolImpl.class)) + " " + m.fullyQualifiedName() + "#" + p.index()));
        });
    }

    private static String bool(Value.Bool value) {
        return value == null ? "NONE" : String.valueOf(value.isTrue());
    }

    /** The name of a type's {@code IMMUTABLE_TYPE} value, or {@code NONE} when the analysis concluded nothing. */
    private static String verdict(TypeInfo typeInfo) {
        Value.Immutable immutable = typeInfo.analysis()
                .getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        return immutable == null ? "NONE" : immutable.toString();
    }

    /**
     * Whether a type reached either immutable level. Reported because it is the single number that says the
     * annotated APIs were in play: with no library annotations the analysis cannot conclude immutability for
     * anything built on a library type, so this is exactly zero while everything else still looks healthy.
     */
    private static boolean isImmutable(TypeInfo typeInfo) {
        Value.Immutable immutable = typeInfo.analysis()
                .getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        return immutable != null && immutable.isAtLeastImmutableHC();
    }

    /** Log what was isolated, so a run that "succeeded" cannot hide how much it skipped. */
    private static int report(String phase, List<AnalyzerException> exceptions) {
        if (exceptions.isEmpty()) return 0;
        LOGGER.error("{} produced {} error(s); the affected elements were skipped:", phase, exceptions.size());
        int i = 1;
        for (AnalyzerException ae : exceptions) {
            Info info = ae.getInfo();
            String at = info == null || info.source() == null ? "?" : info.source().compact2();
            Throwable cause = ae.getCause() == null ? ae : ae.getCause();
            LOGGER.error("  [{}] {} ({}): {}: {}", i++, info, at, cause.getClass().getName(), cause.getMessage());
        }
        return exceptions.size();
    }
}
