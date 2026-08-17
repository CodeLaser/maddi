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

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseSplitExpression0 extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            public abstract class SplitExpression_0 {

                private final int base;
                private int j;

                public SplitExpression_0(int base) {
                    this.base = base;
                }

                protected abstract int method(int i1, int i2, int i3, int i4);

                private int compute(int i) {
                    return (int) Math.pow(base, i);
                }

                public int same1(int k) {
                    return method(compute(3), compute(k), j = k + 2, compute(j));
                }
                        
                public int same2(int k) {
                    int c3 = compute(3);
                    int ck = compute(k);
                    int cj = j = k + 2;
                    int c2 = compute(j);
                    return method(c3, ck, cj, c2);
                }

                public int same3(int k) {
                    int ck = compute(k);
                    int a = j = k + 2;
                    int c2 = compute(j);
                    int c3 = compute(3);
                    return method(c3, ck, a, c2);
                }

                public int same5(int k) {
                    int ck = compute(k);
                    int c3 = compute(3);
                    int a = j = k + 2;
                    int cj = compute(j);
                    // note: the + in +a gets evaluated away
                    return method(c3, ck, +a, cj);
                }

                public int same6(int k) {
                    // note: 2+1 gets evaluated to 3
                    int c3 = compute(2+1);
                    return method(c3, compute(k), (j = k + 2), compute(j));
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertEquals("SplitExpression_0", typeInfo.fullyQualifiedName());
        assertEquals(2, typeInfo.fields().size());
        FieldInfo base = typeInfo.fields().get(0);
        assertTrue(base.isFinal());
        FieldInfo j = typeInfo.fields().get(1);
        assertFalse(j.isFinal());

        assertEquals(7, typeInfo.methods().size());
        assertEquals(1, typeInfo.constructors().size());

        MethodInfo constructor = typeInfo.findConstructor(1);
        assertSame(constructor, typeInfo.constructors().get(0));
        assertTrue(constructor.isPublic());
        assertEquals(1, constructor.methodBody().size());
        if (constructor.methodBody().statements().get(0) instanceof ExpressionAsStatement eas
            && eas.expression() instanceof Assignment assignment) {
            assertEquals("this.base=base", assignment.toString());
            if (assignment.variableTarget() instanceof FieldReference fr) {
                assertTrue(fr.scopeIsThis());
                assertEquals("base", fr.fieldInfo().name());
                assertSame(base, fr.fieldInfo());
            } else fail();
            if (assignment.value() instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo pi) {
                assertSame(pi, constructor.parameters().get(0));
            } else fail();
        } else fail();


        MethodInfo method = typeInfo.findUniqueMethod("method", 4);
        assertTrue(method.isAbstract());

        MethodInfo compute = typeInfo.findUniqueMethod("compute", 1);
        assertEquals(1, compute.methodBody().size());
        if (compute.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof Cast cast
            && cast.expression() instanceof MethodCall mc) {
            assertSame(runtime.intTypeInfo(), cast.parameterizedType().typeInfo());
            assertEquals(2, mc.parameterExpressions().size());
            if (mc.object() instanceof TypeExpression te) {
                assertEquals("java.lang.Math", te.parameterizedType().typeInfo().fullyQualifiedName());
            } else fail();
        } else fail();

        MethodInfo same1 = typeInfo.findUniqueMethod("same1", 1);
        assertEquals("SplitExpression_0.same1(int):0:k", same1.parameters().get(0).fullyQualifiedName());

        MethodInfo same2 = typeInfo.findUniqueMethod("same2", 1);
        assertEquals("SplitExpression_0.same2(int):0:k", same2.parameters().get(0).fullyQualifiedName());

        if (same1.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof MethodCall mc) {
            Expression arg1 = mc.parameterExpressions().get(1);
            if (arg1 instanceof MethodCall mc2 && mc2.parameterExpressions().get(0) instanceof VariableExpression ve) {
                assertEquals("SplitExpression_0.same1(int):0:k", ve.variable().fullyQualifiedName());
            }
        } else fail();

        MethodInfo same6 = typeInfo.findUniqueMethod("same6", 1);
        assertEquals(2, same6.methodBody().size());
        if (same6.methodBody().statements().get(1) instanceof ReturnStatement rs
            && rs.expression() instanceof MethodCall mc) {
            assertEquals(4, mc.parameterExpressions().size());
            if (mc.parameterExpressions().get(2) instanceof EnclosedExpression ee) {
                assertEquals("this.j=k+2", ee.inner().toString());
                if (ee.inner() instanceof Assignment assignment) {
                    assertEquals("j", assignment.variableTarget().simpleName());
                    if (assignment.variableTarget() instanceof FieldReference fr) {
                        assertSame(runtime.intTypeInfo(), fr.parameterizedType().typeInfo());
                    } else fail();
                    assertSame(runtime.intTypeInfo(), assignment.parameterizedType().typeInfo());
                } else fail();
            }
        } else fail();
    }
}
