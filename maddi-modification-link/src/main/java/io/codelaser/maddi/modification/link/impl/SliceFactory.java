package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.Variable;

public class SliceFactory {

    // null when the base's container type cannot provide the slice's field: a bare type parameter (no typeInfo),
    // or a container of smaller arity than the index (supertype type-parameter repetition, see findField callers)
    public static DependentVariable create(Runtime runtime, FieldReference base, int negativeIndex) {
        TypeInfo container = base.fieldInfo().type().typeInfo();
        if (container == null || container.fields().size() < -negativeIndex) return null;
        FieldInfo fieldInfo = container.fields().get(-1 - negativeIndex);
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
        if (container == null) return null; // e.g. the type is a bare type parameter: no container, no fields
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
        if (container == null) return null; // e.g. the type is a bare type parameter: no container, no fields
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
