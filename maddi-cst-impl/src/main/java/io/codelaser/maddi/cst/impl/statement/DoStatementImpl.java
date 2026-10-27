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
import io.codelaser.maddi.cst.api.statement.DoStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DoStatementImpl extends StatementImpl implements DoStatement {
    private final Expression expression;
    private final Block block;

    public DoStatementImpl(List<Comment> comments,
                           Source source,
                           List<AnnotationExpression> annotations,
                           String label,
                           Expression expression, Block block) {
        super(comments, source, annotations, 0, label);
        this.expression = expression;
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoStatementImpl that)) return false;
        return Objects.equals(expression, that.expression) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, block);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return new DoStatementImpl(comments(), source(), annotations(), label(), expression, tSubBlocks.get(0));
    }

    public static class Builder extends StatementImpl.Builder<DoStatement.Builder> implements DoStatement.Builder {
        private Expression expression;
        private Block block;

        @Override
        public Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public Builder setBlock(Block block) {
            this.block = block;
            return this;
        }

        @Override
        public DoStatement build() {
            return new DoStatementImpl(comments, source, annotations, label, expression, block);
        }
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public Block block() {
        return block;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            block.visit(predicate);
            expression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            visitor.startSubBlock(0);
            block.visit(visitor);
            visitor.endSubBlock(0);
            expression.visit(visitor);
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return outputBuilder(qualification)
                .add(KeywordImpl.DO)
                .add(block.print(qualification))
                .add(KeywordImpl.WHILE)
                .add(SymbolEnum.LEFT_PARENTHESIS_AFTER_KEYWORD)
                .add(expression.print(qualification))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(block.variables(descendMode), expression.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return Stream.concat(block.typesReferenced(predicate), expression.typesReferenced(predicate));
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;

        Expression tex = expression.translate(translationMap);
        List<Statement> translatedBlock = block.translate(translationMap);
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (tex != expression || hasBeenTranslated(translatedBlock, block)
            || tAnnotations != annotations()
            || !analysis().isEmpty() && translationMap.isClearAnalysis()) {
            DoStatement newDo = new DoStatementImpl(comments(), source(), tAnnotations, label(), tex,
                    ensureBlock(translatedBlock));
            if (!translationMap.isClearAnalysis()) newDo.analysis().setAll(analysis());
            return translationMap.postTranslationHandler(this, List.of(newDo));
        }
        return List.of(this);
    }

    @Override
    public DoStatement withSource(Source newSource) {
        return new DoStatementImpl(comments(), newSource, annotations(), label(), expression, block);
    }

    @Override
    public Statement rewire(InfoMapView infoMap) {
        return new DoStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                expression.rewire(infoMap), block.rewire(infoMap));
    }
}
