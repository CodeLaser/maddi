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
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.analysis.LinkFacts;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;

import java.util.List;
import java.util.Set;
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

    /**
     * The side of an eventually immutable type's transition a method belongs to ({@code @Mark}, {@code @Only}): Rust's
     * typestate, a state parameter on the struct.
     */
    enum Side {
        /** Either state; also a {@code @TestMark}. */
        ANY,
        /** {@code @Only(before=…)} */
        BEFORE,
        /** {@code @Only(after=…)} */
        AFTER,
        /** {@code @Mark}: the transition, consuming the object before and returning it after. */
        MARK
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
            // its modifications are not the caller's concern: interior mutability (RustCells)
            if (method.isIgnoreModification()) return Receiver.SHARED;
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
        public boolean ignoreModifications(MethodInfo method) {
            return method.isIgnoreModification();
        }

        @Override
        public boolean staticSideEffects(MethodInfo method) {
            return method.analysis().getOrDefault(PropertyImpl.STATIC_SIDE_EFFECTS_METHOD, ValueImpl.BoolImpl.FALSE)
                    .isTrue();
        }

        @Override
        public Side side(MethodInfo method) {
            Value.Eventual eventual = method.analysis().getOrDefault(PropertyImpl.EVENTUAL_METHOD,
                    ValueImpl.EventualImpl.NOT_EVENTUAL);
            if (eventual.isMark()) return Side.MARK;
            if (eventual.isOnly()) return Boolean.TRUE.equals(eventual.after()) ? Side.AFTER : Side.BEFORE;
            return Side.ANY;
        }

        @Override
        public java.util.Set<String> marks(MethodInfo method) {
            return method.analysis().getOrDefault(PropertyImpl.EVENTUAL_METHOD,
                    ValueImpl.EventualImpl.NOT_EVENTUAL).fields();
        }

        @Override
        public boolean immutable(TypeInfo typeInfo) {
            Value.Immutable immutable = typeInfo.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                    ValueImpl.ImmutableImpl.class);
            return immutable != null && immutable.isImmutable();
        }

        @Override
        public boolean immutableHc(TypeInfo typeInfo) {
            Value.Immutable immutable = typeInfo.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                    ValueImpl.ImmutableImpl.class);
            return immutable != null && immutable.isImmutableHC();
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
        return of(nullability, ownership, LinkFacts.NONE);
    }

    /**
     * ... and where the results come from, from the links ({@link LinkFacts}), where the verdicts cannot say: a new
     * object is owned whatever its independence (a builder's {@code build()}), and the parameters it keeps are moved
     * in; a result inside the receiver borrows it; one inside a single borrowed parameter borrows that one.
     */
    static RustFacts of(NullabilityFacts nullability, RustFacts ownership, LinkFacts links) {
        return new RustFacts() {
            @Override
            public Receiver receiver(MethodInfo method) {
                return ownership.receiver(method);
            }

            @Override
            public Passing passing(ParameterInfo parameter) {
                MethodInfo m = parameter.methodInfo();
                if (links.result(m) == LinkFacts.Result.NEW && links.keptInResult(m).contains(parameter)) {
                    return Passing.OWNED;
                }
                return ownership.passing(parameter);
            }

            @Override
            public boolean resultBorrowsReceiver(MethodInfo method) {
                LinkFacts.Result result = links.result(method);
                if (result == LinkFacts.Result.NEW && method.returnType().isPrimitiveExcludingVoid()) {
                    return ownership.resultBorrowsReceiver(method);
                }
                // a new object holding the receiver's contents is a view (unmodifiableList(list), a borrow) or a copy
                // (a builder's 'new R(list)'): a new object where every return creates one
                if (result == LinkFacts.Result.NEW && (!links.receiverInResult(method) || returnsNew(method))) return false;
                if (result == LinkFacts.Result.EXISTING && links.receiverInResult(method)) return true;
                return ownership.resultBorrowsReceiver(method);
            }

            @Override
            public boolean resultBorrowsParameter(MethodInfo method) {
                if (links.result(method) != LinkFacts.Result.EXISTING || links.receiverInResult(method)) return false;
                Set<ParameterInfo> kept = links.keptInResult(method);
                // one borrowed parameter: the result's lifetime is elided to its
                return kept.size() == 1 && method.parameters().stream()
                        .filter(p -> !RustTypes.isCopy(p.parameterizedType()) && passing(p) != Passing.OWNED)
                        .toList().equals(List.copyOf(kept));
            }

            @Override
            public boolean fluent(MethodInfo method) {
                return ownership.fluent(method);
            }

            @Override
            public boolean ignoreModifications(MethodInfo method) {
                return ownership.ignoreModifications(method);
            }

            @Override
            public boolean staticSideEffects(MethodInfo method) {
                return ownership.staticSideEffects(method);
            }

            @Override
            public Side side(MethodInfo method) {
                return ownership.side(method);
            }

            @Override
            public java.util.Set<String> marks(MethodInfo method) {
                return ownership.marks(method);
            }

            @Override
            public boolean immutable(TypeInfo typeInfo) {
                return ownership.immutable(typeInfo);
            }

            @Override
            public boolean immutableHc(TypeInfo typeInfo) {
                return ownership.immutableHc(typeInfo);
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

    /**
     * The result is part of an object one of the method's parameters holds, and of nothing else: a borrow tied to that
     * parameter ({@code first(List<String> l)} returning {@code l.get(0)}). From the links only ({@link LinkFacts}).
     */
    default boolean resultBorrowsParameter(MethodInfo method) {
        return false;
    }

    /** {@code @IgnoreModifications}: the method's modifications are not its callers' concern (see RustCells). */
    default boolean ignoreModifications(MethodInfo method) {
        return false;
    }

    /**
     * {@code @StaticSideEffects}: the method modifies another type's static state, directly or through a method that
     * does. An {@code unsafe fn}; the printer adds what Rust guards besides, a read and a type's own state
     * ({@link RustUnsafety}).
     */
    default boolean staticSideEffects(MethodInfo method) {
        return false;
    }

    default Side side(MethodInfo method) {
        return Side.ANY;
    }

    /** The fields of the transition {@link #side} refers to: the label of {@code @Mark} and {@code @Only}. */
    default java.util.Set<String> marks(MethodInfo method) {
        return java.util.Set.of();
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

    /**
     * {@code @Immutable(hc=true)}: immutable, with hidden content. Sharing the object needs no lock and no
     * {@code RefCell}: an {@code Rc<T>}, whose clone is a share, as Java's reference is; its hidden content is shared
     * by cloning, under a {@code Clone} bound (an {@code Rc} again, or a copy that cannot be told apart).
     */
    default boolean immutableHc(TypeInfo typeInfo) {
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

    /* every return statement of the method creates its result */
    private static boolean returnsNew(MethodInfo method) {
        if (method.methodBody() == null) return false;
        boolean[] all = {true};
        boolean[] any = {false};
        method.methodBody().visit(e -> {
            if (e instanceof ReturnStatement rs) {
                any[0] = true;
                all[0] &= rs.expression() instanceof ConstructorCall;
            }
            return !(e instanceof Lambda);
        });
        return any[0] && all[0];
    }
}
