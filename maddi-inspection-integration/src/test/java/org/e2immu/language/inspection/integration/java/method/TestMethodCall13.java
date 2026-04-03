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

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodCall toList = (MethodCall) method.methodBody().statements().getFirst().expression();
        assertEquals("java.util.stream.Stream.toList()", toList.methodInfo().fullyQualifiedName());
        MethodCall map = (MethodCall) toList.object();
        assertEquals("java.util.stream.Stream.map(java.util.function.Function<? super T,? extends R>)",
                map.methodInfo().fullyQualifiedName());
        assertEquals("Type java.util.stream.Stream<X>", map.concreteReturnType().toString());
        assertEquals("Type java.util.List<X>", toList.concreteReturnType().toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Arrays;public class X<T> {
                T[] ts;
                private T get(int index) {
                    return ts[index];
                }
                public static <K> K method(int i, X<K> x) {
                    K k = x.get(i);
                    return k;
                }
                public void print() { System.out.println(ts); }
            }
            """;

    @DisplayName("which of the println methods?")
    @Test
    public void test4() {
        TypeInfo C = javaInspector.parse(INPUT4);

        MethodInfo print = C.findUniqueMethod("print", 0);
        MethodCall println = (MethodCall) print.methodBody().statements().getFirst().expression();
        // definitely not 'println(double)'
        assertEquals("java.io.PrintStream.println(Object)", println.methodInfo().fullyQualifiedName());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import org.assertj.core.api.SoftAssertions;
            public class C {
                public void test(String input) {
                    SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(input)
                                    .withFailMessage("fail1")
                                    .startsWith("abc");
                            softly.assertThat(input)
                                    .withFailMessage("fail2")
                                    .endsWith("def");
                        });
                }
            }
            """;

    /*
    problem seems to be the passing on of method return values? all variable combinations run green.
     */
    @DisplayName("SoftAssertions")
    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

    @Language("java")
    private static final String INPUT5b = """
            package a.b;
            import org.assertj.core.api.SoftAssertions;
            import org.assertj.core.api.StringAssert;
            public class C {
                public void test(String input) {
                    SoftAssertions.assertSoftly(softly -> {
                        StringAssert stringAssert = softly.assertThat(input);
                        StringAssert sa = stringAssert.withFailMessage("fail");
                        sa.startsWith("abc");
            
                        StringAssert sa2 = softly.assertThat(input).withFailMessage("fail");
                        sa2.startsWith("abc");
            
                        StringAssert sa3 = softly.assertThat(input);
                        sa3.withFailMessage("fail").startsWith("abc");
                    });
                }
            }
            """;

    @DisplayName("SoftAssertions, explicit intermediate variable")
    @Test
    public void test5b() {
        javaInspector.parse(INPUT5b);
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.io.Serializable;
            public class C {
                @FunctionalInterface
                interface ArgumentMatcher <T> {
                    boolean matches(T t);
                }
                static <T> T argThat(ArgumentMatcher<T> matcher) { return null; }
                static <T> T verify(T mock) { return mock; } // from Mockito
                interface CrudRepository<T, ID> {
                    <S extends T> S save(S entity);
                }
                interface JpaRepository <T, ID> extends CrudRepository<T, ID> {
                }
                enum ListStatus { STATUS }
                class ListEntry extends Serializable {
                    private ListStatus status;
                    public ListStatus getStatus() {
                        return status;
                    }
                }
                interface ListEntryRepository extends JpaRepository<ListEntry, Long> {}
            
                void test(ListEntryRepository listEntryRepository) {
                    ListEntryRepository repository = verify(listEntryRepository);
                    repository.save(argThat(e -> e.getStatus() == ListStatus.STATUS));
            
                    // also fails: verify(listEntryRepository).save(argThat(e -> e.getStatus() == listStatus.STATUS));
                }
            
                void testGreen1(ListEntryRepository listEntryRepository) {
                    ListEntryRepository repository = verify(listEntryRepository);
                    ArgumentMatcher<ListEntry> am = e -> e.getStatus() == ListStatus.STATUS;
                    repository.save(argThat(am));
                }
            
                void testGreen2(ListEntryRepository listEntryRepository) {
                    ListEntryRepository repository = verify(listEntryRepository);
                    ListEntry entry = argThat(e -> e.getStatus() == ListStatus.STATUS);
                    repository.save(entry);
                }
            }
            """;

    @DisplayName("Mockito ArgumentMatcher")
    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.io.Serializable;
            class C {
                interface Assert <SELF extends Assert<SELF,ACTUAL>, ACTUAL> { }
                abstract class AbstractAssert<SELF extends AbstractAssert<SELF,ACTUAL>, ACTUAL> implements Assert<SELF,ACTUAL>{ }
                abstract class AbstractBooleanAssert <SELF extends AbstractBooleanAssert<SELF>> extends AbstractAssert<SELF,Boolean> {
                    SELF isFalse();
                }
                static AbstractBooleanAssert<?> assertThat(Boolean actual) { return null; }
                class SessionType implements Serializable {
                    Boolean getCountInQuota() { return false; }
                }
                class ArgumentCaptor<T> {
                    T getValue();
                    static <U, S extends U> ArgumentCaptor<U> forClass(Class<S> clazz) { return null; }
                }
                void test() {
                    var captor = ArgumentCaptor.forClass(SessionType.class);
                    assertThat(captor.getValue().getCountInQuota()).isFalse();
                }
                void testGreen() {
                    ArgumentCaptor<SessionType> captor = ArgumentCaptor.forClass(SessionType.class);
                    assertThat(captor.getValue().getCountInQuota()).isFalse();
                }
            }
            """;

    @DisplayName("var LVC does not receive correct type")
    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.Set;
            import static org.junit.jupiter.api.Assertions.*;
            import static org.mockito.Mockito.*;
            class C {
                static String contains(String substring) { return substring; }
                interface Constants {
                    String C = "user";
                }
                void method(Set<String> set) {
                    assertTrue(set.contains(Constants.C), "message");
                }
            }
            """;

    @DisplayName("mockito makes another contains() method available, one that returns a String...")
    @Test
    public void test8() {
        javaInspector.parse(INPUT8);
    }
}