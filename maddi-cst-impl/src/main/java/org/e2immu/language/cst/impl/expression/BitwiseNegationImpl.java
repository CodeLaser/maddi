package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.BitwiseNegation;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.util.internal.util.IntUtil;

import java.util.List;

public class BitwiseNegationImpl extends UnaryOperatorImpl implements BitwiseNegation {

    public BitwiseNegationImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence, Expression expression) {
        super(comments, source, operator, expression, precedence);
    }

    @Override
    public Double numericValue() {
        Double d = expression.numericValue();
        if (d != null && IntUtil.isMathematicalInteger(d)) {
            long bitwiseNegation = ~((long) (double) d);
            return (double) bitwiseNegation;
        }
        return null;
    }

    @Override
    public int wrapperOrder() {
        return 0;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new BitwiseNegationImpl(comments(), source(), operator, precedence, expression.rewire(infoMap));
    }
}
