/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration.java.type;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.type.IsAssignableFrom;
import io.codelaser.maddi.inspection.api.resource.MD5FingerPrint;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

import static org.junit.jupiter.api.Assertions.*;

public class TestIsAssignableFrom extends CommonTest {

    @Test
    public void test() {
        TypeInfo closeable = javaInspector.compiledTypesManager().type(Closeable.class);
        assertEquals("java.io.Closeable", closeable.fullyQualifiedName());
        TypeInfo iterable = javaInspector.compiledTypesManager().typeIfLoaded(Iterable.class);
        assertEquals("java.lang.Iterable", iterable.fullyQualifiedName());
        ParameterizedType closeablePt = runtime.newParameterizedType(closeable, 0);
        ParameterizedType iterableCloseable = runtime.newParameterizedType(iterable, List.of(closeablePt));
        assertEquals("Iterable<java.io.Closeable>", iterableCloseable.fullyQualifiedName());
        ParameterizedType closeableArray = runtime.newParameterizedType(closeable, 1);
        assertEquals("java.io.Closeable[]", closeableArray.fullyQualifiedName());

        // there is no special code in IsAssignableFrom to make this work!
        // there is, however, special code in HiddenContentSelector to deal with the 'transfer'
        // from array base to type parameter
        assertFalse(iterableCloseable.isAssignableFrom(runtime, closeableArray));
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
        TypeInfo list = javaInspector.compiledTypesManager().type(List.class);
        assertSame(MD5FingerPrint.NO_FINGERPRINT, list.compilationUnit().fingerPrintOrNull());
        assertSame(MD5FingerPrint.NO_FINGERPRINT, list.compilationUnit().sourceSet().fingerPrintOrNull());
    }

    @Test
    public void test3() {
        ParameterizedType stringPt = runtime.stringParameterizedType();
        TypeInfo longPredicate = javaInspector.compiledTypesManager().typeIfLoaded(LongPredicate.class);
        assertNotNull(longPredicate);
        ParameterizedType longPredicatePt = longPredicate.asParameterizedType();
        assertFalse(longPredicatePt.isAssignableFrom(runtime, stringPt));
        assertFalse(stringPt.isAssignableFrom(runtime, longPredicatePt));

        TypeInfo comparable = javaInspector.compiledTypesManager().typeIfLoaded(Comparable.class);
        ParameterizedType erasedComparable = comparable.asSimpleParameterizedType();
        assertEquals(-1, new IsAssignableFrom(runtime, erasedComparable, longPredicatePt)
                .execute(true, false, IsAssignableFrom.Mode.COVARIANT_ERASURE));
        assertEquals(-1, new IsAssignableFrom(runtime, longPredicatePt, erasedComparable)
                .execute(true, false, IsAssignableFrom.Mode.COVARIANT_ERASURE));
    }

    @Test
    public void test4() {
        TypeInfo longPredicate = javaInspector.compiledTypesManager().typeIfLoaded(LongPredicate.class);
        assertNotNull(longPredicate);
        TypeInfo longConsumer = javaInspector.compiledTypesManager().typeIfLoaded(LongConsumer.class);
        assertNotNull(longConsumer);
        ParameterizedType longPredicatePt = longPredicate.asSimpleParameterizedType();
        ParameterizedType longConsumerPt = longConsumer.asSimpleParameterizedType();
        assertFalse(longPredicatePt.isAssignableFrom(runtime, longConsumerPt));
        assertFalse(longConsumerPt.isAssignableFrom(runtime, longPredicatePt));
        assertEquals(13, new IsAssignableFrom(runtime, longConsumerPt, longPredicatePt)
                .execute(false, true, IsAssignableFrom.Mode.COVARIANT_ERASURE));
    }

}
