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
package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * What the modification analysis knows about the program, for a printer that writes read-only types (Kotlin's
 * {@code List} over {@code MutableList}, C#'s {@code IReadOnlyList}): which collections are not modified, and which
 * methods return their argument. The printer asks; {@link #STORED} answers from what maddi-mod's analyzer stored on
 * the declarations, which is nothing when no analysis ran. {@link #NONE} answers "not known" everywhere: the printer
 * without the modification analysis, for an ablation beside {@link NullabilityFacts#NONE}.
 */
public interface ModificationFacts {

    /** Nothing known: every collection printed mutable. */
    ModificationFacts NONE = new ModificationFacts() {
    };

    /** The verdicts the analyzer stored: {@code STRUCTURALLY_UNMODIFIED_*} (else {@code UNMODIFIED_*}), {@code IDENTITY_METHOD}. */
    ModificationFacts STORED = new ModificationFacts() {
        @Override
        public boolean structurallyUnmodified(FieldInfo fieldInfo) {
            return fieldInfo.isStructurallyUnmodified();
        }

        @Override
        public boolean structurallyUnmodified(ParameterInfo parameterInfo) {
            return parameterInfo.isStructurallyUnmodified();
        }

        @Override
        public boolean identity(MethodInfo methodInfo) {
            return methodInfo.isIdentity();
        }

        @Override
        public boolean structurallyNonModifying(MethodInfo methodInfo) {
            return methodInfo.isStructurallyNonModifying();
        }

        @Override
        public boolean nonModifying(MethodInfo methodInfo) {
            return methodInfo.isNonModifying();
        }

        /*
        The same reading as the analyzer's DynamicImmutabilityInference.ofMethodCall: IMMUTABLE_METHOD at least
        immutable-HC is a claim about the result only where it is not the shallow analyzer's fallback from the declared
        return type. A type variable is immutable-HC as a type (Objects.requireNonNull returns T, and hands back what
        it was given), and an @Identity method returns its argument. A declared type that is itself immutable-HC
        (Guava's ImmutableList) does make the claim: the value is one.
         */
        @Override
        public boolean immutableResult(MethodInfo methodInfo) {
            if (methodInfo.isConstructor() || methodInfo.isIdentity()) return false;
            ParameterizedType returnType = methodInfo.returnType();
            if (returnType == null || returnType.typeParameter() != null || returnType.typeInfo() == null) return false;
            Value.Immutable ofType = returnType.typeInfo().analysis()
                    .getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
            if (ofType != null && ofType.isAtLeastImmutableHC()) return true;
            Value.Immutable ofMethod = methodInfo.analysis()
                    .getOrNull(PropertyImpl.IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.class);
            return ofMethod != null && ofMethod.isAtLeastImmutableHC();
        }
        @Override
        public boolean unmodifiableResult(MethodInfo methodInfo) {
            if (methodInfo.isConstructor()) return false;
            Value.Bool ofMethod = methodInfo.analysis()
                    .getOrNull(PropertyImpl.UNMODIFIABLE_VIEW_METHOD, ValueImpl.BoolImpl.class);
            if (ofMethod != null && ofMethod.isTrue()) return true;
            ParameterizedType returnType = methodInfo.returnType();
            if (returnType == null || returnType.arrays() > 0 || returnType.typeInfo() == null) return false;
            Value.Bool ofType = returnType.typeInfo().analysis()
                    .getOrNull(PropertyImpl.UNMODIFIABLE_TYPE, ValueImpl.BoolImpl.class);
            return ofType != null && ofType.isTrue();
        }
    };

    /** No code modifies the collection the field holds: no element is added or removed (its elements may change). */
    default boolean structurallyUnmodified(FieldInfo fieldInfo) {
        return false;
    }

    /** The method does not add to or remove from the collection the parameter holds. */
    default boolean structurallyUnmodified(ParameterInfo parameterInfo) {
        return false;
    }

    /** The method returns its first argument ({@code @Identity}): what goes in comes out, read-only or not. */
    default boolean identity(MethodInfo methodInfo) {
        return false;
    }

    /**
     * The method adds nothing to and removes nothing from its receiver (it may change the elements): a call a
     * read-only collection type allows (CodeLaser/maddi#141). The verdict of the analysis, or the library's
     * contract ({@code @NotModified}), rather than a list of names.
     */
    default boolean structurallyNonModifying(MethodInfo methodInfo) {
        return false;
    }

    /**
     * The method modifies nothing: neither its receiver nor what that holds ({@code NON_MODIFYING_METHOD}). Two
     * calls in a row answer the same, so a printer may call it once where Java calls it twice (CodeLaser/maddi#164).
     */
    default boolean nonModifying(MethodInfo methodInfo) {
        return false;
    }

    /**
     * A call of the method returns an object that is at least immutable-HC ({@code IMMUTABLE_METHOD}, from the
     * archive's {@code @ImmutableContainer} on {@code List.of} and {@code List.copyOf}, or a type that is one, as
     * Guava's {@code ImmutableList}), so whatever holds only such values can be typed read-only (CodeLaser/maddi#141).
     * The immutability of the result only: the container aspect ({@code @Container}, no method modifies its
     * parameters) is a separate property, and not read here. Read from the stored properties, so that a better annotation in the archive
     * reaches the printer without a change to it.
     */
    default boolean immutableResult(MethodInfo methodInfo) {
        return false;
    }

    /**
     * The method's result is an object nothing is added to or removed from through that reference, though it may be
     * a view of one that changes: {@code Collections.unmodifiableList(x)} ({@code UNMODIFIABLE_VIEW_METHOD}, a
     * contract on library methods and computed from the returned values otherwise), or a value of a type that
     * admits no structural change through any reference ({@code UNMODIFIABLE_TYPE}). Weaker than
     * {@link #immutableResult}; enough for a read-only collection type (CodeLaser/maddi-mod#52).
     */
    default boolean unmodifiableResult(MethodInfo methodInfo) {
        return false;
    }
}
