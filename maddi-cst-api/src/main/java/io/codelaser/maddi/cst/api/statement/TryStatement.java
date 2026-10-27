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

package io.codelaser.maddi.cst.api.statement;

import io.codelaser.maddi.annotation.Independent;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;

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

        CatchClause rewire(@Independent(hc = true) InfoMapView infoMap);

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
