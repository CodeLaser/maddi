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

package io.codelaser.maddi.modification.prepwork.clonebench;

import io.codelaser.maddi.modification.common.CloneBenchCorpus;
import io.codelaser.maddi.modification.prepwork.CommonTest;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;

/*
Frequency histogram of EXTERNAL (JDK/library) method calls over the clone-bench corpus, to see which APIs
real-world code leans on -- it asserts nothing, it reports. Reads "testarchive" via CloneBenchCorpus.

It used to read "../../testtransform", which was wrong twice over: a hardcoded relative path, and a
CodeLaser-internal corpus that maddi (standalone, upstream of the jfocus repos) has no business depending
on. It only ever read the ORIGINAL sources there anyway -- the "_t"/"_o" transform outputs were filtered
out -- and those originals are a fork of these same testarchive files.

The move was measured, not assumed. The nine testtransform directories map 1:1 onto the nine below, with
identical file counts. 1125 of 9154 files differ textually (12%: imports, a stray negation, renamed
classes), yet the histogram is invariant: per directory, dowhile gave 805 vs 806 calls / 221 vs 222
distinct methods and switch_fors gave 1487 vs 1487 / 135 vs 135, with identical top-15 in both. testarchive
also parses with zero failures, which retires the old "a small number of files have been modified for this
test to run" caveat (those fixes landed when the 'analyzed' branch merged into main, 2026-07-26).
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

    /** The clone-bench directories sampled, in testarchive naming. */
    private static final List<String> CORPUS_DIRECTORIES = List.of(
            "dowhile_pure_compiles", "fors_pure_compiles", "bubblesort_for_withunit",
            "foreach_pure_compiles", "switch_fors_compiles", "switch_pure_compiles",
            "try_pure_compiles", "try_wr_compiles", "while_pure_compiles");

    // the per-directory source sets parsed by this test; under openjdk each must be registered (and distinct) so
    // its classpath resolves and identically-named clone-bench types in different directories do not collide.
    @Override
    protected List<String> openJdkExtraSourceSetNames() {
        return CORPUS_DIRECTORIES;
    }

    public void process(String name, Map<String, Integer> methodHistogram) throws IOException {
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);

        File src = CloneBenchCorpus.sourceDirectory(name).toFile();
        assertTrue(src.isDirectory(), "clone-bench directory not found: " + src);
        // no "_t"/"_o" filter needed: testarchive holds the original sources only, the transform outputs live
        // in the (separate, CodeLaser-owned) testtransform repository.
        File[] javaFiles = src.listFiles(f -> f.getName().endsWith(".java"));
        assertNotNull(javaFiles);
        LOGGER.info("Found {} java files in {}", javaFiles.length, src);
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
        CloneBenchCorpus.assumeAvailable();
        Map<String, Integer> methodHistogram = new HashMap<>();
        for (String directory : CORPUS_DIRECTORIES) {
            process(directory, methodHistogram);
        }

        methodHistogram.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                .forEach(e -> LOGGER.info("{} {}", e.getValue(), e.getKey()));
    }
}
