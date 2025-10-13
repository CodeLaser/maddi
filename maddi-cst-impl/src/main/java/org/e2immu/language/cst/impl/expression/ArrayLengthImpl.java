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
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.ArrayLength;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ArrayLengthImpl extends ExpressionImpl implements ArrayLength {
    private final Expression scope;
    private final ParameterizedType intPt;

    private ArrayLengthImpl(List<Comment> comments, Source source, ParameterizedType intPt, Expression scope) {
        super(comments, source, 1 + scope.complexity());
        this.scope = scope;
        this.intPt = intPt;
    }

    @Override
    public Expression withSource(Source source) {
        return new ArrayLengthImpl(comments(), source, intPt, scope);
    }

    public static class Builder extends ElementImpl.Builder<ArrayLength.Builder> implements ArrayLength.Builder {
        private Expression expression;
        private final ParameterizedType intParameterizedType;

        public Builder(ParameterizedType intParameterizedType) {
            this.intParameterizedType = intParameterizedType;
        }

        @Override
        public Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public ArrayLength build() {
            return new ArrayLengthImpl(comments, source, intParameterizedType, expression);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArrayLengthImpl that)) return false;
        return scope.equals(that.scope);
    }

    @Override
    public int hashCode() {
        return scope.hashCode();
    }

    @Override
    public Expression scope() {
        return scope;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return intPt;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.ACCESS;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_ARRAY_LENGTH;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof ArrayLength al) {
            return scope.compareTo(al.scope());
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            scope.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            scope.visit(visitor);
        }
        visitor.afterExpression(this);
    }


    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), scope))
                .add(SymbolEnum.DOT).add(KeywordImpl.LENGTH);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return scope.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return scope.typesReferenced();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedScope = scope.translate(translationMap);
        if (translatedScope == scope) return this;
        return new ArrayLengthImpl(comments(), source(), intPt, translatedScope);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new ArrayLengthImpl(comments(), source(), intPt, scope.rewire(infoMap));
    }
}
