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

package io.codelaser.maddi.modification.analyzer.io;

import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl;
import io.codelaser.maddi.modification.link.io.LinkCodec;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import io.codelaser.maddi.modification.prepwork.io.WriteAnalysisResults;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.util.Trie;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A type in the <b>unnamed</b> package, written out with the trie keyed by <i>package name</i>.
 * <p>
 * That keying is what {@code AnalysisResultsCache} in jfocus-metrics uses, and it is the one that breaks:
 * {@code "".split("\\.")} yields a single empty part, which reached {@code capitalize} and threw
 * {@code StringIndexOutOfBoundsException: Index 0 out of bounds for length 0}. The sibling tests key by
 * fully qualified name, where no part is ever empty, so none of them could see it.
 * <p>
 * Found on the timefold-solver corpus, which contains {@code TestdataInUnnamedPackageSolution} — a fixture
 * for exactly this case. The write is best-effort there and the exception was caught and logged, so a whole
 * project's analysis cache silently never wrote, and every run paid the full analysis again.
 */
public class TestWriteAnalysisUnnamedPackage extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            public class X {
                private final String name;
                public X(String name) { this.name = name; }
                public String getName() { return name; }
            }
            """;

    @DisplayName("a type in the unnamed package can be written and read back")
    @Test
    public void test1() throws IOException {
        TypeInfo X = javaInspector.parse("X", INPUT1);
        assertEquals("", X.packageName(), "expected the unnamed package");

        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo getName = X.findUniqueMethod("getName", 0);
        MethodLinkedVariables before = getName.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(before);

        // keyed by PACKAGE name, as the analysis cache does -- not by fully qualified name
        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(X.packageName().split("\\."), X);

        File dest = new File("build/json-unnamed-package");
        if (dest.mkdirs()) { /* fresh */ }
        Codec codec = new LinkCodec(javaInspector).codec();
        new WriteAnalysisResults(runtime).write(dest, typeTrie, codec);

        File written = new File(dest, "_unnamed_package.json");
        assertTrue(written.canRead(), () -> "expected " + written + ", found "
                                            + List.of(java.util.Objects.requireNonNull(dest.list())));

        // and it round-trips: the restored analysis equals what was written
        javaInspector.invalidateAllSources();
        TypeInfo X1 = javaInspector.parse("X", INPUT1);
        new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources()).go(codec, written.toPath());

        MethodInfo getName1 = X1.findUniqueMethod("getName", 0);
        MethodLinkedVariables after = getName1.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals(before, after);
    }
}
