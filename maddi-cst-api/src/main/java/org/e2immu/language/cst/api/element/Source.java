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

public interface Source extends Comparable<Source> {

    boolean isNoSource();

    int beginLine();

    int beginPos();

    int endLine();

    int endPos();

    // for statements only
    String index();

    boolean isCompiledClass();

    DetailedSources detailedSources();

    Source withBeginPos(int beginPos);

    Source withEndPos(int endPos);

    Source withDetailedSources(DetailedSources detailedSources);

    Source mergeDetailedSources(DetailedSources detailedSources);

    // computations: override if you need to use these frequently

    default String compact() {
        return beginLine() + "-" + beginPos();
    }

    default String compact2() {
        return beginLine() + "-" + beginPos() + ":" + endLine() + "-" + endPos();
    }

    Source withIndex(String newIndex);

    default boolean isContainedIn(int line, int pos) {
        if (line > beginLine() && line < endLine()) return true;
        if (line < beginLine() || line > endLine()) return false;
        if (line == beginLine() && line == endLine()) return beginPos() <= pos && pos <= endPos();
        if (line == beginLine()) return beginPos() <= pos;
        return pos <= endPos();
    }
}
