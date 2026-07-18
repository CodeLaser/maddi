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
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.InfoMapView;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

import java.util.List;

/**
 * The {@code try ... catch ... finally} statement, including try-with-resources. The {@code try} body is
 * the primary {@link Statement#block()}; the {@code catch} clauses are {@link #catchClauses()}, the
 * {@code finally} body is {@link #finallyBlock()} (possibly empty), and the resource declarations are
 * {@link #resources()}.
 */
public interface TryStatement extends Statement {

    /**
     * A single {@code catch} clause. Its declared variable is {@link #catchVariable()}, the caught types
     * are {@link #exceptionTypes()} (more than one for a multi-catch {@code A | B}), and its body is
     * {@link #block()}.
     */
    interface CatchClause extends Element {
        /**
         * @return the caught exception types; more than one for a multi-catch ({@code catch (A | B e)}).
         */
        List<ParameterizedType> exceptionTypes();

        /**
         * @return {@code true} if the catch variable is declared {@code final}.
         */
        boolean isFinal();

        CatchClause rewire(InfoMapView infoMap);

        CatchClause translate(TranslationMap translationMap);

        /**
         * @return the variable bound to the caught exception.
         */
        LocalVariable catchVariable();

        /**
         * @return the body executed when an exception is caught.
         */
        Block block();

        /**
         * @return an immutable copy of this clause with its body replaced; this instance is unchanged.
         */
        CatchClause withBlock(Block newBlock);

        interface Builder extends Element.Builder<Builder> {

            @Fluent
            Builder setBlock(Block block);

            @Fluent
            Builder addType(ParameterizedType type);

            @Fluent
            Builder setFinal(boolean isFinal);

            @Fluent
            Builder setCatchVariable(LocalVariable catchVariable);

            CatchClause build();
        }
    }

    /**
     * @return the {@code finally} body; an empty block when there is no {@code finally}.
     */
    Block finallyBlock();

    /**
     * @return the {@code catch} clauses, in source order.
     */
    List<CatchClause> catchClauses();

    /**
     * @return the try-with-resources declarations, empty for a plain {@code try}. Each is either a
     * {@link LocalVariableCreation} or an {@link ExpressionAsStatement} wrapping a variable expression.
     */
    List<Statement> resources();

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setBlock(Block block);

        @Fluent
        Builder setFinallyBlock(Block block);

        @Fluent
        Builder addCatchClause(CatchClause catchClause);

        @Fluent
        Builder addResource(Statement resource);

        TryStatement build();
    }

    String NAME = "try";

    @Override
    default String name() {
        return NAME;
    }
}
