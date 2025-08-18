package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.CharConstant;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;

public class CharConstantImpl extends ConstantExpressionImpl<Character> implements CharConstant {

    private final char constant;
    private final ParameterizedType parameterizedType;

    public CharConstantImpl(Predefined predefined, char constant) {
        this(List.of(), null, predefined.charParameterizedType(), constant);
    }

    public CharConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, char constant) {
        super(comments, source, '\0' == constant ? 1 : 2);
        this.parameterizedType = parameterizedType;
        this.constant = constant;
    }

    @Override
    public Expression withSource(Source source) {
        return new CharConstantImpl(comments(), source, parameterizedType, constant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharConstantImpl that = (CharConstantImpl) o;
        return constant == that.constant;
    }

    @Override
    public int hashCode() {
        return constant;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl("'" + escaped(constant) + "'"));
    }

    public static String escaped(char constant) {
        return switch (constant) {
            case '\0' -> "\\0";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\f' -> "\\f";
            case '\'' -> "\\'";
            case '\"' -> "\\\"";
            case '\\' -> "\\\\";
            default -> {
                if (constant >= 32 && constant <= 127) yield Character.toString(constant);
                String hex = Integer.toString(constant, 16);
                yield "\\u" + "0".repeat(Math.max(0, 4 - hex.length())) + hex;
            }
        };
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_CHAR;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return constant - ((CharConstant) expression).constant();
    }

    @Override
    public Character constant() {
        return constant;
    }
}
