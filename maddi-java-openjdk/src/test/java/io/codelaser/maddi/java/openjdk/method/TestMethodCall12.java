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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
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
