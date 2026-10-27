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
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.FloatConstant;
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.cst.api.expression.Numeric;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Predefined;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;

public class FloatConstantImpl extends ConstantExpressionImpl<Float> implements Numeric, FloatConstant {

    private final float value;
    private final ParameterizedType parameterizedType;
    // how the literal was written, when that is not how its value prints (#49); null otherwise
    private final String sourceSpelling;

    public FloatConstantImpl(Predefined predefined, float value) {
        this(List.of(), null, predefined.floatParameterizedType(), value);
    }

    public FloatConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, float value) {
        this(comments, source, parameterizedType, value, null);
    }

    public FloatConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, float value,
                             String sourceSpelling) {
        super(comments, source, value != 0 ? 2 : 1);
        this.parameterizedType = parameterizedType;
        this.value = value;
        this.sourceSpelling = sourceSpelling;
    }

    @Override
    public Expression withSource(Source source) {
        return new FloatConstantImpl(comments(), source, parameterizedType, value, sourceSpelling);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloatConstantImpl that = (FloatConstantImpl) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(value);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String text = sourceSpelling != null ? sourceSpelling : value + "F";
        return new OutputBuilderImpl().add(new TextImpl(text));
    }

    @Override
    public String sourceSpelling() {
        return sourceSpelling;
    }

    @Override
    public FloatConstantImpl withSourceSpelling(String sourceSpelling) {
        return new FloatConstantImpl(comments(), source(), parameterizedType, value, sourceSpelling);
    }

    @Override
    public Number number() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_FLOAT;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return (int) Math.signum(value - ((FloatConstant) expression).constant());
    }

    @Override
    public Float constant() {
        return value;
    }

    @Override
    public Expression negate() {
        return new FloatConstantImpl(comments(), source(), parameterizedType, value);
    }

    @Override
    public Expression bitwiseNegation() {
        throw new UnsupportedOperationException();
    }

}
