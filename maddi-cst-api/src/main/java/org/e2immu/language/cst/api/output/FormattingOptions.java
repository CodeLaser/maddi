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

package org.e2immu.language.cst.api.output;

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

    boolean binaryOperatorsAtEndOfLine();

    boolean compact();

    boolean allStaticFieldsRequireType();

    boolean allFieldsRequireThis();

    int lengthOfLine();

    boolean skipComments();

    int spacesInTab();

    int tabsForLineSplit();

    WrapStyle wrapStyle();
}
