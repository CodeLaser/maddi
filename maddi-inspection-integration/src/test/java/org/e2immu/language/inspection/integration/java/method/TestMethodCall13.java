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

package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestMethodCall13 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import java.util.Map;
            import java.util.stream.Collectors;
            
            class X {
                interface ClinicalDataCount { 
                    boolean accept();
                }
                interface ClinicalDataCountItem {
                     List<ClinicalDataCount> getCounts();
                     void setCounts(List<ClinicalDataCount> list);
                     String getAttributeId();
                 }
            
                public Map<String, ClinicalDataCountItem> method(List<ClinicalDataCountItem> clinicalDataCountItems) {
                    return clinicalDataCountItems.stream()
                          // Exclude NA category
                          .map(
                              clinicalDataCountItem -> {
                                List<ClinicalDataCount> filteredClinicalDataCount =
                                    clinicalDataCountItem.getCounts().stream()
                                        .filter(
                                            clinicalDataCount -> {
                                              if(clinicalDataCount.accept()) {
                                                  return true;
                                              }
                                              return false;
                                            })
                                        .collect(Collectors.toList());
                                clinicalDataCountItem.setCounts(filteredClinicalDataCount);
                                return clinicalDataCountItem;
                              })
                          .collect(
                              Collectors.toMap(
                                  clinicalDataCountItem -> clinicalDataCountItem.getAttributeId(),
                                  clinicalDataCountItem -> clinicalDataCountItem));
                   }
               }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        Expression expression = method.methodBody().statements().getFirst().expression();
        assertNotNull(expression);
    }


    @Language("java")
    private static final String INPUT1b = """
            package a.b;
            import java.util.List;
            import java.util.Map;
            import java.util.stream.Collectors;
            
            class X {
                interface ClinicalDataCount {
                    boolean accept();
                }
                interface ClinicalDataCountItem {
                     List<ClinicalDataCount> getCounts();
                     void setCounts(List<ClinicalDataCount> list);
                     String getAttributeId();
                 }
            
                public Map<String, ClinicalDataCountItem> method(List<ClinicalDataCountItem> clinicalDataCountItems) {
                    return clinicalDataCountItems.stream()
                          .map(clinicalDataCountItem -> {
                                clinicalDataCountItem.setCounts(List.of());
                                return clinicalDataCountItem;
                              })
                          .collect(
                              Collectors.toMap(
                                  clinicalDataCountItem -> clinicalDataCountItem.getAttributeId(),
                                  clinicalDataCountItem -> clinicalDataCountItem));
                   }
               }
            """;

    @Test
    public void test1b() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1b);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        Expression expression = method.methodBody().statements().getFirst().expression();
        assertNotNull(expression);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            public class C {
                public <Y> Y identity(Y y)  { return y; }
                public <X> List<X> method(List<X> list) {
                    return list.stream().map(this::identity).toList();
                }
                public <X> List<X> method2(List<X> list) {
                    Stream<X> stream = list.stream().map(this::identity);
                    return stream.toList();
                }
            }
            """;

    @DisplayName("identity function")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);
        {
            MethodInfo method = C.findUniqueMethod("method", 1);
            MethodCall mc = (MethodCall) method.methodBody().statements().getFirst().expression();
            assertNotNull(mc);
            assertEquals("java.util.List<X>", mc.parameterizedType().detailedString());
            assertEquals("Type java.util.List<X>", mc.concreteReturnType().toString());
            MethodCall mc2 = (MethodCall) mc.object();
            assertEquals("Type java.util.stream.Stream<X>", mc2.concreteReturnType().toString());
        }
        {
            MethodInfo method = C.findUniqueMethod("method2", 1);
            LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            MethodCall mc = (MethodCall) lvc.localVariable().assignmentExpression();
            assertNotNull(mc);
            assertEquals("java.util.stream.Stream<X>", mc.parameterizedType().detailedString());
            assertEquals("Type java.util.stream.Stream<X>", mc.concreteReturnType().toString());

            MethodCall mc2 = (MethodCall) mc.object();
            assertEquals("Type java.util.stream.Stream<X>", mc2.concreteReturnType().toString());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            public class C {
                <Y> Y first(Y[] ys)  { return ys[0]; }
                <X> List<X> method(List<X[]> list) {
                    return list.stream().map(this::first).toList();
                }
            }
            """;

    @DisplayName("take first function")
    @Test
    public void test3() {
        TypeInfo C = javaInspector.parse(INPUT3);
        {
            MethodInfo method = C.findUniqueMethod("method", 1);
            MethodCall mc = (MethodCall) method.methodBody().statements().getFirst().expression();
            assertNotNull(mc);
            assertEquals("java.util.List<X[]>", mc.parameterizedType().detailedString());
            assertEquals("Type java.util.List<X[]>", mc.concreteReturnType().toString());
            MethodCall mc2 = (MethodCall) mc.object();
            assertEquals("Type java.util.stream.Stream<X[]>", mc2.concreteReturnType().toString());
        }

    }
}