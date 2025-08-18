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
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.SynchronizedStatement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SynchronizedStatementImpl extends StatementImpl implements SynchronizedStatement {
    private final Block block;
    private final Expression expression;

    public SynchronizedStatementImpl(List<Comment> comments, Source source, List<AnnotationExpression> annotations,
                                     String label, Expression expression, Block block) {
        super(comments, source, annotations, 1 + block.complexity() + expression.complexity(), label);
        this.expression = Objects.requireNonNull(expression);
        this.block = Objects.requireNonNull(block); // use empty expression if no message
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SynchronizedStatementImpl that)) return false;
        return Objects.equals(block, that.block) && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, expression);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return new SynchronizedStatementImpl(comments(), source(), annotations(), label(), expression, tSubBlocks.get(0));
    }

    public static class Builder extends StatementImpl.Builder<SynchronizedStatement.Builder> implements SynchronizedStatement.Builder {
        private Block block;
        private Expression expression;

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
        public SynchronizedStatement build() {
            return new SynchronizedStatementImpl(comments, source, annotations, label, expression, block);
        }
    }

    @Override
    public Block block() {
        return block;
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expression.visit(predicate);
            block.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            expression.visit(visitor);
            block.visit(visitor);
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return outputBuilder(qualification)
                .add(KeywordImpl.SYNCHRONIZED)
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(expression.print(qualification))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(block.print(qualification));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(expression.variables(descendMode), block.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(expression.typesReferenced(), block.typesReferenced());
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;

        // translations in order of appearance
        Expression tex = translationMap.translateExpression(expression);
        List<Statement> translatedBlock = block.translate(translationMap);
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (tex != expression || hasBeenTranslated(translatedBlock, block)
            || tAnnotations != annotations()) {
            if (translatedBlock.size() == 1 && translatedBlock.getFirst() instanceof Block b) {
                SynchronizedStatementImpl sync = new SynchronizedStatementImpl(comments(), source(),
                        tAnnotations, label(), tex, b);
                if (!translationMap.isClearAnalysis()) sync.analysis().setAll(analysis());
                return List.of(sync);
            }
            throw new UnsupportedOperationException();
        }
        return List.of(this);
    }

    @Override
    public boolean hasSubBlocks() {
        return true;
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new SynchronizedStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                expression.rewire(infoMap), block.rewire(infoMap));
    }
}
