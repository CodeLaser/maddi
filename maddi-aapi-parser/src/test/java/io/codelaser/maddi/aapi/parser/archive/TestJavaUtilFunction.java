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

package io.codelaser.maddi.aapi.parser.archive;

import io.codelaser.maddi.aapi.parser.CommonTest;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.Test;

import java.util.function.*;

import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.*;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.ImmutableImpl.*;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.IndependentImpl.*;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.NotNullImpl.NOT_NULL;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.NotNullImpl.NULLABLE;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaUtilFunction extends CommonTest {

    @Test
    public void testConsumer() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Consumer.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(FALSE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertTrue(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testConsumerAccept() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Consumer.class);
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
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Function.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(FALSE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertTrue(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testFunctionAccept() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Function.class);
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
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Supplier.class);
        assertSame(MUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        assertTrue(typeInfo.isFunctionalInterface());
    }

    @Test
    public void testSupplierGet() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Supplier.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("get", 0);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        assertSame(INDEPENDENT_HC, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
    }

    @Test
    public void testPredicateTest() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Predicate.class);
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
        // Predicate.test's argument is now @NotModified (UNMODIFIED_PARAMETER = TRUE): a predicate inspects its
        // input to decide a boolean, it does not mutate it. (The method itself stays modifying, asserted above.)
        assertSame(TRUE, p0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
    }

    @Test
    public void testBiConsumerAccept() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(BiConsumer.class);
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
        assertFalse(compiledTypesManager().typeIfLoaded(Function.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(Function.class).findUniqueMethod("compose", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(Consumer.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(BiConsumer.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(Predicate.class).findUniqueMethod("and", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(Predicate.class).findUniqueMethod("or", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(Predicate.class).findUniqueMethod("negate", 0).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(BiPredicate.class).findUniqueMethod("negate", 0).isModifying());
    }

    // Predicate was missing the type-level @Independent(hc=true) all its siblings carry.
    @Test
    public void testPredicateIndependentHc() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Predicate.class);
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
            TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(c);
            assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT),
                    () -> c.getSimpleName() + " should be @Independent(hc=true)");
        }
    }

    // Every java.util.function interface is @Independent(hc=true) (a lambda may capture hidden content).
    @Test
    public void testAllFunctionalInterfacesIndependentHc() {
        for (Class<?> c : new Class<?>[]{
                BiConsumer.class, BiFunction.class, BinaryOperator.class, BiPredicate.class, BooleanSupplier.class,
                Consumer.class, DoubleBinaryOperator.class, DoubleConsumer.class, DoubleFunction.class,
                DoublePredicate.class, DoubleSupplier.class, DoubleToIntFunction.class, DoubleToLongFunction.class,
                DoubleUnaryOperator.class, Function.class, IntBinaryOperator.class, IntConsumer.class,
                IntFunction.class, IntPredicate.class, IntSupplier.class, IntToDoubleFunction.class,
                IntToLongFunction.class, IntUnaryOperator.class, LongBinaryOperator.class, LongConsumer.class,
                LongFunction.class, LongPredicate.class, LongSupplier.class, LongToDoubleFunction.class,
                LongToIntFunction.class, LongUnaryOperator.class, ObjDoubleConsumer.class, ObjIntConsumer.class,
                ObjLongConsumer.class, Predicate.class, Supplier.class, ToDoubleBiFunction.class,
                ToDoubleFunction.class, ToIntBiFunction.class, ToIntFunction.class, ToLongBiFunction.class,
                ToLongFunction.class, UnaryOperator.class}) {
            TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(c);
            assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT),
                    () -> c.getSimpleName() + " should be @Independent(hc=true)");
        }
    }

    // The generic-input specializations must mark their SAM's generic parameter @Modified, like
    // Consumer/Function (an unknown lambda body may mutate its argument); the primitive input of e.g.
    // ObjIntConsumer stays unmodified.
    @Test
    public void testGenericInputSamsAreModified() {
        assertTrue(compiledTypesManager().typeIfLoaded(ToIntFunction.class).findUniqueMethod("applyAsInt", 1)
                .parameters().getFirst().isModified());
        ParameterInfo p0 = compiledTypesManager().typeIfLoaded(ObjIntConsumer.class).findUniqueMethod("accept", 2)
                .parameters().getFirst();
        assertTrue(p0.isModified(), "ObjIntConsumer.accept T param must be @Modified");
        assertTrue(compiledTypesManager().typeIfLoaded(ToIntBiFunction.class).findUniqueMethod("applyAsInt", 2)
                .parameters().get(1).isModified());
    }

    @Test
    public void testBiFunctionAndFriendsDefaultHelpersNonModifying() {
        assertFalse(compiledTypesManager().typeIfLoaded(BiFunction.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(IntPredicate.class).findUniqueMethod("and", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(IntPredicate.class).findUniqueMethod("negate", 0).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(IntUnaryOperator.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(IntUnaryOperator.class).findUniqueMethod("compose", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(DoubleConsumer.class).findUniqueMethod("andThen", 1).isModifying());
        assertFalse(compiledTypesManager().typeIfLoaded(LongPredicate.class).findUniqueMethod("or", 1).isModifying());
    }

}
