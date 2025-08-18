package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
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

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class AssignmentImpl extends ExpressionImpl implements Assignment {
    private final VariableExpression target;
    private final Expression value;
    private final Variable variableTarget;
    private final MethodInfo assignmentOperator;
    private final boolean assignmentOperatorIsPlus;
    private final MethodInfo binaryOperator;
    private final Boolean prefixPrimitiveOperator;

    public AssignmentImpl(VariableExpression target, Expression value) {
        this(List.of(), null, target, value, null, false,
                null, null);
    }

    public AssignmentImpl(List<Comment> comments, Source source,
                          VariableExpression target, Expression value, MethodInfo assignmentOperator,
                          boolean assignmentOperatorIsPlus, MethodInfo binaryOperator, Boolean prefixPrimitiveOperator) {
        super(comments, source, 1 + target.complexity() + value.complexity());
        this.value = value;
        this.target = target;
        this.variableTarget = target.variable();
        this.assignmentOperator = assignmentOperator;
        this.assignmentOperatorIsPlus = assignmentOperatorIsPlus;
        this.binaryOperator = binaryOperator;
        this.prefixPrimitiveOperator = prefixPrimitiveOperator;
    }

    @Override
    public Expression withSource(Source source) {
        return new AssignmentImpl(comments(), source, target, value, assignmentOperator, assignmentOperatorIsPlus,
                binaryOperator, prefixPrimitiveOperator);
    }

    public static class Builder extends ElementImpl.Builder<Assignment.Builder> implements Assignment.Builder {
        private VariableExpression target;
        private Expression value;
        private MethodInfo assignmentOperator;
        private boolean assignmentOperatorIsPlus;
        private MethodInfo binaryOperator;
        private Boolean prefixPrimitiveOperator;

        @Override
        public Builder setTarget(VariableExpression target) {
            this.target = target;
            return this;
        }

        @Override
        public Builder setValue(Expression value) {
            this.value = value;
            return this;
        }

        @Override
        public Builder setAssignmentOperator(MethodInfo assignmentOperator) {
            this.assignmentOperator = assignmentOperator;
            return this;
        }

        @Override
        public Builder setPrefixPrimitiveOperator(Boolean prefixPrimitiveOperator) {
            this.prefixPrimitiveOperator = prefixPrimitiveOperator;
            return this;
        }

        @Override
        public Builder setBinaryOperator(MethodInfo binaryOperator) {
            this.binaryOperator = binaryOperator;
            return this;
        }

        @Override
        public Builder setAssignmentOperatorIsPlus(boolean assignmentOperatorIsPlus) {
            this.assignmentOperatorIsPlus = assignmentOperatorIsPlus;
            return this;
        }

        @Override
        public Assignment build() {
            return new AssignmentImpl(comments, source, target, value, assignmentOperator, assignmentOperatorIsPlus,
                    binaryOperator, prefixPrimitiveOperator);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentImpl that)) return false;
        return Objects.equals(target, that.target) && Objects.equals(value, that.value)
               && Objects.equals(assignmentOperator, that.assignmentOperator)
               && Objects.equals(prefixPrimitiveOperator, that.prefixPrimitiveOperator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, value, assignmentOperator, prefixPrimitiveOperator);
    }

    @Override
    public VariableExpression target() {
        return target;
    }

    @Override
    public Expression value() {
        return value;
    }

    @Override
    public Variable variableTarget() {
        return variableTarget;
    }

    @Override
    public MethodInfo assignmentOperator() {
        return assignmentOperator;
    }

    @Override
    public Boolean prefixPrimitiveOperator() {
        return prefixPrimitiveOperator;
    }

    @Override
    public MethodInfo binaryOperator() {
        return binaryOperator;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return target.parameterizedType();
    }

    @Override
    public Precedence precedence() {
        if (prefixPrimitiveOperator != null) {
            return prefixPrimitiveOperator ? PrecedenceEnum.ASSIGNMENT : PrecedenceEnum.POST_INCREMENT;
        }
        return PrecedenceEnum.ASSIGNMENT;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof Assignment other) {
            int c = target.compareTo(other.target());
            if (c != 0) return c;
            return value.compareTo(other.value());
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            value.visit(predicate);
            target.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            value.visit(visitor);
            target.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        if (prefixPrimitiveOperator != null) {
            String operator = assignmentOperatorIsPlus ? "++" : "--";
            if (prefixPrimitiveOperator) {
                return new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix(operator))
                        .add(outputInParenthesis(qualification, precedence(), target));
            }
            return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), target))
                    .add(SymbolEnum.plusPlusSuffix(operator));
        }
        //  != null && primitiveOperator != primitives.assignOperatorInt ? "=" + primitiveOperator.name : "=";
        String operator = assignmentOperator == null ? "=" : assignmentOperator.name();
        return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), target))
                .add(SymbolEnum.assignment(operator))
                .add(outputInParenthesis(qualification, precedence(), value));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(target.variables(descendMode), value.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(target.typesReferenced(), value.typesReferenced());
    }

    @Override
    public Assignment withValue(Expression value) {
        return new AssignmentImpl(comments(), source(), target, value, assignmentOperator, assignmentOperatorIsPlus,
                binaryOperator, prefixPrimitiveOperator);
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        VariableExpression translatedTarget = (VariableExpression) target.translate(translationMap);
        Expression translatedValue = value.translate(translationMap);
        if (translatedValue == this.value && translatedTarget == this.target) return this;

        Assignment a = new AssignmentImpl(comments(), source(), translatedTarget,
                translatedValue, assignmentOperator, assignmentOperatorIsPlus, binaryOperator, prefixPrimitiveOperator);
        if (translationMap.translateAgain()) {
            return a.translate(translationMap);
        }
        return a;
    }

    @Override
    public boolean assignmentOperatorIsPlus() {
        return assignmentOperatorIsPlus;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new AssignmentImpl(comments(), source(), (VariableExpression) target.rewire(infoMap),
                value.rewire(infoMap), assignmentOperator, assignmentOperatorIsPlus, binaryOperator,
                prefixPrimitiveOperator);
    }
}
