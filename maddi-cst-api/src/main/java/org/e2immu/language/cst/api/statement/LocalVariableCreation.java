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

package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.variable.LocalVariable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface LocalVariableCreation extends Statement {

    boolean isFinal();

    interface Modifier {
        boolean isFinal(); // the only one in Java; in Kotlin, this represents "val"

        boolean isWithoutTypeSpecification(); // Java "var"
    }

    boolean isVar();

    Set<Modifier> modifiers();

    LocalVariable localVariable();

    List<LocalVariable> otherLocalVariables();

    Stream<LocalVariable> localVariableStream();

    /**
     * Convenience method
     *
     * @return all the new local variables created here
     */
    default Set<LocalVariable> newLocalVariables() {
        return localVariableStream().collect(Collectors.toUnmodifiableSet());
    }

    default boolean hasSingleDeclaration() {
        return otherLocalVariables().isEmpty();
    }

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder addModifier(Modifier modifier);

        @Fluent
        Builder setLocalVariable(LocalVariable localVariable);

        @Fluent
        Builder addOtherLocalVariable(LocalVariable localVariable);

        LocalVariableCreation build();
    }

    String NAME = "localVariableCreation";

    @Override
    default String name() {
        return NAME;
    }

    LocalVariableCreation withSource(Source newSource);

}
