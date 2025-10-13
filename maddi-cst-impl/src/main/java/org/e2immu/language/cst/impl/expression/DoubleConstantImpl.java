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
import org.e2immu.language.cst.api.expression.DoubleConstant;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.FloatConstant;
import org.e2immu.language.cst.api.expression.Numeric;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;
import org.e2immu.util.internal.util.IntUtil;

import java.util.List;

public class DoubleConstantImpl extends ConstantExpressionImpl<Double> implements Numeric, DoubleConstant {

    private final double value;
    private final ParameterizedType parameterizedType;

    public DoubleConstantImpl(Predefined predefined, double value) {
        this(List.of(), null, predefined.doubleParameterizedType(), value);
    }

    public DoubleConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, double value) {
        super(comments, source, 0 == value ? 1 : 2);
        this.parameterizedType = parameterizedType;
        this.value = value;
    }

    @Override
    public Expression withSource(Source source) {
        return new DoubleConstantImpl(comments(), source, parameterizedType, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoubleConstantImpl that = (DoubleConstantImpl) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(Double.toString(value)));
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
        return ExpressionComparator.ORDER_CONSTANT_DOUBLE;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return (int) Math.signum(value - ((DoubleConstant) expression).constant());
    }

    @Override
    public Double constant() {
        return value;
    }

    @Override
    public Expression negate() {
        return new DoubleConstantImpl(comments(), source(), parameterizedType, -value);
    }

    @Override
    public Expression bitwiseNegation() {
        throw new UnsupportedOperationException();
    }

    public static String formatNumber(double d, Class<? extends Numeric> clazz) {
        if (IntUtil.isMathematicalInteger(d)) {
            return Long.toString((long) d);
        }
        if (clazz.equals(FloatConstant.class)) {
            return d + "f";
        }
        return Double.toString(d);
    }

}
