package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestClass1 {

    @Test
    public void test() throws Exception {
        Runtime runtime = new RuntimeImpl();
        SingleDirExplorer sde = new SingleDirExplorer(runtime);
        String sourceDir = "../maddi-inspection-openjdk-example/src/main/java";
        SourceSet sourceSet = new SourceSetImpl(
                "source", List.of(Path.of(sourceDir)),
                URI.create("file:" + Path.of(sourceDir).toAbsolutePath()),
                StandardCharsets.UTF_8, false, false, false,
                false, false, Set.of(), Set.of());
        List<TypeInfo> types = sde.go(sourceSet, "../maddi-inspection-openjdk-example/libs");
        assertEquals(1, types.size());
        TypeInfo class1 = types.getFirst();
        assertEquals("org.e2immu.example.Class1", class1.fullyQualifiedName());

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
                assertEquals("slf4j-api-2.0.15.jar",
                        mc.object().parameterizedType().typeInfo().compilationUnit().sourceSet().name());
                if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                    assertEquals("LOGGER", fr.fieldInfo().name());
                    assertEquals("Type org.slf4j.Logger", fr.parameterizedType().toString());
                    assertTrue(fr.parameterizedType().typeInfo().methods().size() > 70);
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

        MethodInfo compareTo = enclosed.findUniqueMethod("compareTo", 1);
        assertEquals(1, compareTo.parameters().size());
        ParameterInfo p0 = compareTo.parameters().getFirst();
        assertEquals("Type org.e2immu.example.Class1.Enclosed<T>", p0.parameterizedType().toString());
        assertEquals(1, p0.annotations().size());
        assertEquals("org.jetbrains.annotations.NotNull",
                p0.annotations().getFirst().typeInfo().fullyQualifiedName());
        Statement returnCompare = compareTo.methodBody().statements().getFirst();
        assertEquals("", returnCompare.expression().toString());
    }
}
