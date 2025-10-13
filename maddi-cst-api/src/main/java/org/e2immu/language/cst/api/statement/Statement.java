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

package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;
import java.util.stream.Stream;

public interface Statement extends Element {

    boolean hasSubBlocks();

    default boolean isSynthetic() {
        return false;
    }

    // a label to possibly jump to
    String label();

    // many have one
    default Block block() {
        return null;
    }

    String name();

    // all other blocks, except for block()
    @NotNull
    default Stream<Block> otherBlocksStream() {
        return Stream.of();
    }

    // many have one; if they have one, this is the one (if they have many, this is the first)
    default Expression expression() {
        return null;
    }

    // return all blocks, including empty ones!
    @NotNull
    default Stream<Block> subBlockStream() {
        return Stream.concat(Stream.ofNullable(block()), otherBlocksStream());
    }

    Statement withBlocks(List<Block> tSubBlocks);

    interface Builder<B extends Builder<?>> extends Element.Builder<B> {
        @Fluent
        B setLabel(String label);

        Statement build();
    }

    List<Statement> translate(TranslationMap translationMap);

    // from analysis
    default boolean alwaysEscapes() {
        return false;
    }

    Statement rewire(InfoMap infoMap);

    default List<AnnotationExpression> rewireAnnotations(InfoMap infoMap) {
        return annotations().stream().map(ae -> (AnnotationExpression) ae.rewire(infoMap)).toList();
    }
}
