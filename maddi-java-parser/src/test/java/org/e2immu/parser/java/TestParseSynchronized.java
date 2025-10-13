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

import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.SynchronizedStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseSynchronized extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public synchronized void method1() {
                 method2();
              }
              private final String s = "?";
              public void method2() {
                synchronized (s) {
                  System.out.println(s);
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo m1 = typeInfo.findUniqueMethod("method1", 0);
        assertTrue(m1.isSynchronized());
        MethodInfo m2 = typeInfo.findUniqueMethod("method2", 0);
        assertFalse(m2.isSynchronized());
        FieldInfo fieldInfo = typeInfo.getFieldByName("s", true);
        if (m2.methodBody().statements().get(0) instanceof SynchronizedStatement s) {
            if (s.expression() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                assertSame(fieldInfo, fr.fieldInfo());
            } else fail();
            assertEquals(1, s.block().size());
        }
    }
}
