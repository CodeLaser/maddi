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

package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.ElementType;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.annotation.RetentionPolicy;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE_HC;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT_HC;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaLangAnnotation extends CommonTest {

    @Test
    public void testAnnotationAnnotationType() {
        TypeInfo typeInfo = compiledTypesManager().get(Annotation.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("annotationType", 0);
        assertFalse(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, INDEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, IMMUTABLE_HC));
    }

    // Annotation is an interface (extensible by definition), so @ImmutableContainer(hc=true)
    // must land as IMMUTABLE_HC. Because it carries hidden content, its independence is the
    // HC variant (@Independent(hc=true)), not deep @Independent -- hence we assert directly
    // rather than via testImmutableContainer(), which expects deep independence.
    @Test
    public void testAnnotationIsImmutableHc() {
        TypeInfo typeInfo = compiledTypesManager().get(Annotation.class);
        assertSame(IMMUTABLE_HC, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertTrue(typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE).isTrue());
    }

    // ElementType is a final enum with no type parameters: not extensible, no hidden content.
    // @ImmutableContainer must therefore land as deep IMMUTABLE (hc=false).
    @Test
    public void testElementTypeIsImmutable() {
        TypeInfo typeInfo = compiledTypesManager().get(ElementType.class);
        testImmutableContainer(typeInfo, false);
        assertFalse(typeInfo.findUniqueMethod("values", 0).isModifying());
        assertFalse(typeInfo.findUniqueMethod("valueOf", 1).isModifying());
    }

    // RetentionPolicy: same reasoning as ElementType.
    @Test
    public void testRetentionPolicyIsImmutable() {
        TypeInfo typeInfo = compiledTypesManager().get(RetentionPolicy.class);
        testImmutableContainer(typeInfo, false);
        assertFalse(typeInfo.findUniqueMethod("values", 0).isModifying());
        assertFalse(typeInfo.findUniqueMethod("valueOf", 1).isModifying());
    }

    // Throwable subclasses are left MUTABLE, so their accessors need an explicit @NotModified
    // to override the @Modified method default. Confirm the override reaches analysis().
    @Test
    public void testAnnotationTypeMismatchExceptionAccessors() {
        TypeInfo typeInfo = compiledTypesManager().get(AnnotationTypeMismatchException.class);
        assertFalse(typeInfo.findUniqueMethod("element", 0).isModifying());
        assertFalse(typeInfo.findUniqueMethod("foundType", 0).isModifying());
    }

    @Test
    public void testIncompleteAnnotationExceptionAccessors() {
        TypeInfo typeInfo = compiledTypesManager().get(IncompleteAnnotationException.class);
        assertFalse(typeInfo.findUniqueMethod("annotationType", 0).isModifying());
        assertFalse(typeInfo.findUniqueMethod("elementName", 0).isModifying());
    }

}
