/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypePrinter;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.impl.info.TypePrinterImpl;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

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

    /**
     * ⛔ <b>ARITY IS PART OF A METHOD'S IDENTITY, AND THE ONE-METHOD SHORTCUT IGNORED IT.</b> With a single
     * method under a name, the map answered it for <em>every</em> parameter count and only checked with an
     * {@code assert} — so a test JVM saw an {@code AssertionError} and a production run got a method it did
     * not ask for. Both halves are wrong in the same place, and the silent half is the worse one.
     * <p>
     * Found by parsing timefold-solver: <b>99 compilation units</b> dropped on this assertion, all of them
     * class-file loads of a method the type does not have. The loader's contract for that is
     * {@code getMethod} returning null and {@code getOrLoadMethod} then loading it — which the no-method-map
     * branch beside it does — so the three-argument lookup answers null rather than throwing.
     */
    @DisplayName("a method of a different arity is not the method that was asked for")
    @Test
    public void arityIsPartOfTheIdentity() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo ti = runtime.newTypeInfo(cu, "Test");
        cu.setTypes(List.of(ti));

        MethodInfo only = makeMethod(runtime, ti);
        only.builder().addParameter("i", runtime.intParameterizedType()).builder().commit();
        only.builder().commitParameters().commit();
        ti.builder().addMethod(only).setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType()).computeAccess().commit();

        assertSame(only, ti.findUniqueMethod(METHOD, 1));
        // the two-argument form is documented to throw when there is no match, and one arity is no match
        assertThrows(NoSuchElementException.class, () -> ti.findUniqueMethod(METHOD, 2));
        // the three-argument form is the loader's lookup: absent is an answer, not a failure
        assertNull(ti.findUniqueMethod(METHOD, 2, () -> "int,int"));
        assertNull(ti.findUniqueMethod("noSuchName", 1, () -> "int"));
    }
}
