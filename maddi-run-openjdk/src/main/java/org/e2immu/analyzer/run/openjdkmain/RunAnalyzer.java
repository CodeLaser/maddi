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

package org.e2immu.analyzer.run.openjdkmain;

import org.e2immu.analyzer.aapi.parser.AnalysisHints;
import org.e2immu.analyzer.aapi.parser.AnalysisHintsCompiler;
import org.e2immu.analyzer.aapi.parser.AnalysisHintsConfiguration;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.io.AnalysisFingerprint;
import org.e2immu.analyzer.run.rewire.RunRewireTests;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.analyzer.modification.prepwork.io.WriteAnalysisResults;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.analyzer.run.config.report.ErrorReport;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.integration.JavaInspectorFactory;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RunAnalyzer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunAnalyzer.class);

    private final Configuration configuration;
    private int exitValue;
    private Summary summary;
    private Throwable terminalError;
    private final List<Message> analysisMessages = new ArrayList<>();

    public RunAnalyzer(Configuration configuration) {
        this.configuration = configuration;
    }

    public int exitValue() {
        return exitValue;
    }

    @Override
    public void run() {
        try {
            AnalysisHintsConfiguration ac = configuration.analysisHintsConfiguration();
            // use cases 2 (analysis hints -> analysis results) and 3 (write updated hints): both go through the AnalysisHintsCompiler
            if (ac != null && (ac.analysisResultsTargetDir() != null || ac.updatedHintsDir() != null)) {
                runAnalysisHintsCompiler();
                return;
            }
            runAnalyzer();
        } catch (Summary.FailFastException ffe) {
            terminalError = ffe;
            exitValue = Main.EXIT_PARSER_ERROR;
        } catch (IOException ioe) {
            terminalError = ioe;
            exitValue = Main.EXIT_IO_EXCEPTION;
        } catch (RuntimeException re) {
            terminalError = re;
            exitValue = Main.EXIT_INTERNAL_EXCEPTION;
        } catch (AssertionError | StackOverflowError err) {
            // backstop: an assertion or deep recursion in analysis must not kill the process with an
            // uncaught throwable; report it and exit with a code, like any other internal failure
            terminalError = err;
            exitValue = Main.EXIT_INTERNAL_EXCEPTION;
        }
        // enumerate whatever was collected/thrown to the user (previously printSummaries() was an empty no-op)
    }

    // Lower the root level to INFO only when logback is the active SLF4J backend (the CLI). Done reflectively so
    // this module carries no compile/runtime dependency on logback: under another backend -- the Gradle worker's
    // provider, or Maven core's slf4j binding when embedded as the mvn plugin -- logback is absent, and the level
    // is left as configured.
    private static void setLogbackRootLevelToInfoIfPresent() {
        try {
            org.slf4j.Logger root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            Class<?> logbackLogger = Class.forName("ch.qos.logback.classic.Logger");
            if (logbackLogger.isInstance(root)) {
                Class<?> level = Class.forName("ch.qos.logback.classic.Level");
                logbackLogger.getMethod("setLevel", level).invoke(root, level.getField("INFO").get(null));
            }
        } catch (ReflectiveOperationException logbackNotOnClasspath) {
            // no logback backend -> nothing to tune
        }
    }

    private void runAnalyzer() throws IOException {
        setLogbackRootLevelToInfoIfPresent();

        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.setJdkInternals(configuration.generalConfiguration().jdkInternals());
        InputConfiguration inputConfiguration = configuration.inputConfiguration();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.base::java.util");
        AnalysisHintsConfiguration ac = configuration.analysisHintsConfiguration();

        List<String> analysisSteps = configuration.generalConfiguration().analysisSteps();
        boolean modification = analysisSteps.contains(Main.AS_MODIFICATION);
        if (!modification) {
            LOGGER.info("Skip loading analyzed package files, modification analysis disabled.");
        }

        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true)
                .setFailFast(false)
                .setParallel(configuration.generalConfiguration().parallel())
                .setLombok(inputConfiguration.containsLombok())
                .setIgnoreModule(true)
                .build();
        Summary summary;
        try {
            summary = javaInspector.parse(parseOptions);
        } catch (RuntimeException parseError) {
            // a front-end may throw a raw parser exception on a syntax error (e.g. the openjdk body parser) rather
            // than accumulating it in the Summary; treat any parse-phase failure uniformly as a parser error
            terminalError = parseError;
            exitValue = Main.EXIT_PARSER_ERROR;
            return;
        }
        this.summary = summary;
        // check errors BEFORE the assert below: parseResult() throws when haveErrors(), and with assertions on
        // (tests) that would mask a parse error as an internal exception. Report + exit with a parser code.
        if (summary.haveErrors()) {
            exitValue = Main.EXIT_PARSER_ERROR;
            return;
        }
        assert summary.parseResult().primaryTypes().stream()
                .flatMap(TypeInfo::recursiveSubTypeStream)
                .noneMatch(ti -> ti.simpleName().endsWith("$"))
                : "It looks like the analysis hints types are part of the primary types of the parse result";

        if (modification) {
            // use case 1: load pre-analyzed analysis-hints (analysis results) for library types, so their
            // annotations are available to the modification analysis. AFTER the parse: only then has the
            // compiled-types manager been populated — loading earlier resolved 0 of the hint types
            // ("module not on the classpath" for java.lang.Object; semantic audit 2026-07-18, task #29).
            List<String> preloadAnalysisResultsDirs = ac == null ? List.of() : ac.preloadAnalysisResultsDirs();
            if (!preloadAnalysisResultsDirs.isEmpty()) {
                SourceSet sourceSetOfRequest = javaInspector.mainSources();
                if (sourceSetOfRequest == null) {
                    sourceSetOfRequest = inputConfiguration.sourceSets().stream().findAny().orElse(null);
                    LOGGER.info("Cannot find a 'main' source set, default to {}", sourceSetOfRequest);
                }
                LOGGER.info("Loading analyzed analysis hints from {}", preloadAnalysisResultsDirs);
                new LoadAnalysisResults(javaInspector.runtime(), sourceSetOfRequest).go(preloadAnalysisResultsDirs);
            }
        }

        boolean printMemory = configuration.generalConfiguration().debugTargets().contains("memory");
        if (printMemory) {
            printMemUse();
        }
        if (analysisSteps.size() == 1 && Main.AS_NONE.equalsIgnoreCase(analysisSteps.getFirst())) {
            return;
        }
        ComputeCallGraph ccg;

        boolean rewireTests = analysisSteps.contains(Main.AS_REWIRE_TESTS);
        boolean prep = modification || rewireTests || analysisSteps.contains(Main.AS_PREP);
        if (prep) {
            ParseResult parseResult = summary.parseResult();
            Predicate<TypeInfo> externalsToAccept = _ -> false;
            LOGGER.info("Running prep analyzer on {} types", summary.types().size());
            PrepAnalyzer prepAnalyzer = new PrepAnalyzer(javaInspector.runtime(),
                    new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
            ccg = prepAnalyzer.doPrimaryTypesReturnComputeCallGraph(Set.copyOf(parseResult.primaryTypes()),
                    parseResult.sourceSetToModuleInfoMap().values(),
                    externalsToAccept, parseOptions.parallel());
            assert ccg.graph().vertices().stream().noneMatch(v -> v.t() instanceof TypeInfo typeInfo && typeInfo.simpleName().endsWith("$"))
                    : "It looks like the analysis hints types are part of the call graph.";

            // fault isolation: one failing type/method no longer aborts the whole run; surface what was skipped
            List<AnalyzerException> prepExceptions = prepAnalyzer.exceptions();
            if (!prepExceptions.isEmpty()) {
                LOGGER.error("Prep analysis produced {} error(s); the affected types/methods were skipped:",
                        prepExceptions.size());
                int i = 1;
                for (AnalyzerException ae : prepExceptions) {
                    Info info = ae.getInfo();
                    String at = info == null || info.source() == null ? "?" : info.source().compact2();
                    Throwable cause = ae.getCause() == null ? ae : ae.getCause();
                    LOGGER.error("  [{}] {} ({}): {}: {}", i++, info, at,
                            cause.getClass().getName(), cause.getMessage());
                }
                exitValue = Main.EXIT_ANALYSER_ERROR;
                // do NOT skip modification: the offending types were isolated by prep itself, and a handful
                // of isolated types must not deny analysis to a corpus (elasticsearch: 4 of 43k). The exit
                // code still reports the errors.
            }

            if (printMemory) {
                printMemUse();
            }
            if (rewireTests) {
                LOGGER.info("Start rewire tests");
                new RunRewireTests(inputConfiguration, javaInspector, summary.parseResult(), ccg.graph())
                        .go();
                if (printMemory) {
                    printMemUse();
                }
            }
        } else {
            ccg = null;
        }
        if (modification) {
            ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
            LOGGER.info("Computing analysis order");
            List<Info> order = cao.go(ccg.graph(), parseOptions.parallel());
            LOGGER.info("Call graph analysis order has size {}; start modification analysis", order.size());

            // do actual modification analysis
            IteratingAnalyzer.Configuration modConfig = new IteratingAnalyzerImpl.ConfigurationBuilder()
                    .setMaxIterations(30) // safety net only: the loop exits on convergence/certification/plateau
                    // (timefold PARALLEL=8 certified exactly at 20 — late worklist iterations cost ~3s, so headroom is free)
                    .setStopWhenCycleDetectedAndNoImprovements(true) // plateau early-exit, see IteratingAnalyzerImpl
                    // SHADOWDIFF (phase-1 reachability diff, PLAN §13) needs LINKED_VARIABLES_ARGUMENTS,
                    // which only trackObjectCreations produces; note track-on shifts some verdicts
                    // (P2.1 measured: nil cost, 0.12% churn on fernflower)
                    .setTrackObjectCreations(System.getenv("SHADOWDIFF") != null)
                    // MODREACH (PLAN §14 P2.3a, presence-only house convention): post-convergence
                    // reachability pass becomes the single writer of the three modification
                    // properties; implies trackObjectCreations
                    .setModificationViaReachability(System.getenv("MODREACH") != null)
                    .setFaultTolerant(true) // isolate a crash on one element; report it, don't abort the whole run
                    .setWarnNearMisses(configuration.generalConfiguration().warnNearMisses())
                    .build();
            IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(javaInspector, modConfig);
            // task #34: CHECKPOINT=<dir> writes pass-boundary deltas so a crashed multi-hour run can
            // resume; CHECKPOINT_RESTORE (presence, with CHECKPOINT set) preloads the directory first —
            // the verify-certify sweep of the resumed run is the soundness net. Value-carrying gates,
            // FPDUMP convention.
            String checkpointDir = System.getenv("CHECKPOINT");
            if (checkpointDir != null && !checkpointDir.isBlank()) {
                if (System.getenv("CHECKPOINT_RESTORE") != null) {
                    try {
                        int loaded = new org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults(
                                javaInspector.runtime(), javaInspector.mainSources())
                                .goDirTolerant(new org.e2immu.analyzer.modification.link.io.LinkCodec(javaInspector)
                                        .restoreCodec(), new File(checkpointDir));
                        LOGGER.info("CHECKPOINT_RESTORE: preloaded {} primary types from {}", loaded, checkpointDir);
                    } catch (IOException | RuntimeException e) {
                        LOGGER.error("CHECKPOINT_RESTORE failed, continuing cold: {}", e.toString());
                    }
                }
                var linkCodec = new org.e2immu.analyzer.modification.link.io.LinkCodec(javaInspector);
                analyzer.setValueFeed(new org.e2immu.analyzer.modification.analyzer.CheckpointWriter(
                        javaInspector.runtime(), linkCodec::codec, new File(checkpointDir)));
                LOGGER.info("CHECKPOINT: writing pass-boundary deltas to {}", checkpointDir);
            }
            // task #35 phase C/D: INCREMENTAL=<dir of a prior CHECKPOINT run> — restore that run's
            // values, detect changed primary types by SOURCE fingerprint, seed the early-cutoff
            // worklist with the changed types' elements, and union the persisted consumption edges
            // into the wake relation. Unchanged elements keep their carried (restored) values; the
            // run stops when the worklist is dry. Value-carrying gate, FPDUMP convention.
            java.util.Set<org.e2immu.language.cst.api.info.Info> initialDirty = null;
            String incrementalDir = System.getenv("INCREMENTAL");
            if (incrementalDir != null && !incrementalDir.isBlank()) {
                try {
                    var state = org.e2immu.analyzer.modification.prepwork.io.IncrementalState
                            .load(new File(incrementalDir));
                    if (state.sourceFingerprints().isEmpty()) {
                        LOGGER.warn("INCREMENTAL: no usable state in {}; running cold", incrementalDir);
                    } else {
                        int loaded = new org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults(
                                javaInspector.runtime(), javaInspector.mainSources())
                                .goDirTolerant(new org.e2immu.analyzer.modification.link.io.LinkCodec(javaInspector)
                                        .restoreCodec(), new File(incrementalDir));
                        java.util.Set<String> changed = state.changedTypes(summary.parseResult().primaryTypes());
                        initialDirty = new java.util.HashSet<>();
                        int unrestored = 0;
                        java.util.Map<String, org.e2immu.language.cst.api.info.TypeInfo> typesByFqn = new java.util.HashMap<>();
                        for (var info : order) {
                            var pt = info.typeInfo() == null ? null : info.typeInfo().primaryType();
                            if (pt == null) continue;
                            typesByFqn.putIfAbsent(pt.fullyQualifiedName(), pt);
                            if (changed.contains(pt.fullyQualifiedName())) {
                                initialDirty.add(info);
                            } else if (info.analysis().isEmpty()) {
                                // the restore's decode tail: an element with NO carried values stays
                                // null (no verification pass in incremental mode). Re-analyzing them
                                // (INCREMENTAL_FILL, presence gate) floods the worklist far past the
                                // tail itself (measured: slower than a cold run on fernflower) — the
                                // real fix is restore coverage (the shared codec fix list). Default:
                                // fast resume, holes counted here and reported.
                                unrestored++;
                                if (System.getenv("INCREMENTAL_FILL") != null) initialDirty.add(info);
                            }
                        }
                        java.util.Map<org.e2immu.language.cst.api.info.Info,
                                java.util.Set<org.e2immu.language.cst.api.info.Info>> wake = new java.util.HashMap<>();
                        state.consumers().forEach((consumedFqn, consumerFqns) -> {
                            var consumedType = typesByFqn.get(consumedFqn);
                            if (consumedType == null) return;
                            java.util.Set<org.e2immu.language.cst.api.info.Info> consumers = new java.util.HashSet<>();
                            for (String c : consumerFqns) {
                                var t = typesByFqn.get(c);
                                if (t != null) consumers.add(t);
                            }
                            if (!consumers.isEmpty()) wake.put(consumedType, consumers);
                        });
                        if (analyzer instanceof org.e2immu.analyzer.modification.analyzer.impl
                                .IteratingAnalyzerImpl iai) {
                            iai.setExternalWakeEdges(wake);
                        }
                        LOGGER.info("INCREMENTAL: restored {} type files, {} changed primary type(s), "
                                    + "{} dirty seed element(s) ({} unrestored), {} wake-edge sources",
                                loaded, changed.size(), initialDirty.size(), unrestored, wake.size());
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("INCREMENTAL setup failed; running cold: {}", e.toString());
                    initialDirty = null;
                }
            }
            try {
                if (initialDirty != null) {
                    // clear-before-recompute: a dirtied element's carried cross-type-derived values
                    // must not block the fresh, possibly-lowering re-analysis
                    java.util.function.Consumer<org.e2immu.language.cst.api.info.Info> clearHook = info -> {
                        info.analysis().removeIf(AnalysisFingerprint.CROSS_TYPE_DERIVED_ONLY);
                        if (info instanceof org.e2immu.language.cst.api.info.MethodInfo mi) {
                            mi.parameters().forEach(p ->
                                    p.analysis().removeIf(AnalysisFingerprint.CROSS_TYPE_DERIVED_ONLY));
                        }
                    };
                    analyzer.analyze(order, ccg.graph(), initialDirty, clearHook);
                } else {
                    analyzer.analyze(order, ccg.graph()); // graph enables worklist narrowing (default ON, NOWORKLIST=1 opts out)
                }
            } catch (RuntimeException | AssertionError analyzerError) {
                terminalError = analyzerError;
                exitValue = Main.EXIT_ANALYSER_ERROR;
                return;
            }
            // phase-1 shadow diff (PLAN §13): one-shot reachability over the converged artifacts,
            // no writes; names the frozen optimistic values the evidence contradicts (§9.4 cross-read)
            if (System.getenv("SHADOWDIFF") != null) {
                try {
                    var report = new org.e2immu.analyzer.modification.analyzer.shadow.ShadowModificationPass()
                            .go(order);
                    LOGGER.info("SHADOWDIFF {}", report.summary());
                    // cause chain appended: distinguishes direct refused-downgrades from the E2/E6
                    // union-over-implementations conservatism (§7.2) when classifying
                    report.divergences().stream()
                            .sorted(java.util.Comparator.comparing(Object::toString))
                            .forEach(d -> LOGGER.info("SHADOWDIFF DIV {} || {}", d, report.explain(d.info())));
                    // reverse = the pass missed something frozen-modified: a shadow-pass gap, must be
                    // triaged to zero before the pass can gate phase 2 (its own soundness contract)
                    report.reverseDivergences().forEach(d -> LOGGER.info("SHADOWDIFF REV {}", d));
                } catch (RuntimeException | AssertionError e) {
                    LOGGER.error("SHADOWDIFF failed: {}", e.toString());
                }
            }
            analysisMessages.addAll(analyzer.messages());
            // analysisFingerprint: store each source set's rollup for incremental early-cutoff (docs/analysis-rewiring.md)
            int fpSets = AnalysisFingerprint.storePerSourceSet(javaInspector.runtime(),
                    summary.parseResult().primaryTypes()).size();
            LOGGER.info("Stored analysis fingerprints for {} source set(s)", fpSets);
            // task #35 phase C: a checkpointed run leaves per-type OUTPUT fingerprints + the
            // recorded consumption edges (CHECKPOINT arms the recorder) so the next run can seed
            // the early-cutoff worklist with changed types + their DIRECT consumers
            if (checkpointDir != null && !checkpointDir.isBlank()) {
                try {
                    org.e2immu.analyzer.modification.prepwork.io.IncrementalState
                            .capture(javaInspector.runtime(), summary.parseResult().primaryTypes(),
                                    org.e2immu.language.cst.impl.analysis.ConsumptionEdgeRecorder.edgesSnapshot())
                            .save(new File(checkpointDir));
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("Cannot save incremental state: {}", e.toString());
                }
            }
            if (analysisMessages.stream().anyMatch(m -> m.level().isError())) {
                exitValue = Main.EXIT_ANALYSER_ERROR;
            }

            // write results
            String targetDir = configuration.generalConfiguration().analysisResultsDir();
            if (targetDir != null && !Main.AS_NONE.equalsIgnoreCase(targetDir)) {
                Trie<TypeInfo> trie = new Trie<>();
                LOGGER.info("Writing results for {} types to {}", summary.types().size(), targetDir);
                summary.types().forEach(ti -> trie.add(ti.packageName().split("\\."), ti));
                WriteAnalysisResults writeAnalysisResults = new WriteAnalysisResults(javaInspector.runtime());
                writeAnalysisResults.write(targetDir, trie);
            } else {
                LOGGER.warn("Not writing out results, " + Main.ANALYSIS_RESULTS_DIR + " is empty");
            }
        }
    }

    private static final int MB = 1024 * 1024;

    private void printMemUse() {
        System.gc();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        LOGGER.info("Heap Memory Usage: {} MB initial, {} MB used, {} MB committed, {} MB max",
                heapUsage.getInit() / MB, heapUsage.getUsed() / MB, heapUsage.getCommitted() / MB, heapUsage.getMax() / MB);
    }
    /**
     * Use case 2 (compile analysis hints sources into analyzed-analysis-hints results) and use case 3 (write updated hint
     * files): both are driven by {@link AnalysisHintsCompiler}. We derive one {@link AnalysisHints} per (non
     * -library) source set of the input configuration -- library name and hints path from the source set,
     * results directory from {@code analysisResultsTargetDir}, updated-hints directory from
     * {@code updatedHintsDir}, package filter from {@code hintsPackages}, preload dirs from
     * {@code preloadAnalysisResultsDirs}.
     */
    private void runAnalysisHintsCompiler() throws IOException {
        AnalysisHintsConfiguration ac = configuration.analysisHintsConfiguration();
        InputConfiguration inputConfiguration = configuration.inputConfiguration();

        String resultsDir = ac.analysisResultsTargetDir() != null ? ac.analysisResultsTargetDir()
                : configuration.generalConfiguration().analysisResultsDir();
        if (resultsDir == null || Main.AS_NONE.equalsIgnoreCase(resultsDir)) {
            throw new IllegalStateException("AnalysisHints compilation needs an "
                                            + Main.ANALYSIS_RESULTS_TARGET_DIR + " (or an "
                                            + Main.ANALYSIS_RESULTS_DIR + ")");
        }
        Path analysisResultsDir = Path.of(resultsDir);
        Path updatedHintsPath = ac.updatedHintsDir() == null ? null : Path.of(ac.updatedHintsDir());
        String packagePrefix = ac.hintsPackages().isEmpty() ? null : ac.hintsPackages().getFirst();

        AnalysisHintsCompiler compiler = new AnalysisHintsCompiler(configurationFactory());
        for (SourceSet sourceSet : inputConfiguration.sourceSets()) {
            if (sourceSet.externalLibrary() || sourceSet.sourceDirectories().isEmpty()) continue;
            AnalysisHints hints = new AnalysisHints.Builder()
                    .setLibraryName(sourceSet.name())
                    .setHintsPath(sourceSet.sourceDirectories().getFirst())
                    .setPackagePrefix(packagePrefix)
                    .setPreloadAnalysisResultsDirs(ac.preloadAnalysisResultsDirs())
                    .setAnalysisResultsDir(analysisResultsDir)
                    .setUpdatedHintsPath(updatedHintsPath)
                    .build();
            LOGGER.info("Compiling analysis hints for source set {} (hints {})", sourceSet.name(),
                    sourceSet.sourceDirectories().getFirst());
            List<Message> messages = compiler.go(hints);
            analysisMessages.addAll(messages);
            LOGGER.info("AnalysisHints compilation of {} produced {} message(s)", sourceSet.name(), messages.size());
        }
        if (analysisMessages.stream().anyMatch(m -> m.level().isError())) {
            exitValue = Main.EXIT_ANALYSER_ERROR;
        }
        LOGGER.info("End of e2immu, analysis-hints compiler mode.");
    }

    /** A {@link JavaInspectorFactory} over the input configuration: its class-path parts are the dependencies,
     * and each requested source set becomes the sole source of a fresh openjdk inspector. */
    private JavaInspectorFactory configurationFactory() {
        InputConfiguration inputConfiguration = configuration.inputConfiguration();
        List<SourceSet> classPathParts = inputConfiguration.classPathParts();
        String workingDirectory = inputConfiguration.workingDirectory() == null ? null
                : inputConfiguration.workingDirectory().toString();
        return new JavaInspectorFactory() {
            @Override
            public List<SourceSet> dependencies() {
                return classPathParts;
            }

            @Override
            public JavaInspector withSources(SourceSet sourceSet) throws IOException {
                JavaInspector javaInspector = new JavaInspectorImpl(true, false);
                InputConfiguration hintsInput = new InputConfigurationImpl.Builder()
                        .setWorkingDirectory(workingDirectory)
                        .addSourceSets(sourceSet)
                        .addClassPathParts(classPathParts)
                        .build();
                javaInspector.initialize(hintsInput);
                return javaInspector;
            }
        };
    }

    record PackageFilter(List<String> acceptedPackages) implements Predicate<Info> {

        @Override
        public boolean test(Info info) {
            if (acceptedPackages.isEmpty()) {
                return true;
            }
            String myPackageName = info.typeInfo().packageName();
            for (String s : acceptedPackages) {
                if (s.endsWith(".")) {
                    if (myPackageName.startsWith(s)) return true;
                    String withoutDot = s.substring(0, s.length() - 1);
                    if (myPackageName.equals(withoutDot)) return true;
                } else if (myPackageName.equals(s)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void printSummaries() {
        ErrorReport.report(summary, terminalError, analysisMessages);
    }
}
