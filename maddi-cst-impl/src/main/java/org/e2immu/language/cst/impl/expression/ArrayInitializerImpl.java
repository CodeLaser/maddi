package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.util.internal.util.ListUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ArrayInitializerImpl extends ExpressionImpl implements ArrayInitializer {

    private final ParameterizedType commonType;
    private final List<Expression> expressions;

    public ArrayInitializerImpl(List<Expression> expressions, ParameterizedType commonType) {
        this(List.of(), null, expressions, commonType);
    }

    public ArrayInitializerImpl(List<Comment> comments, Source source, List<Expression> expressions, ParameterizedType commonType) {
        super(comments, source, expressions.stream().mapToInt(Expression::complexity).sum() + 1);
        this.commonType = commonType;
        this.expressions = expressions;
    }

    public static class Builder extends ElementImpl.Builder<ArrayInitializer.Builder> implements ArrayInitializer.Builder {
        private List<Expression> expressions;
        private ParameterizedType commonType;

        @Override
        public ArrayInitializer.Builder setExpressions(List<Expression> expressions) {
            this.expressions = expressions;
            return this;
        }

        @Override
        public ArrayInitializer.Builder setCommonType(ParameterizedType commonType) {
            this.commonType = commonType;
            return this;
        }

        @Override
        public ArrayInitializer build() {
            return new ArrayInitializerImpl(comments, source, List.copyOf(expressions), commonType);
        }
    }

    @Override
    public Expression withSource(Source source) {
        return new ArrayInitializerImpl(comments(), source, expressions, commonType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayInitializerImpl that = (ArrayInitializerImpl) o;
        return Objects.equals(expressions, that.expressions);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expressions);
    }

    @Override
    public List<Expression> expressions() {
        return expressions;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return commonType.copyWithArrays(commonType.arrays() + 1);
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.BOTTOM;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_ARRAY;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof ArrayInitializer ai) {
            return ListUtil.compare(expressions, ai.expressions());
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expressions.forEach(v -> v.visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            expressions.forEach(v -> v.visit(visitor));
        }
        visitor.afterExpression(this);
    }

    @Override
    public boolean isConstant() {
        return expressions.stream().allMatch(Expression::isConstant);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl()
                .add(SymbolEnum.LEFT_BRACE)
                .add(expressions.stream().map(expression -> expression.print(qualification))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)))
                .add(SymbolEnum.RIGHT_BRACE);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expressions.stream().flatMap(e -> e.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return expressions.stream().flatMap(Expression::typesReferenced);
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;
        List<Expression> translatedExpressions = expressions.stream()
                .map(e -> e.translate(translationMap))
                .collect(translationMap.toList(expressions));
        ParameterizedType translatedType = translationMap.translateType(commonType);
        if (translatedType == commonType && translatedExpressions == expressions) return this;
        return new ArrayInitializerImpl(comments(), source(), translatedExpressions, translatedType);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        List<Expression> rewiredExpressions = expressions.stream().map(e -> e.rewire(infoMap)).toList();
        return new ArrayInitializerImpl(comments(), source(), rewiredExpressions, commonType.rewire(infoMap));
    }
}
