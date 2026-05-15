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

package org.e2immu.language.inspection.openjdk.expression;

import org.e2immu.language.cst.api.expression.Cast;
import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestInstanceOf extends CommonTest {
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              interface I {
                 String map(C c);
              }
              String method(Object o, C c) {
                if(o instanceof I) {
                    I i = (I)o;
                    return i.map(c);
                }
                return null;
              }
              String method2(Object o, C c) {
                if(o instanceof I i) {
                    return i.map(c);
                }
                return null;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        if (method.methodBody().statements().getFirst() instanceof IfElseStatement ifElse) {
            if (ifElse.expression() instanceof InstanceOf io) {
                assertNull(io.patternVariable());
                assertEquals("Type a.b.C.I", io.testType().toString());
                assertEquals("Type boolean", io.parameterizedType().toString());
            } else fail();
            if (ifElse.block().statements().getFirst() instanceof LocalVariableCreation lvc) {
                if (lvc.localVariable().assignmentExpression() instanceof Cast cast) {
                    assertEquals("o", cast.expression().toString());
                    assertEquals("Type a.b.C.I", cast.parameterizedType().toString());
                }
            } else fail();
        } else fail();

        MethodInfo method2 = typeInfo.findUniqueMethod("method2", 2);
        if (method2.methodBody().statements().getFirst() instanceof IfElseStatement ifElse) {
            if (ifElse.expression() instanceof InstanceOf io) {
                assertEquals("i", io.patternVariable().localVariable().simpleName());
            } else fail();
            if (ifElse.block().statements().getFirst() instanceof ReturnStatement rs) {
                if (rs.expression() instanceof MethodCall mc) {
                    if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv) {
                        assertEquals("i", lv.simpleName());
                        assertEquals("Type a.b.C.I", lv.parameterizedType().toString());
                    } else fail();
                } else fail();
            } else fail();
        } else fail();
    }
}
