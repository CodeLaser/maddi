package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSealed extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class X {
                static sealed class P permits A, B, C {
            
                }
                static final class A extends P {
            
                }
                static non-sealed class B extends P {
            
                }
                static sealed class C extends P permits D, E {
            
                }
                static final class D extends C {}
                static final class E extends C {}
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

        TypeInfo P = X.findSubType("P", true);
        assertTrue(P.isSealed());
        assertFalse(P.isFinal());
        assertFalse(P.isNonSealed());
        assertEquals(3, P.permittedWhenSealed().size());
        assertEquals("""
                [TypeReference[typeInfo=a.b.X.A, explicit=true], \
                TypeReference[typeInfo=a.b.X.B, explicit=true], \
                TypeReference[typeInfo=a.b.X.C, explicit=true]]\
                """, P.typesReferenced().toList().toString());

        TypeInfo A = X.findSubType("A", true);
        assertFalse(A.isSealed());
        assertTrue(A.isFinal());
        assertFalse(A.typeModifiers().contains(javaInspector.runtime().typeModifierNonSealed()));
        assertFalse(A.isNonSealed());
        assertSame(A, P.permittedWhenSealed().getFirst());
        assertSame(P, A.parentClass().typeInfo());

        TypeInfo B = X.findSubType("B", true);
        assertFalse(B.isSealed());
        assertFalse(B.isFinal());
        assertTrue(B.typeModifiers().contains(javaInspector.runtime().typeModifierNonSealed()));
        assertTrue(B.isNonSealed());
        assertSame(B, P.permittedWhenSealed().get(1));
    }

}
