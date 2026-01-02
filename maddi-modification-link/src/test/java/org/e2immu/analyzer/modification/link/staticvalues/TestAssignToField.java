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
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAssignToField extends CommonTest {
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import org.e2immu.annotation.NotModified;
            import java.util.ArrayList;
            import java.util.List;
            public class X {
                record A(List<String> list1, List<String> list2) {
                }
            
                public A method1(String in1, String in2) {
                    A a = new A(new ArrayList<>(), new ArrayList<>());
                    a.list1.add(in1);
                    int i1 = a.list1.size();
            
                    a.list2.add(in2);
                    int i2 = a.list2.size();
            
                    return a;
                }
            }
            """;

    @DisplayName("direct assignment")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method1 = X.findUniqueMethod("method1", 2);
        MethodLinkedVariables mlv = method1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method1));

        Statement s1 = method1.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo viA = vd1.variableInfo("a");
        assertEquals("a.list1.§$s∋0:in1", viA.linkedVariables().toString());
        assertEquals("[-, -] --> method1.list1.§$s∋0:in1,method1.list2.§$s∋1:in2", mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X<T> {
                record A<T>(List<T> list1, List<T> list2) {
                }
            
                A<T> method1(T in1, T in2) {
                    A<T> a = new A<>(new ArrayList<>(), new ArrayList<>());
                    a.list1.add(in1);
                    int i1 = a.list1.size();
            
                    a.list2.add(in2);
                    int i2 = a.list2.size();
            
                    return a;
                }
            }
            """;

    @DisplayName("direct assignment, type parameter")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method1 = X.findUniqueMethod("method1", 2);
        MethodLinkedVariables mlv = method1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method1));

        Statement s1 = method1.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo viA = vd1.variableInfo("a");
        assertEquals("a.list1.§ts∋0:in1", viA.linkedVariables().toString());

        Statement s2 = method1.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);
        VariableInfo viI1 = vd2.variableInfo("i1");
        assertEquals("-", viI1.linkedVariables().toString());

        Statement s3 = method1.methodBody().statements().get(3);
        VariableData vd3 = VariableDataImpl.of(s3);
        VariableInfo viA3 = vd3.variableInfo("a");
        assertEquals("a.list1.§ts∋0:in1,a.list2.§ts∋1:in2", viA3.linkedVariables().toString());

        assertEquals("[-, -] --> method1.list1.§ts∋0:in1,method1.list2.§ts∋1:in2", mlv.toString());
    }

}
