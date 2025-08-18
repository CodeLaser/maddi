package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Equals;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Negation;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;

public class NegationImpl extends UnaryOperatorImpl implements Negation {

    public NegationImpl(MethodInfo operator, Precedence precedence, Expression expression) {
        super(List.of(), null, operator, expression, precedence);
    }

    public NegationImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence, Expression expression) {
        super(comments, source, operator, expression, precedence);
    }

    @Override
    public Expression withSource(Source source) {
        return new NegationImpl(comments(), source, operator, precedence, expression);
    }

    @Override
    public Double numericValue() {
        Double d = expression.numericValue();
        return d == null ? null : -d;
    }

    @Override
    public boolean isNegatedOrNumericNegative() {
        return true;
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedExpression = expression.translate(translationMap);
        if (translatedExpression == expression) return this;
        if (translatedExpression instanceof Negation negation) {
            return negation.expression(); // double negation gets cancelled
        }
        return new NegationImpl(comments(), source(), operator, precedence, translatedExpression);
    }

    @Override
    public int wrapperOrder() {
        return 0;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        if (expression instanceof Equals equals) {
            return new OutputBuilderImpl().add(outputInParenthesis(qualification, equals.precedence(), equals.lhs()))
                    .add(SymbolEnum.NOT_EQUALS)
                    .add(outputInParenthesis(qualification, equals.precedence(), equals.rhs()));
        }
        return new OutputBuilderImpl().add(expression.isNumeric() ? SymbolEnum.UNARY_MINUS : SymbolEnum.UNARY_BOOLEAN_NOT)
                .add(outputInParenthesis(qualification, precedence(), expression));
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new NegationImpl(comments(), source(), operator, precedence, expression.rewire(infoMap));
    }
}
