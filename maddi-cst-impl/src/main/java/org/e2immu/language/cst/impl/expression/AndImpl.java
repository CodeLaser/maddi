package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.And;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.util.internal.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class AndImpl extends ExpressionImpl implements And {

    private final List<Expression> expressions;
    private final ParameterizedType booleanPt;

    public AndImpl(Predefined predefined, List<Expression> expressions) {
        this(predefined.booleanParameterizedType(), expressions);
    }

    private AndImpl(ParameterizedType booleanPt, List<Expression> expressions) {
        super(1 + expressions.stream().mapToInt(Expression::complexity).sum());
        this.expressions = expressions;
        this.booleanPt = booleanPt;
        assert expressions.size() > 1;
    }

    public AndImpl(List<Comment> comments, Source source, ParameterizedType booleanPt, List<Expression> expressions) {
        super(comments, source, 1 + expressions.stream().mapToInt(Expression::complexity).sum());
        this.expressions = expressions;
        this.booleanPt = booleanPt;
        assert expressions.size() > 1;
    }

    @Override
    public Expression withSource(Source source) {
        return new AndImpl(comments(), source, booleanPt, expressions);
    }

    @Override
    public List<Expression> expressions() {
        return expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        And andValue = (And) o;
        return expressions.equals(andValue.expressions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressions);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        Precedence precedence = precedence();
        return new OutputBuilderImpl()
                .add(expressions.stream().map(e -> outputInParenthesis(qualification, precedence, e))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_AND)));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return expressions.stream().flatMap(Expression::typesReferenced);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return booleanPt;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.LOGICAL_AND;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_AND;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        And andValue = (And) expression;
        return ListUtil.compare(expressions, andValue.expressions());
    }


    @Override
    public Stream<Variable> variables(DescendMode descendIntoFieldReferences) {
        return expressions.stream().flatMap(v -> v.variables(descendIntoFieldReferences));
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
            expressions.forEach(e -> e.visit(visitor));
        }
        visitor.afterExpression(this);
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;
        List<Expression> translatedExpressions = expressions.isEmpty() ? expressions : expressions.stream()
                .map(e -> e.translate(translationMap))
                .collect(translationMap.toList(expressions));
        if (expressions == translatedExpressions) return this;
        return new AndImpl(comments(), source(), booleanPt, translatedExpressions);
    }


    public static class Builder extends ElementImpl.Builder<And.Builder> implements And.Builder {
        private final List<Expression> expressions = new ArrayList<>();
        private ParameterizedType booleanPt;

        @Override
        public And.Builder addExpressions(List<Expression> expressions) {
            this.expressions.addAll(expressions);
            return this;
        }

        @Override
        public And.Builder addExpression(Expression expression) {
            this.expressions.add(expression);
            return this;
        }

        public And.Builder setBooleanParameterizedType(ParameterizedType parameterizedType) {
            this.booleanPt = parameterizedType;
            return this;
        }

        @Override
        public And build() {
            return new AndImpl(comments, source, booleanPt, List.copyOf(expressions));
        }
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new AndImpl(comments(), source(), booleanPt,
                expressions.stream().map(e -> e.rewire(infoMap)).toList());
    }
}
