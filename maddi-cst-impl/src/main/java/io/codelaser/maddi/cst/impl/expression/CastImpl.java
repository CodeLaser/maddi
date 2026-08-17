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

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Cast;
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
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CastImpl extends ExpressionImpl implements Cast {
    private final Expression expression;
    private final ParameterizedType parameterizedType;

    public CastImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, Expression expression) {
        super(comments, source, 1 + expression.complexity());
        this.expression = expression;
        this.parameterizedType = parameterizedType;
    }

    @Override
    public Expression withSource(Source source) {
        return new CastImpl(comments(), source, parameterizedType, expression);
    }

    public static class Builder extends ElementImpl.Builder<Cast.Builder> implements Cast.Builder {
        private Expression expression;
        private ParameterizedType parameterizedType;

        @Override
        public Cast.Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public Cast.Builder setParameterizedType(ParameterizedType parameterizedType) {
            this.parameterizedType = parameterizedType;
            return this;
        }

        @Override
        public Cast build() {
            return new CastImpl(comments, source, parameterizedType, expression);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CastImpl cast = (CastImpl) o;
        return Objects.equals(expression, cast.expression) && Objects.equals(parameterizedType, cast.parameterizedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, parameterizedType);
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.CAST;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof Cast cast) {
            return expression.compareTo(cast.expression());
        }
        throw new InternalCompareToException();
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
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                .add(parameterizedType.print(qualification, false, DiamondEnum.SHOW_ALL))
                .add(SymbolEnum.RIGHT_PARENTHESIS_AFTER_CAST)
                .add(outputInParenthesis(qualification, precedence(), expression));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expression.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return Stream.concat(parameterizedType.typesReferenced(TypeReferenceNature.EXPLICIT, source().detailedSources()),
                expression.typesReferenced(predicate));
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedExpression = expression.translate(translationMap);
        ParameterizedType translatedType = translationMap.translateType(this.parameterizedType);
        if (translatedExpression == this.expression && translatedType == this.parameterizedType) return this;
        Expression result = new CastImpl(comments(), source(), translatedType, translatedExpression);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new CastImpl(comments(), source(), parameterizedType.rewire(infoMap), expression.rewire(infoMap));
    }
}
