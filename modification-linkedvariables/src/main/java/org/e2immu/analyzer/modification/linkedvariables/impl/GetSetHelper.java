package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.Map;

import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.STATIC_VALUES_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.GET_SET_FIELD;

class GetSetHelper {
    private final Runtime runtime;

    public GetSetHelper(Runtime runtime) {
        this.runtime = runtime;
    }

    public void copyStaticValuesForGetSet(MethodInfo methodInfo) {
        Value.FieldValue getSet = methodInfo.analysis().getOrDefault(GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
        if (getSet.field() != null && !methodInfo.analysis().haveAnalyzedValueFor(STATIC_VALUES_METHOD)) {
            assert getSet.field().isSynthetic();
            assert !methodInfo.isConstructor();
            StaticValues sv;
            if (getSet.setter()) {
                // setter
                sv = setter(methodInfo, getSet);
            } else {
                // getter
                sv = getter(methodInfo, getSet);
            }
            methodInfo.analysis().set(STATIC_VALUES_METHOD, sv);
        }
    }

    private StaticValues getter(MethodInfo methodInfo, Value.FieldValue getSet) {
        Expression indexOrNull;
        if (methodInfo.parameters().isEmpty()) {
            // straight
            indexOrNull = null;
        } else {
            // indexing
            ParameterInfo indexParameter = methodInfo.parameters().get(getSet.parameterIndexOfIndex());
            indexOrNull = runtime.newVariableExpressionBuilder()
                    .setVariable(indexParameter)
                    .setSource(indexParameter.source())
                    .build();
        }
        VariableExpression thisVe = runtime.newVariableExpressionBuilder()
                .setSource(methodInfo.source())
                .setVariable(runtime.newThis(methodInfo.typeInfo().asParameterizedType()))
                .build();
        Variable variable = getSet.createVariable(runtime, thisVe, indexOrNull);
        VariableExpression ve = runtime.newVariableExpressionBuilder().setVariable(variable)
                .setSource(methodInfo.source())
                .build();
        return new StaticValuesImpl(null, ve, false, Map.of());
    }

    private StaticValues setter(MethodInfo methodInfo, Value.FieldValue getSet) {
        Variable valueParameter;
        Expression indexOrNull;
        if (getSet.hasIndex()) {
            // indexing
            // DECISION: if the object type is 'int' as well, we follow the convention of List.set(index. element)
            ParameterInfo indexParameter = methodInfo.parameters().get(getSet.parameterIndexOfIndex());
            valueParameter = methodInfo.parameters().get(1 - getSet.parameterIndexOfIndex());

            // see org.e2immu.language.inspection.api.util.GetSetUtil.extractFieldType
            if (getSet.field().type().arrays() > 0) {
                // indexing in array: this.objects[i]=o
                indexOrNull = runtime.newVariableExpressionBuilder()
                        .setVariable(indexParameter)
                        .setSource(indexParameter.source())
                        .build();
            } else {
                throw new UnsupportedOperationException("indexing is in a virtual array");
            }
        } else {
            assert !methodInfo.parameters().isEmpty();
            // normal setter
            valueParameter = methodInfo.parameters().get(0);
            indexOrNull = null;
        }
        VariableExpression thisVe = runtime.newVariableExpressionBuilder()
                .setVariable(runtime.newThis(methodInfo.typeInfo().asParameterizedType()))
                .setSource(methodInfo.source())
                .build();
        Variable target = getSet.createVariable(runtime, thisVe, indexOrNull);
        Expression expression;
        if (methodInfo.isFluent()) {
            expression = runtime.newVariableExpressionBuilder()
                    .setVariable(runtime.newThis(methodInfo.typeInfo().asParameterizedType()))
                    .setSource(methodInfo.source())
                    .build();
        } else {
            expression = null;
        }
        VariableExpression valPar = runtime.newVariableExpressionBuilder()
                .setVariable(valueParameter)
                .setSource(valueParameter.source())
                .build();
        return new StaticValuesImpl(null, expression, false, Map.of(target, valPar));
    }

}
