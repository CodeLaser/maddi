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

package org.e2immu.analyzer.modification.prepwork.clonebench;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;

/*
IMPORTANT: use "analyzed" branch of "testarchive".
A small number of files have been modified wrt the main branch, for this test to run.
 */
@Tag("slow")
public class TestCloneBenchMethodHistogram extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCloneBenchMethodHistogram.class);
    final JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder().build();

    public TestCloneBenchMethodHistogram() {
        super("jmod:java.desktop",
                "jmod:java.compiler",
                "jmod:java.datatransfer",
                "jmod:java.sql",
                "jmod:java.logging",
                "jmod:java.instrument",
                "jmod:java.rmi",
                "jmod:java.management");
    }

    // the per-directory source sets parsed by this test; under openjdk each must be registered (and distinct) so
    // its classpath resolves and identically-named clone-bench types in different directories do not collide.
    @Override
    protected List<String> openJdkExtraSourceSetNames() {
        return List.of("testDoWhile", "testFor", "testForBubbleSort", "testForeachPureCompiles",
                "testSwitchFor", "testSwitchPureCompiles", "testTry", "testTryResource", "testWhile");
    }

    public void process(String name, Map<String, Integer> methodHistogram) throws IOException {
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);

        String directory = "../../testtransform/" + name + "/src/main/java/";
        File src = new File(directory);
        assertTrue(src.isDirectory());
        File[] javaFiles = src.listFiles(f -> f.getName().endsWith(".java") && !isProcessed(f.getName()));
        assertNotNull(javaFiles);
        LOGGER.info("Found {} java files in {}", javaFiles.length, directory);
        // the openjdk parser resolves the classpath via the source set's dependencies and asserts the source set
        // is registered in the input configuration; use the per-directory source set built (and registered) at
        // setup, which carries javaBase + the jmods and is distinct per directory (so identically-named clone-bench
        // types in different directories do not collide). The maddi parser resolves against its global classpath,
        // so an ad-hoc source set is fine there.
        SourceSet sourceSet;
        if (openJdkParser) {
            sourceSet = openJdkSourceSetsByName.get(name);
        } else {
            sourceSet = new SourceSetImpl.Builder().setName(name)
                    .setUri(URI.create("file:/"))
                    .build();
            sourceSet.computePriorityDependencies();
        }
        for (File javaFile : javaFiles) {
            process(sourceSet, analyzer, javaFile, methodHistogram);
        }
    }

    private static final Pattern PROCESSED = Pattern.compile(".+_t(_o\\d)?.java");

    private static boolean isProcessed(String name) {
        return PROCESSED.matcher(name).matches();
    }

    private void process(SourceSet sourceSet, PrepAnalyzer analyzer, File javaFile, Map<String, Integer> methodHistogram) {
        LOGGER.info("Start parsing {} in set {}", javaFile, sourceSet.name());
        TypeInfo typeInfo = javaInspector.parseSingleFileInSourceSet(javaFile.toURI(), sourceSet, parseOptions).parseResult().firstType();
        List<Info> analysisOrder = analyzer.doPrimaryType(typeInfo);
        LOGGER.info("-    analysis order size {}", analysisOrder.size());
        analysisOrder.stream().filter(info -> info instanceof MethodInfo)
                .forEach(info -> {
                    MethodInfo mi = (MethodInfo) info;
                    mi.methodBody().visit(e -> {
                        if (e instanceof MethodCall mc && mc.methodInfo().typeInfo().primaryType() != typeInfo) {
                            methodHistogram.merge(mc.methodInfo().fullyQualifiedName(), 1, Integer::sum);
                        }
                        return true;
                    });
                });
    }

    @Test
    public void test() throws IOException {
        Map<String, Integer> methodHistogram = new HashMap<>();
        process("testDoWhile", methodHistogram);
        process("testFor", methodHistogram);
        process("testForBubbleSort", methodHistogram);
        process("testForeachPureCompiles", methodHistogram);
        process("testSwitchFor", methodHistogram);
        process("testSwitchPureCompiles", methodHistogram);
        process("testTry", methodHistogram);
        process("testTryResource", methodHistogram);
        process("testWhile", methodHistogram);

        methodHistogram.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                .forEach(e -> LOGGER.info("{} {}", e.getValue(), e.getKey()));
    }
}
