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

import io.codelaser.maddi.cst.api.element.*;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.Diamond;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.codelaser.maddi.cst.api.element.Element.TypeReferenceNature.EXPLICIT;

public class TypeExpressionImpl extends ExpressionImpl implements TypeExpression {
    public final ParameterizedType parameterizedType;
    public final Diamond diamond;

    public TypeExpressionImpl(ParameterizedType parameterizedType, Diamond diamond) {
        this(List.of(), null, parameterizedType, diamond);
    }

    public TypeExpressionImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, Diamond diamond) {
        super(comments, source, 1);
        this.parameterizedType = Objects.requireNonNull(parameterizedType);
        this.diamond = diamond;
    }

    public static class Builder extends ElementImpl.Builder<TypeExpression.Builder> implements TypeExpression.Builder {
        private ParameterizedType parameterizedType;
        private Diamond diamond;

        @Override
        public TypeExpression.Builder setParameterizedType(ParameterizedType parameterizedType) {
            this.parameterizedType = parameterizedType;
            return this;
        }

        @Override
        public Builder setDiamond(Diamond diamond) {
            this.diamond = diamond;
            return this;
        }

        @Override
        public TypeExpression build() {
            return new TypeExpressionImpl(comments, source, parameterizedType, diamond);
        }
    }

    @Override
    public Expression withSource(Source source) {
        return new TypeExpressionImpl(comments(), source, parameterizedType, diamond);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeExpression that = (TypeExpression) o;
        return parameterizedType.equals(that.parameterizedType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterizedType);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(parameterizedType.print(qualification, false, diamond));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        TypeInfo typeInfo = parameterizedType().typeInfo();
        if (typeInfo == null) {
            if (parameterizedType.typeParameter() != null) {
                return parameterizedType.typeParameter().typeBounds()
                        .stream().flatMap(pt -> pt.typesReferenced(TypeReferenceNature.IMPLICIT,
                                null));
            }
            return Stream.of();
        }
        DetailedSources detailedSources = source() == null ? null : source().detailedSources();
        TypeInfo qualifier = detailedSources == null ? typeInfo : detailedSources.qualifier(typeInfo);
        Element.TypeReference typeReference = new ElementImpl.TypeReference(typeInfo, EXPLICIT, qualifier);
        return Stream.of(typeReference);
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeExpression(this);
        visitor.afterExpression(this);
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.TOP;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_TYPE;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof TypeExpression te) {
            return parameterizedType.detailedString().compareTo(te.parameterizedType().detailedString());
        } else throw new InternalCompareToException();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        ParameterizedType translatedType = translationMap.translateType(parameterizedType);
        if (translatedType == parameterizedType) return this;
        Expression result = new TypeExpressionImpl(comments(), source(), translatedType, diamond);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new TypeExpressionImpl(comments(), source(), parameterizedType.rewire(infoMap), diamond);
    }
}
