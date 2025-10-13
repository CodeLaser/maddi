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

package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;

import java.util.Objects;

/*
we must be a bit memory-conscious: no unnecessary fields because there may be millions of elements
 */
public class SourceImpl implements Source {
    public static final Source NO_SOURCE = new SourceImpl(null, 0, 0, 0, 0);
    private final String index;
    private final int beginLine;
    private final int beginPos;
    private final int endLine;
    private final int endPos;
    private final DetailedSources detailedSources;

    public SourceImpl(String index, int beginLine, int beginPos, int endLine, int endPos) {
        this(index, beginLine, beginPos, endLine, endPos, null);
    }

    public SourceImpl(String index, int beginLine, int beginPos, int endLine, int endPos,
                      DetailedSources detailedSources) {
        // we internalize, because there are many repeats here ("0", "1", ...)
        this.index = index == null ? null : index.intern();
        this.beginLine = beginLine;
        this.beginPos = beginPos;
        this.endLine = endLine;
        this.endPos = endPos;
        this.detailedSources = detailedSources;
    }

    @Override
    public boolean isNoSource() {
        return this == NO_SOURCE || beginLine == 0 || endLine == 0 || beginPos == 0 || endPos == 0;
    }

    public static Source forCompiledClass(CompilationUnit compilationUnit) {
        return new SourceImpl(null, -1, -1, -1, -1, null);
    }

    @Override
    public DetailedSources detailedSources() {
        return detailedSources;
    }

    @Override
    public Source withDetailedSources(DetailedSources detailedSources) {
        if (Objects.equals(detailedSources, this.detailedSources)) return this;
        return new SourceImpl(index, beginLine, beginPos, endLine, endPos, detailedSources);
    }

    @Override
    public Source mergeDetailedSources(DetailedSources detailedSources) {
        DetailedSources newDetailedSources = this.detailedSources == null ? detailedSources
                : detailedSources == null ? this.detailedSources
                : this.detailedSources.merge(detailedSources);
        return new SourceImpl(index, beginLine, beginPos, endLine, endPos, newDetailedSources);
    }

    @Override
    public boolean isCompiledClass() {
        return beginLine == -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceImpl source)) return false;
        return beginLine == source.beginLine && beginPos == source.beginPos && endLine == source.endLine && endPos == source.endPos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginLine, beginPos, endLine, endPos);
    }

    @Override
    public int compareTo(Source o) {
        if (o instanceof SourceImpl s) {
            int bl = beginLine - s.beginLine;
            if (bl != 0) return bl;
            int bp = beginPos - s.beginPos;
            if (bp != 0) return bp;
            int el = endLine - s.endLine;
            if (el != 0) return el;
            return endPos - endLine;
        } else throw new UnsupportedOperationException();
    }


    @Override
    public int beginLine() {
        return beginLine;
    }

    @Override
    public int beginPos() {
        return beginPos;
    }

    @Override
    public int endLine() {
        return endLine;
    }

    @Override
    public int endPos() {
        return endPos;
    }

    @Override
    public Source withBeginPos(int beginPos) {
        return new SourceImpl(index, beginLine, beginPos, endLine, endPos);
    }

    @Override
    public Source withEndPos(int endPos) {
        return new SourceImpl(index, beginLine, beginPos, endLine, endPos);
    }

    @Override
    public String index() {
        return index;
    }

    @Override
    public String toString() {
        if (isNoSource()) return "NO_SOURCE";
        return index + "@" + beginLine + ":" + beginPos + "-" + endLine + ":" + endPos;
    }

    @Override
    public Source withIndex(String newIndex) {
        return new SourceImpl(newIndex, beginLine, beginPos, endLine, endPos, detailedSources);
    }
}
