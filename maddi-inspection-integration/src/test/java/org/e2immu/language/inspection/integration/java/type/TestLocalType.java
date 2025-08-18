package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestLocalType extends CommonTest {

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                interface A { void method(String s); }
                A make(String t) {
                    final class C implements A {
                        @Override
                        void method(String s) {
                            System.out.println(s+t);
                        }
                    }
                    return new C();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo make = X.findUniqueMethod("make", 1);
        LocalTypeDeclaration ltd = (LocalTypeDeclaration) make.methodBody().statements().getFirst();
        assertEquals("a.b.X.0$make$C", ltd.typeInfo().fullyQualifiedName());
        MethodInfo method = ltd.typeInfo().findUniqueMethod("method", 1);
        assertEquals("a.b.X.0$make$C.method(String)", method.fullyQualifiedName());
        assertNotNull(method.methodBody());
        assertEquals("System.out.println(s+t)",
                method.methodBody().statements().getFirst().expression().toString());
    }

}
