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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The implicit {@code this} receiver of an unqualified instance-method call inside an anonymous class must be the
 * enclosing instance that declares (or inherits) the method, not the anonymous type itself. javac models the
 * receiver as the innermost type; {@code ScanCompilationUnit} corrects this by walking the enclosing-type chain.
 */
public class TestImplicitThisEnclosing extends CommonTest {

    // the run() method of the anonymous class that is the argument of 'new Runnable(){...}' returned by 'method'
    private static MethodInfo anonRun(MethodInfo method) {
        ConstructorCall cc = (ConstructorCall) ((ReturnStatement) method.methodBody().statements().getFirst()).expression();
        return cc.anonymousClass().findUniqueMethod("run", 0);
    }

    private static TypeInfo implicitThisOf(MethodInfo runMethod, String calledMethodName) {
        for (Statement s : runMethod.methodBody().statements()) {
            Expression e = s.expression();
            if (e instanceof MethodCall mc && mc.objectIsImplicit() && mc.methodInfo().name().equals(calledMethodName)) {
                return ((This) ((VariableExpression) mc.object()).variable()).typeInfo();
            }
        }
        throw new AssertionError("no implicit call to " + calledMethodName);
    }

    @Language("java")
    private static final String DIRECT = """
            package a.b;
            public class X {
                void m() { }
                Runnable r() {
                    return new Runnable() { public void run() { m(); } };
                }
            }
            """;

    @DisplayName("anonymous class calling a method of the directly enclosing type")
    @Test
    public void testDirect() {
        TypeInfo X = scan("a.b.X", DIRECT);
        TypeInfo thisType = implicitThisOf(anonRun(X.findUniqueMethod("r", 0)), "m");
        assertEquals("a.b.X", thisType.fullyQualifiedName());
    }

    @Language("java")
    private static final String NESTED = """
            package a.b;
            public class X {
                void m() { }
                Runnable r() {
                    return new Runnable() {
                        public void run() {
                            Runnable inner = new Runnable() { public void run() { m(); } };
                        }
                    };
                }
            }
            """;

    @DisplayName("anonymous class two levels deep calling a method of the outer type")
    @Test
    public void testNested() {
        TypeInfo X = scan("a.b.X", NESTED);
        MethodInfo outerRun = anonRun(X.findUniqueMethod("r", 0));
        // the inner anonymous Runnable, assigned to local variable 'inner'
        ConstructorCall innerCc = (ConstructorCall) ((LocalVariableCreation) outerRun.methodBody().statements().getFirst())
                .localVariable().assignmentExpression();
        MethodInfo innerRun = innerCc.anonymousClass().findUniqueMethod("run", 0);
        TypeInfo thisType = implicitThisOf(innerRun, "m");
        assertEquals("a.b.X", thisType.fullyQualifiedName());
    }

    @Language("java")
    private static final String INHERITED = """
            package a.b;
            public class W {
                static class Base { void m() { } }
                static class X extends Base {
                    Runnable r() {
                        return new Runnable() { public void run() { m(); } };
                    }
                }
            }
            """;

    @DisplayName("anonymous class calling a method INHERITED by the enclosing type from a superclass")
    @Test
    public void testInherited() {
        TypeInfo W = scan("a.b.W", INHERITED);
        TypeInfo X = W.findSubType("X");
        MethodInfo m = W.findSubType("Base").findUniqueMethod("m", 0);
        TypeInfo thisType = implicitThisOf(anonRun(X.findUniqueMethod("r", 0)), "m");
        // the receiver is the enclosing X (which inherits m), not the anonymous type and not Base (the declarer)
        assertEquals("a.b.W.X", thisType.fullyQualifiedName());
        assertEquals("a.b.W.Base", m.typeInfo().fullyQualifiedName());
        assertTrue(thisType != m.typeInfo());
    }
}
