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

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
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
