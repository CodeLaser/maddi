package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfoContainer;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.GET_SET_FIELD;

public record PropagateComponents(Runtime runtime,
                                  VariableData variableDataPrevious,
                                  Stage stageOfPrevious) {

    interface PropagateData {
        void accept(Expression mapKey, boolean mapValue, Map<Variable, Expression> svMap);
    }

    void propagateComponents(Property property,
                             MethodCall mc,
                             ParameterInfo pi,
                             PropagateData consumer) {
        Value.VariableBooleanMap modifiedComponents = pi.analysis().getOrNull(property,
                ValueImpl.VariableBooleanMapImpl.class);
        if (modifiedComponents != null) {
            Expression pe = mc.parameterExpressions().get(pi.index());
            if (pe instanceof VariableExpression ve) {
                if (variableDataPrevious != null) {
                    VariableInfoContainer vicOrNull = variableDataPrevious
                            .variableInfoContainerOrNull(ve.variable().fullyQualifiedName());
                    if (vicOrNull != null) {
                        VariableInfo best = vicOrNull.best(stageOfPrevious);
                        List<Variable> staticallyAssigned = best.linkedVariables().primaryAssigned();
                        if (!staticallyAssigned.isEmpty()) {
                            for (Map.Entry<Variable, Boolean> entry : modifiedComponents.map().entrySet()) {
                                propagate(mc, pi, consumer, entry, staticallyAssigned);
                            }
                        }
                    }
                }
            }
        }
    }

    private void propagate(MethodCall mc,
                           ParameterInfo pi,
                           PropagateData consumer,
                           Map.Entry<Variable, Boolean> entry,
                           List<Variable> staticallyAssigned) {
        Map<Variable, Expression> completedMap = completeMap(staticallyAssigned, variableDataPrevious,
                stageOfPrevious);
        Map<Variable, Expression> augmented = augmentWithImplementation(pi.parameterizedType(),
                staticallyAssigned, completedMap);
        Variable afterArgumentExpansion = expandArguments(mc, pi, entry.getKey());
        Expression e = augmented.get(afterArgumentExpansion);
        consumer.accept(e, entry.getValue(), augmented);
    }


    // see TestStaticValuesModification,test4
    private Variable expandArguments(MethodCall mc, ParameterInfo ignore, Variable key) {
        TranslationMap.Builder tmb = runtime.newTranslationMapBuilder();
        for (ParameterInfo pi : mc.methodInfo().parameters()) {
            if (ignore != pi) {
                VariableExpression ve = runtime.newVariableExpressionBuilder().setVariable(pi)
                        .setSource(pi.source())
                        .build();
                tmb.put(ve, mc.parameterExpressions().get(pi.index()));
            }
        }
        TranslationMap tm = tmb.build();
        return tm.translateVariableRecursively(key);
    }

    private Map<Variable, Expression> augmentWithImplementation(ParameterizedType targetType,
                                                                List<Variable> staticallyAssigned,
                                                                Map<Variable, Expression> completedMap) {
        Map<Variable, Expression> completedAndAugmentedWithImplementation;
        // FIXME
      /*  if (svParam.type() != null && !svParam.type().equals(targetType)) {
            assert targetType.isAssignableFrom(runtime, svParam.type());
            completedAndAugmentedWithImplementation = new HashMap<>(completedMap);
            completedMap.forEach((v, e) -> {
                Variable tv = liftVariable(v);
                if (tv != v && !completedAndAugmentedWithImplementation.containsKey(tv)) {
                    completedAndAugmentedWithImplementation.put(tv, e);
                }
            });
        } else {*/
        completedAndAugmentedWithImplementation = completedMap;
        // }
        return completedAndAugmentedWithImplementation;
    }

    /*
    See TestModificationFunctional,3. We want to replace ti.si.ri.i by t.s.r.i,
    where 'ti,si,ri,i' are variables/fields in the implementation types TImpl,SImpl,RImpl, and
    't.s.r.i' are synthetic fields corresponding to accessors in the interface types T, S, R.

    TODO: there could be multiple results. We'll first do the situation with one result.

    TODO also lift dependent variable's index?
     */
    private Variable liftVariable(Variable v) {
        if (v instanceof DependentVariable dv && dv.arrayExpression() instanceof VariableExpression av
            && av.variable() instanceof FieldReference fr && fr.scope() instanceof VariableExpression ve) {
            FieldInfo liftField = liftField(fr.fieldInfo());
            if (liftField != fr.fieldInfo()) {
                // we can go up: there is (synthetic) field higher up.
                Variable liftedScope = liftScope(ve.variable(), liftField.owner().asSimpleParameterizedType());
                if (liftedScope != ve.variable()) {
                    // the scope can join us
                    VariableExpression scope = runtime.newVariableExpressionBuilder()
                            .setVariable(liftedScope)
                            .setSource(ve.source())
                            .build();
                    FieldReference newFr = runtime.newFieldReference(liftField, scope, liftField.type());
                    VariableExpression newFrVe = runtime.newVariableExpressionBuilder()
                            .setVariable(newFr)
                            .setSource(ve.source())
                            .build();
                    return runtime.newDependentVariable(newFrVe, dv.indexExpression());
                }
            }
        }
        if (v instanceof FieldReference fr && fr.scope() instanceof VariableExpression ve) {
            FieldInfo liftField = liftField(fr.fieldInfo());
            if (liftField != fr.fieldInfo()) {
                // we can go up: there is (synthetic) field higher up.
                Variable liftedScope = liftScope(ve.variable(), liftField.owner().asSimpleParameterizedType());
                if (liftedScope != ve.variable()) {
                    // the scope can join us
                    VariableExpression scope = runtime.newVariableExpressionBuilder()
                            .setSource(ve.source())
                            .setVariable(liftedScope)
                            .build();
                    return runtime.newFieldReference(liftField, scope, liftField.type());
                }
            }
        }
        return v;
    }

    /*
    given a field 'i' in RImpl, with accessor RImpl.i(), is there a (synthetic?) field higher up in the hierarchy?
    We go via the accessor.
    If that fails, we try to find an identically named field.
     */
    private FieldInfo liftField(FieldInfo fieldInfo) {
        MethodInfo accessor = fieldInfo.typeInfo().methodStream()
                .filter(mi -> accessorOf(mi) == fieldInfo).findFirst().orElse(null);
        if (accessor != null && !accessor.overrides().isEmpty()) {
            MethodInfo override = accessor.overrides().stream().findFirst().orElseThrow();
            FieldInfo fieldOfOverride = accessorOf(override);
            if (fieldOfOverride != null) {
                return fieldOfOverride;
            }
        }
        String fieldName = fieldInfo.name();
        // see TestStaticValuesAssignment,3 for an example where this returns a field in a supertype (RI.set -> R.set)
        return fieldInfo.owner().recursiveSuperTypeStream()
                .map(ti -> ti.getFieldByName(fieldName, false))
                .filter(Objects::nonNull).findFirst().orElse(fieldInfo);
    }

    private static FieldInfo accessorOf(MethodInfo methodInfo) {
        Value.FieldValue fv = methodInfo.analysis().getOrNull(GET_SET_FIELD, ValueImpl.GetSetValueImpl.class);
        return fv == null ? null : fv.field();
    }

    private Variable liftScope(Variable variable, ParameterizedType requiredType) {
        if (variable instanceof FieldReference) {
            Variable lifted = liftVariable(variable);
            if (lifted.parameterizedType().equals(requiredType)) {
                // success!
                return lifted;
            }
        } else if (requiredType.isAssignableFrom(runtime, variable.parameterizedType())) {
            if (variable instanceof This) {
                return runtime.newThis(requiredType.typeInfo().asParameterizedType());
            }
            throw new RuntimeException(); // ?? what to do?
        }
        return variable; // failure to lift
    }

        /*
        this.r=r in the svParam.values() map.
        the static values of r are: this.function=someMethodReference.
        We want to add: this.r.function=someMethodReference.
         */

    private Map<Variable, Expression> completeMap(List<Variable> staticallyAssigned,
                                                  VariableData variableDataPrevious,
                                                  Stage stageOfPrevious) {
        Map<Variable, Expression> result = new HashMap<>();
        for (Variable v : staticallyAssigned) {
            result.put(v, runtime.newVariableExpression(v));
        }
        // FIXME
    /*    while (true) {
            Map<Variable, Expression> extra = new HashMap<>();
            for (Map.Entry<Variable, Expression> entry : result.entrySet()) {
                if (entry.getValue() instanceof VariableExpression ve && !result.containsKey(ve.variable())) {
                    VariableInfoContainer vic = variableDataPrevious.variableInfoContainerOrNull(ve.variable()
                            .fullyQualifiedName());
                    if (vic != null) {
                        VariableInfo vi = vic.best(stageOfPrevious);
                        if (vi.staticValues() != null) {
                            vi.staticValues().values().entrySet().stream()
                                    .filter(e -> e.getKey() instanceof FieldReference)
                                    .forEach(e -> {
                                        FieldReference fr = (FieldReference) e.getKey();
                                        VariableExpression scope = runtime.newVariableExpressionBuilder()
                                                .setSource(e.getValue().source())
                                                .setVariable(entry.getKey()).build();
                                        Variable newV = runtime.newFieldReference(fr.fieldInfo(),
                                                scope, fr.parameterizedType());
                                        if (!result.containsKey(newV)) {
                                            extra.put(newV, e.getValue());
                                        }
                                    });
                        }
                    }
                }
            }
            if (extra.isEmpty()) break;
            result.putAll(extra);
        }*/
        return result;
    }

}
