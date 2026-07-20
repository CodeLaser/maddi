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

package org.e2immu.analyzer.modification.analyzer.clonebench;

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.ModAnalyzerForTesting;
import org.e2immu.analyzer.modification.analyzer.impl.SingleIterationAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults.ANALYZED_RESULTS;
import static org.e2immu.language.inspection.resource.SourceSetImpl.testProtocolSourceSet;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Tag;

/*
IMPORTANT: use "analyzed" branch of "testarchive".
A small number of files have been modified wrt the main branch, for this test to run.

All directories are parsed ONCE, up front, in a single JavaInspector/runtime (one source set per directory so that
identically-named default-package types in different directories do not collide). This loads the JDK once and runs
javac serially -- avoiding both the thousands of independent JDK loads of a per-file parse and the intermittent
"tree.starImportScope is null" that concurrent javac produces. Only the (javac-free) analysis is parallelized.
*/
@Tag("slow")
public class TestCloneBench extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCloneBench.class);
    private final JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder().build();

    public TestCloneBench() {
        super("jmod:java.desktop",
                "jmod:java.compiler",
                "jmod:java.datatransfer",
                "jmod:java.sql",
                "jmod:java.logging",
                "jmod:java.instrument",
                "jmod:java.rmi",
                "jmod:java.management");
    }

    private static final String[] DIRS = {"bubblesort_for_withunit", "collections_layered",
            "dowhile_pure_compiles", "dowhile_pure_selected_withunit",
            "foreach_pure_compiles", "foreach_selection1_withunit",
            "fors_pure_compiles", "fors_pure_selected_withunit",
            "switch_fors_compiles", "switch_fors_selected_withunit",
            "switch_pure_compiles", "switch_pure_selected_withunit",
            "try_pure_compiles", "try_wr_compiles",
            "while_pure_compiles", "while_pure_selected_withunit"
    };

    // one InputConfiguration with a source set per directory; JDK/library analysis is loaded once. Parsing itself is
    // deferred to test(). Overrides CommonTest.beforeEach because this test needs real source directories (parsed in
    // one go via parse(ParseOptions)) rather than the empty per-file placeholder source sets.
    @Override
    @BeforeEach
    public void beforeEach() throws IOException {
        List<SourceSet> dirSets = new ArrayList<>();
        for (String dir : DIRS) {
            Path srcDir = Path.of("../../testarchive/" + dir + "/src/main/java");
            dirSets.add(new SourceSetImpl.Builder()
                    .setName(dir)
                    .setSourceDirectories(List.of(srcDir))
                    .setUri(srcDir.toUri())
                    .build());
        }
        List<String> jdkModules = Arrays.stream(jmods)
                .map(s -> s.startsWith("jmod:") ? s.substring("jmod:".length()) : s).toList();
        javaInspector = org.e2immu.analyzer.modification.common.CommonTest.javaInspectorWithExtras(
                dirSets.getFirst(), dirSets.subList(1, dirSets.size()), jdkModules);
        dirSets.forEach(SourceSet::computePriorityDependencies);
        runtime = javaInspector.runtime();
        javaInspector.setParameterNames(true);
        javaInspector.onlyPreload(); // trigger the configured package preloads (java.time etc.) before loading results
        new LoadAnalysisResults(javaInspector.runtime(), testProtocolSourceSet()).go(ANALYZED_RESULTS);
        // an instance prep-analyzer/analyzer for compatibility; the parallel workers build their own (below)
        prepAnalyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer = new SingleIterationAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder().build());
    }

    @Test
    public void test() throws Exception {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("graph-algorithm")).setLevel(Level.WARN);

        // PARSE everything once (serial javac, JDK loaded once).
        Summary summary = javaInspector.parse(parseOptions);

        // the types to analyze: source primary types from the clone-bench directories, excluding the '_t.java'
        // helper files (as the per-file version did).
        List<TypeInfo> types = summary.types().stream()
                .filter(TypeInfo::isPrimaryType)
                .filter(t -> {
                    URI uri = t.compilationUnit().uri();
                    return uri != null && uri.getPath() != null
                           && uri.getPath().endsWith(".java") && !uri.getPath().endsWith("_t.java");
                })
                .toList();
        LOGGER.warn("Parsed {} types to analyze", types.size());

        // ANALYSIS is javac-free (maddi CST + bytecode loading), so it runs in parallel. Each worker has its own
        // prep-analyzer/analyzer but shares the one runtime; the clone-bench snippets are independent, and the shared
        // JDK/library analysis is pre-loaded (LoadAnalysisResults), so there are no cross-type writes to race on.
        int parallelism = Integer.getInteger("clonebench.parallelism",
                Math.max(1, Math.min(java.lang.Runtime.getRuntime().availableProcessors(), 4)));
        LOGGER.warn("Analyzing with parallelism {}", parallelism);

        AtomicInteger counter = new AtomicInteger();
        Map<MethodInfo, Integer> typeHistogram = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<TypeInfo> queue = new ConcurrentLinkedQueue<>(types);

        List<Callable<Void>> workers = new ArrayList<>();
        for (int w = 0; w < parallelism; w++) {
            workers.add(() -> {
                PrepAnalyzer prep = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
                ModAnalyzerForTesting an = new SingleIterationAnalyzerImpl(javaInspector,
                        new IteratingAnalyzerImpl.ConfigurationBuilder().build());
                TypeInfo typeInfo;
                while ((typeInfo = queue.poll()) != null) {
                    analyze(prep, an, typeInfo, counter.incrementAndGet(), typeHistogram);
                }
                return null;
            });
        }
        List<Future<Void>> futures;
        try (ExecutorService exec = Executors.newFixedThreadPool(parallelism)) {
            futures = exec.invokeAll(workers);
        }
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AssertionError ae) throw ae;
                if (cause instanceof Exception ex) throw ex;
                throw new RuntimeException(cause);
            }
        }
        LOGGER.warn("Analyzed {} types", counter.get());
    }

    private void analyze(PrepAnalyzer prep, ModAnalyzerForTesting an, TypeInfo typeInfo, int count,
                         Map<MethodInfo, Integer> typeHistogram) throws IOException {
        LOGGER.info("Analyzing #{}, {}", count, typeInfo);
        List<Info> analysisOrder = prep.doPrimaryType(typeInfo);
        an.go(analysisOrder);
        String printed = javaInspector.print2(typeInfo.compilationUnit());

        // write to <dir>/src/main/analyzed/<File>.java, mirroring the original per-file output location
        Path srcFile = Path.of(typeInfo.compilationUnit().uri());
        Path analyzedDir = srcFile.getParent().getParent().resolve("analyzed");
        Files.createDirectories(analyzedDir);
        Files.writeString(analyzedDir.resolve(srcFile.getFileName().toString()), printed, StandardCharsets.UTF_8);

        analysisOrder.stream().filter(info -> info instanceof MethodInfo)
                .forEach(info -> {
                    MethodInfo mi = (MethodInfo) info;
                    if (!mi.isAbstract() && !mi.isSyntheticConstructor()) {
                        assertNotNull(mi.methodBody(), "For method " + mi);
                        mi.methodBody().visit(e -> {
                            if (e instanceof MethodCall mc && mc.methodInfo().typeInfo().primaryType() != typeInfo) {
                                typeHistogram.merge(mc.methodInfo(), 1, Integer::sum);
                            }
                            return true;
                        });
                    }
                });
    }
}
