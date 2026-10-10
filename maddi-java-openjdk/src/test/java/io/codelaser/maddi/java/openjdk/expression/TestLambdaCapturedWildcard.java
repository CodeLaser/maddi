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

package io.codelaser.maddi.java.openjdk.expression;

import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CodeLaser/maddi#112. javac types the inner lambda's parameter as {@code List<CAP#1>}, CAP#1 the capture of
 * {@code ? extends T}. The captured type variable's upper bound is the type variable {@code T}; the converter
 * rebuilt the wildcard from the bound's type info, which a type variable does not have, and left
 * {@code List<? extends >}: unprintable as soon as the lambda becomes an anonymous class or a helper method.
 */
public class TestLambdaCapturedWildcard extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package q;
            import java.util.List;
            import java.util.function.Consumer;
            public class F {
                interface Box<T> { void onComplete(Consumer<? super List<T>> c); }
                static <T> void find(List<Box<? extends T>> boxes) {
                    boxes.forEach(box -> box.onComplete(result -> System.out.println(result.size())));
                }
            }
            """;

    @DisplayName("the capture of '? extends T' keeps its bound T")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("q.F", INPUT);
        MethodInfo find = typeInfo.findUniqueMethod("find", 1);
        List<Lambda> lambdas = new ArrayList<>();
        find.methodBody().visit(e -> {
            if (e instanceof Lambda lambda) lambdas.add(lambda);
            return true;
        });
        assertEquals(2, lambdas.size());
        Lambda inner = lambdas.stream().filter(l -> "result".equals(l.methodInfo().parameters().getFirst().name()))
                .findFirst().orElseThrow();
        assertEquals("java.util.List<? extends T>",
                inner.methodInfo().parameters().getFirst().parameterizedType().fullyQualifiedName());
        assertEquals("java.util.function.Consumer<java.util.List<? extends T>>",
                inner.concreteFunctionalType().fullyQualifiedName());
        Lambda outer = lambdas.stream().filter(l -> "box".equals(l.methodInfo().parameters().getFirst().name()))
                .findFirst().orElseThrow();
        assertEquals("q.F.Box<? extends T>",
                outer.methodInfo().parameters().getFirst().parameterizedType().fullyQualifiedName());
    }
}
