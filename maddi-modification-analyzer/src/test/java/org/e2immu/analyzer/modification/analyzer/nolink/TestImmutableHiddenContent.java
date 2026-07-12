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

package org.e2immu.analyzer.modification.analyzer.nolink;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent immutability/hidden-content tests. A type with a type-parameter field carries hidden content (the actual
 * type of that field is not known here), so it is at most {@code @Immutable(hc=true)}; a type whose fields are all of
 * concrete immutable type, in a final class, is deeply immutable (road-to-immutability, "Hidden content"). Asserts the
 * computed IMMUTABLE_TYPE property.
 */
public class TestImmutableHiddenContent extends CommonTest {

    private static Value.Immutable immutable(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                // no type parameter, concrete immutable fields -> deeply immutable
                record IntPair(int a, int b) {}
                // one type parameter -> the field 't' is hidden content -> immutable with hidden content
                record Box<T>(T t) {}
                // two type parameters -> still immutable-hc
                record Pair<K, V>(K k, V v) {}
            }
            """;

    @DisplayName("a type-parameter field yields @Immutable(hc); only concrete immutable fields give deep immutability")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        Value.Immutable intPair = immutable(X.findSubType("IntPair"));
        assertTrue(intPair.isImmutable(), "no hidden content -> deeply immutable, have " + intPair);

        Value.Immutable box = immutable(X.findSubType("Box"));
        assertTrue(box.isImmutableHC(), "type parameter T is hidden content, have " + box);
        assertFalse(box.isImmutable(), "not deeply immutable, have " + box);

        Value.Immutable pair = immutable(X.findSubType("Pair"));
        assertTrue(pair.isImmutableHC(), "type parameters are hidden content, have " + pair);
    }
}
