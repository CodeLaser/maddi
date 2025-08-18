package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.Map;

import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;

public class AnalysisHelper {

    public Value.Immutable typeImmutableNullIfUndecided(ParameterizedType parameterizedType) {
        return typeImmutable(parameterizedType, Map.of(), true);
    }

    public Value.Immutable typeImmutable(ParameterizedType parameterizedType) {
        return typeImmutable(parameterizedType, Map.of(), false);
    }

    /*
     Why dynamic value? See MethodCall.dynamicImmutable().
     If the dynamic value is IMMUTABLE_HC and the computed value is MUTABLE, we still need to go through the
     immutableDeterminedByTypeParameters() code... therefore a simple remote 'max()' operation does not work.
     */
    public Value.Immutable typeImmutable(ParameterizedType parameterizedType,
                                         Map<ParameterizedType, Value.Immutable> dynamicValues) {
        return typeImmutable(parameterizedType, dynamicValues, false);
    }

    private Value.Immutable typeImmutable(ParameterizedType parameterizedType,
                                          Map<ParameterizedType, Value.Immutable> dynamicValues,
                                          boolean nullIfUndecided) {
        if (parameterizedType.arrays() > 0) {
            return ValueImpl.ImmutableImpl.FINAL_FIELDS;
        }
        if (parameterizedType.isTypeOfNullConstant() || parameterizedType.isVoid()) {
            return ValueImpl.ImmutableImpl.NO_VALUE;
        }
        TypeInfo bestType = parameterizedType.bestTypeInfo();
        if (bestType == null) {
            return ValueImpl.ImmutableImpl.IMMUTABLE_HC;
        }
        if (bestType.isPrimitiveExcludingVoid()) {
            return ValueImpl.ImmutableImpl.IMMUTABLE;
        }

        Value.Immutable dynamicBaseValue;
        Value.Immutable immutableOfCurrent = dynamicValues.get(parameterizedType);
        if (immutableOfCurrent != null) {
            dynamicBaseValue = immutableOfCurrent;
        } else {
            Value.Immutable inMap = bestType.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
            if (inMap == null) {
                if (nullIfUndecided && isPartOfSourceCode(bestType)) return null;
                dynamicBaseValue = ValueImpl.ImmutableImpl.MUTABLE;
            } else {
                dynamicBaseValue = inMap;
            }
        }
        if (dynamicBaseValue.isAtLeastImmutableHC() && !parameterizedType.parameters().isEmpty()) {
            Value.Bool ignoreTypeParameters = bestType.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE_INDEPENDENT_OF_TYPE_PARAMETERS,
                    FALSE);
            if (ignoreTypeParameters.isFalse()) {
                return parameterizedType.parameters().stream()
                        .map(pt -> typeImmutable(pt, dynamicValues))
                        .reduce(ValueImpl.ImmutableImpl.IMMUTABLE, Value.Immutable::min);
            }
        }
        return dynamicBaseValue;
    }

    /*
    If a type is external, the shallow analyzer/annotated API system has provided values.
    Then, we must fall back to the default.
     */
    private boolean isPartOfSourceCode(TypeInfo bestType) {
        SourceSet sourceSet = bestType.compilationUnit().sourceSet();
        return sourceSet != null && !sourceSet.externalLibrary();
    }

    public Value.Immutable typeImmutable(TypeInfo currentType, ParameterizedType type) {
        if (type.typeInfo() == null) {
            return ValueImpl.ImmutableImpl.IMMUTABLE_HC;
        }
        if (currentType.primaryType().equals(type.typeInfo().primaryType())) {
            return ValueImpl.ImmutableImpl.MUTABLE; // self-ref; ALWAYS mutable
        }
        return typeImmutable(type, Map.of());
    }


    public Value.Independent typeIndependent(ParameterizedType parameterizedType) {
        if (parameterizedType.arrays() > 0) {
            // because the "fields" of the array, i.e. the cells, can be mutated
            return ValueImpl.IndependentImpl.DEPENDENT;
        }
        TypeInfo bestType = parameterizedType.bestTypeInfo();
        if (bestType == null) {
            // unbound type parameter, null constant
            return ValueImpl.IndependentImpl.INDEPENDENT_HC;
        }
        if (bestType.isPrimitiveExcludingVoid()) {
            return ValueImpl.IndependentImpl.INDEPENDENT;
        }
        Value.Immutable immutable = bestType.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.MUTABLE);
        if (immutable.isImmutableHC() && !parameterizedType.parameters().isEmpty()) {
            Value.Bool tpBool = bestType.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE_INDEPENDENT_OF_TYPE_PARAMETERS,
                    FALSE);
            if (tpBool.isFalse()) {
                return parameterizedType.parameters().stream()
                        .map(this::typeIndependent)
                        .reduce(ValueImpl.IndependentImpl.INDEPENDENT, Value.Independent::min);
            }
        }
        return bestType.analysis().getOrDefault(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.DEPENDENT);
    }

    public Value.NotNull notNullOfType(ParameterizedType parameterizedType) {
        if (parameterizedType.isPrimitiveExcludingVoid()) {
            return ValueImpl.NotNullImpl.NOT_NULL;
        }
        if (parameterizedType.isVoid()) {
            return ValueImpl.NotNullImpl.NO_VALUE;
        }
        return ValueImpl.NotNullImpl.NULLABLE;
    }

    public Value.Bool typeContainer(ParameterizedType parameterizedType) {
        if (parameterizedType.arrays() > 0) {
            return TRUE;
        }
        if (parameterizedType.isTypeOfNullConstant()) {
            return FALSE;
        }
        if (parameterizedType.isUnboundTypeParameter()) {
            return TRUE;
        }
        TypeInfo bestType = parameterizedType.bestTypeInfo();
        if (bestType == null) {
            return TRUE;
        }
        return bestType.analysis().getOrDefault(PropertyImpl.CONTAINER_TYPE, FALSE);
    }

    public Value.Independent typeIndependentFromImmutableOrNull(TypeInfo currentType, ParameterizedType type) {
        Value.Independent independent = typeIndependentFromImmutableOrNull(type);
        if (independent == null && currentType.equals(type.bestTypeInfo())) {
            return DEPENDENT; // self-reference
        }
        return independent;
    }

    public Value.Independent typeIndependentFromImmutableOrNull(ParameterizedType type) {
        Value.Immutable immutable = typeImmutableNullIfUndecided(type);
        if (immutable == null) return null;
        if (immutable.isMutable()) return DEPENDENT;
        if (immutable.isImmutableHC()) return INDEPENDENT_HC;
        return INDEPENDENT;
    }
}
