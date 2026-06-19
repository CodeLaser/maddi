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

/**
 * Common super-interface for the jump statements {@link BreakStatement} and {@link ContinueStatement}.
 */
public interface BreakOrContinueStatement extends Statement {

    /**
     * @return the label this {@code break}/{@code continue} jumps to (the {@code L} in {@code break L;}),
     * or {@code null} when it targets the innermost enclosing loop/switch. Distinct from
     * {@link Statement#label()}, which is the label attached <em>to</em> this statement.
     */
    String goToLabel();
}
