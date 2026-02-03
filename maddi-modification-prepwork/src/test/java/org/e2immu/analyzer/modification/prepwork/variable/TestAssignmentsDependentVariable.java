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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAssignmentsDependentVariable extends CommonTest {

    @Language("java")
    String A = """
            package a;
            import java.util.Arrays;
            import java.util.List;
            
            public class A {
                public void m(List<String> strings) {
                    double[] x = strings.stream().mapToDouble(String::length).toArray();
                    double[] xCopy = Arrays.copyOf(x, x.length);
                    double xStart = xCopy[0];
                    double xEnd = xCopy[xCopy.length - 1];
                }
            }
            """;


    @DisplayName("index of creation of dependent variable")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(A);
        MethodInfo method = X.findUniqueMethod("m", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vd3 = VariableDataImpl.of((method.methodBody().statements().getLast()));
        assertEquals("a.A.m(java.util.List<String>):0:strings, x, xCopy, xCopy[0], xCopy[`10-29`], xEnd, xStart",
                vd3.knownVariableNamesToString());
        VariableInfo viXCopy = vd3.variableInfo("xCopy");
        assertEquals("D:1, A:[1]", viXCopy.assignments().toString());

        // the variable xCopy[xCopy.length-1] is not explicitly created in the code, but it appears
        // for the first time in statement 3
        VariableInfo viXCopyIndexed = vd3.variableInfo("xCopy[`10-29`]");
        assertEquals("xCopy[xCopy.length-1]", viXCopyIndexed.variable().toString());
        assertEquals("D:1, A:[]", viXCopyIndexed.assignments().toString());
    }
}
