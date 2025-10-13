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

package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
IMPORTANT: use "analyzed" branch of "testarchive".
A small number of files have been modified wrt the main branch, for this test to run.
 */
public class TestCloneBench extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCloneBench.class);

    public TestCloneBench() {
        super(true, "jmod:java.desktop",
                "jmod:java.compiler",
                "jmod:java.datatransfer",
                "jmod:java.sql",
                "jmod:java.logging",
                "jmod:java.instrument",
                "jmod:java.rmi",
                "jmod:java.management");
    }

    public void process(String name, AtomicInteger counter, Map<MethodInfo, Integer> typeHistogram) throws IOException {
        String directory = "../../testarchive/" + name + "/src/main/java/";
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
            File outFile = new File(analyzedDir, javaFile.getName());
            process(name, javaFile, outFile, counter.incrementAndGet(), typeHistogram);
        }
    }

    private void process(String setName, File javaFile, File outFile, int count,
                         Map<MethodInfo, Integer> typeHistogram) throws IOException {
        String input = Files.readString(javaFile.toPath());
        LOGGER.info("Start parsing #{}, {}, file of size {}", count, javaFile, input.length());

        TypeInfo typeInfo = javaInspector.parse(input, javaFile.getName(), setName);

        List<Info> analysisOrder = prepWork(typeInfo);
        analyzer.go(analysisOrder);
        String printed = javaInspector.print2(typeInfo);
        Files.writeString(outFile.toPath(), printed, StandardCharsets.UTF_8);

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
    public void test() throws IOException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("graph-algorithm")).setLevel(Level.WARN);

        AtomicInteger counter = new AtomicInteger();
        Map<MethodInfo, Integer> typeHistogram = new HashMap<>();

        for (String dir : DIRS) {
            process(dir, counter, typeHistogram);
        }
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
