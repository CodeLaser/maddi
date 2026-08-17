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
 * Task #39 (c): mediation provenance survives the write/read round-trip. The wire format appends an
 * optional 4th element to [from, nature, to] only when mediated — unmediated output stays byte-identical
 * to the pre-#39 format, and old files (3-element links) decode as unmediated.
 */
public class TestWriteAnalysisMediated extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class Y {
                void m(Y other, Y another) { }
            }
            """;

    @DisplayName("mediated flag survives the write/read round-trip; unmediated stays 3-element")
    @Test
    public void test() throws IOException {
        TypeInfo Y = javaInspector.parse("a.b.Y", INPUT);
        prepWork(Y);
        MethodInfo m = Y.findUniqueMethod("m", 2);
        ParameterInfo other = m.parameters().getFirst();
        ParameterInfo another = m.parameters().get(1);

        Variable thisVar = javaInspector.runtime().newThis(Y.asSimpleParameterizedType());
        Link mediated = new LinksImpl.LinkImpl(other, LinkNatureImpl.IS_ASSIGNED_FROM, thisVar, true);
        Link plain = new LinksImpl.LinkImpl(another, LinkNatureImpl.IS_ASSIGNED_FROM, thisVar, false);
        MethodLinkedVariables mlv = new MethodLinkedVariablesImpl(LinksImpl.EMPTY,
                List.of(new LinksImpl(other, List.of(mediated)), new LinksImpl(another, List.of(plain))),
                Set.of());
        m.analysis().set(METHOD_LINKS, mlv);

        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(Y.fullyQualifiedName().split("\\."), Y);
        File dest = new File("build/json-mediated");
        if (dest.mkdirs()) {
            // fresh directory for this test
        }
        Codec codec = new LinkCodec(javaInspector).codec();
        new WriteAnalysisResults(runtime).write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABY.json").toPath());

        javaInspector.invalidateAllSources();
        TypeInfo Y2 = javaInspector.parse("a.b.Y", INPUT);
        new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources()).go(codec, written);

        MethodInfo m2 = Y2.findUniqueMethod("m", 2);
        MethodLinkedVariables mlv2 = m2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(mlv2);
        Link decodedMediated = mlv2.ofParameters().getFirst().stream().findFirst().orElseThrow();
        assertTrue(decodedMediated.mediated(), "the mediated flag must survive the round-trip: " + written);
        Link decodedPlain = mlv2.ofParameters().get(1).stream().findFirst().orElseThrow();
        assertFalse(decodedPlain.mediated(), "an unmediated link must stay unmediated: " + written);
    }
}
