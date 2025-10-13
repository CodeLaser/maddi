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

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
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
