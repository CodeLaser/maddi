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

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestInitializerMethods extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            
            import java.util.*;
            
            public class C {
                private static final Map<String, Integer> PRIORITY = new HashMap<>();
            
                static {
                    PRIORITY.put("e2container", 1);
                    PRIORITY.put("e2immutable", 2);
                }
            
                static {
                    PRIORITY.put("e1container", 3);
                    PRIORITY.put("e1immutable", 4);
                }
            
                private static int priority(String in) {
                    return PRIORITY.getOrDefault(in.substring(0, in.indexOf('-')), 10);
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        List<MethodInfo> methods = typeInfo.methods();
        assertEquals(3, methods.size());
        MethodInfo static0 = typeInfo.findUniqueMethod("<static_0>", 0);
        assertTrue(static0.isStatic());
        MethodInfo static1 = typeInfo.findUniqueMethod("<static_1>", 0);
        assertTrue(static1.isStatic());
        assertEquals(2, static1.methodBody().statements().size());
    }


    @Language("java")
    private static final String INPUT1 = """
            package a;
            class A {
                private static int x;
            
                static {
                    x = 0;
                }
            
                public A() {}
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.A", INPUT1);
        assertEquals(1, typeInfo.constructors().size());

        MethodInfo m0 = typeInfo.methods().getFirst();
        assertEquals("a.A.<static_0>()", m0.toString());
        assertEquals(1, m0.methodBody().statements().size());
        assertTrue(m0.isStaticInitializer());
        // NOTE: differs from maddi's own parser, 12:5. This is equally correct.
        assertEquals("-@5:5-7:5", m0.source().toString());

        MethodInfo c0 = typeInfo.constructors().getLast();
        assertEquals("a.A.<init>()", c0.toString());
        assertEquals(1, c0.methodBody().statements().size());
        assertTrue(c0.methodBody().statements().getFirst() instanceof ExplicitConstructorInvocation eci
                   && eci.isSuper());
        assertEquals("-@9:5-9:17", c0.source().toString());
    }


    // instance initializers are copied into every constructor

    @Language("java")
    private static final String INPUT2 = """
            package a;
            class A {
                private int x;
            
                {
                    x = 0;
                }
            
                public A() {}
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = scan("a.A", INPUT2);
        // identical signature, therefore: main constructor
        assertEquals(1, typeInfo.constructors().size());
        MethodInfo c0 = typeInfo.constructors().getFirst();
        assertEquals("a.A.<init>()", c0.toString());
        assertEquals(1, c0.methodBody().statements().size());
        assertTrue(c0.methodBody().statements().getFirst() instanceof ExplicitConstructorInvocation eci
                   && eci.isSuper());
        assertFalse(c0.isStaticInitializer());
        assertEquals("-@9:5-9:17", c0.source().toString());

        MethodInfo m0 = typeInfo.methods().getFirst();
        assertEquals("a.A.<init_0>()", m0.toString());
        assertEquals(1, m0.methodBody().statements().size());
        assertEquals("-@5:5-7:5", m0.source().toString());
        assertTrue(m0.isInstanceInitializer());
    }

}
