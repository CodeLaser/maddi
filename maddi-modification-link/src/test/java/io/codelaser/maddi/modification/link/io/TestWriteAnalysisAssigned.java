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

package io.codelaser.maddi.modification.link.io;

import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import io.codelaser.maddi.modification.prepwork.io.WriteAnalysisResults;
import io.codelaser.maddi.util.Trie;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The 'assigned' own-field set survives the write/read round-trip. The wire format appends an
 * "A"-tagged trailing list only when the set is non-empty — output without assigned fields stays
 * byte-identical to the previous format, and old files (no such entry) decode with an empty set.
 */
public class TestWriteAnalysisAssigned extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class Y {
                int i;
                void m() { i++; }
                void n() { }
            }
            """;

    @DisplayName("assigned set survives the round-trip; absent entry decodes as empty")
    @Test
    public void test() throws IOException {
        TypeInfo Y = javaInspector.parse("a.b.Y", INPUT);
        prepWork(Y);
        MethodInfo m = Y.findUniqueMethod("m", 0);
        MethodInfo n = Y.findUniqueMethod("n", 0);
        FieldInfo i = Y.getFieldByName("i", true);

        Variable thisVar = javaInspector.runtime().newThis(Y.asSimpleParameterizedType());
        Variable thisI = javaInspector.runtime().newFieldReference(i);
        MethodLinkedVariables mlvM = new MethodLinkedVariablesImpl(LinksImpl.EMPTY, List.of(),
                Set.of(thisVar), Set.of(thisI));
        m.analysis().set(METHOD_LINKS, mlvM);
        // non-default (all-empty values are not written): modified only, no assigned entry on the wire
        MethodLinkedVariables mlvN = new MethodLinkedVariablesImpl(LinksImpl.EMPTY, List.of(),
                Set.of(thisVar), Set.of());
        n.analysis().set(METHOD_LINKS, mlvN);

        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(Y.fullyQualifiedName().split("\\."), Y);
        File dest = new File("build/json-assigned");
        if (dest.mkdirs()) {
            // fresh directory for this test
        }
        Codec codec = new LinkCodec(javaInspector).codec();
        new WriteAnalysisResults(runtime).write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABY.json").toPath());

        javaInspector.invalidateAllSources();
        TypeInfo Y2 = javaInspector.parse("a.b.Y", INPUT);
        new LoadAnalysisResults(javaInspector.runtime(), javaInspector.mainSources()).go(codec, written);

        MethodInfo m2 = Y2.findUniqueMethod("m", 0);
        MethodLinkedVariables mlvM2 = m2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(mlvM2);
        assertEquals("this.i", mlvM2.sortedAssignedString(),
                "the assigned set must survive the round-trip: " + written);
        assertEquals("this", mlvM2.sortedModifiedString());

        MethodInfo n2 = Y2.findUniqueMethod("n", 0);
        MethodLinkedVariables mlvN2 = n2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(mlvN2);
        assertTrue(mlvN2.assigned().isEmpty(), "no 'A' entry decodes as an empty set: " + written);
    }
}
