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
import org.e2immu.language.cst.api.statement.YieldStatement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class YieldStatementImpl extends StatementImpl implements YieldStatement {

    private final Expression expression;

    public YieldStatementImpl(Expression expression) {
        this.expression = expression;
    }

    public YieldStatementImpl(List<Comment> comments,
                              Source source,
                              List<AnnotationExpression> annotations,
                              String label,
                              Expression expression) {
        super(comments, source, annotations, expression.complexity(), label);
        this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YieldStatementImpl that)) return false;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expression);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = outputBuilder(qualification).add(KeywordImpl.YIELD);
        if (!expression.isEmpty()) {
            outputBuilder.add(SpaceEnum.ONE).add(expression.print(qualification));
        }
        outputBuilder.add(SymbolEnum.SEMICOLON);
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expression.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return expression.typesReferenced();
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

    public static class Builder extends StatementImpl.Builder<YieldStatement.Builder> implements YieldStatement.Builder {
        private Expression expression;

        @Override
        public Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public YieldStatement build() {
            return new YieldStatementImpl(comments, source, annotations, label, expression);
        }
    }

    @Override
    public boolean hasSubBlocks() {
        return false;
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        Expression tex = expression.translate(translationMap);
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (tex != expression || !analysis().isEmpty() && translationMap.isClearAnalysis()
            || tAnnotations != annotations()) {
            YieldStatementImpl ys = new YieldStatementImpl(comments(), source(), tAnnotations, label(), tex);
            if (!translationMap.isClearAnalysis()) ys.analysis().setAll(analysis());
            return List.of(ys);
        }
        return List.of(this);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return this;// no blocks
    }

    @Override
    public YieldStatement withSource(Source newSource) {
        return new YieldStatementImpl(comments(), newSource, annotations(), label(), expression);
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new YieldStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(), expression.rewire(infoMap));
    }
}
