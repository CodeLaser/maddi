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

import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.translate.TranslationMap;

/**
 * Base interface for source comments that are attached to CST elements and preserved during
 * parsing and printing.
 * <p>
 * Concrete sub-types are {@link SingleLineComment} ({@code // …}) and
 * {@link MultiLineComment} ({@code /* … *}{@code /}), with {@link JavaDoc} ({@code /** … *}{@code /})
 * extending {@code MultiLineComment}.
 * <p>
 * Non-JavaDoc comments carry no type or variable references, so {@link #rewire} and {@link #translate}
 * always return {@code this}.
 */
public interface Comment extends Element {

    /** Returns the raw text of the comment as it appears in source. */
    String comment();

    default Comment rewire(InfoMap infoMap) {
        return this;
    }

    default Comment translate(TranslationMap translationMap) {
        return this;
    }
}
