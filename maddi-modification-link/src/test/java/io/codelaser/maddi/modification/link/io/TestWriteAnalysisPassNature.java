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

package org.e2immu.analyzer.modification.link.io;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.analyzer.modification.prepwork.io.WriteAnalysisResults;
import org.e2immu.util.internal.util.Trie;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip of the pass-carrying ≡ variant (☷): the nature encodes as [symbol, methodInfo...] and
 * decodes back through makeIdenticalTo. Prerequisite for checkpoint/resume (task #34): shallow summaries
 * of Iterator.remove-style annotations carry ☷, and before this the symbol was not decodable at all
 * ("Unknown symbol ☷" far from the cause).
 */
public class TestWriteAnalysisPassNature extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                private int i;
                void m(X other) { }
                void remove() { i++; }
            }
            """;

    @DisplayName("pass-carrying ☷ nature survives the write/read round-trip")
    @Test
    public void test() throws IOException {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        prepWork(X);
        MethodInfo m = X.findUniqueMethod("m", 1);
        MethodInfo remove = X.findUniqueMethod("remove", 0);
        ParameterInfo other = m.parameters().getFirst();

        // parameter links: 0:other ☷(remove) this — identical as long as 'remove' is not involved
        Variable thisVar = javaInspector.runtime().newThis(X.asSimpleParameterizedType());
        Link passLink = new LinksImpl.LinkImpl(other, LinkNatureImpl.makeIdenticalTo(Set.of(remove)), thisVar);
        MethodLinkedVariables mlv = new MethodLinkedVariablesImpl(
                LinksImpl.EMPTY, List.of(new LinksImpl(other, List.of(passLink))), Set.of());
        m.analysis().set(METHOD_LINKS, mlv);

        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(X.fullyQualifiedName().split("\\."), X);
        File dest = new File("build/json-pass-nature");
        if (dest.mkdirs()) {
            // fresh directory for this test
        }
        Codec codec = new LinkCodec(javaInspector).codec();
        new WriteAnalysisResults(runtime).write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABX.json").toPath());
        assertTrue(written.contains("☷"), "encoded form must carry the pass symbol: " + written);
        assertTrue(written.contains("Mremove(0)") || written.contains("remove"),
                "encoded form must carry the pass method: " + written);

        javaInspector.invalidateAllSources();
        TypeInfo X2 = javaInspector.parse("a.b.X", INPUT);
        new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources()).go(codec, written);

        MethodInfo m2 = X2.findUniqueMethod("m", 1);
        MethodLinkedVariables mlv2 = m2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(mlv2);
        Link decoded = mlv2.ofParameters().getFirst().stream().findFirst().orElseThrow();
        assertEquals("☷", decoded.linkNature().toString());
        assertEquals(1, decoded.linkNature().pass().size());
        MethodInfo decodedPass = decoded.linkNature().pass().iterator().next();
        assertEquals("a.b.X.remove()", decodedPass.fullyQualifiedName());
        assertEquals(mlv.toString(), mlv2.toString());
    }
}
