package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Cast;
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
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CastImpl extends ExpressionImpl implements Cast {
    private final Expression expression;
    private final ParameterizedType parameterizedType;

    public CastImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, Expression expression) {
        super(comments, source, 1 + expression.complexity());
        this.expression = expression;
        this.parameterizedType = parameterizedType;
    }

    @Override
    public Expression withSource(Source source) {
        return new CastImpl(comments(), source, parameterizedType, expression);
    }

    public static class Builder extends ElementImpl.Builder<Cast.Builder> implements Cast.Builder {
        private Expression expression;
        private ParameterizedType parameterizedType;

        @Override
        public Cast.Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public Cast.Builder setParameterizedType(ParameterizedType parameterizedType) {
            this.parameterizedType = parameterizedType;
            return this;
        }

        @Override
        public Cast build() {
            return new CastImpl(comments, source, parameterizedType, expression);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CastImpl cast = (CastImpl) o;
        return Objects.equals(expression, cast.expression) && Objects.equals(parameterizedType, cast.parameterizedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, parameterizedType);
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.CAST;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof Cast cast) {
            return expression.compareTo(cast.expression());
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            expression.visit(visitor);
        }
        visitor.afterExpression(this);
    }


    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                .add(parameterizedType.print(qualification, false, DiamondEnum.SHOW_ALL))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(outputInParenthesis(qualification, precedence(), expression));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expression.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(parameterizedType.typesReferencedMadeExplicit(), expression.typesReferenced());
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedExpression = expression.translate(translationMap);
        ParameterizedType translatedType = translationMap.translateType(this.parameterizedType);
        if (translatedExpression == this.expression && translatedType == this.parameterizedType) return this;
        return new CastImpl(comments(), source(), translatedType, translatedExpression);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new CastImpl(comments(), source(), parameterizedType.rewire(infoMap), expression.rewire(infoMap));
    }
}
