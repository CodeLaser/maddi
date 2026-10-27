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
import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
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
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.util.ListUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ArrayInitializerImpl extends ExpressionImpl implements ArrayInitializer {

    private final ParameterizedType commonType;
    private final List<Expression> expressions;

    public ArrayInitializerImpl(List<Expression> expressions, ParameterizedType commonType) {
        this(List.of(), null, expressions, commonType);
    }

    public ArrayInitializerImpl(List<Comment> comments, Source source, List<Expression> expressions, ParameterizedType commonType) {
        super(comments, source, expressions.stream().mapToInt(Expression::complexity).sum() + 1);
        this.commonType = commonType;
        this.expressions = expressions;
    }

    public static class Builder extends ElementImpl.Builder<ArrayInitializer.Builder> implements ArrayInitializer.Builder {
        private List<Expression> expressions;
        private ParameterizedType commonType;

        @Override
        public ArrayInitializer.Builder setExpressions(List<Expression> expressions) {
            this.expressions = expressions;
            return this;
        }

        @Override
        public ArrayInitializer.Builder setCommonType(ParameterizedType commonType) {
            this.commonType = commonType;
            return this;
        }

        @Override
        public ArrayInitializer build() {
            return new ArrayInitializerImpl(comments, source, List.copyOf(expressions), commonType);
        }
    }

    @Override
    public Expression withSource(Source source) {
        return new ArrayInitializerImpl(comments(), source, expressions, commonType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayInitializerImpl that = (ArrayInitializerImpl) o;
        return Objects.equals(expressions, that.expressions);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expressions);
    }

    @Override
    public List<Expression> expressions() {
        return expressions;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return commonType.copyWithArrays(commonType.arrays() + 1);
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.BOTTOM;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_ARRAY;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof ArrayInitializer ai) {
            return ListUtil.compare(expressions, ai.expressions());
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expressions.forEach(v -> v.visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            expressions.forEach(v -> v.visit(visitor));
        }
        visitor.afterExpression(this);
    }

    @Override
    public boolean isConstant() {
        return expressions.stream().allMatch(Expression::isConstant);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl()
                .add(SymbolEnum.LEFT_BRACE)
                .add(expressions.stream().map(expression -> expression.print(qualification))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)))
                .add(SymbolEnum.RIGHT_BRACE);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expressions.stream().flatMap(e -> e.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return expressions.stream().flatMap(expression -> expression.typesReferenced(predicate));
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;
        List<Expression> translatedExpressions = expressions.stream()
                .map(e -> e.translate(translationMap))
                .collect(translationMap.toList(expressions));
        ParameterizedType translatedType = translationMap.translateType(commonType);
        if (translatedType == commonType && translatedExpressions == expressions) return this;
        ArrayInitializer result = new ArrayInitializerImpl(comments(), source(), translatedExpressions, translatedType);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        List<Expression> rewiredExpressions = expressions.stream().map(e -> e.rewire(infoMap)).toList();
        return new ArrayInitializerImpl(comments(), source(), rewiredExpressions, commonType.rewire(infoMap));
    }
}
