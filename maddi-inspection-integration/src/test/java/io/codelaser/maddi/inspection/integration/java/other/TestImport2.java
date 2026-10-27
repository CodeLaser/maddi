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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestImport2 extends CommonTest2 {


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
    public void testImport() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT, "a.b.Child", CHILD,
                "c.b.GrandChild", GRANDCHILD);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo gc = pr1.findType("c.b.GrandChild");
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
    public void testImportPriority() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.A", A, "b.A", B, "c.C", C);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo c = pr1.findType("c.C");
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
    public void testImportSub() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.BF", BF, "b.CF", CF, "c.CCF", CCF);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo c = pr1.findType("c.CCF");
        assertEquals("[Type b.CF, Type a.BF.L]", c.interfacesImplemented().toString());
    }

}
