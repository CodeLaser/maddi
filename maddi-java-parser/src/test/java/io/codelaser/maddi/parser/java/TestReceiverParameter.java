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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.parsers.java.JavaParser;
import org.parsers.java.ParseException;

import static io.codelaser.maddi.parser.java.TestRecordPatternGrammar.assertParses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * The receiver parameter (JLS 8.4.1): 'X this' as the first formal parameter of an instance method, or
 * 'Outer Outer.this' in an inner class's constructor. It declares nothing and exists only to carry type
 * annotations. javac accepts it; the CongoCC grammar did not, so the detailed-source pre-scan of a file using it
 * failed and the whole unit lost its comments and positions (maddi#99; three hibernate-core files).
 *
 * Grammar fix in the congo fork (examples/java/Java.ccc, ReceiverParameter); requested upstream too.
 */
public class TestReceiverParameter extends CommonTestParse {

    @Test
    public void plain() {
        assertParses("class X { private void resetStart(X this) { } }");
    }

    @Test
    public void followedByParameters() {
        assertParses("class X { int m(X this, int a, String... rest) { return a; } }");
    }

    @Test
    public void generic() {
        assertParses("class X<T> { void m(X<T> this, T t) { } }");
    }

    @Test
    public void annotated() {
        assertParses("class X { @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface Ro { } "
                + "void m(@Ro X this) { } }");
    }

    @Test
    public void innerClassConstructor() {
        assertParses("class Outer { class Inner { Inner(Outer Outer.this) { } } }");
    }

    // still rejected where javac rejects it: a receiver parameter is legal in first position only
    @Test
    public void notInSecondPosition() {
        JavaParser p = new JavaParser("class X { void m(int a, X this) { } }");
        p.setParserTolerant(false);
        assertThrows(ParseException.class, p::CompilationUnit);
    }

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              int m(C this, int a) {
                return a;
              }
              class Inner {
                Inner(C C.this, String s) {
                }
              }
            }
            """;

    // the CST does not model the receiver: it is not a parameter, as in javac's MethodTree.getParameters()
    @Test
    public void notAParameter() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        assertEquals("a", m.parameters().getFirst().name());
        TypeInfo inner = typeInfo.findSubType("Inner");
        MethodInfo constructor = inner.findConstructor(1);
        assertEquals("s", constructor.parameters().getFirst().name());
    }
}
