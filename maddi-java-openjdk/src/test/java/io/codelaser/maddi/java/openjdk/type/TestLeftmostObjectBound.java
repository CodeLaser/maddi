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

package io.codelaser.maddi.java.openjdk.type;

import static org.junit.jupiter.api.Assertions.*;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.java.openjdk.CommonTest;

/*
maddi#226: an explicit leftmost Object bound decides a type parameter's erasure (JLS 4.6), so it must survive
class-file loading. Guava 10's

  <K, V, I extends Object & Iterable<V> & Iterator<V>> ImmutableMap<K, V> uniqueIndex(I, Function<? super V, K>)

erases to uniqueIndex(Object, Function). ClassSymbolScanner dropped the Object, I erased to Iterable, and
MethodMapImpl asserted on two uniqueIndex(Iterable, Function) overloads.
*/
public class TestLeftmostObjectBound extends CommonTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            class X {
                static Object callMax(java.util.List<String> list) { return java.util.Collections.max(list); }
                static void plain(Object o) { }
                static java.util.List<String> callEmptyList() { return java.util.Collections.emptyList(); }
                static <K, V> void uniqueIndex(Iterable<V> values, Function<? super V, K> f) { }
                static <K, V, I extends Object & Iterable<V> & Iterator<V>> void uniqueIndex(I values, Function<? super V, K> f) { }
                static <K, V> void uniqueIndex(Iterator<V> values, Function<? super V, K> f) { }
            }
            """;

    // source: the three overloads keep distinct keys
    @Test
    public void sourceOverloadsDistinct() {
        TypeInfo X = scan("a.b.X", INPUT);
        assertEquals(3, X.methods().stream().filter(m -> "uniqueIndex".equals(m.name())).count());
        MethodInfo middle = X.methods().stream()
                .filter(m -> "uniqueIndex".equals(m.name()) && m.typeParameters().size() == 3).findFirst().orElseThrow();
        assertEquals(objectKey(X), middle.parameters().getFirst().parameterizedType().erasedForFQN()
                .fullyQualifiedName());
    }

    // class file: Collections.max is <T extends Object & Comparable<? super T>> T max(Collection<? extends T>)
    @Test
    public void compiledKeepsLeftmostObject() {
        TypeInfo X = scan("a.b.X", INPUT);
        ReturnStatement rs = (ReturnStatement) X.findUniqueMethod("callMax", 1).methodBody().statements().getFirst();
        MethodInfo max = ((MethodCall) rs.expression()).methodInfo();
        assertEquals("java.util.Collections.max(java.util.Collection)", max.fullyQualifiedName());
        TypeParameter t = max.typeParameters().getFirst();
        assertEquals(2, t.typeBounds().size(), t.typeBounds().toString());
        assertTrue(t.typeBounds().getFirst().typeInfo().isJavaLangObject());
        assertEquals(objectKey(X), max.returnType().erasedForFQN().fullyQualifiedName());

        // a plain <T> still has no bounds
        ReturnStatement rs2 = (ReturnStatement) X.findUniqueMethod("callEmptyList", 0).methodBody().statements()
                .getFirst();
        MethodInfo emptyList = ((MethodCall) rs2.expression()).methodInfo();
        assertTrue(emptyList.typeParameters().getFirst().typeBounds().isEmpty());
    }

    private static String objectKey(TypeInfo X) {
        return X.methods().stream().filter(m -> "plain".equals(m.name())).findFirst().orElseThrow()
                .parameters().getFirst().parameterizedType().erasedForFQN().fullyQualifiedName();
    }
}
