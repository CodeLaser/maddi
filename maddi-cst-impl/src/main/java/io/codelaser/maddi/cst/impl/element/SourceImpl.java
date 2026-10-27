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

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;

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
            int bl = Integer.compare(beginLine, s.beginLine);
            if (bl != 0) return bl;
            int bp = Integer.compare(beginPos, s.beginPos);
            if (bp != 0) return bp;
            int el = Integer.compare(endLine, s.endLine);
            if (el != 0) return el;
            return Integer.compare(endPos, s.endPos);
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

    @Override
    public Source max(Source other) {
        String minIndex = index.compareTo(other.index()) <= 0 ? index : other.index();
        int min = Math.min(beginLine, other.beginLine());
        int max = Math.max(endLine, other.endLine());
        return new SourceImpl(minIndex, min,
                beginLine == other.beginLine() ? Math.min(beginPos, other.beginPos()) : min == beginLine
                        ? beginPos : other.beginPos(),
                max,
                endLine == other.endLine() ? Math.max(endPos, other.endPos()) : max == endLine
                        ? endPos : other.endPos());
    }

    @Override
    public Source ofIndex(String string, int from, int length) {
        if (from < 0 || length <= 0 || from + length > string.length()) return null;
        int bp = this.beginPos();
        int bl = this.beginLine();
        for (int i = 0; i < from; ++i) {
            char c = string.charAt(i);
            if (c == '\n') {
                bp = 1;
                bl++;
            } else {
                bp++;
            }
        }
        return new SourceImpl(index, bl, bp, bl, bp + length - 1);
    }
}
