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

package io.codelaser.maddi.cst.api.output;

public interface FormattingOptions {

    /**
     * Strategy for wrapping the elements of a guide block (parameter lists, argument lists, etc.)
     * once the block does not fit on the current line.
     * <p>
     * CHOP_DOWN — split at every candidate position (one element per line). Historical default.
     * GREEDY_FILL — fill each line up to {@link #lengthOfLine()}, only splitting when the next
     *               element would overflow. Falls back to CHOP_DOWN behaviour for a block whose
     *               own elements already contain newlines (a nested block that had to wrap).
     */
    enum WrapStyle {
        CHOP_DOWN, GREEDY_FILL
    }

    boolean compact();

    boolean allStaticFieldsRequireType();

    boolean allFieldsRequireThis();

    int lengthOfLine();

    boolean skipComments();

    int spacesInTab();

    WrapStyle wrapStyle();

    /**
     * When true, guide blocks marked as a {@code prioritySplit} (class bodies, method bodies,
     * other {@code generatorForBlock} structures) are always broken onto their own indented
     * lines, even when they would fit on the current line. This yields conventional
     * "brace on its own line" layout instead of the compact, single-line-if-it-fits default.
     * Default false, preserving the historical compact behaviour.
     */
    boolean alwaysBreakPriorityBlocks();

    /**
     * Brace placement. When true (Allman style), the '{' of a class, method or statement body goes on a line of its
     * own, aligned with the declaration or statement it belongs to, and the body is always broken onto its own
     * lines; 'else', 'catch', 'finally' and the 'while' of a do-while start the line after a '}'. An array
     * initializer, a lambda's or an empty body keep their brace. Default false: the brace ends the line (K&R).
     * A consumer that inferred a project's style maps "braces not at end of line" to this
     * (CodeLaser/maddi#49).
     */
    default boolean bracesOnOwnLine() {
        return false;
    }

    /**
     * Blank lines. When true, a blank line is written where the source has one, between statements, between
     * members, after the package declaration and after the imports, and nowhere else; the formatter's own blank
     * lines, around statements and members that span several lines, are not written. Code that a tool moves then
     * keeps the vertical layout its authors gave it. An element without a source has no blank line in front of it.
     * Default false: the formatter's own blank lines (CodeLaser/maddi#49).
     */
    default boolean blankLinesFromSource() {
        return false;
    }
}
