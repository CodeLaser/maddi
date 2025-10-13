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

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseConstructorCall extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class C {
              int k;
              public static void main(String[] args) {
                C c = new C();
                System.out.println(c);
              }
              public C() {
                k = 1;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo constructor = typeInfo.findConstructor(0);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertSame(constructor, cc.constructor());
            assertSame(typeInfo, cc.parameterizedType().typeInfo());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class C<K> {
              K k;
              public static void main(String[] args) {
                C<String> c = new C<>();
                C<Integer> d = new C<Integer>();
                C<K> e = new C();
                System.out.println(c+"="+d);
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo constructor = typeInfo.findConstructor(0);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            ParameterizedType pt = lvc.localVariable().parameterizedType();
            assertSame(typeInfo, pt.typeInfo());
            assertEquals(1, pt.parameters().size());
            assertSame(runtime.stringTypeInfo(), pt.parameters().getFirst().typeInfo());

            assertSame(constructor, cc.constructor());
            assertSame(typeInfo, cc.parameterizedType().typeInfo());
            assertTrue(cc.diamond().isYes());

            assertEquals("new C<>()", cc.toString());
            assertEquals("C<String> c=new C<>();", lvc.toString());
        } else fail();
        if (main.methodBody().statements().get(1) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            ParameterizedType pt = lvc.localVariable().parameterizedType();

            assertSame(constructor, cc.constructor());
            ParameterizedType ccPt = cc.parameterizedType();
            assertSame(typeInfo, ccPt.typeInfo());
            assertEquals(1, ccPt.parameters().size());
            assertEquals(pt, ccPt);
            assertTrue(cc.diamond().isShowAll());
            assertEquals("new C<Integer>()", cc.toString());
            assertEquals("C<Integer> d=new C<Integer>();", lvc.toString());
        } else fail();

        if (main.methodBody().statements().get(2) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {

            assertSame(constructor, cc.constructor());
            ParameterizedType ccPt = cc.parameterizedType();
            assertSame(typeInfo, ccPt.typeInfo());
            assertEquals(0, ccPt.parameters().size());
            assertTrue(cc.diamond().isNo());
            assertEquals("new C()", cc.toString());
            assertEquals("C<K> e=new C();", lvc.toString());
        } else fail();
    }
}
