package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestInitializers extends CommonTest {

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
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals(1, typeInfo.constructors().size());

        MethodInfo m0 = typeInfo.methods().getFirst();
        assertEquals("a.A.<static_0>()", m0.toString());
        assertEquals(1, m0.methodBody().statements().size());
        assertTrue(m0.isStaticInitializer());
        assertEquals("@5:12-7:5", m0.source().toString());

        MethodInfo c0 = typeInfo.constructors().getLast();
        assertEquals("a.A.<init>()", c0.toString());
        assertTrue(c0.methodBody().statements().isEmpty());
        assertEquals("@9:5-9:17", c0.source().toString());
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
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        // identical signature, therefore: main constructor
        assertEquals(1, typeInfo.constructors().size());
        MethodInfo c0 = typeInfo.constructors().getFirst();
        assertEquals("a.A.<init>()", c0.toString());
        assertEquals(0, c0.methodBody().statements().size());
        assertFalse(c0.isStaticInitializer());
        assertEquals("@9:5-9:17", c0.source().toString());

        MethodInfo m0 = typeInfo.methods().getFirst();
        assertEquals("a.A.<init_0>()", m0.toString());
        assertEquals(1, m0.methodBody().statements().size());
        assertEquals("@5:5-7:5", m0.source().toString());
        assertTrue(m0.isInstanceInitializer());
    }

}
