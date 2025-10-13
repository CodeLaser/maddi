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

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMethodCall12 extends CommonTest2 {

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
    public void test() throws IOException {
        init(Map.of("b.B", bB, "c.C", cC));
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
    public void test2() throws IOException {
        ParseResult parseResult = init(Map.of("a.ArrayList", aA2, "b.B", bB2, "c.C", cC2));
        TypeInfo C = parseResult.findType("c.C");
        FieldInfo arrayList = C.getFieldByName("arrayList", true);
        assertEquals("Type a.ArrayList", arrayList.type().toString());
    }
}
