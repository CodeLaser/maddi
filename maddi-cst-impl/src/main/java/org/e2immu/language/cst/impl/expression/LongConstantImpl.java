package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.LongConstant;
import org.e2immu.language.cst.api.expression.Numeric;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;

public class LongConstantImpl extends ConstantExpressionImpl<Long> implements Numeric, LongConstant {

    private final long value;
    private final ParameterizedType parameterizedType;

    public LongConstantImpl(Predefined predefined, long value) {
        this(List.of(), null, predefined.longParameterizedType(), value);
    }

    public LongConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, long value) {
        super(comments, source, 0 == value ? 1 : 2);
        this.parameterizedType = parameterizedType;
        this.value = value;
    }

    @Override
    public Expression withSource(Source source) {
        return new LongConstantImpl(comments(), source, parameterizedType, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LongConstantImpl that = (LongConstantImpl) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(value + "L"));
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
        return ExpressionComparator.ORDER_CONSTANT_LONG;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return (int) Math.signum(value - ((IntConstant) expression).constant());
    }

    @Override
    public Long constant() {
        return value;
    }

    @Override
    public Expression negate() {
        return new LongConstantImpl(comments(), source(), parameterizedType, -value);
    }

    @Override
    public Expression bitwiseNegation() {
        return new LongConstantImpl(comments(), source(), parameterizedType, ~value);
    }
}
