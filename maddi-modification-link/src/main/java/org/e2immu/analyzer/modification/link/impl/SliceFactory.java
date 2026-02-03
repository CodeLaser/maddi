package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

public class SliceFactory {

    public static DependentVariable create(Runtime runtime, FieldReference base, int negativeIndex) {
        FieldInfo fieldInfo = base.fieldInfo().type().typeInfo().fields().get(-1 - negativeIndex);
        return create(runtime, base, negativeIndex, fieldInfo);
    }

    public static DependentVariable create(Runtime runtime, Variable base, int negativeIndex, FieldInfo field) {
        assert negativeIndex <= -1;
        VariableExpression arrayExpression = runtime.newVariableExpression(base);
        assert !base.parameterizedType().isStandardFunctionalInterface();
        ParameterizedType sliceType = field.type().copyWithOneMoreArray();
        return runtime.newDependentVariable(arrayExpression, runtime.newInt(negativeIndex), sliceType);
    }

    public record FF(FieldInfo fieldInfo, int index) {
        public int negative() {
            return -1 - index;
        }
    }

    public static FF findField(TypeParameter typeParameter, TypeInfo container) {
        int i = 0;
        for (FieldInfo fieldInfo : container.fields()) {
            if (typeParameter.equals(fieldInfo.type().typeParameter())) {
                return new FF(fieldInfo, i);
            }
            ++i;
        }
        return null;
    }

    public static FF findField(ParameterizedType parameterizedType, TypeInfo container) {
        int i = 0;
        for (FieldInfo fieldInfo : container.fields()) {
            if (parameterizedType.equals(fieldInfo.type())) {
                return new FF(fieldInfo, i);
            }
            ++i;
        }
        return null;
    }

}
