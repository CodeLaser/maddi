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
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.Test;

import java.util.function.*;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NOT_NULL;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NULLABLE;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaUtilFunction extends CommonTest {

    @Test
    public void testConsumer() {
        TypeInfo typeInfo = compiledTypesManager().get(Consumer.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(FALSE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertTrue(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testConsumerAccept() {
        TypeInfo typeInfo = compiledTypesManager().get(Consumer.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("accept", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(NO_VALUE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(IMMUTABLE_HC, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(NULLABLE, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(FALSE, p0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
    }

    @Test
    public void testFunction() {
        TypeInfo typeInfo = compiledTypesManager().get(Function.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(FALSE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertTrue(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testFunctionAccept() {
        TypeInfo typeInfo = compiledTypesManager().get(Function.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("apply", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE_HC, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(NULLABLE, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(IMMUTABLE_HC, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(NULLABLE, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(FALSE, p0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
    }

    @Test
    public void testSupplier() {
        TypeInfo typeInfo = compiledTypesManager().get(Supplier.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertTrue(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testSupplierGet() {
        TypeInfo typeInfo = compiledTypesManager().get(Supplier.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("get", 0);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }

    @Test
    public void testPredicateTest() {
        TypeInfo typeInfo = compiledTypesManager().get(Predicate.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("test", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
        assertSame(IMMUTABLE_HC, p0.analysis().getOrDefault(IMMUTABLE_PARAMETER, MUTABLE));
        assertSame(NULLABLE, p0.analysis().getOrDefault(NOT_NULL_PARAMETER, NULLABLE));
        assertSame(FALSE, p0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
    }

    @Test
    public void testBiConsumerAccept() {
        TypeInfo typeInfo = compiledTypesManager().get(BiConsumer.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("accept", 2);
        assertEquals("java.util.function.BiConsumer.accept(Object,Object)", methodInfo.fullyQualifiedName());
        ParameterizedType u = methodInfo.parameters().get(1).parameterizedType();
        assertEquals("Type param U", u.toString());
        assertEquals(0, u.arrays());
    }

    // The default helper methods build a NEW composed function without touching `this`, so they must
    // be non-modifying (unlike the SAM apply/accept/test, which may modify their input argument).
    @Test
    public void testDefaultHelpersNonModifying() {
        assertFalse(compiledTypesManager().get(Function.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().get(Function.class).findUniqueMethod("compose", 1).isModifying());
        assertFalse(compiledTypesManager().get(Consumer.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().get(BiConsumer.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().get(Predicate.class).findUniqueMethod("and", 1).isModifying());
        assertFalse(compiledTypesManager().get(Predicate.class).findUniqueMethod("or", 1).isModifying());
        assertFalse(compiledTypesManager().get(Predicate.class).findUniqueMethod("negate", 0).isModifying());
        assertFalse(compiledTypesManager().get(BiPredicate.class).findUniqueMethod("negate", 0).isModifying());
    }

    // Predicate was missing the type-level @Independent(hc=true) all its siblings carry.
    @Test
    public void testPredicateIndependentHc() {
        TypeInfo typeInfo = compiledTypesManager().get(Predicate.class);
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
    }

    // BiFunction and the primitive families get the same treatment: @Independent(hc=true) at the type,
    // and non-modifying default helpers.
    @Test
    public void testBiFunctionAndFriendsIndependentHc() {
        for (Class<?> c : new Class<?>[]{
                BiFunction.class, UnaryOperator.class,
                IntPredicate.class, LongPredicate.class, DoublePredicate.class,
                IntUnaryOperator.class, LongUnaryOperator.class, DoubleUnaryOperator.class,
                IntConsumer.class, LongConsumer.class, DoubleConsumer.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT),
                    () -> c.getSimpleName() + " should be @Independent(hc=true)");
        }
    }

    @Test
    public void testBiFunctionAndFriendsDefaultHelpersNonModifying() {
        assertFalse(compiledTypesManager().get(BiFunction.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().get(IntPredicate.class).findUniqueMethod("and", 1).isModifying());
        assertFalse(compiledTypesManager().get(IntPredicate.class).findUniqueMethod("negate", 0).isModifying());
        assertFalse(compiledTypesManager().get(IntUnaryOperator.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().get(IntUnaryOperator.class).findUniqueMethod("compose", 1).isModifying());
        assertFalse(compiledTypesManager().get(DoubleConsumer.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().get(LongPredicate.class).findUniqueMethod("or", 1).isModifying());
    }

}
