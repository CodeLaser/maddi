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
import org.e2immu.language.cst.api.expression.CommaExpression;
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
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.util.internal.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CommaExpressionImpl extends ExpressionImpl implements CommaExpression {

    private final List<Expression> expressions;

    public CommaExpressionImpl(List<Comment> comments, Source source, List<Expression> expressions) {
        super(comments, source, 1 + expressions.stream().mapToInt(Element::complexity).sum());
        this.expressions = expressions;
        assert !expressions.isEmpty();
    }

    @Override
    public Expression withSource(Source source) {
        return new CommaExpressionImpl(comments(), source, expressions);
    }

    public static class Builder extends ElementImpl.Builder<CommaExpression.Builder> implements CommaExpression.Builder {
        private final List<Expression> expressions = new ArrayList<>();

        @Override
        public CommaExpression.Builder addExpression(Expression expression) {
            this.expressions.add(expression);
            return this;
        }

        @Override
        public CommaExpression.Builder addExpressions(List<Expression> expressions) {
            this.expressions.addAll(expressions);
            return this;
        }

        @Override
        public CommaExpression build() {
            return new CommaExpressionImpl(comments, source, List.copyOf(expressions));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommaExpressionImpl that)) return false;
        return expressions.equals(that.expressions);
    }

    @Override
    public int hashCode() {
        return expressions.hashCode();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        List<Expression> translatedExpressions = expressions.stream()
                .map(e -> e.translate(translationMap))
                .collect(translationMap.toList(expressions));
        if (translatedExpressions == expressions) return this;
        return new CommaExpressionImpl(comments(), source(), translatedExpressions);
    }

    @Override
    public List<Expression> expressions() {
        return expressions;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return expressions.get(expressions.size() - 1).parameterizedType();
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.BOTTOM;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_COMMA;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expressions instanceof CommaExpression ce) {
            return ListUtil.compare(expressions, ce.expressions());
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expressions.forEach(e -> e.visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            expressions.forEach(e -> e.visit(visitor));
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return expressions.stream().map(expression -> expression.print(qualification))
                .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expressions.stream().flatMap(e -> e.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return expressions.stream().flatMap(Element::typesReferenced);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new CommaExpressionImpl(comments(), source(),
                expressions.stream().map(e -> e.rewire(infoMap)).toList());
    }
}
