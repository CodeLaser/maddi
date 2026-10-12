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
import io.codelaser.maddi.cst.api.info.ParameterInfo;

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
}
