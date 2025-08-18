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
import org.e2immu.language.cst.api.statement.AssertStatement;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class AssertStatementImpl extends StatementImpl implements AssertStatement {
    private final Expression message;
    private final Expression expression;

    public AssertStatementImpl(List<Comment> comments, Source source, List<AnnotationExpression> annotations,
                               String label, Expression expression, Expression message) {
        super(comments, source, annotations, 1 + message.complexity() + expression.complexity(), label);
        this.expression = Objects.requireNonNull(expression);
        this.message = Objects.requireNonNull(message); // use empty expression if no message
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssertStatementImpl that)) return false;
        return Objects.equals(message, that.message) && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, expression);
    }

    public static class Builder extends StatementImpl.Builder<AssertStatement.Builder> implements AssertStatement.Builder {
        private Expression message;
        private Expression expression;

        @Override
        public AssertStatement.Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public AssertStatement.Builder setMessage(Expression message) {
            this.message = message;
            return this;
        }

        @Override
        public AssertStatement build() {
            return new AssertStatementImpl(comments, source, annotations, label, expression, message);
        }
    }

    @Override
    public Expression message() {
        return message;
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expression.visit(predicate);
            message.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            expression.visit(visitor);
            message.visit(visitor);
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return outputBuilder(qualification)
                .add(KeywordImpl.ASSERT)
                .add(SpaceEnum.ONE)
                .add(expression.print(qualification))
                .add(message.isEmpty() ? new OutputBuilderImpl()
                        : new OutputBuilderImpl().add(SymbolEnum.COLON).add(message.print(qualification)))
                .add(SymbolEnum.SEMICOLON);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(expression.variables(descendMode), message.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(expression.typesReferenced(), message.typesReferenced());
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        Expression tex = expression.translate(translationMap);
        Expression msg = message.translate(translationMap);
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (tex != expression || msg != message
            || !analysis().isEmpty() && translationMap.isClearAnalysis()
            || tAnnotations != annotations()) {
            AssertStatement as = new AssertStatementImpl(comments(), source(), tAnnotations, label(), tex, msg);
            if (!translationMap.isClearAnalysis()) as.analysis().setAll(analysis());
            return List.of(as);
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
    public AssertStatement withSource(Source newSource) {
        return new AssertStatementImpl(comments(), newSource, annotations(), label(), expression, message);
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new AssertStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                expression.rewire(infoMap), message.rewire(infoMap));
    }
}
