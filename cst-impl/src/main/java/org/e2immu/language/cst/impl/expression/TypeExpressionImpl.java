package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TypeExpressionImpl extends ExpressionImpl implements TypeExpression {
    public final ParameterizedType parameterizedType;
    public final Diamond diamond;

    public TypeExpressionImpl(ParameterizedType parameterizedType, Diamond diamond) {
        this(List.of(), null, parameterizedType, diamond);
    }

    public TypeExpressionImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, Diamond diamond) {
        super(comments, source, 1);
        this.parameterizedType = Objects.requireNonNull(parameterizedType);
        this.diamond = diamond;
    }

    public static class Builder extends ElementImpl.Builder<TypeExpression.Builder> implements TypeExpression.Builder {
        private ParameterizedType parameterizedType;
        private Diamond diamond;

        @Override
        public TypeExpression.Builder setParameterizedType(ParameterizedType parameterizedType) {
            this.parameterizedType = parameterizedType;
            return this;
        }

        @Override
        public Builder setDiamond(Diamond diamond) {
            this.diamond = diamond;
            return this;
        }

        @Override
        public TypeExpression build() {
            return new TypeExpressionImpl(comments, source, parameterizedType, diamond);
        }
    }

    @Override
    public Expression withSource(Source source) {
        return new TypeExpressionImpl(comments(), source, parameterizedType, diamond);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeExpression that = (TypeExpression) o;
        return parameterizedType.equals(that.parameterizedType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterizedType);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(parameterizedType.print(qualification, false, diamond));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.of(new ElementImpl.TypeReference(parameterizedType().typeInfo(), true));
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeExpression(this);
        visitor.afterExpression(this);
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.TOP;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_TYPE;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof TypeExpression te) {
            return parameterizedType.detailedString().compareTo(te.parameterizedType().detailedString());
        } else throw new InternalCompareToException();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        ParameterizedType translatedType = translationMap.translateType(parameterizedType);
        if (translatedType == parameterizedType) return this;
        return new TypeExpressionImpl(comments(), source(), translatedType, diamond);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new TypeExpressionImpl(comments(), source(), parameterizedType.rewire(infoMap), diamond);
    }
}
