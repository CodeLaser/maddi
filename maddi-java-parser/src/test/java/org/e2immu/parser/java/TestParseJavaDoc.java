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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseJavaDoc extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            /**
             * Line 1
             * Link to {@link D} and to {@see D#a()}
             */
            interface C {
               /**
                * This is a method
                * @param in1
                * @param in2 some comment
                * @return a value
                * @throws RuntimeException only when not happy
                */
                int method(String in1, String in2) throws RuntimeException;
            }
            /**
             * Referring to {@linkplain #field}? or to {@link #a}?
             * {@see #b(int,java.lang.String)} or {@see a.b.D#b(int,String)} for more info.
             * @param <T> is the type parameter
             */
            class D<T> extends C {
                String field;
                public int a() {
                    return 3;
                }
                public void b(int i, String j) {}
                /**
                 * empty
                 * @param in
                 * @return identity
                 * @param <T> method param
                 */
                public static <T> T staticMethod(T in) {
                    return in;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertEquals("C", typeInfo.simpleName());
        JavaDoc javaDoc = typeInfo.javaDoc();
        assertNotNull(javaDoc);
        assertEquals(2, javaDoc.tags().size());

        JavaDoc.Tag tag0 = javaDoc.tags().getFirst();
        assertEquals(JavaDoc.TagIdentifier.LINK, tag0.identifier());
        assertFalse(tag0.blockTag());
        assertEquals("4-12:4-20", tag0.source().compact2());
        assertEquals("a.b.D", ((TypeInfo) tag0.resolvedReference()).fullyQualifiedName());
        TypeInfo D = (TypeInfo) tag0.resolvedReference();

        JavaDoc.Tag tag1 = javaDoc.tags().getLast();
        assertEquals(JavaDoc.TagIdentifier.SEE, tag1.identifier());
        assertFalse(tag1.blockTag());
        assertEquals("4-29:4-40", tag1.source().compact2());
        assertEquals("a.b.D.a()", ((MethodInfo) tag1.resolvedReference()).fullyQualifiedName());

        assertEquals("""
                *
                 * Line 1
                 * Link to {@link D} and to {@see D#a()}\
                """, javaDoc.comment());

        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        JavaDoc javaDocMethod = methodInfo.javaDoc();
        assertNotNull(javaDocMethod);
        assertEquals("""
                *
                    * This is a method
                    * @param in1\s
                    * @param in2 some comment
                    * @return a value
                    * @throws RuntimeException only when not happy\
                """, javaDocMethod.comment());

        JavaDoc.Tag tag2 = methodInfo.javaDoc().tags().getFirst();
        assertSame(JavaDoc.TagIdentifier.PARAM, tag2.identifier());
        assertEquals("a.b.C.method(String,String):0:in1", tag2.resolvedReference().toString());
        JavaDoc.Tag tag3 = methodInfo.javaDoc().tags().get(1);
        assertSame(JavaDoc.TagIdentifier.PARAM, tag3.identifier());
        assertEquals("a.b.C.method(String,String):1:in2", tag3.resolvedReference().toString());
        JavaDoc.Tag tag4 = methodInfo.javaDoc().tags().get(2);
        assertSame(JavaDoc.TagIdentifier.RETURN, tag4.identifier());
        JavaDoc.Tag tag5 = methodInfo.javaDoc().tags().get(3);
        assertSame(JavaDoc.TagIdentifier.THROWS, tag5.identifier());
        assertEquals("java.lang.RuntimeException", tag5.resolvedReference().toString());

        JavaDoc.Tag tagD0 = D.javaDoc().tags().getFirst();
        assertEquals("a.b.D.field", tagD0.resolvedReference().toString());
        JavaDoc.Tag tagD1 = D.javaDoc().tags().get(1);
        assertEquals("a.b.D.a()", tagD1.resolvedReference().toString());
        JavaDoc.Tag tagD2 = D.javaDoc().tags().get(2);
        assertEquals("a.b.D.b(int,String)", tagD2.resolvedReference().toString());
        JavaDoc.Tag tagD3 = D.javaDoc().tags().get(3);
        assertEquals("a.b.D.b(int,String)", tagD3.resolvedReference().toString());
        JavaDoc.Tag tagD4 = D.javaDoc().tags().get(4);
        assertEquals("T=TP#0 in D", tagD4.resolvedReference().toString());

        MethodInfo staticMethod = D.findUniqueMethod("staticMethod", 1);
        JavaDoc.Tag tagDS0 = staticMethod.javaDoc().tags().getFirst();
        assertEquals("a.b.D.staticMethod(T):0:in", tagDS0.resolvedReference().toString());
        JavaDoc.Tag tagDS1 = staticMethod.javaDoc().tags().get(1);
        assertSame(JavaDoc.TagIdentifier.RETURN, tagDS1.identifier());
        JavaDoc.Tag tagDS2 = staticMethod.javaDoc().tags().get(2);
        assertEquals("T=TP#0 in D.staticMethod", tagDS2.resolvedReference().toString());
    }
}
