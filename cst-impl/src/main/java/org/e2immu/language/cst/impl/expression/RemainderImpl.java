package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Divide;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.expression.Remainder;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public class RemainderImpl extends BinaryOperatorImpl implements Remainder {

    public RemainderImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence,
                         Expression lhs, Expression rhs, ParameterizedType parameterizedType) {
        super(comments, source, operator, precedence, lhs, rhs, parameterizedType);
    }

    public RemainderImpl(Runtime runtime, Expression lhs, Expression rhs) {
        super(List.of(), null, runtime.remainderOperatorInt(), runtime.precedenceMultiplicative(), lhs,
                rhs, runtime.widestTypeUnbox(lhs.parameterizedType(), rhs.parameterizedType()));
    }

    @Override
    public Expression withSource(Source source) {
        return new RemainderImpl(comments(), source, operator, precedence, lhs, rhs, parameterizedType);
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new RemainderImpl(comments(), source(), operator, precedence, lhs.rewire(infoMap), rhs.rewire(infoMap),
                parameterizedType);
    }
}
