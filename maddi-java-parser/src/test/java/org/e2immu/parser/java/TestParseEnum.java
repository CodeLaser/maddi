package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseEnum extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            enum E {
              A, B, C;
              public boolean isA() {
                return this == A;
              }
              public boolean isB() {
                 return "B".equals(name());
              }
              public static E[] all() {
                 return values();
              }
              public static E make(String s) {
                 return valueOf(s);
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertTrue(typeInfo.typeNature().isEnum());

        assertEquals("Type Enum<a.b.E>", typeInfo.parentClass().toString());

        FieldInfo a = typeInfo.getFieldByName("A", true);
        assertTrue(a.isStatic());

        // TODO assertTrue(a.access().isPublic());
        assertTrue(a.isFinal());
        assertTrue(a.isPropertyFinal());
        assertEquals(3, typeInfo.fields().size());

        MethodInfo isA = typeInfo.findUniqueMethod("isA", 0);
        if (isA.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof BinaryOperator bo && bo.rhs() instanceof VariableExpression ve &&
            ve.variable() instanceof FieldReference fr) {
            assertSame(a, fr.fieldInfo());
        } else fail();

        MethodInfo values = typeInfo.findUniqueMethod("values", 0);
        assertEquals("Type a.b.E[]", values.returnType().toString());
        MethodInfo valueOf = typeInfo.findUniqueMethod("valueOf", 1);
        assertEquals("Type a.b.E", valueOf.returnType().toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            enum E {
              A(true, "s"), B(false), C(false, "d");
              private final boolean b;
              private final String s;
  
              E(boolean b) { this(b, ""); }
  
              E(boolean b, String s) {
                 this.b = b;
                 this.s = s;
              }
              public String s() {
                return s;
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        assertTrue(typeInfo.typeNature().isEnum());
        assertFalse(typeInfo.isExtensible());

        FieldInfo a = typeInfo.getFieldByName("A", true);
        assertTrue(a.isStatic());
        assertEquals("new E(true,\"s\")", a.initializer().toString());

        FieldInfo b = typeInfo.getFieldByName("B", true);
        assertTrue(b.isStatic());
        assertEquals("new E(false)", b.initializer().toString());
    }

    private static final String INPUT3 = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            public class RMultiLevel {
                public enum Effective {
                    E1, E2;
                    public static Effective of(int index) {
                        return index == 1 ? E1: E2;
                    }
                }
                public enum Level {
                    ONE, TWO, THREE
                }
            }
            """;
    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        TypeInfo effective = typeInfo.findSubType("Effective");
        assertTrue(effective.typeNature().isEnum());
    }

}
