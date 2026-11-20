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

package io.codelaser.maddi.cst.print.csharp;

import java.util.function.Predicate;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.analysis.ModificationFacts;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;

/**
 * What the analysis knows about the program, for the C# the printer chooses: the modification analysis's verdicts
 * (an unmodified collection parameter is read-only in C#) and the nullability analysis's ({@code string?}), the
 * {@link NullabilityFacts} every null-checked target asks for. The printer asks; {@link #STORED} answers from what
 * maddi-mod's analyzer stored on the declarations, which is nothing when no analysis ran, so that the output is then
 * the same as without facts. A client with an analysis report of its own passes it to {@link #of} (maddi-mod's C#
 * ratchets do).
 * <p>
 * C# needs a declaration's top-level state only: a type argument it prints is not nullable ({@code List<Exprent>}),
 * and the {@link NullableState}-valued methods here take that state from the verdict.
 */
public interface CSharpFacts extends NullabilityFacts {

    /** No facts: every answer is "unknown". */
    CSharpFacts NONE = new CSharpFacts() {
    };

    /** The verdicts stored on the declarations ({@code UNMODIFIED_PARAMETER}, {@code STRUCTURALLY_UNMODIFIED_FIELD}, {@code NULLABILITY_*}). */
    CSharpFacts STORED = of(NullabilityFacts.STORED, ModificationFacts.STORED);

    /**
     * {@link #STORED}, from a nullability analysis of the whole program under a null-marked policy: a declaration
     * without {@code NULLABLE} is not null, and C#'s nullable reference types are enabled.
     */
    CSharpFacts STORED_NULL_MARKED = of(NullabilityFacts.STORED.nullMarked(), ModificationFacts.STORED);

    /** {@code nullability}'s answers, and {@code modification}'s for the modification analysis's. */
    static CSharpFacts of(NullabilityFacts nullability, ModificationFacts modification) {
        return new ComposedFacts(nullability, modification);
    }

    /** As above, for a client with the modification analysis's one verdict on parameters: {@code unmodified}. */
    static CSharpFacts of(NullabilityFacts nullability, Predicate<ParameterInfo> unmodified) {
        return of(nullability, new ModificationFacts() {
            @Override
            public boolean unmodified(ParameterInfo parameterInfo) {
                return unmodified.test(parameterInfo);
            }
        });
    }

    /** The method does not modify the object {@code parameter} holds: a collection is read-only there. */
    default boolean unmodified(ParameterInfo parameter) {
        return false;
    }

    /**
     * No code adds to or removes from the collection the field holds (its elements may change): a private collection
     * field is read-only, {@code IReadOnlyList<T>}, where its uses allow ({@link CSharpReadOnly}).
     */
    default boolean structurallyUnmodified(FieldInfo field) {
        return false;
    }

    /**
     * The method adds nothing to and removes nothing from its receiver: a library's read, by the archive's contract
     * ({@code @NotModified}), which makes a call of it on a view of a collection a read of the collection.
     */
    default boolean structurallyNonModifying(MethodInfo method) {
        return false;
    }

    /**
     * A call of the method returns an immutable value or an unmodifiable view ({@code List.of}, {@code List.copyOf},
     * {@code Collections.unmodifiableList}): a method that returns only such values, or read-only fields, may have
     * a read-only result ({@link CSharpReadOnly}).
     */
    default boolean unmodifiableResult(MethodInfo method) {
        return false;
    }

    /** {@code NULLABLE}: the field may hold null, its type is {@code T?}. */
    default NullableState nullable(FieldInfo field) {
        return state(field(field));
    }

    default NullableState nullable(ParameterInfo parameter) {
        return state(parameter(parameter));
    }

    /** The method may return null. */
    default NullableState nullable(MethodInfo method) {
        return state(returnType(method));
    }

    /**
     * A local variable of {@code method}, identified by the element that declares it (a LocalVariableCreation, a
     * ForEachStatement): {@code NULLABLE} when a null may reach it, and for a verdict that is no decision (see
     * {@link NullabilityFacts#decided}). Only the analysis's report knows; not stored.
     */
    default NullableState nullable(MethodInfo method, Element declaration, LocalVariable variable) {
        return state(NullabilityFacts.decided(local(method, declaration, variable)));
    }

    private static NullableState state(ParameterizedType verdict) {
        return verdict == null ? NullableState.UNSPECIFIED : verdict.nullable();
    }
}
