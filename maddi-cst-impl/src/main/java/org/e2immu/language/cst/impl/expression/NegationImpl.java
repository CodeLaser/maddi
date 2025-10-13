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
