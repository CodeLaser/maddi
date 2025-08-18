package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.BooleanConstant;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;

public class BooleanConstantImpl extends ConstantExpressionImpl<Boolean> implements BooleanConstant {
    private final ParameterizedType booleanPt;
    private final boolean constant;

    public BooleanConstantImpl(Predefined predefined, boolean constant) {
        this(List.of(), null, predefined.booleanParameterizedType(), constant);
    }

    public BooleanConstantImpl(List<Comment> comments, Source source, ParameterizedType booleanPt, boolean constant) {
        super(comments, source, 1);
        this.booleanPt = booleanPt;
        this.constant = constant;
    }

    @Override
    public Expression withSource(Source source) {
        return new BooleanConstantImpl(comments(), source, booleanPt, constant);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof BooleanConstant bc && constant == bc.constant();
    }

    @Override
    public int hashCode() {
        return constant ? 1 : 0;
    }

    @Override
    public Boolean constant() {
        return constant;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return booleanPt;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_BOOLEAN;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        BooleanConstant bc = (BooleanConstant) expression;
        if (constant == bc.constant()) return 0;
        return constant ? -1 : 1;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(Boolean.toString(constant)));
    }

    public BooleanConstant negate() {
        return new BooleanConstantImpl(comments(), source(), booleanPt, !constant);
    }
}
