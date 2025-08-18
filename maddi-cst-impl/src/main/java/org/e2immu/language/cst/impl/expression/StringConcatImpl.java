package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.expression.StringConcat;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Factory;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;

import java.util.List;

public class StringConcatImpl extends BinaryOperatorImpl implements StringConcat {
    public StringConcatImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence,
                            Expression lhs, Expression rhs, ParameterizedType parameterizedType) {
        super(comments, source, operator, precedence, lhs, rhs, parameterizedType);
    }

    public StringConcatImpl(Predefined predefined, Expression lhs, Expression rhs) {
        super(List.of(), null, predefined.plusOperatorString(), PrecedenceEnum.ADDITIVE, lhs,
                rhs, predefined.stringParameterizedType());
    }

    @Override
    public Expression withSource(Source source) {
        return new StringConcatImpl(comments(), source, operator, precedence, lhs, rhs, parameterizedType);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new StringConcatImpl(comments(), source(), operator, precedence, lhs.rewire(infoMap),
                rhs.rewire(infoMap), parameterizedType);
    }
}
