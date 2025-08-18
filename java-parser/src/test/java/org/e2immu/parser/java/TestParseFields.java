package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.MultiLineComment;
import org.e2immu.language.cst.api.element.SingleLineComment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.CharConstant;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.FieldModifier;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseFields extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            // class comment
            class C {
              /* not final */
              int a;
              private String b;
              // a transient character
              transient char c = ' ';
              @SuppressWarnings("!")
              public final double d = 3 + a;
              public int a() {
                  return a;
              }
              // comment for t
              String t = "t", s = "s";
              public int aa() {
                  return C.this.a;
              }
            }
            class D extends C {
                public int a() {
                    return C.super.a;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertEquals("C", typeInfo.simpleName());

        assertEquals(1, typeInfo.comments().size());
        if (typeInfo.comments().get(0) instanceof SingleLineComment c) {
            assertEquals(" class comment", c.comment());
        } else fail();


        FieldInfo a = typeInfo.fields().get(0);
        assertSame(a, typeInfo.getFieldByName("a", true));
        assertTrue(a.access().isPackage());
        assertEquals(5, a.source().beginLine());
        assertEquals(1, a.comments().size());
        if (a.comments().get(0) instanceof MultiLineComment mlc) {
            assertEquals("/* not final */", mlc.print(null).toString());
        } else fail();

        FieldInfo b = typeInfo.fields().get(1);
        assertTrue(b.access().isPrivate());
        assertEquals("java.lang.String", b.type().typeInfo().fullyQualifiedName());

        FieldInfo c = typeInfo.fields().get(2);
        assertTrue(c.access().isPackage());
        assertTrue(c.modifiers().stream().anyMatch(FieldModifier::isTransient));
        assertEquals("char", c.type().typeInfo().fullyQualifiedName());
        assertEquals("char", c.type().typeInfo().simpleName());
        Expression ce = c.initializer();
        if (ce instanceof CharConstant cc) {
            assertEquals(' ', cc.constant());
            assertEquals("' '", ce.print(null).toString());
        } else fail();
        assertFalse(c.isFinal());

        FieldInfo d = typeInfo.getFieldByName("d", true);
        assertTrue(d.isFinal());
        assertTrue(d.access().isPackage()); // and not public, because the enclosing type is package
        assertEquals("double", d.type().typeInfo().fullyQualifiedName());
        Expression de = d.initializer();
        if (de instanceof BinaryOperator bo) {
            if (bo.rhs() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                assertSame(a, fr.fieldInfo());
                if (fr.scope() instanceof VariableExpression ve2 && ve2.variable() instanceof This thisVar) {
                    assertSame(typeInfo, thisVar.typeInfo());
                } else fail();
                assertTrue(fr.scopeIsRecursivelyThis());
                assertTrue(fr.scopeIsThis());
            } else fail();
        } else fail();
        assertEquals(1, d.annotations().size());

        MethodInfo methodInfo = typeInfo.methods().get(0);
        assertEquals("a", methodInfo.name());
        Block block = methodInfo.methodBody();
        assertEquals(1, block.size());
        if (block.statements().get(0) instanceof ReturnStatement rs) {
            if (rs.expression() instanceof VariableExpression ve) {
                if (ve.variable() instanceof FieldReference fr) {
                    assertSame(a, fr.fieldInfo());
                } else fail();
            } else fail("Have " + rs.expression().getClass());
            assertEquals("return this.a;", rs.toString());
        } else fail();


        FieldInfo t = typeInfo.getFieldByName("t", true);
        assertEquals(1, t.comments().size());
        FieldInfo s = typeInfo.getFieldByName("s", true);
        assertTrue(s.comments().isEmpty());
        FieldInfo u = typeInfo.getFieldByName("u", false);
        assertNull(u);
    }
}
