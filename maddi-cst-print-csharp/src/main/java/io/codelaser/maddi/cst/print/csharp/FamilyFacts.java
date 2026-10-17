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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.analysis.ForwardingNullabilityFacts;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The analysis's facts, with a method's result nullable where a method of the program overriding it may return null:
 * C# lets an override narrow {@code T?} to {@code T}, not widen {@code T} to {@code T?} (CS8764, CS8766), and a call
 * through the base reaches the override. The analysis decides each method alone; the printer, its override family.
 */
final class FamilyFacts extends ForwardingNullabilityFacts implements CSharpFacts {

    private final CSharpFacts facts;
    private final Set<MethodInfo> overriddenByNullable;

    private FamilyFacts(CSharpFacts facts, Set<MethodInfo> overriddenByNullable) {
        super(facts);
        this.facts = facts;
        this.overriddenByNullable = Set.copyOf(overriddenByNullable);
    }

    /** {@code facts} over the methods of {@code types}; {@code facts} itself without a nullability analysis. */
    static CSharpFacts of(CSharpFacts facts, Collection<TypeInfo> types) {
        if (!facts.nullabilityAnalysed()) return facts;
        Set<MethodInfo> overridden = new HashSet<>();
        for (TypeInfo t : types) {
            for (MethodInfo m : t.methods()) {
                if (m.isStatic() || facts.nullable(m) != NullableState.NULLABLE) continue;
                m.overrides().stream().filter(o -> o != m).forEach(overridden::add);
            }
        }
        return overridden.isEmpty() ? facts : new FamilyFacts(facts, overridden);
    }

    @Override
    public ParameterizedType returnType(MethodInfo methodInfo) {
        ParameterizedType verdict = super.returnType(methodInfo);
        if (!overriddenByNullable.contains(methodInfo)) return verdict;
        ParameterizedType declared = verdict != null ? verdict : methodInfo.returnType();
        if (declared.isVoid() || declared.isPrimitiveExcludingVoid() && declared.arrays() == 0) return verdict;
        return declared.withNullable(NullableState.NULLABLE);
    }

    @Override
    public NullableState nullable(MethodInfo method) {
        if (!overriddenByNullable.contains(method)) return facts.nullable(method);
        ParameterizedType declared = method.returnType();
        return declared.isVoid() || declared.isPrimitiveExcludingVoid() && declared.arrays() == 0
                ? facts.nullable(method) : NullableState.NULLABLE;
    }

    @Override
    public NullableState nullable(FieldInfo field) {
        return facts.nullable(field);
    }

    @Override
    public NullableState nullable(ParameterInfo parameter) {
        return facts.nullable(parameter);
    }

    @Override
    public NullableState nullable(MethodInfo method, Element declaration, LocalVariable variable) {
        return facts.nullable(method, declaration, variable);
    }

    @Override
    public boolean unmodified(ParameterInfo parameter) {
        return facts.unmodified(parameter);
    }
}
