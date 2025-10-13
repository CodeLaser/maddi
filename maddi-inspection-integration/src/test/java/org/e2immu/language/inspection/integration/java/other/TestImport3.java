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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestImport3 extends CommonTest2 {


    @Language("java")
    String PARENT = """
            package a.b;
            public class Parent {
                public interface SubInterface {
                    void method();
                }
            }
            """;

    @Language("java")
    String SIBLING = """
            package a.b;
            public class Sibling {
                static class Child extends Parent {
                    SubInterface si;
                }
            }
            """;

    @Test
    public void testImport() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT, "a.b.Sibling", SIBLING);
        ParseResult pr = init(sourcesByFqn);
        TypeInfo sibling = pr.findType("a.b.Sibling");
        TypeInfo child = sibling.findSubType("Child");
        FieldInfo si = child.getFieldByName("si", true);
        assertEquals("Type a.b.Parent.SubInterface", si.type().toString());
    }


    @Language("java")
    String PARENT2 = """
            package a.b;
            public class Parent {
                public static String[] STRINGS = { "a", "b", "c" };
            }
            """;
    @Language("java")
    String CHILD2 = """
            package a.b;
            public class Child extends Parent {
            }
            """;

    @Language("java")
    String USE2 = """
            package c;
            import static a.b.Child.STRINGS;
            public class Use {
                void method() {
                    for(String s: STRINGS) {
                        System.out.println(s);
                    }
                }
            }
            """;

    @Test
    public void testImport2() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT2, "a.b.Child", CHILD2, "c.Use", USE2);
        ParseResult pr = init(sourcesByFqn);

    }

}
