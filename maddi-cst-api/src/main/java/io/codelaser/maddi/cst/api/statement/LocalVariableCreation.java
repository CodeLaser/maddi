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

package io.codelaser.maddi.cst.api.statement;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.variable.LocalVariable;

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
