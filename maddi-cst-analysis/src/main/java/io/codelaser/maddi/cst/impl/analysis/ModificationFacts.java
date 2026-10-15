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

        /*
        The same reading as the analyzer's DynamicImmutabilityInference.ofMethodCall: IMMUTABLE_METHOD at least
        immutable-HC is a claim about the result only where it is not the shallow analyzer's fallback from the declared
        return type. A type variable is immutable-HC as a type (Objects.requireNonNull returns T, and hands back what
        it was given), and an @Identity method returns its argument. A declared type that is itself immutable-HC
        (Guava's ImmutableList) does make the claim: the value is one.
         */
        @Override
        public boolean immutableContainer(MethodInfo methodInfo) {
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
     * A call of the method returns an immutable container ({@code @ImmutableContainer}: {@code List.of},
     * {@code List.copyOf}, Guava's {@code ImmutableList.of}), so whatever holds only such values can be typed
     * read-only (CodeLaser/maddi#141). Read from the stored properties, so that a better annotation in the archive
     * reaches the printer without a change to it.
     */
    default boolean immutableContainer(MethodInfo methodInfo) {
        return false;
    }
}
