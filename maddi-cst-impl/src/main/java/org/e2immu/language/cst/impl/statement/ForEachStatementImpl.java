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
import org.e2immu.language.cst.api.statement.ForEachStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ForEachStatementImpl extends StatementImpl implements ForEachStatement {
    private final LocalVariableCreation initializer;
    private final Expression expression;
    private final Block block;

    public ForEachStatementImpl(List<Comment> comments,
                                Source source,
                                List<AnnotationExpression> annotations,
                                String label,
                                LocalVariableCreation initializer,
                                Expression expression, Block block) {
        super(comments, source, annotations, 0, label);
        this.initializer = initializer;
        this.expression = expression;
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForEachStatementImpl that)) return false;
        return Objects.equals(initializer, that.initializer) && Objects.equals(expression, that.expression)
               && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initializer, expression, block);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return new ForEachStatementImpl(comments(), source(), annotations(), label(), initializer, expression,
                tSubBlocks.get(0));
    }

    public static class Builder extends StatementImpl.Builder<ForEachStatement.Builder> implements ForEachStatement.Builder {
        private LocalVariableCreation initializer;
        private Expression expression;
        private Block block;

        @Override
        public ForEachStatement.Builder setInitializer(LocalVariableCreation initializer) {
            this.initializer = initializer;
            return this;
        }

        @Override
        public ForEachStatement.Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public ForEachStatement.Builder setBlock(Block block) {
            this.block = block;
            return this;
        }

        @Override
        public ForEachStatement build() {
            return new ForEachStatementImpl(comments, source, annotations, label, initializer, expression, block);
        }
    }

    @Override
    public LocalVariableCreation initializer() {
        return initializer;
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
            initializer.visit(predicate);
            expression.visit(predicate);
            block.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            initializer.visit(visitor);
            expression.visit(visitor);
            visitor.startSubBlock(0);
            block.visit(visitor);
            visitor.endSubBlock(0);
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = outputBuilder(qualification);
        LocalVariable lv = initializer.localVariable();
        return outputBuilder.add(KeywordImpl.FOR)
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(initializer.isVar() ? new OutputBuilderImpl().add(KeywordImpl.VAR)
                        : lv.parameterizedType().print(qualification, false, DiamondEnum.SHOW_ALL))
                .add(SpaceEnum.ONE)
                .add(new TextImpl(lv.simpleName()))
                .add(SymbolEnum.COLON)
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
        return Stream.concat(initializer.typesReferenced(),
                Stream.concat(expression.typesReferenced(), block.typesReferenced()));
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;

        // translations in order of appearance
        LocalVariableCreation translatedLvc = (LocalVariableCreation) initializer.translate(translationMap).getFirst();
        Expression translated = expression.translate(translationMap);
        List<Statement> translatedBlock = block.translate(translationMap);
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (translatedLvc != initializer
            || expression != translated
            || translatedBlock.getFirst() != block
            || !analysis().isEmpty() && translationMap.isClearAnalysis()
            || tAnnotations != annotations()) {
            ForEachStatementImpl fs = new ForEachStatementImpl(comments(), source(), tAnnotations, label(),
                    translatedLvc, translated, ensureBlock(translatedBlock));
            if (!translationMap.isClearAnalysis()) fs.analysis().setAll(analysis());
            return List.of(fs);
        }
        return List.of(this);
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new ForEachStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                (LocalVariableCreation) initializer.rewire(infoMap), expression.rewire(infoMap), block.rewire(infoMap));
    }
}
