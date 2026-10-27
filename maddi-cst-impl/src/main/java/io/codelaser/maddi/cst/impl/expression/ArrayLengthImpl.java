/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.ArrayLength;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

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
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return scope.typesReferenced(predicate);
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedScope = scope.translate(translationMap);
        if (translatedScope == scope) return this;
        ArrayLength result = new ArrayLengthImpl(comments(), source(), intPt, translatedScope);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new ArrayLengthImpl(comments(), source(), intPt, scope.rewire(infoMap));
    }
}
