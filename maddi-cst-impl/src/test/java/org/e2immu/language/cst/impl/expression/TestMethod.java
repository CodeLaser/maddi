/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypePrinter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.impl.info.TypePrinterImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethod {

    public static final String METHOD = "method";

    @Test
    public void test() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo ti = runtime.newTypeInfo(cu, "Test");
        cu.setTypes(List.of(ti));
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
                class Test{public String toString(){}}\
                """;
        assertEquals(src, tp.print(new ImportComputerImpl(), runtime.qualificationFullyQualifiedNames(),
                true).toString());

        assertTrue(mi.isPublic());
        assertFalse(mi.isPubliclyAccessible());

        assertSame(mi, ti.findUniqueMethod("toString", 0));
        assertSame(mi, ti.findUniqueMethod("toString", 0, null));
    }

    private MethodInfo makeMethod(Runtime runtime, TypeInfo ti) {
        MethodInfo mi = runtime.newMethod(ti, METHOD, runtime.methodTypeMethod());
        mi.builder()
                .setReturnType(runtime.stringParameterizedType())
                .addMethodModifier(runtime.methodModifierPublic())
                .setAccess(runtime.accessPublic())
                .setMethodBody(runtime.emptyBlock());
        return mi;
    }

    @Test
    public void testMethodMap() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo ti = runtime.newTypeInfo(cu, "Test");
        cu.setTypes(List.of(ti));

        // method()
        MethodInfo mi1 = makeMethod(runtime, ti);
        mi1.builder().commitParameters().commit();

        // method(int)
        MethodInfo mi2 = makeMethod(runtime, ti);
        mi2.builder().addParameter("i", runtime.intParameterizedType()).builder().commit();
        mi2.builder().commitParameters().commit();

        // method(int,String)
        MethodInfo mi3 = makeMethod(runtime, ti);
        mi3.builder().addParameter("i", runtime.intParameterizedType()).builder().commit();
        mi3.builder().addParameter("s", runtime.stringParameterizedType()).builder().commit();
        mi3.builder().commitParameters().commit();

        // method(int,String)
        MethodInfo mi4 = makeMethod(runtime, ti);
        mi4.builder().addParameter("i", runtime.intParameterizedType()).builder().commit();
        mi4.builder().addParameter("j", runtime.intParameterizedType()).builder().commit();
        mi4.builder().commitParameters().commit();

        // method(String,int)
        MethodInfo mi5 = makeMethod(runtime, ti);
        mi5.builder().addParameter("s", runtime.stringParameterizedType()).builder().commit();
        mi5.builder().addParameter("j", runtime.intParameterizedType()).builder().commit();
        mi5.builder().commitParameters().commit();

        ti.builder().addMethod(mi1).addMethod(mi2).addMethod(mi3).addMethod(mi4).addMethod(mi5)
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType()).computeAccess().commit();

        assertSame(mi1, ti.findUniqueMethod(METHOD, 0));
        assertSame(mi2, ti.findUniqueMethod(METHOD, 1, null));
        assertSame(mi3, ti.findUniqueMethod(METHOD, 2, () -> "int,String"));
        assertSame(mi4, ti.findUniqueMethod(METHOD, 2, () -> "int,int"));
        assertSame(mi5, ti.findUniqueMethod(METHOD, 2, () -> "String,int"));
    }
}
