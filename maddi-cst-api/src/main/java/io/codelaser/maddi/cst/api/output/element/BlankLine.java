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


package io.codelaser.maddi.cst.api.output.element;


/**
 * Marks a blank line in the source in front of the element that follows: a statement, a member, the imports or a
 * type declaration. A printer adds it when the sources of two neighbours say so; the formatter writes it only when
 * {@link io.codelaser.maddi.cst.api.output.FormattingOptions#blankLinesFromSource()} is set, and then writes no
 * blank line of its own (CodeLaser/maddi#49). It is empty otherwise.
 * <p>
 * It is a {@link Space}: no space, never a split. A client that walks an output and knows nothing of blank lines
 * (a tokenizer) treats it as the empty space it is to them, rather than meeting an element type it does not know.
 * A formatter that does know must test for it BEFORE it tests for a space.
 */
public interface BlankLine extends Space {
}
