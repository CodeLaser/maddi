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
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
IMPORTANT: use "analyzed" branch of "testarchive".
A small number of files have been modified wrt the main branch, for this test to run.
 */
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

    // each directory is parsed in its own (openjdk) source set, registered at setup
    @Override
    protected List<String> openJdkExtraSourceSetNames() {
        return List.of(DIRS);
    }

    // one file to analyze, tagged with the source set (directory) it belongs to and its output location
    private record FileTask(String setName, File javaFile, File outFile) {
    }

    // Process a single file using ONE worker's isolated bundle. Each bundle has its own JavaInspector/runtime, so a
    // bundle is confined to a single thread (the runtime's type cache is a plain HashMap). Files within a source set
    // are treated as independent (clone-bench snippets), so splitting them across bundles is safe.
    private void process(AnalyzerBundle bundle, FileTask task, int count,
                         Map<MethodInfo, Integer> typeHistogram) throws IOException {
        String input = Files.readString(task.javaFile().toPath());
        LOGGER.info("Start parsing #{}, {}, file of size {}", count, task.javaFile(), input.length());

        TypeInfo typeInfo = bundle.javaInspector().parseSingleFileInSourceSet(task.javaFile().toURI(),
                bundle.sourceSetsByName().get(task.setName()), parseOptions).parseResult().firstType();

        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        bundle.analyzer().go(analysisOrder);
        String printed = bundle.javaInspector().print2(typeInfo.compilationUnit());
        Files.writeString(task.outFile().toPath(), printed, StandardCharsets.UTF_8);

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

    private static final String[] DIRS = {"bubblesort_for_withunit", "collections_layered",
            "dowhile_pure_compiles", "dowhile_pure_selected_withunit",
            "foreach_pure_compiles", "foreach_selection1_withunit",
            "fors_pure_compiles", "fors_pure_selected_withunit",
            "switch_fors_compiles", "switch_fors_selected_withunit",
            "switch_pure_compiles", "switch_pure_selected_withunit",
            "try_pure_compiles", "try_wr_compiles",
            "while_pure_compiles", "while_pure_selected_withunit"
    };

    @Test
    public void test() throws Exception {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("graph-algorithm")).setLevel(Level.WARN);

        AtomicInteger counter = new AtomicInteger();
        Map<MethodInfo, Integer> typeHistogram = new ConcurrentHashMap<>();

        // enumerate every file across all directories on the main thread (I/O + directory creation is not thread-safe)
        List<FileTask> allTasks = new ArrayList<>();
        for (String dir : DIRS) {
            String directory = "../../testarchive/" + dir + "/src/main/java/";
            File src = new File(directory);
            assertTrue(src.isDirectory());
            File analyzedDir = new File(src.getParent(), "analyzed");
            if (analyzedDir.mkdirs()) {
                LOGGER.info("Created {}", analyzedDir);
            }
            File[] javaFiles = src.listFiles(f -> f.getName().endsWith(".java") && !f.getName().endsWith("_t.java"));
            assertNotNull(javaFiles);
            LOGGER.info("Found {} java files in {}", javaFiles.length, directory);
            for (File javaFile : javaFiles) {
                allTasks.add(new FileTask(dir, javaFile, new File(analyzedDir, javaFile.getName())));
            }
        }

        // File-level parallelism: N workers pull from a shared queue, each with its OWN isolated bundle (own
        // JavaInspector/runtime). Work-stealing balances the extreme per-directory skew automatically. Setup of an
        // extra bundle is cheap (~8s), so parallelism pays off; tune with -Dclonebench.parallelism=N.
        int parallelism = Integer.getInteger("clonebench.parallelism",
                Math.max(1, Math.min(java.lang.Runtime.getRuntime().availableProcessors(), 4)));
        LOGGER.warn("Analyzing {} files with parallelism {}", allTasks.size(), parallelism);
        ConcurrentLinkedQueue<FileTask> queue = new ConcurrentLinkedQueue<>(allTasks);

        // worker 0 reuses the bundle already built by beforeEach (its fields); the rest build their own
        Map<String, SourceSet> mainSourceSets = new HashMap<>();
        for (String dir : DIRS) mainSourceSets.put(dir, openJdkSourceSetsByName.get(dir));
        AnalyzerBundle mainBundle = new AnalyzerBundle(javaInspector, mainSourceSets, prepAnalyzer, analyzer);

        List<Callable<Void>> workers = new ArrayList<>();
        for (int w = 0; w < parallelism; w++) {
            int idx = w;
            workers.add(() -> {
                AnalyzerBundle bundle = idx == 0 ? mainBundle : buildAnalyzerBundle();
                FileTask task;
                while ((task = queue.poll()) != null) {
                    process(bundle, task, counter.incrementAndGet(), typeHistogram);
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
        LOGGER.warn("Analyzed {} files", counter.get());
/* potentially copy this test, and add it to aapi.parser tests
        LOGGER.warn("JDK calls:");
        Composer composer = new Composer(javaInspector,
                set -> "org.e2immu.analyzer.shallow.aapi.java", w -> w.access().isPublic());
        List<TypeInfo> toCompose =
                typeHistogram.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                        .map(e -> {
                            MethodInfo method = e.getKey();
                            Stream<MethodInfo> stream = Stream.concat(Stream.of(method), method.overrides().stream());
                            boolean alreadyDone = stream.anyMatch(mi -> mi.analysis()
                                    .getOrDefault(PropertyImpl.ANNOTATED_API, ValueImpl.BoolImpl.FALSE).isTrue());
                            LOGGER.warn("{} {}", e.getValue(), method + "  " + (alreadyDone ? "" : "ADD TO A-API"));
                            if (!alreadyDone) {
                                return method.primaryType();
                            }
                            return null;
                        }).filter(Objects::nonNull).distinct().toList();
        Collection<TypeInfo> aapiTypes = composer.compose(toCompose);
        composer.write(aapiTypes, "build/aapis", null);

        Map<String, Integer> problemHistogram = analyzer.getanalyzerExceptions().stream()
                .collect(Collectors.toUnmodifiableMap(t -> t == null || t.getMessage() == null ? "?" : t.getMessage(), t -> 1, Integer::sum));
        problemHistogram.entrySet().stream().sorted((e1, e2) -> e2.getValue() - e1.getValue()).forEach(e -> {
            LOGGER.warn("Error freq {}: {}", e.getValue(), e.getKey());
        });
        int numErrors = analyzer.getanalyzerExceptions().size();
        assertEquals(0, numErrors, "Found " + numErrors + " errors parsing " + counter.get()
                                   + " files. Histogram: " + analyzer.getHistogram());*/
    }
}
