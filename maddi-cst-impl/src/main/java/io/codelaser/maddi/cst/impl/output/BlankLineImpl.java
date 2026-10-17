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


package io.codelaser.maddi.cst.impl.output;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.element.BlankLine;
import io.codelaser.maddi.cst.api.output.element.ElementarySpace;
import io.codelaser.maddi.cst.api.output.element.Split;

import java.util.List;

/**
 * The one {@link BlankLine}, and when to add it: when the source has a blank line between two neighbours.
 */
public enum BlankLineImpl implements BlankLine {
    INSTANCE;

    @Override
    public String minimal() {
        return "";
    }

    @Override
    public String write(FormattingOptions options) {
        return "";
    }

    @Override
    public String generateJavaForDebugging() {
        return ".add(BlankLineImpl.INSTANCE)";
    }

    // as a Space, for the clients that do not know BlankLine: no space, never a split
    @Override
    public ElementarySpace elementarySpace(FormattingOptions options) {
        return ElementarySpaceEnum.NONE;
    }

    @Override
    public ElementarySpace nice() {
        return ElementarySpaceEnum.NONE;
    }

    @Override
    public Split split() {
        return SplitEnum.NEVER;
    }

    /**
     * @param previousEndLine the last line of what comes before, 0 or less when not known
     * @param next            what follows, with its comments and annotations, which are written above it
     * @return true when the source has at least one empty line between the two
     */
    public static boolean between(int previousEndLine, Element next) {
        if (previousEndLine <= 0 || next == null) return false;
        int begin = firstLine(next.source(), next.comments(), next.annotations());
        return begin > 0 && begin - previousEndLine >= 2;
    }

    public static boolean between(Element previous, Element next) {
        return previous != null && previous.source() != null && between(previous.source().endLine(), next);
    }

    private static int firstLine(Source source, List<Comment> comments, List<AnnotationExpression> annotations) {
        if (source == null || source.beginLine() <= 0) return -1;
        int begin = source.beginLine();
        for (Comment comment : comments) {
            if (comment.source() != null && comment.source().beginLine() > 0) {
                begin = Math.min(begin, comment.source().beginLine());
            }
        }
        for (AnnotationExpression annotation : annotations) {
            if (annotation.source() != null && annotation.source().beginLine() > 0) {
                begin = Math.min(begin, annotation.source().beginLine());
            }
        }
        return begin;
    }
}
