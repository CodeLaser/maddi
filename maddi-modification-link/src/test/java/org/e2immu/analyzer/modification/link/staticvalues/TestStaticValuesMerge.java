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

package org.e2immu.analyzer.modification.link.staticvalues;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStaticValuesMerge extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                void method(int[] array) {
                    int y = array[0];
                    if(array[1]>10) {
                        y += 9;
                    }
                    System.out.println(array[0] +" ? "+y);
                    array[0] = y;
                }
            }
            """;

    @DisplayName("test the 'erase' system, 1")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo vi0y = vd0.variableInfo("y");
        assertEquals("y<0:array,y==0:array[0]", vi0y.linkedVariables().toString());

        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd100 = VariableDataImpl.of(s1.block().statements().getFirst());
        VariableInfo vi100y = vd100.variableInfo("y");
        assertEquals("-", vi100y.linkedVariables().toString()); // multiple values

        Statement s2 = method.methodBody().statements().get(2);
        VariableData vd1 = VariableDataImpl.of(s2);
        VariableInfo vi1y = vd1.variableInfo("y");
                assertEquals("-", vi1y.linkedVariables().toString()); // multiple values

    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                void method(int[] array) {
                    int y = array[0];
                    if(array[1]>10) {
                        y = 9 * array[3] - array[4];
                    }
                    System.out.println(array[0] +" ? "+y);
                    array[0] = y;
                }
            }
            """;

    @DisplayName("test the 'erase' system, 2")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo vi0y = vd0.variableInfo("y");
        assertEquals("y<0:array,y==0:array[0]", vi0y.linkedVariables().toString());

        VariableData vd100 = VariableDataImpl.of(method.methodBody().statements().get(1).block().statements().getFirst());
        VariableInfo vi100y = vd100.variableInfo("y");
        assertEquals("-", vi100y.linkedVariables().toString()); // binary operator

        VariableData vd2 = VariableDataImpl.of(method.methodBody().statements().get(2));
        VariableInfo vi1y = vd2.variableInfo("y");
        assertEquals("-", vi1y.linkedVariables().toString()); // value + empty -> empty

    }

}
