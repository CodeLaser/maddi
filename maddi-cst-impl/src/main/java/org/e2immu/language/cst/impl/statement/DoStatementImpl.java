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

package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.DoStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

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
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(expression.print(qualification))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(block.variables(descendMode), expression.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(block.typesReferenced(), expression.typesReferenced());
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
            return List.of(newDo);
        }
        return List.of(this);
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new DoStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                expression.rewire(infoMap), block.rewire(infoMap));
    }
}
