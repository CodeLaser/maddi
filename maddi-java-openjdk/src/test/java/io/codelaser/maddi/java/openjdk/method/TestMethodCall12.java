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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMethodCall12 extends CommonTest {

    @Language("java")
    private static final String bB = """
            package b;
            public class B {
                public static String contains(String s) {
                    return s.toLowerCase();
                }
            }
            """;

    @Language("java")
    private static final String cC = """
            package c;
            import static b.B.*;
            import java.util.Set;
            class C {
                public void m(Set<String> set) {
                    if(success(set.contains("abc"))) {
                        System.out.println("yes");
                    }
                }
                private boolean success(boolean b) {
                    return !b;
                }
            }
            """;

    @DisplayName("avoid instance->static if type of object is different")
    @Test
    public void test() {
        scan(false, "b.B", bB, "c.C", cC);
    }

    @Language("java")
    private static final String aA2 = """
            package a;
            public class ArrayList {
            }
            """;

    @Language("java")
    private static final String bB2 = """
            package b;
            import java.util.ArrayList;
            public class B extends ArrayList<String> {
                public B() { super(); }
            }
            """;

    @Language("java")
    private static final String cC2 = """
            package c;
            import a.ArrayList;
            import b.B;
            class C extends B {
                ArrayList arrayList;
            }
            """;

    @Test
    public void test2() {
        Map<String, TypeInfo> result = scan(false, "a.ArrayList", aA2, "b.B", bB2, "c.C", cC2);
        TypeInfo C = result.get("c.C");
        FieldInfo arrayList = C.getFieldByName("arrayList", true);
        assertEquals("Type a.ArrayList", arrayList.type().toString());
    }

    @Language("java")
    private static final String ASSERTIONS_C = """
            package c;

            class C {
                interface Assert <SELF extends Assert<SELF,ACTUAL>, ACTUAL> { }
                abstract class AbstractAssert<SELF extends AbstractAssert<SELF,ACTUAL>, ACTUAL> implements Assert<SELF,ACTUAL>{ }
                abstract class AbstractBooleanAssert <SELF extends AbstractBooleanAssert<SELF>> extends AbstractAssert<SELF,Boolean> {
                    SELF isFalse() { return null; }
                }
                static AbstractBooleanAssert<?> assertThat(boolean actual) { return null; }
                static AbstractBooleanAssert<?> assertThat(Boolean actual) { return null; }
            }
            """;

    @Language("java")
    private static final String ASSERTIONS_I = """
            package c;

            public class I extends C {
                abstract class AbstractObjectAssert <SELF extends AbstractObjectAssert<SELF,ACTUAL>, ACTUAL> extends AbstractAssert<SELF,ACTUAL> {}
                interface ComparableAssert <SELF extends ComparableAssert<SELF,ACTUAL>, ACTUAL extends Comparable<? super ACTUAL>> {
                    void isFalse();
                }
                public abstract class AbstractComparableAssert <SELF extends AbstractComparableAssert<SELF,ACTUAL>, ACTUAL extends Comparable<? super ACTUAL>> extends AbstractObjectAssert<SELF,ACTUAL> implements ComparableAssert<SELF,ACTUAL> {}

                public static <T extends Comparable<? super T>> AbstractComparableAssert<?,T> assertThat(T actual) { return null; }
            }
            """;

    @Language("java")
    private static final String C3 = """
            package a.b;
            import static c.I.assertThat;
            class C {

                static class Builder { }
                static class DS {
                    final boolean method(Builder builder) {
                        return true;
                    }
                }
                void test(DS ds, Builder builder) {
                    assertThat(ds.method(builder)).isFalse();
                }
            }
            """;

    @DisplayName("assertThat boolean")
    @Test
    public void test3() {
        scan(false, "c.I", ASSERTIONS_I, "c.C", ASSERTIONS_C, "a.b.C3", C3);
    }
}
