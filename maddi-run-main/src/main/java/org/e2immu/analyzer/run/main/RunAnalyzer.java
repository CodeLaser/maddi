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

package org.e2immu.analyzer.run.main;

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfiguration;
import org.e2immu.analyzer.aapi.parser.AnnotatedApiParser;
import org.e2immu.analyzer.aapi.parser.Composer;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.io.LoadAnalyzedPackageFiles;
import org.e2immu.analyzer.modification.io.WriteAnalysis;
import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RunAnalyzer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunAnalyzer.class);

    private final Configuration configuration;
    private int exitValue;

    public RunAnalyzer(Configuration configuration) {
        this.configuration = configuration;
    }

    public int exitValue() {
        return exitValue;
    }

    @Override
    public void run() {
        try {
            AnnotatedAPIConfiguration ac = configuration.annotatedAPIConfiguration();
            if (ac.annotatedApiTargetDir() != null) {
                runComposer();
                return;
            }
            if (ac.analyzedAnnotatedApiTargetDir() != null) {
                runShallowAnalyzer();
                return;
            }
            runAnalyzer();
        } catch (Summary.FailFastException ffe) {
            Throwable cause = ffe.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            LOGGER.error("Caught exception", cause);
            exitValue = 1;
        } catch (IOException ioe) {
            LOGGER.error("Caught IO exception: {}", ioe.getMessage());
            exitValue = 1;
        } catch (RuntimeException re) {
            LOGGER.error("Runtime exception", re);
            exitValue = 1;
        }
    }

    private void runAnalyzer() throws IOException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);

        JavaInspector javaInspector = new JavaInspectorImpl(true, true);
        InputConfiguration inputConfiguration = configuration.inputConfiguration();
        javaInspector.initialize(inputConfiguration);
        AnnotatedAPIConfiguration ac = configuration.annotatedAPIConfiguration();

        List<String> analysisSteps = configuration.generalConfiguration().analysisSteps();
        boolean modification = analysisSteps.contains(Main.AS_MODIFICATION);
        if (modification) {
            SourceSet sourceSetOfRequest = javaInspector.mainSources();
            if (sourceSetOfRequest == null) {
                sourceSetOfRequest = inputConfiguration.sourceSets().stream().findAny().orElse(null);
                LOGGER.info("Cannot find a 'main' source set, default to {}", sourceSetOfRequest);
            }
            new LoadAnalyzedPackageFiles(sourceSetOfRequest).go(javaInspector, ac.analyzedAnnotatedApiDirs());
        } else {
            LOGGER.info("Skip loading analyzed package files, modification analysis disabled.");
        }

        JavaInspector.ParseOptions parseOptions = new JavaInspectorImpl.ParseOptionsBuilder()
                .setDetailedSources(true)
                .setFailFast(true)
                .setParallel(configuration.generalConfiguration().parallel())
                .setLombok(inputConfiguration.containsLombok())
                .build();
        Summary summary = javaInspector.parse(parseOptions);
        boolean printMemory = configuration.generalConfiguration().debugTargets().contains("memory");
        if (printMemory) {
            printMemUse();
        }
        if (summary.haveErrors()) {
            LOGGER.error("Have parsing errors, bailing out");
            return;
        }
        if (analysisSteps.size() == 1 && Main.AS_NONE.equalsIgnoreCase(analysisSteps.getFirst())) {
            return;
        }
        ComputeCallGraph ccg;

        boolean rewireTests = analysisSteps.contains(Main.AS_REWIRE_TESTS);
        boolean prep = modification || rewireTests || analysisSteps.contains(Main.AS_PREP);
        if (prep) {
            ParseResult parseResult = summary.parseResult();
            Predicate<TypeInfo> externalsToAccept = t -> false;
            LOGGER.info("Running prep analyzer on {} types", summary.types().size());
            PrepAnalyzer prepAnalyzer = new PrepAnalyzer(javaInspector.runtime());
            prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
            ccg = prepAnalyzer.doPrimaryTypesReturnComputeCallGraph(Set.copyOf(parseResult.primaryTypes()),
                    parseResult.sourceSetToModuleInfoMap().values(),
                    externalsToAccept, parseOptions.parallel());
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
            assert ccg != null;
            ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
            LOGGER.info("Computing analysis order");
            List<Info> order = cao.go(ccg.graph(), parseOptions.parallel());
            LOGGER.info("Call graph analysis order has size {}; start modification analysis", order.size());

            // do actual modification analysis
            IteratingAnalyzer.Configuration modConfig = new IteratingAnalyzerImpl.ConfigurationBuilder()
                    .setStoreErrors(false)
                    .build();
            IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(javaInspector.runtime(), modConfig);
            analyzer.analyze(order);

            // write results
            String targetDir = configuration.generalConfiguration().analysisResultsDir();
            if (targetDir != null && !Main.AS_NONE.equalsIgnoreCase(targetDir)) {
                Trie<TypeInfo> trie = new Trie<>();
                LOGGER.info("Writing results for {} types to {}", summary.types().size(), targetDir);
                summary.types().forEach(ti -> trie.add(ti.packageName().split("\\."), ti));
                WriteAnalysis writeAnalysis = new WriteAnalysis(javaInspector.runtime());
                writeAnalysis.write(targetDir, trie);
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

    private void runShallowAnalyzer() throws IOException {
        AnnotatedAPIConfiguration ac = configuration.annotatedAPIConfiguration();
        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();

        annotatedApiParser.initialize(configuration.inputConfiguration(), ac);
        LOGGER.info("AAPI parser finds {} types", annotatedApiParser.types().size());
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(annotatedApiParser.runtime(), annotatedApiParser,
                true);
        Trie<TypeInfo> trie = new Trie<>();
        ShallowAnalyzer.Result rs = shallowAnalyzer.go(annotatedApiParser.typesParsed());
        LOGGER.info("Shallow analyzer found {} types", rs.allTypes().size());
        annotatedApiParser.types().forEach(ti -> trie.add(ti.packageName().split("\\."), ti));
        WriteAnalysis writeAnalysis = new WriteAnalysis(annotatedApiParser.runtime());
        writeAnalysis.write(ac.analyzedAnnotatedApiTargetDir(), trie);

        LOGGER.info("End of e2immu main, AAPI->AAAPI shallow analyzer.");
    }

    private void runComposer() throws IOException {
        JavaInspector javaInspector = new JavaInspectorImpl();

        javaInspector.initialize(configuration.inputConfiguration());

        AnnotatedAPIConfiguration ac = configuration.annotatedAPIConfiguration();
        String destinationPackage = ac.annotatedApiTargetPackage() == null ? "" : ac.annotatedApiTargetPackage();
        Predicate<Info> filter;
        if (ac.annotatedApiPackages().isEmpty()) {
            filter = w -> true;
            LOGGER.info("No filter.");
        } else {
            filter = new PackageFilter(ac.annotatedApiPackages());
            LOGGER.info("Created package filter based on {}", ac.annotatedApiPackages());
        }
        Composer composer = new Composer(javaInspector, set -> destinationPackage, filter);
        List<TypeInfo> compiledPrimaryTypes = javaInspector.compiledTypesManager()
                .typesLoaded(true).stream().filter(TypeInfo::isPrimaryType).toList();
        LOGGER.info("Loaded {} compiled primary types", compiledPrimaryTypes.size());

        JavaInspector.ParseOptions parseOptions = new JavaInspectorImpl.ParseOptionsBuilder().setFailFast(true).build();
        Summary summary = javaInspector.parse(parseOptions);
        Collection<TypeInfo> sourcePrimaryTypes = summary.types();
        LOGGER.info("Parsed {} primary source types", sourcePrimaryTypes.size());

        List<TypeInfo> primaryTypes = Stream.concat(compiledPrimaryTypes.stream(), sourcePrimaryTypes.stream()).toList();
        Collection<TypeInfo> apiTypes = composer.compose(primaryTypes);
        composer.write(apiTypes, ac.annotatedApiTargetDir(), null);

        LOGGER.info("End of e2immu main, AAPI skeleton generation mode.");
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

    }
}
