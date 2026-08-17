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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.expression.UnaryOperator;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

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
        // complexity is a structural invariant cached at construction: an exact O(1) reject before the
        // recursive descent (negations wrap whole conjunctions in the boolean simplifier's hot path)
        if (complexity() != that.complexity()) return false;
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

    // @IgnoreModifications (road §050): idempotent memo state, disclaimed -- the VariableImpl.cachedHash
    // precedent; lazily cached because the recursive recompute dominated profiles
    @IgnoreModifications
    private int hash;

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = Objects.hash(expression, operator);
            hash = h;
        }
        return h;
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
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return expression.typesReferenced(predicate);
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
        Expression result = new UnaryOperatorImpl(comments(), source(), operator, translatedExpression, precedence);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new UnaryOperatorImpl(comments(), source(), operator, expression.rewire(infoMap), precedence);
    }
}
