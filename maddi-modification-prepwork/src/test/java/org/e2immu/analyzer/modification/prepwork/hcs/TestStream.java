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

package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.hct.ComputeHiddenContent;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStream extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.stream.Stream;
            import java.util.Optional;
            class X {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static Stream<M> m3(Stream<M> stream) {
                    return stream.filter(m -> m.i == 3);
                }
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);

        TypeInfo stream = javaInspector.compiledTypesManager().get(Stream.class);
        HiddenContentTypes hctStream = stream.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=T, 1=Stream", hctStream.detailedSortedTypes());
        MethodInfo filter = stream.findUniqueMethod("filter", 1);

        HiddenContentTypes hctFilter = filter.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=T, 1=Stream - 2=Predicate", hctFilter.detailedSortedTypes());
        HiddenContentSelector hcsFilter = filter.analysis().getOrDefault(HiddenContentSelector.HCS_METHOD, HiddenContentSelector.NONE);
        assertEquals("0=0,1=*", hcsFilter.detailed());
    }
}
