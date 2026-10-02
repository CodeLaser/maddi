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
