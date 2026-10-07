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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;

/**
 * Where a Kotlin {@code ?} goes: a declaration's type with its {@link
 * io.codelaser.maddi.cst.api.type.NullableState} decided, by a nullability analysis the printer does not run
 * itself. maddi-mod's NullabilityPass computes one ({@code Report.verdicts()}). The answer replaces the declared
 * type when it is not null; null means "no verdict", and the printer uses the declared type as it is.
 */
public interface NullabilityVerdicts {

    NullabilityVerdicts NONE = new NullabilityVerdicts() {
    };

    default ParameterizedType field(FieldInfo fieldInfo) {
        return null;
    }

    default ParameterizedType parameter(ParameterInfo parameterInfo) {
        return null;
    }

    default ParameterizedType returnType(MethodInfo methodInfo) {
        return null;
    }

    /**
     * A local variable, identified by the element that declares it (a LocalVariableCreation, ForEachStatement or
     * catch clause) in {@code method}: a local's equality is its name, which does not tell same-named locals apart.
     */
    default ParameterizedType local(MethodInfo method, Element declaration, LocalVariable variable) {
        return null;
    }

    /**
     * The local variable or parameter is known non-null when {@code statement} starts: after
     * {@code if (v == null) return;}, inside {@code if (v != null)}, after {@code v.m()}. Kotlin smart-casts it there
     * as well, so a nullable {@code v} needs no {@code !!}. (Java requires a local a lambda captures to be
     * effectively final, so the smart cast cannot be lost to a lambda.)
     * <p>
     * ⚠ Only facts Kotlin's smart cast follows: NullabilityPass's {@code Report.smartCasts()}, not
     * {@code Report.useSites()}, which also counts {@code Objects.requireNonNull(v)}, {@code assert v != null} and
     * a non-null Java contract, after which Kotlin still needs the {@code !!}.
     */
    default boolean nonNullAt(io.codelaser.maddi.cst.api.statement.Statement statement,
                              io.codelaser.maddi.cst.api.variable.Variable variable) {
        return false;
    }

    /**
     * As {@link #nonNullAt(io.codelaser.maddi.cst.api.statement.Statement, io.codelaser.maddi.cst.api.variable.Variable)},
     * at a method or constructor call: also what the enclosing condition establishes ({@code v.m()} in
     * {@code v != null && v.m()}, {@code f(w)} in {@code w == null ? 0 : f(w)}). For a field: a final one of
     * {@code this}, which Kotlin smart-casts as a val. Report.smartCasts() again.
     */
    default boolean nonNullAt(io.codelaser.maddi.cst.api.expression.Expression expression,
                              io.codelaser.maddi.cst.api.variable.Variable variable) {
        return false;
    }
}
