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

package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.type.IsAssignableFrom2;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

import static org.junit.jupiter.api.Assertions.*;

public class TestIsAssignableFrom2 extends CommonTest {

    @Test
    public void test() {
        TypeInfo closeable = javaInspector.compiledTypesManager().getOrLoad(Closeable.class);
        assertEquals("java.io.Closeable", closeable.fullyQualifiedName());
        TypeInfo iterable = javaInspector.compiledTypesManager().get(Iterable.class);
        assertEquals("java.lang.Iterable", iterable.fullyQualifiedName());
        ParameterizedType closeablePt = runtime.newParameterizedType(closeable, 0);
        ParameterizedType iterableCloseable = runtime.newParameterizedType(iterable, List.of(closeablePt));
        assertEquals("Iterable<java.io.Closeable>", iterableCloseable.fullyQualifiedName());
        ParameterizedType closeableArray = runtime.newParameterizedType(closeable, 1);
        assertEquals("java.io.Closeable[]", closeableArray.fullyQualifiedName());

        // there is no special code in IsAssignableFrom to make this work!
        // there is, however, special code in HiddenContentSelector to deal with the 'transfer'
        // from array base to type parameter
        assertFalse(runtime.isAssignableFrom(iterableCloseable, closeableArray));
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                 interface Parent<T extends Parent<?>> {
                }
                interface Child extends Parent<Child> {
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        TypeInfo parent = X.findSubType("Parent");
        TypeInfo child = X.findSubType("Child");
        assertTrue(parent.asParameterizedType().isAssignableFrom(runtime, child.asParameterizedType()));

        assertEquals("rRYs3LDF1ia1MgjUQEW0Aw==", X.compilationUnit().fingerPrintOrNull().toString());
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        assertSame(MD5FingerPrint.NO_FINGERPRINT, list.compilationUnit().fingerPrintOrNull());
        assertSame(MD5FingerPrint.NO_FINGERPRINT, list.compilationUnit().sourceSet().fingerPrintOrNull());
    }

    @Test
    public void test3() {
        ParameterizedType stringPt = runtime.stringParameterizedType();
        TypeInfo longPredicate = javaInspector.compiledTypesManager().get(LongPredicate.class);
        assertNotNull(longPredicate);
        ParameterizedType longPredicatePt = longPredicate.asParameterizedType();
        assertFalse(longPredicatePt.isAssignableFrom(runtime, stringPt));
        assertFalse(stringPt.isAssignableFrom(runtime, longPredicatePt));

        TypeInfo comparable = javaInspector.compiledTypesManager().get(Comparable.class);
        ParameterizedType erasedComparable = comparable.asSimpleParameterizedType();
        assertFalse(new IsAssignableFrom2(runtime).test(erasedComparable, longPredicatePt));
        assertFalse(new IsAssignableFrom2(runtime).test(longPredicatePt, erasedComparable));
    }

    @Test
    public void test4() {
        TypeInfo longPredicate = javaInspector.compiledTypesManager().get(LongPredicate.class);
        assertNotNull(longPredicate);
        TypeInfo longConsumer = javaInspector.compiledTypesManager().get(LongConsumer.class);
        assertNotNull(longConsumer);
        ParameterizedType longPredicatePt = longPredicate.asSimpleParameterizedType();
        ParameterizedType longConsumerPt = longConsumer.asSimpleParameterizedType();
        assertFalse(runtime.isAssignableFrom(longPredicatePt, longConsumerPt));

        // a predicate is also a consumer, but a consumer is not a predicate
        assertTrue(runtime.isAssignableFrom(longConsumerPt, longPredicatePt));
        assertFalse(runtime.isAssignableFrom(longPredicatePt, longConsumerPt));
    }

    @Test
    public void test5() {
        ParameterizedType longPt = runtime.longParameterizedType();
        ParameterizedType intPt = runtime.intParameterizedType();
        ParameterizedType boxedLongPt = runtime.boxedLongTypeInfo().asParameterizedType();
        ParameterizedType integerPt = runtime.integerTypeInfo().asParameterizedType();

        ParameterizedType objectPt = runtime.objectParameterizedType();
        ParameterizedType objectPt1 = runtime.objectParameterizedType().copyWithArrays(1);
        ParameterizedType stringPt = runtime.stringParameterizedType();
        ParameterizedType stringPt1 = runtime.stringParameterizedType().copyWithArrays(1);
        ParameterizedType nullPt = runtime.newNullConstant(List.of(), runtime.noSource()).parameterizedType();

        assertTrue(runtime.isAssignableFrom(longPt, longPt));
        assertTrue(runtime.isAssignableFrom(longPt, intPt));
        assertFalse(runtime.isAssignableFrom(intPt, longPt));
        assertFalse(runtime.isAssignableFrom(intPt, nullPt));
        assertFalse(runtime.isAssignableFrom(nullPt, intPt));

        assertTrue(runtime.isAssignableFrom(longPt, boxedLongPt));
        assertTrue(runtime.isAssignableFrom(boxedLongPt, longPt));
        assertFalse(runtime.isAssignableFrom(intPt, boxedLongPt));
        assertFalse(runtime.isAssignableFrom(boxedLongPt, intPt));
        assertFalse(runtime.isAssignableFrom(boxedLongPt, integerPt));

        assertTrue(runtime.isAssignableFrom(objectPt, nullPt));
        assertFalse(runtime.isAssignableFrom(nullPt, objectPt));
        assertTrue(runtime.isAssignableFrom(objectPt, stringPt));

        assertTrue(runtime.isAssignableFrom(objectPt, objectPt));

        assertFalse(runtime.isAssignableFrom(stringPt1, stringPt));
        assertFalse(runtime.isAssignableFrom(stringPt, stringPt1));

        assertTrue(runtime.isAssignableFrom(objectPt, objectPt1));
        assertFalse(runtime.isAssignableFrom(objectPt1, objectPt));
    }
}
