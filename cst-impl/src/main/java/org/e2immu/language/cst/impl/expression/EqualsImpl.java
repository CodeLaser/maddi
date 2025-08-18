package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Equals;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public class EqualsImpl extends BinaryOperatorImpl implements Equals {

    public EqualsImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence,
                      Expression lhs, Expression rhs, ParameterizedType parameterizedType) {
        super(comments, source, operator, precedence, lhs, rhs, parameterizedType);
    }

    public EqualsImpl(Runtime runtime, Expression lhs, Expression rhs) {
        super(List.of(), null, lhs.isNumeric() ? runtime.equalsOperatorInt() : runtime.equalsOperatorObject(),
                runtime.precedenceEquality(), lhs, rhs,
                runtime.booleanParameterizedType());
    }

    @Override
    public Expression withSource(Source source) {
        return new EqualsImpl(comments(), source, operator, precedence, lhs, rhs, parameterizedType);
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression tl = lhs.translate(translationMap);
        Expression tr = rhs.translate(translationMap);
        if (tl == lhs && tr == rhs) return this;
        return new EqualsImpl(comments(), source(), operator, precedence, tl, tr, parameterizedType);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new EqualsImpl(comments(), source(), operator, precedence, lhs.rewire(infoMap), rhs.rewire(infoMap),
                parameterizedType);
    }
}
