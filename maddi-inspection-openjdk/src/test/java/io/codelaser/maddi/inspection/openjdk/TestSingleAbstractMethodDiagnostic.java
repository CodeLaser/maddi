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

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.GenericsHelper;
import io.codelaser.maddi.inspection.impl.parser.GenericsHelperImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What {@code GenericsHelperImpl.findSingleAbstractMethodOfInterface} says when there is no SAM.
 * <p>
 * It used to say only "Cannot find a single abstract method in the interface
 * java.util.function.IntPredicate", which reads as a parser bug — IntPredicate plainly has one — and has
 * been taken for one more than once. The usual cause is not the interface: the type is <b>resolvable but
 * not loaded</b>, and a lazily loaded TypeInfo carries no methods, so the scanner's {@code computeSAM} never
 * ran on it. Anything that builds a lambda or a method reference asks for the SAM, so a consumer that
 * generates such code against types it has merely resolved has to preload them first.
 * <p>
 * The cost of that being undiagnosable is not hypothetical: a transform consumer lost whole primary types
 * to it, and the resulting measurement read 36.6% where the true figure was 65.6%. Nothing failed loudly —
 * the number was just smaller.
 */
public class TestSingleAbstractMethodDiagnostic {

    private Runtime runtime;
    private GenericsHelper genericsHelper;
    private JavaInspector javaInspector;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(new InputConfigurationImpl.Builder()
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build());
        runtime = javaInspector.runtime();
        genericsHelper = new GenericsHelperImpl(runtime);
    }

    /**
     * The case that matters. The un-inspected state is constructed directly rather than reached through the
     * lazy loader, and that is worth being exact about: it is the same state
     * ({@code ClassSymbolScanner} calls {@code computeSAM} and {@code commit()} only on the non-lazy branch,
     * so a LAZILY loaded type has neither), but this test does not prove the loader produces it. Measured
     * while writing this: a type merely <em>referenced</em> by parsed source is fully loaded, so the ordinary
     * parse path never lands here — it takes a consumer that resolves types the parse did not touch, which
     * is what the transform does.
     */
    @DisplayName("not loaded: the message says so, and names the remedy")
    @Test
    public void testNotLoaded() {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("a").build();
        TypeInfo notLoaded = runtime.newTypeInfo(cu, "NotLoaded");
        assertFalse(notLoaded.hasBeenInspected(), "the fixture really is in the un-inspected state");

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> genericsHelper.findSingleAbstractMethodOfInterface(notLoaded.asSimpleParameterizedType()));
        assertTrue(e.getMessage().contains("NOT LOADED"), e.getMessage());
        assertTrue(e.getMessage().contains("preload"), "it has to name the remedy, or the reader is no"
                                                       + " better off than with the old message: " + e.getMessage());
    }

    /** A loaded interface with the wrong number of abstract methods: a real answer about the type. */
    @DisplayName("loaded but not functional: the message counts the abstract methods and names them")
    @Test
    public void testLoadedButNotFunctional() {
        javaInspector.parse(java.util.Map.of("a.Two", """
                package a;
                public interface Two {
                    int one();
                    int two();
                }
                """), JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo two = runtime.getFullyQualified("a.Two", true);
        assertTrue(two.hasBeenInspected());

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> genericsHelper.findSingleAbstractMethodOfInterface(two.asSimpleParameterizedType()));
        assertFalse(e.getMessage().contains("NOT LOADED"), "this one IS loaded: " + e.getMessage());
        assertTrue(e.getMessage().contains("2 abstract method"), e.getMessage());
        assertTrue(e.getMessage().contains("one") && e.getMessage().contains("two"), e.getMessage());
    }

    @DisplayName("not an interface at all: say that rather than counting methods")
    @Test
    public void testNotAnInterface() {
        // something has to be parsed first, or the JDK has not been scanned and nothing resolves at all
        javaInspector.parse(java.util.Map.of("a.UsesString", """
                package a;
                public class UsesString { String s = "x"; }
                """), JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo string = runtime.getFullyQualified("java.lang.String", true);
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> genericsHelper.findSingleAbstractMethodOfInterface(string.asSimpleParameterizedType()));
        assertTrue(e.getMessage().contains("not an interface"), e.getMessage());
    }

    /** The happy path still works, and {@code complain=false} still returns null rather than throwing. */
    @DisplayName("a real functional interface still resolves; complain=false still returns null")
    @Test
    public void testStillWorks() {
        // preload BEFORE the parse: it is what asks for the package to be loaded rather than resolved
        javaInspector.preload("java.base::java.util.function");
        javaInspector.parse(java.util.Map.of("a.UsesString", """
                package a;
                public class UsesString { String s = "x"; }
                """), JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo intPredicate = runtime.getFullyQualified("java.util.function.IntPredicate", true);
        assertNotNull(genericsHelper.findSingleAbstractMethodOfInterface(
                intPredicate.asSimpleParameterizedType()), "IntPredicate is a functional interface");

        TypeInfo string = runtime.getFullyQualified("java.lang.String", true);
        assertNull(genericsHelper.findSingleAbstractMethodOfInterface(
                string.asSimpleParameterizedType(), false), "complain=false must not throw");
    }
}
