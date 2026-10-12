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

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * What the analysis knows about the program, for the C# the printer chooses: the modification analysis's verdicts
 * (an unmodified collection parameter is read-only in C#) and the nullability analysis's ({@code string?}). The
 * printer asks; {@link #STORED} answers from what maddi-mod's analyzer stored on the declarations, which is nothing
 * when no analysis ran, so that the output is then the same as without facts. A client with an analysis report of its
 * own implements this interface (maddi-mod's C# ratchets do).
 */
public interface CSharpFacts {

    /** No facts: every answer is "unknown". */
    CSharpFacts NONE = new CSharpFacts() {
    };

    /** The verdicts stored on the declarations ({@code UNMODIFIED_PARAMETER}, {@code NULLABILITY_*}). */
    CSharpFacts STORED = new CSharpFacts() {
        @Override
        public boolean unmodified(ParameterInfo parameter) {
            return parameter.isUnmodified();
        }

        @Override
        public NullableState nullable(FieldInfo field) {
            return stored(field, PropertyImpl.NULLABILITY_FIELD);
        }

        @Override
        public NullableState nullable(ParameterInfo parameter) {
            return stored(parameter, PropertyImpl.NULLABILITY_PARAMETER);
        }

        @Override
        public NullableState nullable(MethodInfo method) {
            return stored(method, PropertyImpl.NULLABILITY_METHOD);
        }

        private static NullableState stored(Info info, Property property) {
            Value.Nullability n = info.analysis().getOrDefault(property, ValueImpl.NullabilityImpl.UNSPECIFIED);
            return n.isDefault() ? NullableState.UNSPECIFIED : n.state();
        }
    };

    /**
     * {@link #STORED}, from a nullability analysis of the whole program under a null-marked policy: a declaration
     * without {@code NULLABLE} is not null, and C#'s nullable reference types are enabled.
     */
    CSharpFacts STORED_NULL_MARKED = new CSharpFacts() {
        @Override
        public boolean unmodified(ParameterInfo parameter) {
            return STORED.unmodified(parameter);
        }

        @Override
        public NullableState nullable(FieldInfo field) {
            return STORED.nullable(field);
        }

        @Override
        public NullableState nullable(ParameterInfo parameter) {
            return STORED.nullable(parameter);
        }

        @Override
        public NullableState nullable(MethodInfo method) {
            return STORED.nullable(method);
        }

        @Override
        public boolean nullabilityAnalysed() {
            return true;
        }
    };

    /** The method does not modify the object {@code parameter} holds: a collection is read-only there. */
    default boolean unmodified(ParameterInfo parameter) {
        return false;
    }

    /** {@code NULLABLE}: the field may hold null, its type is {@code T?}. */
    default NullableState nullable(FieldInfo field) {
        return NullableState.UNSPECIFIED;
    }

    default NullableState nullable(ParameterInfo parameter) {
        return NullableState.UNSPECIFIED;
    }

    /** The method may return null. */
    default NullableState nullable(MethodInfo method) {
        return NullableState.UNSPECIFIED;
    }

    /**
     * A local variable of {@code method}, identified by the element that declares it (a LocalVariableCreation, a
     * ForEachStatement): {@code NULLABLE} when a null may reach it. Only the analysis's report knows; not stored.
     */
    default NullableState nullable(MethodInfo method, Element declaration, LocalVariable variable) {
        return NullableState.UNSPECIFIED;
    }

    /**
     * The local is non-null from its declaration on although its initializer may be null: Java dereferences it
     * unconditionally later in the same block, with no reassignment in between. The printer asserts the initializer,
     * {@code blocks.GetWithKey(j)!}; {@link #nullable(MethodInfo, Element, LocalVariable)} answers non-null for it.
     */
    default boolean assertedAtDeclaration(MethodInfo method, Element declaration, LocalVariable variable) {
        return false;
    }

    /**
     * The local variable, parameter or field is known non-null where {@code statement} starts: after
     * {@code if (v == null) return;}, inside {@code if (v != null)}, after {@code v.m()}. C#'s flow analysis knows
     * it as well, so a dereference there needs no {@code !}.
     */
    default boolean nonNullAt(Statement statement, Variable variable) {
        return false;
    }

    /** As {@link #nonNullAt(Statement, Variable)}, at a call or a field access: also what its condition establishes. */
    default boolean nonNullAt(Expression expression, Variable variable) {
        return false;
    }

    /** The callee may return null, but not this call: no argument it passes makes the result null. */
    default boolean callResultNonNull(Expression call) {
        return false;
    }

    /**
     * The nullability analysis ran over the whole program: a type without {@code ?} is not null, and the translation's
     * project enables C#'s nullable reference types.
     */
    default boolean nullabilityAnalysed() {
        return false;
    }
}
