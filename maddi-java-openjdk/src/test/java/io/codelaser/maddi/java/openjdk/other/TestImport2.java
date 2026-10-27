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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestImport2 extends CommonTest {


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
    String CHILD = """
            package a.b;
            public class Child extends Parent {
                protected void gc(SubInterface si) {
                }
            }
            """;

    @Language("java")
    String GRANDCHILD = """
            package c.b;
            import a.b.Child;
            public class GrandChild extends Child {
                @Override
                protected void gc(SubInterface si) {
                    si.method();
                }
            }
            """;

    @Test
    public void testImport() {
        Map<String, TypeInfo> pr1 = scan(false, "a.b.Parent", PARENT, "a.b.Child", CHILD,
                "c.b.GrandChild", GRANDCHILD);
        TypeInfo gc = pr1.get("c.b.GrandChild");
    }

    @Language("java")
    String A = """
            package a;

            public class A {
            }
            """;

    @Language("java")
    String B = """
            package b;

            public class A {
            }
            """;

    @Language("java")
    String C = """
            package c;
            import a.*;
            import b.A;

            public class C extends A {
            }
            """;

    @Test
    public void testImportPriority() {
        Map<String, TypeInfo> pr1 = scan(false, "a.A", A, "b.A", B, "c.C", C);
        TypeInfo c = pr1.get("c.C");
        assertEquals("Type b.A", c.parentClass().toString());
    }


    @Language("java")
    String BF = """
            package a;
            public interface BF {
                interface L {
                }
            }
            """;

    @Language("java")
    String CF = """
            package b;
            import a.BF;
            public interface CF extends BF {
            }
            """;

    @Language("java")
    String CCF = """
            package c;
            import b.CF;

            public class CCF implements CF, CF.L {
            }
            """;

    @Test
    public void testImportSub() {
        Map<String, TypeInfo> pr1 = scan(false, "a.BF", BF, "b.CF", CF, "c.CCF", CCF);
        TypeInfo c = pr1.get("c.CCF");
        assertEquals("[Type b.CF, Type a.BF.L]", c.interfacesImplemented().toString());
    }

}
