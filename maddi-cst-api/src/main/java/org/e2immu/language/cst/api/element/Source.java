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

package org.e2immu.language.cst.api.element;

/**
 * A source-position range describing where a CST element appears in source.
 * <p>
 * Coordinates are 1-based line numbers and 1-based column positions.
 * A {@code Source} is attached to every {@link Element} for error reporting and IDE navigation;
 * it may optionally carry {@link DetailedSources} with fine-grained positions for individual
 * tokens within the element.
 * <p>
 * The special sentinel returned by {@link #isNoSource()} signals that no position information
 * is available (e.g. for synthesised or predefined elements).
 */
public interface Source extends Comparable<Source> {

    /** Returns {@code true} for the sentinel that represents the absence of source information. */
    boolean isNoSource();

    /** Returns the 1-based line number where this element begins. */
    int beginLine();

    /** Returns the 1-based column position where this element begins. */
    int beginPos();

    /** Returns the 1-based line number where this element ends. */
    int endLine();

    /** Returns the 1-based column position where this element ends (inclusive). */
    int endPos();

    /**
     * Returns the hierarchical index of this statement within its enclosing block
     * (e.g. {@code "0.1.2"}). Only meaningful for statements; returns {@code null} or {@code "-"} otherwise.
     */
    String index();

    /** Returns {@code true} if this source originates from a compiled {@code .class} file rather than parsed source. */
    boolean isCompiledClass();

    /** Returns fine-grained token-level source positions, or {@code null} if not available. */
    DetailedSources detailedSources();

    /** Returns the combination of this source and {@code other}, based on concatenation of sources. */
    Source max(Source other);

    /**
     * Returns the character span on a single line, or {@link Integer#MAX_VALUE} if the
     * element spans multiple lines.
     */
    default int posDiff() {
        if (beginLine() == endLine()) {
            return endPos() - beginPos() + 1;
        }
        return Integer.MAX_VALUE;
    }

    /** Returns a copy of this source with a different begin column. */
    Source withBeginPos(int beginPos);

    /** Returns a copy of this source with a different end column. */
    Source withEndPos(int endPos);

    /** Returns a copy of this source with the given detailed sources replacing any existing ones. */
    Source withDetailedSources(DetailedSources detailedSources);

    /** Returns a copy of this source with the given detailed sources merged into any existing ones. */
    Source mergeDetailedSources(DetailedSources detailedSources);

    /** Returns a short {@code "line-col"} string for logging purposes. */
    default String compact() {
        return beginLine() + "-" + beginPos();
    }

    /** Returns a {@code "beginLine-beginCol:endLine-endCol"} string for logging purposes. */
    default String compact2() {
        return beginLine() + "-" + beginPos() + ":" + endLine() + "-" + endPos();
    }

    /** Returns a copy of this source with the given statement index. */
    Source withIndex(String newIndex);

    /** Returns {@code true} if the given {@code line}:{@code pos} coordinate falls inside this range. */
    default boolean isContainedIn(int line, int pos) {
        if (line > beginLine() && line < endLine()) return true;
        if (line < beginLine() || line > endLine()) return false;
        if (line == beginLine() && line == endLine()) return beginPos() <= pos && pos <= endPos();
        if (line == beginLine()) return beginPos() <= pos;
        return pos <= endPos();
    }

    /** Returns the number of source lines spanned by this element. */
    default int lines() {
        return endLine() - beginLine() + 1;
    }

    default Source ofIndex(String string, int from, int toExcl) {
        throw new UnsupportedOperationException();
    }

}
