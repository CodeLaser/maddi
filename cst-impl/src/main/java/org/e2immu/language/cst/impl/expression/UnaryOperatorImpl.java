package org.e2immu.language.cst.impl.expression;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.expression.UnaryOperator;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class UnaryOperatorImpl extends ExpressionImpl implements UnaryOperator {
    public final Expression expression;
    public final Precedence precedence;
    public final MethodInfo operator;

    public UnaryOperatorImpl(List<Comment> comments, Source source, @NotNull MethodInfo operator, @NotNull Expression expression, Precedence precedence) {
        super(comments, source, 1 + expression.complexity());
        this.expression = Objects.requireNonNull(expression);
        this.precedence = precedence;
        this.operator = Objects.requireNonNull(operator);
    }

    @Override
    public Expression withSource(Source source) {
        return new UnaryOperatorImpl(comments(), source, operator, expression, precedence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryOperatorImpl that = (UnaryOperatorImpl) o;
        return expression.equals(that.expression) &&
               operator.equals(that.operator);
    }

    public Expression expression() {
        return expression;
    }

    @Override
    public MethodInfo operator() {
        return operator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, operator);
    }


    @Override
    public int order() {
        return ExpressionComparator.ORDER_UNARY_OPERATOR; // not yet evaluated
    }

    @Override
    public int internalCompareTo(Expression v) {
        return expression.compareTo(((UnaryOperatorImpl) v).expression);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return expression.parameterizedType();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        if (operator.isPostfix()) {
            return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence, expression))
                    .add(SymbolEnum.plusPlusSuffix(operator.name()));
        }
        return new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix(operator.name()))
                .add(outputInParenthesis(qualification, precedence, expression));
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
    public Precedence precedence() {
        return precedence;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            expression.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public boolean isNumeric() {
        return expression.isNumeric();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedExpression = expression.translate(translationMap);
        if (translatedExpression == expression) return this;
        return new UnaryOperatorImpl(comments(), source(), operator, translatedExpression, precedence);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new UnaryOperatorImpl(comments(), source(), operator, expression.rewire(infoMap), precedence);
    }
}
