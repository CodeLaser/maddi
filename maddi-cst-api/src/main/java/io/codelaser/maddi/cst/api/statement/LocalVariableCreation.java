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

/**
 * A local variable declaration statement, for example {@code int i = 0;} or {@code final var a = b, c = d;}.
 *
 * <p>The first declared variable is {@link #localVariable()}; any further variables sharing the same
 * declaration (the {@code c} in {@code int b, c;}) are {@link #otherLocalVariables()}.
 * {@link #localVariableStream()} streams all of them. Each {@link LocalVariable} carries its own type and
 * (optional) initialiser.
 */
public interface LocalVariableCreation extends Statement {

    /**
     * @return {@code true} if the declaration is {@code final} (or, in Kotlin, {@code val}).
     */
    boolean isFinal();

    /**
     * A declaration modifier. In Java the only modifier is {@code final}; {@code var} is represented as
     * the absence of an explicit type ({@link #isWithoutTypeSpecification()}).
     */
    interface Modifier {
        boolean isFinal(); // the only one in Java; in Kotlin, this represents "val"

        boolean isWithoutTypeSpecification(); // Java "var"
    }

    /**
     * @return {@code true} if the type was written as {@code var} (inferred) rather than spelled out.
     */
    boolean isVar();

    Set<Modifier> modifiers();

    /**
     * @return the first (or only) variable declared here.
     */
    LocalVariable localVariable();

    /**
     * @return the additional variables sharing this declaration ({@code b, c} → {@code c} here), empty
     * for a single declaration.
     */
    List<LocalVariable> otherLocalVariables();

    /**
     * @return a stream of all variables declared here: {@link #localVariable()} followed by
     * {@link #otherLocalVariables()}.
     */
    Stream<LocalVariable> localVariableStream();

    /**
     * Convenience method
     *
     * @return all the new local variables created here
     */
    default Set<LocalVariable> newLocalVariables() {
        return localVariableStream().collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return {@code true} when exactly one variable is declared (no {@link #otherLocalVariables()}).
     */
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

    /**
     * @return an immutable copy of this statement with a different {@link Source}; this instance is
     * unchanged.
     */
    LocalVariableCreation withSource(Source newSource);

    /**
     * Return an immutable copy of this declaration with one more variable appended (to
     * {@link #otherLocalVariables()}), taken from the given single declaration.
     *
     * @param singleLvc a single-variable declaration whose variable is added here
     * @return a new statement; this instance is unchanged
     */
    LocalVariableCreation withAdditionalLocalVariable(LocalVariableCreation singleLvc);

}
