package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.*;
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
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ClassExpressionImpl extends ConstantExpressionImpl<ParameterizedType> implements ClassExpression {
    public final ParameterizedType parameterizedType; // String.class -> String
    public final ParameterizedType classType; // String.class -> Class<String>

    public ClassExpressionImpl(List<Comment> comments,
                               Source source,
                               ParameterizedType parameterizedType,
                               ParameterizedType classType) {
        super(comments, source, 1);
        this.parameterizedType = Objects.requireNonNull(parameterizedType);
        this.classType = Objects.requireNonNull(classType);
    }

    public static class Builder extends ElementImpl.Builder<ClassExpression.Builder> implements ClassExpression.Builder {
        private ParameterizedType parameterizedType;
        private ParameterizedType classType;

        @Override
        public ClassExpression.Builder setParameterizedType(ParameterizedType type) {
            this.parameterizedType = type;
            return this;
        }

        @Override
        public Builder setClassType(ParameterizedType classType) {
            this.classType = classType;
            return this;
        }

        @Override
        public ClassExpression build() {
            return new ClassExpressionImpl(comments, source, parameterizedType, classType);
        }
    }

    @Override
    public Expression withSource(Source source) {
        return new ClassExpressionImpl(comments(), source, parameterizedType, classType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassExpression that = (ClassExpression) o;
        return parameterizedType.equals(that.parameterizedType());
    }

    @Override
    public ParameterizedType constant() {
        return classType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterizedType);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return classType;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(parameterizedType.print(qualification, false, DiamondEnum.NO))
                .add(SymbolEnum.DOT).add(KeywordImpl.CLASS);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return parameterizedType.typesReferencedMadeExplicit();
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_CLASS;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof ClassExpression ce) {
            return parameterizedType.detailedString().compareTo(ce.parameterizedType().detailedString());
        } else throw new InternalCompareToException();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        ParameterizedType translatedType = translationMap.translateType(this.parameterizedType);
        if (this.parameterizedType == translatedType) return this;
        ParameterizedType translatedClassType = translationMap.translateType(classType);
        return new ClassExpressionImpl(comments(), source(), translatedType, translatedClassType);
    }

    @Override
    public ParameterizedType type() {
        return parameterizedType;
    }
}
