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

package io.codelaser.maddi.run.main;

import ch.qos.logback.classic.Level;
import io.codelaser.maddi.analysis.api.AnalysisEngine;
import io.codelaser.maddi.analysis.api.AnalysisEngines;
import io.codelaser.maddi.analysis.api.AnalysisHintsShadows;
import io.codelaser.maddi.analysis.api.HintsSpec;
import io.codelaser.maddi.analysis.api.ModificationOptions;
import io.codelaser.maddi.analysis.api.ModificationOutcome;
import io.codelaser.maddi.analysis.api.ModificationRequest;
import io.codelaser.maddi.analysis.api.PrepRequest;
import io.codelaser.maddi.run.config.AnalysisHintsConfiguration;
import io.codelaser.maddi.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.callgraph.ComputeCallGraph;
import io.codelaser.maddi.run.rewire.RunRewireTests;
import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.run.config.report.ErrorReport;
import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.DetectKotlinSources;
import io.codelaser.maddi.inspection.api.integration.JavaInspectorFactory;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * The modification analysis, from maddi-mod at run time (split stage 3). Asked for only when a step needs it:
     * a parse-only run ({@code --analysis-steps=none}) works without maddi-run-analysis on the class path.
     */
    private AnalysisEngine engine() {
        return AnalysisEngines.require("--analysis-steps=" + String.join(",",
                configuration.generalConfiguration().analysisSteps()));
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
        }
        // enumerate whatever was collected/thrown to the user (previously printSummaries() was an empty no-op)
    }

    private void runAnalyzer() throws IOException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);

        JavaInspector javaInspector = new JavaInspectorImpl(true, true);
        InputConfiguration inputConfiguration = configuration.inputConfiguration();
        // BEFORE the parse: the front end walks for ".java" alone, so a .kt file here is read as nothing and
        // the run would report success over a tree it only partly read
        if (DetectKotlinSources.refuse(inputConfiguration,
                configuration.generalConfiguration().skipKotlinSources(),
                DetectKotlinSources.SKIP_OPTION, LOGGER)) {
            exitValue = Main.EXIT_KOTLIN_SOURCES;
            return;
        }
        javaInspector.initialize(inputConfiguration);
        AnalysisHintsConfiguration ac = configuration.analysisHintsConfiguration();

        List<String> analysisSteps = configuration.generalConfiguration().analysisSteps();
        boolean modification = analysisSteps.contains(Main.AS_MODIFICATION);
        if (modification) {
            SourceSet sourceSetOfRequest = javaInspector.mainSources();
            if (sourceSetOfRequest == null) {
                sourceSetOfRequest = inputConfiguration.sourceSets().stream().findAny().orElse(null);
                LOGGER.info("Cannot find a 'main' source set, default to {}", sourceSetOfRequest);
            }
            // use case 1: load pre-analyzed analysis-hints (analysis results) results for library types
            List<String> preloadAnalysisResultsDirs = ac == null ? List.of() : ac.preloadAnalysisResultsDirs();
            if (!preloadAnalysisResultsDirs.isEmpty()) {
                LOGGER.info("Loading analyzed analysis hints from {}", preloadAnalysisResultsDirs);
                engine().resultsLoader(javaInspector.runtime(), sourceSetOfRequest).load(preloadAnalysisResultsDirs);
            }
        } else {
            LOGGER.info("Skip loading analyzed package files, modification analysis disabled.");
        }

        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true)
                .setFailFast(true)
                .setParallel(configuration.generalConfiguration().parallel())
                .setLombok(inputConfiguration.containsLombok())
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
                .noneMatch(AnalysisHintsShadows::isAnalysisHintsShadow)
                : "It looks like the analysis hints types are part of the primary types of the parse result";

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
            // the prep analyzer's default options (not fault-tolerant), as always in this driver
            ccg = engine().prep(new PrepRequest(javaInspector.runtime(), Set.copyOf(parseResult.primaryTypes()),
                    parseResult.sourceSetToModuleInfoMap().values(), externalsToAccept, parseOptions.parallel(),
                    null)).callGraph();
            assert ccg.graph().vertices().stream()
                    .noneMatch(v -> v.t() instanceof TypeInfo typeInfo
                                    && AnalysisHintsShadows.isAnalysisHintsShadow(typeInfo))
                    : "It looks like the analysis hints types are part of the call graph.";

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

            // do actual modification analysis: in maddi-mod since split stage 3
            ModificationOptions modificationOptions = new ModificationOptions(10, null, false, null,
                    true, // isolate a crash on one element; report it, don't abort the whole run
                    configuration.generalConfiguration().warnNearMisses(),
                    false, // no environment gates
                    true); // store the analysis fingerprints
            ModificationOutcome modificationOutcome;
            try {
                // no call graph: this driver has always analyzed the order without worklist narrowing
                modificationOutcome = engine().modification(new ModificationRequest(javaInspector, order, null,
                        summary.parseResult().primaryTypes(), modificationOptions, null));
            } catch (RuntimeException | AssertionError | StackOverflowError analyzerError) {
                terminalError = analyzerError;
                exitValue = Main.EXIT_ANALYZER_ERROR;
                return;
            }
            analysisMessages.addAll(modificationOutcome.messages());
            if (analysisMessages.stream().anyMatch(m -> m.level().isError())) {
                exitValue = Main.EXIT_ANALYZER_ERROR;
            }

            // write results
            String targetDir = configuration.generalConfiguration().analysisResultsDir();
            if (targetDir != null && !Main.AS_NONE.equalsIgnoreCase(targetDir)) {
                Trie<TypeInfo> trie = new Trie<>();
                LOGGER.info("Writing results for {} types to {}", summary.types().size(), targetDir);
                summary.types().forEach(ti -> trie.add(ti.packageName().split("\\."), ti));
                engine().writeResults(javaInspector.runtime(), targetDir, trie);
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
     * files): both are driven by {@code AnalysisHintsCompiler} (maddi-mod, through {@link AnalysisEngine#hintsCompiler}). One {@link HintsSpec} per (non-library)
     * source set of the input configuration; see the openjdk runner for the field-by-field mapping.
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

        AnalysisEngine.HintsCompiler compiler = engine().hintsCompiler(configurationFactory());
        for (SourceSet sourceSet : inputConfiguration.sourceSets()) {
            if (sourceSet.externalLibrary() || sourceSet.sourceDirectories().isEmpty()) continue;
            HintsSpec hints = new HintsSpec(sourceSet.name(), sourceSet.sourceDirectories().getFirst(), packagePrefix,
                    ac.preloadAnalysisResultsDirs(), analysisResultsDir, updatedHintsPath);
            LOGGER.info("Compiling analysis hints for source set {} (hints {})", sourceSet.name(),
                    sourceSet.sourceDirectories().getFirst());
            List<Message> messages = compiler.compile(hints);
            LOGGER.info("AnalysisHints compilation of {} produced {} message(s)", sourceSet.name(), messages.size());
            analysisMessages.addAll(messages);
        }
        if (analysisMessages.stream().anyMatch(m -> m.level().isError())) {
            exitValue = Main.EXIT_ANALYZER_ERROR;
        }
        LOGGER.info("End of e2immu, analysis-hints compiler mode.");
    }

    /** A {@link JavaInspectorFactory} over the input configuration: its class-path parts are the dependencies,
     * and each requested source set becomes the sole source of a fresh (in-house) inspector. */
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
