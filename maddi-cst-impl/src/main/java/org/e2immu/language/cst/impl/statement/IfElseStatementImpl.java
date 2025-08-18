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
import org.e2immu.language.cst.api.statement.IfElseStatement;
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
                .add(SymbolEnum.LEFT_PARENTHESIS)
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
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(expression.typesReferenced(), Stream.concat(block.typesReferenced(),
                elseBlock.typesReferenced()));
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
            return List.of(ie);
        }
        return List.of(this);
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new IfElseStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                expression.rewire(infoMap), block.rewire(infoMap), elseBlock.rewire(infoMap));
    }
}
