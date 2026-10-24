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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * The analysis's verdicts, as the ownership modes of a Rust signature (CodeLaser/maddi#167). This is the whole point of
 * the translator: rules 1 and 3 of the Road to Immutability are Rust's "aliasing XOR mutability", so
 * <ul>
 *     <li>a non-modifying method takes {@code &self}, a modifying one {@code &mut self}, a {@code @Finalizer}
 *     {@code self};</li>
 *     <li>a parameter the method does not keep (fully independent) is borrowed, {@code &T} when unmodified and
 *     {@code &mut T} when modified; one it keeps (dependent, or independent with hidden content: stored) is moved in,
 *     {@code T};</li>
 *     <li>a result that shares the receiver's accessible content (dependent) is a borrow of it, {@code &T}; so is a
 *     result that is its hidden content, a value of a type parameter (independent with hidden content).</li>
 * </ul>
 * The printer does not soften a verdict with a {@code clone()} or an {@code Rc<RefCell<…>>}: rustc's borrow checker
 * judges the bodies against the signatures, and where it rejects one, the verdict or the printer is wrong.
 */
public interface RustFacts extends NullabilityFacts {

    /** How a method receives {@code self}. */
    enum Receiver {
        /** A static method or a constructor: no {@code self}. */
        NONE,
        /** {@code &self}: the method does not modify the object. */
        SHARED,
        /** {@code &mut self}: the method modifies the object. */
        EXCLUSIVE,
        /** {@code self}: a {@code @Finalizer}, after which the object may not be used. */
        OWNED
    }

    /** How a reference-typed parameter is passed. A primitive is always copied. */
    enum Passing {
        /** {@code &T}: read, not kept. */
        SHARED,
        /** {@code &mut T}: modified, not kept. */
        EXCLUSIVE,
        /** {@code T}: kept by the callee, moved in. */
        OWNED
    }

    /**
     * No facts: what Java says without an analysis. Every method modifies, every parameter is moved in: the most
     * conservative signatures, which compile least.
     */
    RustFacts NONE = new RustFacts() {
    };

    /** The verdicts stored on the declarations: {@code NON_MODIFYING_METHOD}, {@code UNMODIFIED_PARAMETER}, … */
    RustFacts STORED = new RustFacts() {
        @Override
        public Receiver receiver(MethodInfo method) {
            if (method.isStatic() || method.isConstructor()) return Receiver.NONE;
            if (method.isFinalizer()) return Receiver.OWNED;
            return method.isNonModifying() ? Receiver.SHARED : Receiver.EXCLUSIVE;
        }

        @Override
        public Passing passing(ParameterInfo parameter) {
            Value.Independent independent = parameter.analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER,
                    ValueImpl.IndependentImpl.class);
            if (independent == null || !independent.isIndependent()) return Passing.OWNED;
            return parameter.isUnmodified() ? Passing.SHARED : Passing.EXCLUSIVE;
        }

        @Override
        public boolean resultBorrowsReceiver(MethodInfo method) {
            if (method.isStatic() || method.isConstructor() || method.isVoid()) return false;
            Value.Independent independent = method.analysis().getOrNull(PropertyImpl.INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.class);
            // not UNMODIFIABLE_VIEW_METHOD: it marks any unmodifiable result (a fresh string or list too), not only a
            // view of the receiver's state
            if (independent == null) return false;
            // independent with hidden content, and the result is the hidden content itself: the object shared, a
            // borrow of it ('T get()')
            return independent.isDependent() || independent.isIndependentHc() && method.returnType().isTypeParameter();
        }

        @Override
        public boolean fluent(MethodInfo method) {
            return method.isFluent();
        }

        @Override
        public boolean immutable(TypeInfo typeInfo) {
            Value.Immutable immutable = typeInfo.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                    ValueImpl.ImmutableImpl.class);
            return immutable != null && immutable.isImmutable();
        }

        @Override
        public boolean unmodified(FieldInfo fieldInfo) {
            return fieldInfo.isUnmodified();
        }

        @Override
        public ParameterizedType field(FieldInfo fieldInfo) {
            return NullabilityFacts.STORED.field(fieldInfo);
        }

        @Override
        public ParameterizedType parameter(ParameterInfo parameterInfo) {
            return NullabilityFacts.STORED.parameter(parameterInfo);
        }

        @Override
        public ParameterizedType returnType(MethodInfo methodInfo) {
            return NullabilityFacts.STORED.returnType(methodInfo);
        }
    };

    /**
     * The ownership verdicts of {@code ownership}, and the nullability of {@code nullability}: the nullability pass's
     * report, which also knows the locals, with the verdicts stored on the declarations.
     */
    static RustFacts of(NullabilityFacts nullability, RustFacts ownership) {
        return new RustFacts() {
            @Override
            public Receiver receiver(MethodInfo method) {
                return ownership.receiver(method);
            }

            @Override
            public Passing passing(ParameterInfo parameter) {
                return ownership.passing(parameter);
            }

            @Override
            public boolean resultBorrowsReceiver(MethodInfo method) {
                return ownership.resultBorrowsReceiver(method);
            }

            @Override
            public boolean fluent(MethodInfo method) {
                return ownership.fluent(method);
            }

            @Override
            public boolean immutable(TypeInfo typeInfo) {
                return ownership.immutable(typeInfo);
            }

            @Override
            public boolean unmodified(FieldInfo fieldInfo) {
                return ownership.unmodified(fieldInfo);
            }

            @Override
            public ParameterizedType field(FieldInfo fieldInfo) {
                return nullability.field(fieldInfo);
            }

            @Override
            public ParameterizedType parameter(ParameterInfo parameterInfo) {
                return nullability.parameter(parameterInfo);
            }

            @Override
            public ParameterizedType returnType(MethodInfo methodInfo) {
                return nullability.returnType(methodInfo);
            }

            @Override
            public ParameterizedType local(MethodInfo method, Element declaration, LocalVariable variable) {
                return nullability.local(method, declaration, variable);
            }
        };
    }

    default Receiver receiver(MethodInfo method) {
        if (method.isStatic() || method.isConstructor()) return Receiver.NONE;
        return Receiver.EXCLUSIVE;
    }

    default Passing passing(ParameterInfo parameter) {
        return Passing.OWNED;
    }

    /** The method returns part of its receiver's accessible content: its result type is a borrow, {@code &T}. */
    default boolean resultBorrowsReceiver(MethodInfo method) {
        return false;
    }

    /** The method returns its receiver ({@code return this}): a borrow of it, {@code &Self} or {@code &mut Self}. */
    default boolean fluent(MethodInfo method) {
        return false;
    }

    /**
     * {@code @Immutable}, without hidden content: a copy cannot be told from the shared object, so a borrowed value of
     * this type may be cloned where an owned one is expected. With hidden content ({@code @Immutable(hc=true)}) a clone
     * would also copy what Java shares, and could hide a wrong independence verdict.
     */
    default boolean immutable(TypeInfo typeInfo) {
        return false;
    }

    /** No method modifies the field's object: a static one may be shared, read-only, as a {@code static}. */
    default boolean unmodified(FieldInfo fieldInfo) {
        return false;
    }

    /** NULLABLE, by the verdict for {@code declared}: what Rust types as an {@code Option<T>}. */
    static boolean nullable(ParameterizedType verdict) {
        return verdict != null && verdict.nullable() == NullableState.NULLABLE;
    }
}
