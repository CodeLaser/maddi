package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.api.info.TypePrinter;
import org.e2immu.language.cst.impl.info.TypePrinterImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethod {

    @Test
    public void test() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo ti = runtime.newTypeInfo(cu, "Test");
        MethodInfo mi = runtime.newMethod(ti, "toString", runtime.methodTypeMethod());
        mi.builder()
                .setReturnType(runtime.stringParameterizedType())
                .addMethodModifier(runtime.methodModifierPublic())
                .setAccess(runtime.accessPublic())
                .setMethodBody(runtime.emptyBlock())
                .commitParameters().commit();
        ti.builder().addMethod(mi)
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType()).computeAccess().commit();
        TypePrinter tp = new TypePrinterImpl(ti, false);
        String src = """
                package com.foo;
                class Test{public String toString(){}}\
                """;
        assertEquals(src, tp.print(new ImportComputerImpl(), runtime.qualificationFullyQualifiedNames(),
                true).toString());

        assertTrue(mi.isPublic());
        assertFalse(mi.isPubliclyAccessible());
    }
}
