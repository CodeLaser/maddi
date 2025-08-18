package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;
import org.e2immu.util.internal.util.StringUtil;

import java.util.List;
import java.util.Objects;

public class StringConstantImpl extends ConstantExpressionImpl<String> implements StringConstant {
    private final ParameterizedType stringPt;
    private final String constant;

    public StringConstantImpl(Predefined predefined, String constant) {
        this(List.of(), null, predefined.stringParameterizedType(), constant);
    }

    public StringConstantImpl(List<Comment> comments, Source source, ParameterizedType stringPt, String constant) {
        super(comments, source, constant.isEmpty() ? 1 : 2);
        this.stringPt = Objects.requireNonNull(stringPt);
        this.constant = Objects.requireNonNull(constant);
    }

    @Override
    public Expression withSource(Source source) {
        return new StringConstantImpl(comments(), source, stringPt, constant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringConstantImpl that)) return false;
        return constant.equals(that.constant);
    }

    @Override
    public int hashCode() {
        return constant.hashCode();
    }

    @Override
    public String constant() {
        return constant;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return stringPt;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_STRING;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        StringConstant sc = (StringConstant) expression;
        return constant.compareTo(sc.constant());
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String quoted = StringUtil.quote(constant);
        return new OutputBuilderImpl().add(new TextImpl(quoted));
    }

    @Override
    public String toString() {
        return "stringConstant@" + source().compact2();
    }
}
