package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.NullConstant;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;

import java.util.List;

public class NullConstantImpl extends ConstantExpressionImpl<Object> implements NullConstant {

    private final ParameterizedType parameterizedType;

    public NullConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType) {
        super(comments, source, 1);
        assert parameterizedType.isTypeOfNullConstant();
        this.parameterizedType = parameterizedType;
    }

    @Override
    public Expression withSource(Source source) {
        return new NullConstantImpl(comments(), source, parameterizedType);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullConstant;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public Object constant() {
        return null;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_NULL;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return 0; // there's only one
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(KeywordImpl.NULL);
    }
}
