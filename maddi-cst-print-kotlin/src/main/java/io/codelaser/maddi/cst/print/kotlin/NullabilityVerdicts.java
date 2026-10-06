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
}
