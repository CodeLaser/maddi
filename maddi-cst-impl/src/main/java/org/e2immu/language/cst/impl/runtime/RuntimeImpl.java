package org.e2immu.language.cst.impl.runtime;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.ComputeMethodOverrides;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.LanguageConfiguration;
import org.e2immu.language.cst.api.runtime.Eval;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.E2ImmuAnnotationsImpl;
import org.e2immu.language.cst.impl.expression.eval.EvalOptions;
import org.e2immu.language.cst.impl.info.ComputeMethodOverridesImpl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class RuntimeImpl extends FactoryImpl implements Runtime {
    private final Eval eval;
    private final E2ImmuAnnotationsImpl e2ImmuAnnotations = new E2ImmuAnnotationsImpl();
    private final LanguageConfiguration lc = new LanguageConfigurationImpl(true);

    public RuntimeImpl() {
        this(EvalOptions.DEFAULT);
    }

    public RuntimeImpl(EvalOptions evalOptions) {
        eval = new EvalImpl(this, evalOptions);
    }

    @Override
    public LanguageConfiguration configuration() {
        return lc;
    }

    @Override
    public Expression removeClausesFromCondition(Expression condition, Expression clauseToRemove) {
        return eval.removeClausesFromCondition(condition, clauseToRemove);
    }

    @Override
    public Expression complementOfClausesInCondition(Expression condition, Expression clauseToExclude) {
        return eval.complementOfClausesInCondition(condition, clauseToExclude);
    }

    @Override
    public Expression complementOfConditions(List<Expression> conditions, Expression baseCondition) {
        return eval.complementOfConditions(conditions, baseCondition);
    }

    @Override
    public boolean conditionIsMoreSpecificThan(Expression lessSpecific, Expression moreSpecific, boolean allowEquals) {
        return eval.conditionIsMoreSpecificThan(lessSpecific, moreSpecific, allowEquals);
    }

    @Override
    public boolean conditionIsNotMoreSpecificThanAnyOf(Expression condition, Collection<Expression> bases) {
        return conditionIsNotMoreSpecificThanAnyOf(condition, bases);
    }

    @Override
    public Expression combineCondition(Expression baseCondition, Expression clause) {
        return eval.combineCondition(baseCondition, clause);
    }

    @Override
    public boolean isNegationOf(Expression e1, Expression e2) {
        return eval.isNegationOf(e1, e2);
    }

    @Override
    public Expression cast(Expression evaluated, Cast cast) {
        return eval.cast(evaluated, cast);
    }

    @Override
    public Expression unaryOperator(Expression evaluated, UnaryOperator unaryOperator) {
        return eval.unaryOperator(evaluated, unaryOperator);
    }

    @Override
    public Expression inlineConditional(Expression condition,
                                        Expression ifTrue,
                                        Expression ifFalse,
                                        Variable myself,
                                        boolean modifying) {
        return eval.inlineConditional(condition, ifTrue, ifFalse, myself, modifying);
    }

    @Override
    public Expression product(Expression lhs, Expression rhs) {
        return eval.product(lhs, rhs);
    }

    @Override
    public Expression less(Expression lhs, Expression rhs, boolean allowEquals) {
        return eval.less(lhs, rhs, allowEquals);
    }

    @Override
    public Expression wrapInProduct(Expression[] translated, int length) {
        return eval.wrapInProduct(translated, length);
    }

    @Override
    public Expression wrapInSum(Expression[] translated, int length) {
        return eval.wrapInSum(translated, length);
    }

    @Override
    public Expression sum(Expression lhs, Expression rhs) {
        return eval.sum(lhs, rhs);
    }

    @Override
    public Expression negate(Expression expression) {
        return eval.negate(expression);
    }

    @Override
    public Expression remainder(Expression lhs, Expression rhs) {
        return eval.remainder(lhs, rhs);
    }

    @Override
    public Expression equals(Expression lhs, Expression rhs) {
        return eval.equals(lhs, rhs);
    }

    @Override
    public Expression equalsMethod(MethodCall methodCall, Expression lhs, Expression rhs) {
        return eval.equalsMethod(methodCall, lhs, rhs);
    }

    @Override
    public Expression greater(Expression lhs, Expression rhs, boolean allowEquals) {
        return eval.greater(lhs, rhs, allowEquals);
    }

    @Override
    public Expression greaterThanZero(Expression expression, boolean allowEquals) {
        return eval.greaterThanZero(expression, allowEquals);
    }

    @Override
    public Expression instanceOf(Expression evaluated, InstanceOf instanceOf) {
        return eval.instanceOf(evaluated, instanceOf);
    }

    @Override
    public Expression or(List<Expression> expressions) {
        return eval.or(expressions);
    }

    @Override
    public Expression or(Expression... expressions) {
        return eval.or(expressions);
    }

    @Override
    public Expression and(Expression... expressions) {
        return eval.and(expressions);
    }

    @Override
    public Expression and(List<Expression> expressions) {
        return eval.and(expressions);
    }

    @Override
    public Expression divide(Expression lhs, Expression rhs) {
        return eval.divide(lhs, rhs);
    }

    @Override
    public Expression binaryOperator(Expression lhs, Expression rhs, BinaryOperator binaryOperator) {
        return eval.binaryOperator(lhs, rhs, binaryOperator);
    }

    @Override
    public boolean isNotNull0(Expression expression) {
        return eval.isNotNull0(expression);
    }

    @Override
    public Stream<Expression> expandTerms(Expression expression, boolean negate) {
        return eval.expandTerms(expression, negate);
    }

    @Override
    public Stream<Expression> expandFactors(Expression expression) {
        return eval.expandFactors(expression);
    }

    @Override
    public TypeInfo getFullyQualified(String name, boolean complain) {
        throw new UnsupportedOperationException("Override me");
    }

    @Override
    public TypeInfo syntheticFunctionalType(int inputParameters, boolean hasReturnValue) {
        throw new UnsupportedOperationException("Override me");
    }

    @Override
    public Stream<AnnotationExpression> e2immuAnnotations() {
        return e2ImmuAnnotations.streamTypes();
    }

    @Override
    public String e2aAbsent() {
        return E2ImmuAnnotationsImpl.ABSENT;
    }

    @Override
    public AnnotationExpression e2immuAnnotation(String fullyQualifiedName) {
        return e2ImmuAnnotations.get(fullyQualifiedName);
    }

    @Override
    public String e2aContract() {
        return E2ImmuAnnotationsImpl.CONTRACT;
    }

    @Override
    public String e2aContent() {
        return E2ImmuAnnotationsImpl.CONTENT;
    }

    @Override
    public String e2aImplied() {
        return E2ImmuAnnotationsImpl.IMPLIED;
    }

    @Override
    public String e2aHiddenContent() {
        return E2ImmuAnnotationsImpl.HIDDEN_CONTENT;
    }

    @Override
    public String e2aValue() {
        return E2ImmuAnnotationsImpl.VALUE;
    }

    @Override
    public String e2aPar() {
        return E2ImmuAnnotationsImpl.PAR;
    }

    @Override
    public String e2aSeq() {
        return E2ImmuAnnotationsImpl.SEQ;
    }

    @Override
    public String e2aMulti() {
        return E2ImmuAnnotationsImpl.MULTI;
    }

    @Override
    public String e2aAfter() {
        return E2ImmuAnnotationsImpl.AFTER;
    }

    @Override
    public String e2aBefore() {
        return E2ImmuAnnotationsImpl.BEFORE;
    }

    @Override
    public String e2aConstruction() {
        return E2ImmuAnnotationsImpl.CONSTRUCTION;
    }

    @Override
    public String e2aInconclusive() {
        return E2ImmuAnnotationsImpl.INCONCLUSIVE;
    }

    @Override
    public String e2aHcParameters() {
        return E2ImmuAnnotationsImpl.HC_PARAMETERS;
    }

    @Override
    public ComputeMethodOverrides computeMethodOverrides() {
        return new ComputeMethodOverridesImpl();
    }

    /* given a getter call, create the corresponding (indexed) variable */
    @Override
    public Variable getterVariable(MethodCall methodCall) {
        return getSetVariable(methodCall, false);
    }

    /* given a setter call, create the target variable
     *  obj.setX(v) -> o.x
     *  obj.setX(i, v) -> o.x[i]
     */

    @Override
    public Variable setterVariable(MethodCall methodCall) {
        return getSetVariable(methodCall, true);
    }

    private Variable getSetVariable(MethodCall methodCall, boolean setter) {
        Value.FieldValue getSetField = methodCall.methodInfo().getSetField();
        if (getSetField.field() == null || getSetField.setter() != setter) {
            return null;
        }
        ParameterizedType concreteType;
        ParameterizedType getSetFieldType = getSetField.field().type();
        if (getSetFieldType.typeParameter() != null && getSetFieldType.typeParameter().getOwner().isLeft() && getSetFieldType.typeParameter().getOwner().getLeft().fullyQualifiedName().equals("java.util.List")) {
            ParameterizedType pt = setter ? methodCall.parameterExpressions().get(methodCall.parameterExpressions().size() - 1).parameterizedType() : methodCall.concreteReturnType();
            concreteType = pt.copyWithArrays(pt.arrays() + 1);
        } else {
            concreteType = getSetFieldType;
        }
        if (methodCall.parameterExpressions().size() == (setter ? 1 : 0)) {
            return newFieldReference(getSetField.field(), methodCall.object(), concreteType);
        }
        FieldReference fr = newFieldReference(getSetField.field(), methodCall.object(), concreteType);
        FieldReference fr2;
        if (getSetFieldType.arrays() == 0) {
            ParameterizedType pt;
            if (getSetFieldType.parameters().isEmpty()) {
                pt = objectParameterizedType();
            } else {
                pt = getSetFieldType.parameters().get(0);
            }
            TypeInfo list = getFullyQualified(List.class, true);
            MethodInfo get = list.findUniqueMethod("get", 1);
            Value.FieldValue fv = get.getSetField();
            VariableExpression scope = newVariableExpressionBuilder()
                    .setVariable(fr)
                    .setSource(methodCall.object().source())
                    .build();
            fr2 = newFieldReference(fv.field(), scope, pt.copyWithArrays(pt.arrays() + 1));
        } else {
            fr2 = fr;
        }
        Expression index = methodCall.parameterExpressions().get(0);
        assert index.parameterizedType().isMathematicallyInteger();
        VariableExpression array = newVariableExpressionBuilder()
                .setVariable(fr2)
                .setSource(methodCall.object().source())
                .build();
        return newDependentVariable(array, index);
    }

}
