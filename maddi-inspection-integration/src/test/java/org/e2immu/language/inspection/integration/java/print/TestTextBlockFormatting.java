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

package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.expression.TextBlock;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestTextBlockFormatting extends CommonTest {


    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            public class X {
                public void method() {
                    String s = \"""
                        abc\\
                        def
                        123
                        \""";
                }
            }
            """;
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = X.findUniqueMethod("method", 0);
        LocalVariableCreation lvc  = (LocalVariableCreation)  methodInfo.methodBody().statements().get(0);
        TextBlock tb = (TextBlock) lvc.localVariable().assignmentExpression();
        assertNotNull(tb.textBlockFormatting());
        String s = javaInspector.print2(X);
        @Language("java")
        String expect = """
            package a.b;
            public class X {
                public void method() {
                    String s = \"""
                            abc\\
                            def
                            123
                            \""";
                }
            }
            """;
        assertEquals(expect, s);
    }

}
