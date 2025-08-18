/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.impl.output;

import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.element.ElementarySpace;
import org.e2immu.language.cst.api.output.element.Space;
import org.e2immu.language.cst.api.output.element.Split;

import java.util.Objects;


public enum SpaceEnum implements Space {
    // no space, do not split
    NONE(ElementarySpaceEnum.NONE, ElementarySpaceEnum.NONE, SplitEnum.NEVER),

    // exactly one space needed, never split here (e.g. between class and class name); two ONEs collapse into one
    ONE(ElementarySpaceEnum.ONE, ElementarySpaceEnum.ONE, SplitEnum.NEVER),

    // end of annotation; needs minimally one, but can be newline
    ONE_REQUIRED_EASY_SPLIT(ElementarySpaceEnum.ONE, ElementarySpaceEnum.ONE, SplitEnum.EASY),

    // no space needed, split to make things nicer
    NO_SPACE_SPLIT_ALLOWED(ElementarySpaceEnum.NONE, ElementarySpaceEnum.NONE, SplitEnum.EASY),

    RELAXED_NO_SPACE_SPLIT_ALLOWED(ElementarySpaceEnum.RELAXED_NONE, ElementarySpaceEnum.RELAXED_NONE, SplitEnum.EASY),

    ONE_IS_NICE_EASY_SPLIT(ElementarySpaceEnum.RELAXED_NONE, ElementarySpaceEnum.NICE, SplitEnum.EASY),  // no space needed but one in nice, split to make things nicer

    ONE_IS_NICE_SPLIT_BEGIN_END(ElementarySpaceEnum.RELAXED_NONE, ElementarySpaceEnum.NICE, SplitEnum.BEGIN_END),  // no space needed but one in nice, split to make things nicer

    NEWLINE(ElementarySpaceEnum.NEWLINE, ElementarySpaceEnum.NEWLINE, SplitEnum.ALWAYS), // enforce a newline

    // easy either left or right, but consistently according to preferences
    // e.g. && either at beginning of line in sequence, or always at end
    // in nice formatting, one space is used
    ONE_IS_NICE_EASY_L(ElementarySpaceEnum.RELAXED_NONE, ElementarySpaceEnum.NICE, SplitEnum.EASY_L),
    ONE_IS_NICE_EASY_R(ElementarySpaceEnum.RELAXED_NONE, ElementarySpaceEnum.NICE, SplitEnum.EASY_R);

    private final ElementarySpace minimal;
    private final ElementarySpace nice;
    public final Split split;

    SpaceEnum(ElementarySpace minimal, ElementarySpace nice, Split split) {
        this.minimal = Objects.requireNonNull(minimal);
        this.nice = Objects.requireNonNull(nice);
        this.split = Objects.requireNonNull(split);
    }

    @Override
    public String minimal() {
        return minimal.write();
    }

    @Override
    public ElementarySpace elementarySpace(FormattingOptions options) {
        return options.compact() ? minimal : nice;
    }

    @Override
    public String write(FormattingOptions options) {
        return options.compact() ? minimal.write() : nice.write();
    }

    @Override
    public String generateJavaForDebugging() {
        return ".add(SpaceEnum." + this.name() + ")";
    }

    @Override
    public boolean isNewLine() {
        return this == NEWLINE;
    }

    @Override
    public Split split() {
        return split;
    }

    @Override
    public ElementarySpace nice() {
        return nice;
    }
}
