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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;

import java.util.function.Predicate;

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

    /** The verdicts stored on the declarations ({@code UNMODIFIED_PARAMETER}, {@code NULLABILITY_*}). */
    CSharpFacts STORED = of(NullabilityFacts.STORED, ParameterInfo::isUnmodified);

    /**
     * {@link #STORED}, from a nullability analysis of the whole program under a null-marked policy: a declaration
     * without {@code NULLABLE} is not null, and C#'s nullable reference types are enabled.
     */
    CSharpFacts STORED_NULL_MARKED = of(NullabilityFacts.STORED.nullMarked(), ParameterInfo::isUnmodified);

    /** {@code nullability}'s answers, and {@code unmodified} for the modification analysis's. */
    static CSharpFacts of(NullabilityFacts nullability, Predicate<ParameterInfo> unmodified) {
        return new ComposedFacts(nullability, unmodified);
    }

    /** The method does not modify the object {@code parameter} holds: a collection is read-only there. */
    default boolean unmodified(ParameterInfo parameter) {
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
