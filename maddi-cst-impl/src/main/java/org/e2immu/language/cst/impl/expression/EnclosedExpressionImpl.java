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
import org.e2immu.language.cst.api.expression.EnclosedExpression;
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
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EnclosedExpressionImpl extends ExpressionImpl implements EnclosedExpression {
    private final Expression inner;

    public EnclosedExpressionImpl(List<Comment> comments, Source source, Expression inner) {
        super(comments, source, 1 + inner.complexity());
        this.inner = inner;
    }

    @Override
    public Expression withSource(Source source) {
        return new EnclosedExpressionImpl(comments(), source, inner);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnclosedExpressionImpl that = (EnclosedExpressionImpl) o;
        return Objects.equals(inner, that.inner);
    }

    @Override
    public Expression expression() {
        return inner;
    }

    @Override
    public int wrapperOrder() {
        return 1;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(inner);
    }

    @Override
    public Expression inner() {
        return inner;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return inner.parameterizedType();
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.ACCESS;
    }

    @Override
    public int order() {
        return inner.order();
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return inner.compareTo(((EnclosedExpression) expression).inner());
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            inner.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            inner.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                .add(inner.print(qualification)).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return inner.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return inner.typesReferenced();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedInner = inner.translate(translationMap);
        if (translatedInner == inner) return this;
        return new EnclosedExpressionImpl(comments(), source(), translatedInner);
    }

    public static class Builder extends ElementImpl.Builder<EnclosedExpression.Builder> implements EnclosedExpression.Builder {
        private Expression expression;

        @Override
        public EnclosedExpression.Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public EnclosedExpression build() {
            return new EnclosedExpressionImpl(comments, source, expression);
        }
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new EnclosedExpressionImpl(comments(), source(), inner.rewire(infoMap));
    }
}
