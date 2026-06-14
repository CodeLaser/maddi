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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFieldAccess extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            
            
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            
            public class FieldAccess_0 {
            
                interface Analysis {
                }
            
                interface ParameterAnalysis extends Analysis {
            
                }
            
                static abstract class AnalysisImpl implements Analysis {
                    Set<Integer> properties = new HashSet<>();
                }
            
                static abstract class AbstractAnalysisBuilder implements Analysis {
                    Map<Integer, String> properties = new HashMap<>();
                }
            
                static class ParameterAnalysisImpl extends AnalysisImpl implements ParameterAnalysis {
            
                    static class Builder extends AbstractAnalysisBuilder implements ParameterAnalysis {
            
                    }
                }
            
                static class Test1 {
                    ParameterAnalysisImpl parameterAnalysis = new ParameterAnalysisImpl();
            
                    public boolean method(int i) {
                        return parameterAnalysis.properties.contains(i);
                    }
                }
            
                static class Test2 {
                    ParameterAnalysisImpl.Builder parameterAnalysis2 = new ParameterAnalysisImpl.Builder();
            
                    public boolean method(int i) {
                        return parameterAnalysis2.properties.containsKey(i);
                    }
                }
            }
            """;

    @Test
    public void test1() {
        // tests that we can find the field higher up in the hierarchy
        scan("org.e2immu.analyser.resolver.testexample.FieldAccess_0", INPUT1);
    }

    @Language("java")
    private static final String R_MULTILEVEL = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public class RMultiLevel {
                public enum Effective {
                    E1, E2;
                    public static Effective of(int index) {
                        return index == 1 ? E1: E2;
                    }
                }
                public enum Level {
                    ONE, TWO, THREE
                }
            }
            """;

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.ArrayList;
            import java.util.List;
            import java.util.stream.Stream;
            
            import static org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel.Effective.E1;
            
            public class FieldAccess_1 {
            
                interface Analyser {}
            
                abstract static class AbstractAnalyser implements Analyser {
                    public final String k = "3";
                    protected final List<String> messages = new ArrayList<>();
            
                    public List<String> getMessages() {
                        return messages;
                    }
                }
            
                abstract static class ParameterAnalyser extends AbstractAnalyser {
                    public final String s = "3";
            
                    public Stream<String> streamMessages() {
                        return messages.stream();
                    }
                }
            
                public static class CPA extends ParameterAnalyser {
                    public final String t = "3";
            
                    public void method() {
                        messages.add("3: "+E1);
                    }
                }
            }
            """;

    @Test
    public void test2() {
        scan(false,
                "org.e2immu.language.inspection.integration.java.importhelper.RMultiLevel", R_MULTILEVEL,
                "org.e2immu.analyser.resolver.testexample.FieldAccess_1", INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.Map;
            import java.util.stream.Stream;
            
            public record FieldAccess_2(Container c) {
            
                interface VIC {
                    String current();
                }
            
                record Variables(Map<String, VIC> variables) {
                    Stream<Map.Entry<String, VIC>> stream() {
                        return variables.entrySet().stream();
                    }
                }
            
                record Container(Variables v) {
            
                }
            
                public void test() {
                    c.v.stream().map(Map.Entry::getValue).forEach(vic -> System.out.println(vic.current()));
                }
            
                public void test2() {
                    c.v.stream().map(java.util.Map.Entry::getValue).forEach(vic -> System.out.println(vic.current()));
                }
            
            }
            """;

    @Test
    public void test3() {
        scan("org.e2immu.analyser.resolver.testexample.FieldAccess_2", INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.text.CharacterIterator;
            import java.text.StringCharacterIterator;
            public class X {
                public static String forRegex(String s) {
                    final StringBuilder result = new StringBuilder();
                    final StringCharacterIterator iterator = new StringCharacterIterator(s);
                    char character = iterator.current();
                    while (character != StringCharacterIterator.DONE) {
                        if (character == '.') {
                            result.append("\\\\.");
                        } else {
                            result.append(CharacterIterator.DONE);
                        }
                        character = iterator.next();
                    }
                    return result.toString();
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo X = scan("a.b.X", INPUT4);
        List<Element.TypeReference> typeReferences = X.typesReferenced(null)
                .filter(Element.TypeReference::explicit)
                .filter(tr -> "java.text".equals(tr.typeInfo().packageName()))
                .distinct()
                .toList();
        assertEquals("""
                [java.text.StringCharacterIterator[E], java.text.CharacterIterator[E]]\
                """, typeReferences.toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            public class X {
                int length;
                int getLength() { return length; }
                void setLength(int length) {
                    this.length = length;
                }
                static X create(int v) {
                    X x = new X();
                    x.length = v;
                    return x;
                }
            }
            """;

    @Test
    public void test5() {
        TypeInfo X = scan("a.b.X", INPUT5);
        FieldInfo length = X.getFieldByName("length", true);
        assertEquals("length", length.name());
        MethodInfo create = X.findUniqueMethod("create", 1);
        Assignment assignment = (Assignment) create.methodBody().statements().get(1).expression();
        assertEquals("a.b.X.length#x", assignment.variableTarget().fullyQualifiedName());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            class C {
                static class X {
                    String s = null;
                }
                static class Y extends X {
                    String someMethod(String string) {
                       return s + string;
                    }
                }
            }
            """;

    @Test
    public void test6() {
        TypeInfo C = scan("a.b.C", INPUT6);
        TypeInfo X = C.findSubType("X");
        FieldInfo s = X.getFieldByName("s", true);
        assertEquals("s", s.name());
        assertEquals("4-9:4-24", s.source().detailedSources().detail(DetailedSources.FIELD_DECLARATION).compact2());
        assertEquals("4-16:4-23", s.source().compact2());
        assertEquals("4-16:4-16", s.source().detailedSources().detail(s.name()).compact2());
        assertEquals("4-9:4-14", s.source().detailedSources().detail(s.type()).compact2());

        TypeInfo Y = C.findSubType("Y");
        MethodInfo someMethod = Y.findUniqueMethod("someMethod", 1);
        BinaryOperator bo = (BinaryOperator) someMethod.methodBody().lastStatement().expression();
        VariableExpression veS = (VariableExpression) bo.lhs();
        assertEquals("8-19:8-19", veS.source().compact2());
        if (veS.variable() instanceof FieldReference fr) {
            assertEquals("8-19:8-19", veS.source().detailedSources().detail(fr.fieldInfo()).compact2());
        }
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            class C {
                String t;
                static class X {
                    String s;
                }
                class Y extends X {
                    String someMethod(String string) {
                        return C.this.t + string;
                    }
                }
            }
            """;

    @Test
    public void test7() {
        TypeInfo C = scan("a.b.C", INPUT7);
        TypeInfo Y = C.findSubType("Y");
        MethodInfo someMethod = Y.findUniqueMethod("someMethod", 1);
        BinaryOperator bo = (BinaryOperator) someMethod.methodBody().lastStatement().expression();
        VariableExpression veS = (VariableExpression) bo.lhs();
        assertEquals("9-20:9-27", veS.source().compact2());
        if (veS.variable() instanceof FieldReference fr) {
            assertEquals("9-27:9-27", veS.source().detailedSources().detail(fr.fieldInfo()).compact2());
        }
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            class C {
                public final String s;
                C(String s) {
                    this.s = s;
                }
            }
            """;

    @Test
    public void test8() {
        TypeInfo typeInfo = scan("a.b.C", INPUT8);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo C = typeInfo.findConstructor(1);
        ExpressionAsStatement c0 = (ExpressionAsStatement) C.methodBody().statements().get(1);
        Assignment a = (Assignment) c0.expression();
        FieldReference thisS = (FieldReference) a.variableTarget();

        // s
        assertEquals("5-14:5-14", a.target().source().detailedSources().detail(thisS.fieldInfo()).compact2());

        // this
        VariableExpression ve = (VariableExpression) thisS.scope();
        assertEquals("5-9:5-12", ve.source().compact2());
        DetailedSources ds = ve.source().detailedSources();
        This thisVar = (This) (ve.variable());
        assertEquals("5-9:5-12", ds.detail(thisVar).compact2());
    }

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.List;
            class C {
              List<String> stringList;
              List<Integer> intList1 = List.of(), intList2, intList3 = null;
              int i, iArray[], j;
            }
            """;

    @Test
    public void test9() {
        TypeInfo typeInfo = scan("a.b.C", INPUT9);
        assertEquals("C", typeInfo.simpleName());

        FieldInfo stringList = typeInfo.fields().get(0);
        assertEquals("Type java.util.List<String>", stringList.type().toString());

        FieldInfo intList1 = typeInfo.fields().get(1);
        assertEquals("Type java.util.List<Integer>", intList1.type().toString());
        assertEquals("List.of()", intList1.initializer().toString());

        FieldInfo intList2 = typeInfo.fields().get(2);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertTrue(intList2.initializer().isEmpty());

        FieldInfo intList3 = typeInfo.fields().get(3);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertEquals("null", intList3.initializer().toString());

        FieldInfo iArray = typeInfo.getFieldByName("iArray", true);
        assertEquals("Type int[]", iArray.type().toString());
        FieldInfo j = typeInfo.getFieldByName("j", true);
        assertEquals("Type int", j.type().toString());
    }


    @Language("java")
    private static final String INPUT10 = """
            package a.b;
            class B {
                // max with comment
                final static int MAX = 3, MIN = 2;
                public boolean m(int j) {
                  return B.MAX < j;
                }
            }
            """;

    @Test
    public void test10() {
        TypeInfo typeInfo = scan("a.b.B", INPUT10);
        assertEquals("B", typeInfo.simpleName());
        FieldInfo max = typeInfo.getFieldByName("MAX", true);
        FieldInfo min = typeInfo.getFieldByName("MIN", true);

        assertEquals("4-22:4-28", max.source().compact2()); // MAX = 3
        assertEquals("4-31:4-37", min.source().compact2()); // MIN = 2

        DetailedSources maxDs = max.source().detailedSources();
        assertEquals("4-22:4-24", maxDs.detail(max.name()).compact2());
        // NOTE difference with maddi implementation: comment is not included in FIELD_DECLARATION
        // NOTE difference with maddi implementation: first field = FIELD_DECLARATION up to first...
        assertEquals("4-5:4-29", maxDs.detail(DetailedSources.FIELD_DECLARATION).compact2());

        DetailedSources minDs = min.source().detailedSources();
        assertEquals("4-31:4-33", minDs.detail(min.name()).compact2());
        assertEquals("4-5:4-38", minDs.detail(DetailedSources.FIELD_DECLARATION).compact2());

        Statement s0 = typeInfo.findUniqueMethod("m", 1).methodBody().statements().getFirst();
        if (s0.expression() instanceof BinaryOperator bo) {
            assertEquals("B.MAX", bo.lhs().toString());
            if (bo.lhs() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                assertEquals("B", fr.scope().toString());
                assertEquals("6-14:6-14", fr.scope().source().compact2());
            } else fail();
        } else fail("Have " + s0.expression());

    }


    @Language("java")
    private static final String INPUT11 = """
            package a.b;
            class B {
                final int min = 5; // min with comment
                final static int MAX = 3; // max with comment
                public boolean m(int j) {
                  return B.MAX < j;
                }
            }
            """;

    // NOTE: there is code in ParseFieldDeclaration for exactly
    @Test
    public void test11() {
        TypeInfo typeInfo = scan("a.b.B", INPUT11);
        assertEquals("B", typeInfo.simpleName());

        FieldInfo min = typeInfo.getFieldByName("min", true);
        assertEquals(1, min.comments().size());
        assertEquals(" min with comment", min.comments().getFirst().comment());
        FieldInfo max = typeInfo.getFieldByName("MAX", true);
        assertEquals(1, max.comments().size());
        assertEquals(" max with comment", max.comments().getFirst().comment());
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        assertTrue(m.comments().isEmpty());
    }
}
