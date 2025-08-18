package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;


public class SumImpl extends BinaryOperatorImpl implements Sum {

    public SumImpl(Runtime runtime, Expression lhs, Expression rhs) {
        super(List.of(), null, runtime.plusOperatorInt(), runtime.precedenceAdditive(), lhs, rhs,
                runtime.widestTypeUnbox(lhs.parameterizedType(), rhs.parameterizedType()));
        assert lhs.isNumeric() : "Have " + lhs + ", " + lhs.parameterizedType(); // and definitely not StringConstant!
        assert rhs.isNumeric() : "Have " + rhs + ", " + rhs.parameterizedType();
    }

    public SumImpl(List<Comment> comments, Source source, MethodInfo operator, Precedence precedence,
                   Expression lhs, Expression rhs, ParameterizedType parameterizedType) {
        super(comments, source, operator, precedence, lhs, rhs, parameterizedType);
    }

    @Override
    public Expression withSource(Source source) {
        return new SumImpl(comments(), source, operator, precedence, lhs, rhs, parameterizedType);
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_SUM;
    }

    /*
        Generate the expression lhs+rhs == 0.
        Handle lhs-rhs==0  === lhs==rhs, etc.
        This is part of the system that computes equality.
         */
    @Override
    public Expression isZero(Runtime runtime) {
        if (lhs instanceof Negation negation && !(rhs instanceof Negation)) {
            return runtime.equals(negation.expression(), rhs);
        }
        if (rhs instanceof Negation negation && !(lhs instanceof Negation)) {
            return runtime.equals(lhs, negation.expression());
        }
        if (lhs instanceof Negation nLhs) {
            return runtime.equals(nLhs, rhs);
        }
        return runtime.equals(lhs, runtime.negate(rhs));
    }

    /*
    extra code to avoid writing a+-b ~ a-b
     */
    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), lhs));
        boolean ignoreOperator = rhs instanceof Negation || rhs instanceof Sum sum2 && (sum2.lhs() instanceof Negation);
        if (!ignoreOperator) {
            outputBuilder.add(SymbolEnum.binaryOperator(operator.name()));
        }
        return outputBuilder.add(outputInParenthesis(qualification, precedence(), rhs));
    }

    // recursive method
    public Double numericPartOfLhs() {
        Numeric n;
        if ((n = lhs.asInstanceOf(Numeric.class)) != null) return n.doubleValue();
        Sum s;
        if ((s = lhs.asInstanceOf(Sum.class)) != null) return s.numericPartOfLhs();
        return null;
    }

    // can only be called when there is a numeric part somewhere!
    public Expression nonNumericPartOfLhs(Runtime runtime) {
        if (lhs.isInstanceOf(Numeric.class)) return rhs;
        Sum s;
        if ((s = lhs.asInstanceOf(Sum.class)) != null) {
            // the numeric part is somewhere inside lhs
            Expression nonNumeric = s.nonNumericPartOfLhs(runtime);
            return runtime.sum(nonNumeric, rhs);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;
        Expression tl = lhs.translate(translationMap);
        Expression tr = rhs.translate(translationMap);
        if (tl == lhs && tr == rhs) return this;
        return new SumImpl(comments(), source(), operator, precedence, tl, tr, parameterizedType);
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new SumImpl(comments(), source(), operator, precedence, lhs.rewire(infoMap), rhs.rewire(infoMap),
                parameterizedType);
    }
}
