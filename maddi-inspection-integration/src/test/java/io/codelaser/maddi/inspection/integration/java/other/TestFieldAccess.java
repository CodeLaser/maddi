/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.ArrayList;
            import java.util.List;
            import java.util.stream.Stream;
            
            import static io.codelaser.maddi.inspection.integration.java.importhelper.RMultiLevel.Effective.E1;
            
            public class FieldAccess_1 {
            
                interface Analyzer {}
            
                abstract static class AbstractAnalyzer implements Analyzer {
                    public final String k = "3";
                    protected final List<String> messages = new ArrayList<>();
            
                    public List<String> getMessages() {
                        return messages;
                    }
                }
            
                abstract static class ParameterAnalyzer extends AbstractAnalyzer {
                    public final String s = "3";
            
                    public Stream<String> streamMessages() {
                        return messages.stream();
                    }
                }
            
                public static class CPA extends ParameterAnalyzer {
                    public final String t = "3";
            
                    public void method() {
                        messages.add("3: "+E1);
                    }
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
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
        javaInspector.parse(INPUT3);
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
        TypeInfo X = javaInspector.parse(INPUT4);
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
        TypeInfo X = javaInspector.parse(INPUT5);
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
                    String s;
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
        TypeInfo C = javaInspector.parse(INPUT6, JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo X = C.findSubType("X");
        FieldInfo s = X.getFieldByName("s", true);
        assertEquals("s", s.name());
        assertEquals("4-16:4-16", s.source().detailedSources().detail(s.name()).compact2());
        TypeInfo Y = C.findSubType("Y");
        MethodInfo someMethod = Y.findUniqueMethod("someMethod", 1);
        BinaryOperator bo = (BinaryOperator) someMethod.methodBody().lastStatement().expression();
        VariableExpression veS = (VariableExpression) bo.lhs();
        assertEquals("8-19:8-19", veS.source().compact2());
        if (veS.variable() instanceof FieldReference fr) {
            assertEquals("8-19:8-19", veS.source().detailedSources().detail(fr.fieldInfo()).compact2());
        }
    }
}
