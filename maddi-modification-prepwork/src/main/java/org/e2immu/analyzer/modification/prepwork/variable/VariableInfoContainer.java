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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.variable.Variable;

public interface VariableInfoContainer {
     /* note: local variables also have a variableNature. This one can override their value
        but only when this variable nature is VariableDefinedOutsideLoop. All the others have to agree exactly,
        and can be used interchangeably.
         */

    @NotNull
    Variable variable();

    @NotNull
    VariableNature variableNature();

    // default statement/modification time
    int IGNORE_STATEMENT_TIME = -1;

    // prefixes in assignment id
    // see TestLevelSuffixes to visually understand the order

    String NOT_YET_READ = "-";

    boolean hasEvaluation();

    boolean hasMerge();

    boolean isPrevious();

    boolean has(Stage level);

    /**
     * General method for obtaining the "most relevant" <code>VariableInfo</code> object describing the state
     * of the variable after executing this statement.
     *
     * @return a VariableInfo object, always. There would not have been a <code>VariableInfoContainer</code> if there
     * was not at least one <code>VariableInfo</code> object.
     */
    @NotNull
    VariableInfo best();

    /*
     * like current, but then with a limit
     */
    @NotNull
    VariableInfo best(Stage level);

    /**
     * Returns either the current() of the previous VIC, or the initial value if this is the first statement
     * where this variable occurs.
     */
    @NotNull
    VariableInfo getPreviousOrInitial();

    /**
     * @return if the variable was created in this statement
     */
    boolean isInitial();

    boolean isRecursivelyInitial();

    @NotNull
    VariableInfo getRecursiveInitial();

    VariableInfo bestCurrentlyComputed();

    String indexOfDefinition();

    /**
     * A back-reference-free snapshot of this container: its {@code previousOrInitial} link into the
     * previous statement's container is collapsed to a value snapshot, so this container no longer pins
     * the chain of earlier statements' containers — the basis of the VariableData flatten-snapshot
     * memory lever (see {@code maddi-modification-analyzer/DESIGN-vardata-flatten.md}). The value-bearing
     * queries are preserved: {@link #best()}, {@link #best(Stage)}, {@link #has(Stage)},
     * {@link #bestCurrentlyComputed()}, {@link #indexOfDefinition()} and {@link #getPreviousOrInitial()}
     * all return what they did. Only the structural "where did this come from" queries change —
     * {@link #isInitial()}/{@link #isPrevious()}/{@link #isRecursivelyInitial()}/
     * {@link #getRecursiveInitial()} — whose callers are validated as none-external-that-matter in the
     * design. Returns {@code this} when already back-reference-free.
     */
    VariableInfoContainer flattened();
}
