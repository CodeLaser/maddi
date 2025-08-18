package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.GreaterThanZero;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BinaryOperatorImpl extends ExpressionImpl implements BinaryOperator {

    protected final MethodInfo operator;
    protected final Precedence precedence;
    protected final Expression lhs;
    protected final Expression rhs;
    protected final ParameterizedType parameterizedType;

    public BinaryOperatorImpl(List<Comment> comments, Source source,
                              MethodInfo operator, Precedence precedence, Expression lhs, Expression rhs,
                              ParameterizedType parameterizedType) {
        super(comments, source, 1 + lhs.complexity() + rhs.complexity());
        this.lhs = Objects.requireNonNull(lhs);
        this.rhs = Objects.requireNonNull(rhs);
        this.operator = Objects.requireNonNull(operator);
        this.precedence = Objects.requireNonNull(precedence);
        this.parameterizedType = Objects.requireNonNull(parameterizedType);
    }

    @Override
    public Expression withSource(Source source) {
        return new BinaryOperatorImpl(comments(), source, operator, precedence, lhs, rhs, parameterizedType);
    }

    public static class Builder extends ElementImpl.Builder<BinaryOperator.Builder> implements BinaryOperator.Builder {
        protected MethodInfo operator;
        protected Precedence precedence;
        protected Expression lhs;
        protected Expression rhs;
        protected ParameterizedType parameterizedType;

        @Override
        public Builder setParameterizedType(ParameterizedType parameterizedType) {
            this.parameterizedType = parameterizedType;
            return this;
        }

        @Override
        public BinaryOperator.Builder setLhs(Expression lhs) {
            this.lhs = lhs;
            return this;
        }

        @Override
        public BinaryOperator.Builder setRhs(Expression rhs) {
            this.rhs = rhs;
            return this;
        }

        @Override
        public BinaryOperator.Builder setOperator(MethodInfo operator) {
            this.operator = operator;
            return this;
        }

        @Override
        public BinaryOperator.Builder setPrecedence(Precedence precedence) {
            this.precedence = precedence;
            return this;
        }

        @Override
        public BinaryOperator build() {
            return new BinaryOperatorImpl(comments, source, operator, precedence, lhs, rhs, parameterizedType);
        }
    }

    @Override
    public Expression lhs() {
        return lhs;
    }

    @Override
    public Expression rhs() {
        return rhs;
    }

    @Override
    public MethodInfo operator() {
        return operator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryOperatorImpl that = (BinaryOperatorImpl) o;
        return Objects.equals(operator, that.operator) && Objects.equals(lhs, that.lhs) && Objects.equals(rhs, that.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, lhs, rhs);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public Precedence precedence() {
        return precedence;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_BINARY_OPERATOR; // not yet evaluated
    }

    @Override
    public Stream<Variable> variables(DescendMode descendIntoFieldReferences) {
        return Stream.concat(lhs.variables(descendIntoFieldReferences), rhs.variables(descendIntoFieldReferences));
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            lhs.visit(predicate);
            rhs.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            lhs.visit(visitor);
            rhs.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public int internalCompareTo(Expression expression) {
        BinaryOperator b;
        if (expression instanceof BinaryOperator binaryOperator) {
            b = binaryOperator;
        } else if (expression instanceof GreaterThanZero gt0) {
            return compareBinaryToGt0(this, gt0);
        } else {
            int c = rhs.compareTo(expression);
            if (c == 0) c = lhs.compareTo(expression);
            return c;
        }
        int c0 = compareVariables(this, b);
        if (c0 != 0) return c0;

        // if there's a variable, it'll be in rhs
        // so priority is on the right hand side!!!
        int c = rhs.compareTo(b.rhs());
        if (c == 0) {
            c = lhs.compareTo(b.lhs());
        }
        return c;
    }

    public static int compareBinaryToGt0(BinaryOperator e1, GreaterThanZero e2) {
        int c = compareVariables(e1, e2);
        if (c != 0) return c;
        return -1;// binary operator (equals, e.g.) left of comparison
    }

    public static int compareVariables(Expression e1, Expression e2) {
        List<Variable> variables1 = e1.variableStreamDescend().toList();
        List<Variable> variables2 = e2.variableStreamDescend().toList();
        int s1 = variables1.size();
        int s2 = variables2.size();
        if (s1 == 0 && s2 == 0) return 0;
        if (s1 == 1 && s2 == 0 || s1 == 0) return s1 - s2;
        if (s1 == 1 && s2 == 1)
            return variables1.get(0).fullyQualifiedName().compareTo(variables2.get(0).fullyQualifiedName());

        // now the more complex situation
        Set<Variable> myVariables = new HashSet<>(variables1);
        Set<Variable> otherVariables = new HashSet<>(variables2);
        int varDiff = myVariables.size() - otherVariables.size();
        if (varDiff != 0) return varDiff;
        String myVarStr = myVariables.stream().map(Variable::fullyQualifiedName)
                .sorted().collect(Collectors.joining(","));
        String otherVarStr = otherVariables.stream().map(Variable::fullyQualifiedName)
                .sorted().collect(Collectors.joining(","));
        return myVarStr.compareTo(otherVarStr);
    }


    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), lhs))
                .add(SymbolEnum.binaryOperator(operator.name()))
                .add(outputInParenthesis(qualification, precedence(), rhs));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(lhs.typesReferenced(), rhs.typesReferenced());
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedLhs = lhs.translate(translationMap);
        Expression translatedRhs = rhs.translate(translationMap);
        if (translatedRhs == this.rhs && translatedLhs == this.lhs) return this;
        return new BinaryOperatorImpl(comments(), source(), operator, precedence, translatedLhs, translatedRhs,
                parameterizedType);
    }

    @Override
    public boolean isNumeric() {
        return parameterizedType().isNumeric();
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new BinaryOperatorImpl(comments(), source(), operator, precedence, lhs.rewire(infoMap),
                rhs.rewire(infoMap), parameterizedType);
    }
}
