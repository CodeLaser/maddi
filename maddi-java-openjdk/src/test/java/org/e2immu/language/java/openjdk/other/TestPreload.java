package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestPreload extends CommonTest {
    @Test
    public void test() {
        scan("a.b.X", "package a.b; class X { }");
        TypeInfo functionalInterface = classSymbolScanner.getType("java.lang.FunctionalInterface");
        assertNotNull(functionalInterface);
        assertEquals(3, functionalInterface.annotations().size());
        assertEquals("@Documented", functionalInterface.annotations().getFirst().toString());
        assertEquals("@Retention(RetentionPolicy.RUNTIME)", functionalInterface.annotations().get(1).toString());
        assertEquals("@Target({ElementType.TYPE})", functionalInterface.annotations().get(2).toString());
        assertEquals("Type java.lang.annotation.Annotation",
                functionalInterface.interfacesImplemented().getFirst().toString());
    }
}
