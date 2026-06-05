package org.e2immu.language.java.openjdk.constructor;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestInitializers extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.*;

            public class InspectionGaps_2 {
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
        TypeInfo typeInfo = scan("org.e2immu.analyser.resolver.testexample.InspectionGaps_2", INPUT);
        List<MethodInfo> methods = typeInfo.methods();
        assertEquals(3, methods.size());
        MethodInfo static0 = typeInfo.findUniqueMethod("<static_0>", 0);
        assertTrue(static0.isStatic());
        MethodInfo static1 = typeInfo.findUniqueMethod("<static_1>", 0);
        assertTrue(static1.isStatic());
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
        assertEquals("-@5:5-7:5", m0.source().toString());

        MethodInfo c0 = typeInfo.constructors().getLast();
        assertEquals("a.A.<init>()", c0.toString());
        assertEquals(1, c0.methodBody().statements().size());
        assertTrue(c0.methodBody().statements().getFirst().isSynthetic());
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
        assertTrue(c0.methodBody().statements().getFirst().isSynthetic());
        assertFalse(c0.isStaticInitializer());
        assertEquals("-@9:5-9:17", c0.source().toString());

        MethodInfo m0 = typeInfo.methods().getFirst();
        assertEquals("a.A.<init_0>()", m0.toString());
        assertEquals(1, m0.methodBody().statements().size());
        assertEquals("-@5:5-7:5", m0.source().toString());
        assertTrue(m0.isInstanceInitializer());
    }

}
