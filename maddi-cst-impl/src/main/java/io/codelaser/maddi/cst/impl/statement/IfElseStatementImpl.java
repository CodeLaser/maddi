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
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
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

public class IfElseStatementImpl extends StatementImpl implements IfElseStatement {
    private final Expression expression;
    private final Block block;
    private final Block elseBlock;

    public IfElseStatementImpl(List<Comment> comments, Source source, List<AnnotationExpression> annotations,
                               String label, Expression expression, Block block, Block elseBlock) {
        super(comments, source, annotations,
                1 + expression.complexity() + block.complexity() + elseBlock.complexity(), label);
        this.expression = expression;
        this.block = block;
        this.elseBlock = elseBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IfElseStatementImpl that)) return false;
        return Objects.equals(expression, that.expression) && Objects.equals(block, that.block)
               && Objects.equals(elseBlock, that.elseBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, block, elseBlock);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return new IfElseStatementImpl(comments(), source(), annotations(), label(), expression,
                tSubBlocks.get(0), tSubBlocks.get(1));
    }

    public static class Builder extends StatementImpl.Builder<IfElseStatement.Builder> implements IfElseStatement.Builder {
        private Expression expression;
        private Block block;
        private Block elseBlock;

        @Override
        public IfElseStatement.Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public IfElseStatement.Builder setIfBlock(Block ifBlock) {
            this.block = ifBlock;
            return this;
        }

        @Override
        public IfElseStatement.Builder setElseBlock(Block elseBlock) {
            this.elseBlock = elseBlock;
            return this;
        }

        @Override
        public IfElseStatement build() {
            return new IfElseStatementImpl(comments, source, annotations, label, expression, block, elseBlock);
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
    public Block elseBlock() {
        return elseBlock;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expression.visit(predicate);
            block.visit(predicate);
            if (!elseBlock.isEmpty()) elseBlock.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            expression.visit(visitor);
            visitor.startSubBlock(0);
            block.visit(visitor);
            visitor.endSubBlock(0);
            if (!elseBlock.isEmpty()) {
                visitor.startSubBlock(1);
                elseBlock.visit(visitor);
                visitor.endSubBlock(1);
            }
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = outputBuilder(qualification)
                .add(KeywordImpl.IF)
                .add(SymbolEnum.LEFT_PARENTHESIS_AFTER_KEYWORD)
                .add(expression.print(qualification))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(block.print(qualification));
        if (!elseBlock.isEmpty()) {
            outputBuilder.add(KeywordImpl.ELSE).add(elseBlock.print(qualification));
        }
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(expression.variables(descendMode), Stream.concat(block.variables(descendMode),
                elseBlock.variables(descendMode)));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return Stream.concat(expression.typesReferenced(predicate), Stream.concat(block.typesReferenced(predicate),
                elseBlock.typesReferenced(predicate)));
    }

    @Override
    public Stream<Block> otherBlocksStream() {
        return Stream.of(elseBlock);
    }

    @Override
    public boolean hasSubBlocks() {
        return true;
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        Expression tex = expression.translate(translationMap);
        Block tIf = (Block) block.translate(translationMap).getFirst();
        Block tElse = (Block) elseBlock.translate(translationMap).getFirst();
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (tex != expression || tIf != block || tElse != elseBlock
            || !analysis().isEmpty() && translationMap.isClearAnalysis()
            || tAnnotations != annotations()) {
            IfElseStatement ie = new IfElseStatementImpl(comments(), source(), tAnnotations, label(), tex,
                    tIf, tElse);
            if (!translationMap.isClearAnalysis()) ie.analysis().setAll(analysis());
            return translationMap.postTranslationHandler(this, List.of(ie));
        }
        return List.of(this);
    }

    @Override
    public IfElseStatement withSource(Source newSource) {
        return new IfElseStatementImpl(comments(), newSource, annotations(), label(), expression, block, elseBlock);
    }

    @Override
    public Statement rewire(InfoMapView infoMap) {
        return new IfElseStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                expression.rewire(infoMap), block.rewire(infoMap), elseBlock.rewire(infoMap));
    }
}
