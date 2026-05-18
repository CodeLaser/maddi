package org.e2immu.language.java.openjdk.initial;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestClass1 extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package org.e2immu.example;
            
            import org.jetbrains.annotations.NotNull;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            import java.util.List;
            
            public class Class1 {
                private static final Logger LOGGER = LoggerFactory.getLogger(Class1.class);
            
                private int method() {
                    LOGGER.info("I'm here!");
                    // return a constant
                    return 3;
                }
            
                // a comment on a method
                protected void voidMethod() {
                    int j = method();
                    /* and one one a statement */
                    System.out.println(j);
                }
            
                static class Enclosed<T> implements Comparable<Enclosed<T>> {
                    List<T> list;
            
                    @Override
                    public int compareTo(@NotNull Enclosed<T> o) {
                        return list.size() - o.list.size();
                    }
                }
            
                record R(int k, String s, int[] ints, Double[][] matrix) {
                    double get(int i, int j) {
                        return matrix[i][j];
                    }
                }
            }
            """;

    @Test
    public void test() throws IOException {
        TypeInfo class1 = scan("org.e2immu.example.Class1", INPUT);
        assertTrue(class1.hasBeenInspected());
        assertEquals("org.e2immu.example.Class1", class1.fullyQualifiedName());
        assertEquals(runtime.objectParameterizedType(), class1.parentClass());

        MethodInfo constructor = class1.findConstructor(0);
        assertEquals("org.e2immu.example.Class1.<init>()", constructor.fullyQualifiedName());
        assertTrue(constructor.isSynthetic());
        assertTrue(constructor.methodModifiers().contains(runtime.methodModifierPublic()));

        MethodInfo method = class1.findUniqueMethod("method", 0);
        assertEquals("source::org.e2immu.example.Class1.method()", method.descriptor());
        assertFalse(method.isSynthetic());
        assertTrue(method.methodModifiers().contains(runtime.methodModifierPrivate()));
        assertEquals(runtime.intParameterizedType(), method.returnType());

        Statement callInfo = method.methodBody().statements().getFirst();
        if (callInfo instanceof ExpressionAsStatement eas) {
            if (eas.expression() instanceof MethodCall mc) {
                assertEquals("Class1.LOGGER", mc.object().toString());
                assertEquals("slf4j-api-2.0.17.jar",
                        mc.object().parameterizedType().typeInfo().compilationUnit().sourceSet().name());
                if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                    assertEquals("LOGGER", fr.fieldInfo().name());
                    assertEquals("Type org.slf4j.Logger", fr.parameterizedType().toString());
                    // lazy loading: we've just seen one method, even if there are 60+
                    assertEquals(1, fr.parameterizedType().typeInfo().methods().size());
                } else fail();
                assertEquals(1, mc.parameterExpressions().size());
                if (mc.parameterExpressions().getFirst() instanceof StringConstant sc) {
                    assertEquals("I'm here!", sc.constant());
                } else fail();
            } else fail();
        } else fail();
        Statement return3 = method.methodBody().statements().getLast();
        assertEquals("15-9:15-17", return3.source().compact2());
        if (return3 instanceof ReturnStatement rs) {
            if (rs.expression() instanceof IntConstant ic) {
                assertEquals(3, ic.constant());
                assertEquals("15-16:15-16", ic.source().compact2());
            } else fail();
        } else fail();

        MethodInfo voidMethod = class1.findUniqueMethod("voidMethod", 0);
        assertEquals("source::org.e2immu.example.Class1.voidMethod()", voidMethod.descriptor());
        assertFalse(voidMethod.isSynthetic());
        assertTrue(voidMethod.methodModifiers().contains(runtime.methodModifierProtected()));
        assertEquals(runtime.voidParameterizedType(), voidMethod.returnType());

        Statement vm1 = voidMethod.methodBody().statements().getFirst();
        if (vm1 instanceof LocalVariableCreation lvc) {
            LocalVariable lv = lvc.localVariable();
            assertEquals("j", lv.simpleName());
            assertSame(runtime.intParameterizedType(), lv.parameterizedType());
        } else fail();
        Statement vm2 = voidMethod.methodBody().statements().get(1);
        if (vm2 instanceof ExpressionAsStatement eas && eas.expression() instanceof MethodCall mc) {
            Expression arg1 = mc.parameterExpressions().getFirst();
            if (arg1 instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv) {
                assertEquals("j", lv.simpleName());
            } else fail();
        } else fail();

        TypeInfo enclosed = class1.findSubType("Enclosed");
        assertFalse(enclosed.isInnerClass());
        assertTrue(enclosed.isStatic());
        assertSame(class1, enclosed.compilationUnitOrEnclosingType().getRight());
        assertEquals("T", enclosed.typeParameters().getFirst().simpleName());

        assertEquals("Type Comparable<org.e2immu.example.Class1.Enclosed<T>>",
                enclosed.interfacesImplemented().getFirst().toString());

        MethodInfo compareTo = enclosed.findUniqueMethod("compareTo", 1);
        assertEquals(1, compareTo.parameters().size());
        ParameterInfo p0 = compareTo.parameters().getFirst();
        assertEquals("Type org.e2immu.example.Class1.Enclosed<T>", p0.parameterizedType().toString());
        assertEquals(1, p0.annotations().size());
        assertEquals("org.jetbrains.annotations.NotNull",
                p0.annotations().getFirst().typeInfo().fullyQualifiedName());
        Statement returnCompare = compareTo.methodBody().statements().getFirst();
        assertEquals("this.list.size()-o.list.size()", returnCompare.expression().toString());
        assertEquals("java.lang.Override", compareTo.annotations().getFirst().typeInfo().fullyQualifiedName());

        TypeInfo R = class1.findSubType("R");
        assertTrue(R.typeNature().isRecord());
        assertEquals(4, R.fields().size());
        FieldInfo k = R.getFieldByName("k", true);
        assertTrue(k.type().isInt());

        MethodInfo accessK = R.findUniqueMethod("k", 0);
        assertTrue(accessK.isSynthetic());
        assertTrue(accessK.isPublic());

        FieldInfo s = R.getFieldByName("s", true);
        // check that 'java.lang.String' is not a duplicate object
        assertTrue(s.isFinal());
        assertEquals("s", s.initializer().toString());
        assertEquals(runtime.stringParameterizedType(), s.type());

        FieldInfo ints = R.getFieldByName("ints", true);
        assertEquals("Type int[]", ints.type().toString());

        FieldInfo matrix = R.getFieldByName("matrix", true);
        assertEquals("Type Double[][]", matrix.type().toString());

        MethodInfo get = R.findUniqueMethod("get", 2);
        ReturnStatement rsGet = (ReturnStatement) get.methodBody().statements().getLast();
        assertEquals("this.matrix[i][j]", rsGet.expression().toString());
    }
}
