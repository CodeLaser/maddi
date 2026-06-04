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

package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ForStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestForInitializer extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public void method(String[] strings) {
                    for(int j=0; j<strings.length; ++j) {
                        System.out.println("j = "+j);
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT1);

    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                public void method(String[] strings) {
                    for(int j=0, next=j+1; j<strings.length; ++j) {
                        System.out.println("j = "+j+", next = "+next);
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = scan("a.b.X", INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 1);
        ForStatement fs = (ForStatement) method.methodBody().statements().getFirst();
        assertEquals(1, fs.initializers().size());
        if (fs.initializers().getFirst() instanceof LocalVariableCreation lvc) {
            assertEquals(2, lvc.newLocalVariables().size());
        }
    }
}
