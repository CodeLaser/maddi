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

package io.codelaser.maddi.cst.impl.statement;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.ThrowStatement;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ThrowStatementImpl extends StatementImpl implements ThrowStatement {
    private final Expression expression;

    public ThrowStatementImpl(List<Comment> comments, Source source, List<AnnotationExpression> annotations,
                              String label, Expression expression) {
        super(comments, source, annotations, 1 + expression.complexity(), label);
        this.expression = Objects.requireNonNull(expression);
    }

    public static class Builder extends StatementImpl.Builder<ThrowStatement.Builder> implements ThrowStatement.Builder {
        private Expression expression;

        @Override
        public Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }


        @Override
        public ThrowStatement build() {
            return new ThrowStatementImpl(comments, source, annotations, label, expression);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThrowStatementImpl that = (ThrowStatementImpl) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expression);
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            expression.visit(visitor);
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return outputBuilder(qualification)
                .add(KeywordImpl.THROW)
                .add(SpaceEnum.ONE).add(expression.print(qualification))
                .add(SymbolEnum.SEMICOLON);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expression.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return expression.typesReferenced(predicate);
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;

        Expression tex = expression.translate(translationMap);
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);

        if (tex != expression || !analysis().isEmpty() && translationMap.isClearAnalysis()
            || tAnnotations != annotations()) {
            ThrowStatementImpl ts = new ThrowStatementImpl(comments(), source(), annotations(), label(), tex);
            if (!translationMap.isClearAnalysis()) ts.analysis().setAll(analysis());
            return translationMap.postTranslationHandler(this, List.of(ts));
        }
        return List.of(this);
    }

    @Override
    public boolean hasSubBlocks() {
        return false;
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return this;// no blocks
    }

    @Override
    public ThrowStatement withSource(Source newSource) {
        return new ThrowStatementImpl(comments(), newSource, annotations(), label(), expression);
    }

    @Override
    public Statement rewire(InfoMapView infoMap) {
        return new ThrowStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                expression.rewire(infoMap));
    }
}
