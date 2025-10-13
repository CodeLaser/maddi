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

package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.impl.MethodModAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.impl.ModAnalyzerForTesting;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.LINKED_VARIABLES_ARGUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTrackObjectCreation extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            import java.util.ArrayList;
            import java.util.LinkedList;
            import java.util.List;
            public class X<T> {
                private static final Logger LOGGER = LoggerFactory.getLogger(X.class);
                public static List<T> makeList() {
                    return new LinkedList<>();
                }
                public static List<T> makeList2() {
                    List<T> list = new LinkedList<>();
                    LOGGER.info("List {}", list);
                    return list;
                }
                public static List<T> makeList3(boolean b) {
                    if(b) {
                        List<T> list = new LinkedList<>();
                        LOGGER.info("List {}", list);
                        return list;
                    }
                    return null;
                }
                public static List<T> makeList4(boolean b) {
                    if(b) {
                        List<T> list = new LinkedList<>();
                        LOGGER.info("List {}", list);
                        return list;
                    }
                    return new ArrayList<>();
                }
            }
            """;

    @DisplayName("basics of creation to return value")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        analyze(X);
        {
            MethodInfo makeList = X.findUniqueMethod("makeList", 0);
            VariableData vdMethod = VariableDataImpl.of(makeList.methodBody().lastStatement());
            assertEquals("a.b.X.makeList(), oc:10-16:java.util.LinkedList<T>", vdMethod.knownVariableNamesToString());

            VariableInfo viRv = vdMethod.variableInfo(makeList.fullyQualifiedName());
            assertEquals("-1-:oc:10-16", viRv.linkedVariables().toString());
        }
        {
            MethodInfo makeList = X.findUniqueMethod("makeList2", 0);
            VariableData vdMethod = VariableDataImpl.of(makeList.methodBody().lastStatement());
            assertEquals("a.b.X.LOGGER, a.b.X.makeList2(), list, oc:13-24:java.util.LinkedList<T>",
                    vdMethod.knownVariableNamesToString());

            VariableInfo viRv = vdMethod.variableInfo(makeList.fullyQualifiedName());
            assertEquals("-1-:list, -1-:oc:13-24", viRv.linkedVariables().toString());
        }
        {
            MethodInfo makeList = X.findUniqueMethod("makeList3", 1);
            VariableData vdMethod = VariableDataImpl.of(makeList.methodBody().lastStatement());
            assertEquals("""
                    a.b.X.LOGGER, a.b.X.makeList3(boolean), a.b.X.makeList3(boolean):0:b, \
                    oc:19-28:java.util.LinkedList<T>\
                    """, vdMethod.knownVariableNamesToString());

            VariableInfo viRv = vdMethod.variableInfo(makeList.fullyQualifiedName());
            assertEquals("-1-:oc:19-28", viRv.linkedVariables().toString());
        }
        {
            MethodInfo makeList = X.findUniqueMethod("makeList4", 1);
            VariableData vdMethod = VariableDataImpl.of(makeList.methodBody().lastStatement());
            assertEquals("""
                    a.b.X.LOGGER, a.b.X.makeList4(boolean), a.b.X.makeList4(boolean):0:b, \
                    oc:27-28:java.util.LinkedList<T>, oc:31-16:java.util.ArrayList<T>\
                    """, vdMethod.knownVariableNamesToString());

            VariableInfo viRv = vdMethod.variableInfo(makeList.fullyQualifiedName());
            assertEquals("-1-:oc:27-28, -1-:oc:31-16", viRv.linkedVariables().toString());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            public class X {
                static class M { int i; M(int i) { this.i = i; } void setI(int i) { this.i = i; } }
                static List<M> makeList(int i) {
                    M m = new M(i);
                    return List.of(m);
                }
                static List<M> makeList2(int i) {
                    return List.of(new M(i));
                }
            }
            """;

    @DisplayName("creation to return value via immutable HC")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        analyze(X);
        {
            MethodInfo makeList = X.findUniqueMethod("makeList", 1);
            VariableData vd0 = VariableDataImpl.of(makeList.methodBody().statements().getFirst());
            assertEquals("a.b.X.makeList(int):0:i, m, oc:6-15:a.b.X.M", vd0.knownVariableNamesToString());

            VariableInfo viM = vd0.variableInfo("m");
            assertEquals("-1-:oc:6-15", viM.linkedVariables().toString());

            Statement lastStatement = makeList.methodBody().lastStatement();
            MethodCall mc = (MethodCall) lastStatement.expression();
            assertEquals("java.util.List.of(E)", mc.methodInfo().fullyQualifiedName());
            ParameterInfo p0 = mc.methodInfo().parameters().getFirst();
            Value.Independent independent = p0.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT);
            assertEquals(1, independent.linkToParametersReturnValue().get(-1));

            VariableData vd1 = VariableDataImpl.of(lastStatement);
            assertEquals("0M-4-*M:m, 0M-4-*M:oc:6-15",
                    vd1.variableInfo(makeList.fullyQualifiedName()).linkedVariables().toString());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            public class X {
                static class M { int i; M(int i) { this.i = i; } void setI(int i) { this.i = i; } }
                static List<M> makeList(int i) {
                    M m = new M(i);
                    return go(m);
                }
                static M go(M m) {
                    m.setI(3);
                    return m;
                }
            }
            """;

    @DisplayName("basics of creation to argument")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        analyze(X);
        {
            MethodInfo makeList = X.findUniqueMethod("makeList", 1);

            Statement lastStatement = makeList.methodBody().lastStatement();
            MethodCall mc = (MethodCall) lastStatement.expression();
            assertEquals("a.b.X.go(a.b.X.M)", mc.methodInfo().fullyQualifiedName());
            LinkedVariables.ListOfLinkedVariables list = mc.analysis().getOrDefault(LINKED_VARIABLES_ARGUMENTS,
                    LinkedVariablesImpl.ListOfLinkedVariablesImpl.EMPTY);
            assertEquals("ListOfLinkedVariablesImpl[list=[-1-:m]]", list.toString());
            VariableInfo viM = VariableDataImpl.of(lastStatement).variableInfo("m");
            assertEquals("-1-:oc:6-15", viM.linkedVariables().toString());
        }
    }

    private void analyze(TypeInfo X) {
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setTrackObjectCreations(true).build());
        List<Info> analysisOrder = prepAnalyzer.doPrimaryType(X);
        IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setTrackObjectCreations(true)
                .build();
        ModAnalyzerForTesting analyzer = new MethodModAnalyzerImpl(runtime, configuration);
        analyzer.go(analysisOrder);
    }
}
