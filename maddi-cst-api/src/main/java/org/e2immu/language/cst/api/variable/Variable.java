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

package org.e2immu.language.cst.api.variable;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.util.OneVariable;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.stream.Stream;

/**
 * It is important that we can sort variables, as part of the sorting system of Expressions.
 */
public interface Variable extends Comparable<Variable>, Element, OneVariable {

    @NotNull
    String fullyQualifiedName();

    @NotNull
    String simpleName();

    @NotNull
    ParameterizedType parameterizedType();

    @Override
    default int compareTo(Variable o) {
        return fullyQualifiedName().compareTo(o.fullyQualifiedName());
    }

    default boolean isStatic() {
        return false;
    }

    default boolean scopeIsRecursively(Variable variable) {
        return false;
    }

    default FieldReference fieldReferenceScope() {
        return null;
    }

    default Variable fieldReferenceBase() {
        return null;
    }

    default Stream<Variable> variableStreamDescendIntoScope() {
        return Stream.of(this);
    }

    Variable rewire(InfoMap infoMap);
}
