/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.linkedvariables.staticvalues;

import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfoContainer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.*;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.support.Either;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.*;

public record StaticValuesHelper(Runtime runtime) {


    /*
    If we have an assignment of E=3 to a.b, we add an assignment of this.b=3 to a.
    If we have an assignment of E=3 to a.b.c, we first add an assignment of this.c=3 to a.b,
    then we add an assignment of this.b.c=3 to a.

    If we have an assignment of E=3 to a[0].b, then we add an assignment of this[0].b to a

    If we have an assignment of E=3 to a.b[0], then we add an assignment of this[0]=3 to a.b,
    and an assignment of this.b[0]=3 to a, and an assignment of this.a.b[0]=3 to this
     */
    public void recursivelyAddAssignmentsAtScopeLevel(Map<Variable, List<StaticValues>> staticValuesMap,
                                                      Source source,
                                                      VariableData variableData,
                                                      String statementIndex) {
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        for (Map.Entry<Variable, List<StaticValues>> entry : staticValuesMap.entrySet()) {
            add(source, variableData, statementIndex, entry.getKey(), entry.getValue(), append);
        }
        append.forEach((v, list) -> mergeSvMap(staticValuesMap, v, list));
    }

    void add(Source source, VariableData variableData, String statementIndex,
             Variable variable, List<StaticValues> staticValues, Map<Variable, List<StaticValues>> append) {
        if (variable instanceof FieldReference fr) {
            recursivelyAdd1(append, fr, source, staticValues, variableData, statementIndex);
        } else if (variable instanceof DependentVariable dv
                   && !staticValues.isEmpty()
                   && staticValues.getFirst().expression() != null) {
            Variable base = dv.arrayVariableBase();
            int arrays = dv.arrayVariableBase().parameterizedType().arrays();
            This thisVar = runtime.newThis(runtime.objectParameterizedType().copyWithArrays(arrays)); // fake!!!
            TranslationMap tm = runtime.newTranslationMapBuilder().put(base, thisVar).build();
            Variable indexed = tm.translateVariableRecursively(variable);
            boolean multiple = staticValues.size() > 1;
            StaticValues newSv = new StaticValuesImpl(null, null, multiple,
                    Map.of(indexed, staticValues.getFirst().expression()));
            append.computeIfAbsent(base, b -> new ArrayList<>()).add(newSv);
            add(source, variableData, statementIndex, base, List.of(newSv), append);
        }
    }

    private static void mergeSvMap(Map<Variable, List<StaticValues>> staticValuesMap, Variable v, List<StaticValues> list) {
        staticValuesMap.merge(v, list,
                (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
    }

    void recursivelyAdd1(Map<Variable, List<StaticValues>> append,
                         FieldReference fr,
                         Source source,
                         List<StaticValues> values,
                         VariableData variableData,
                         String statementIndex) {
        Expression svScope = runtime.newVariableExpressionBuilder()
                .setVariable(runtime.newThis(fr.fieldInfo().typeInfo().asParameterizedType()))
                .setSource(source)
                .build();
        recursivelyAdd(append, fr.scope(), fr.scope(), fr, svScope, fr.fieldInfo(), values, variableData, statementIndex);
    }

    // we have assignments to 'a.b'; we now find that 'a' is a variable, so we add modified assignments to 'a'
    private void recursivelyAdd(Map<Variable, List<StaticValues>> append,
                                Expression newScope,
                                Expression fullScope,
                                FieldReference fr,
                                Expression svScope,
                                FieldInfo svFieldInfo,
                                List<StaticValues> values,
                                VariableData variableData,
                                String statementIndex) {
        if (newScope instanceof VariableExpression ve) {
            List<StaticValues> newList = values.stream().map(sv -> {
                Expression expression;
                DependentVariable dependentVariable;
                if (sv.expression() != null && sv.values().isEmpty()) {
                    expression = sv.expression();
                    dependentVariable = null;
                } else if (!sv.values().isEmpty()) {
                    Map.Entry<Variable, Expression> e = sv.values().entrySet().stream().findFirst().orElseThrow();
                    expression = e.getValue();
                    dependentVariable = e.getKey() instanceof DependentVariable dv ? dv : null;
                } else {
                    return null; // empty sv
                }
                Map<Variable, Expression> newMap;
                if (dependentVariable != null) {
                    FieldReference arrayVariable = runtime.newFieldReference(svFieldInfo, svScope, svFieldInfo.type());
                    VariableExpression array = runtime.newVariableExpressionBuilder().setVariable(arrayVariable)
                            .setSource(svScope.source()).build();
                    DependentVariable dv = runtime.newDependentVariable(array, dependentVariable.indexExpression());
                    newMap = Map.of(dv, expression);
                } else {
                    newMap = Map.of(runtime.newFieldReference(svFieldInfo, svScope, svFieldInfo.type()), expression);
                }
                return (StaticValues) new StaticValuesImpl(null, null, false, newMap);
            }).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
            Variable variable = ve.variable();
            if (!newList.isEmpty()) {
                append.put(variable, newList);
            }
            VariableInfoContainer vic = variableData.variableInfoContainerOrNull(variable.fullyQualifiedName());
            if (vic == null) {
                // grab the "original", we'll copy some info
                VariableInfoContainer vicOrig = variableData.variableInfoContainerOrNull(fr.fullyQualifiedName());
                assert vicOrig != null;
                // we're creating new variables as well, added in "recursivelyAddAssignmentsAtScopeLevel"
                Assignments assignments = new Assignments(statementIndex);
                Reads reads = new Reads(statementIndex);
                VariableInfoImpl initial = new VariableInfoImpl(variable, assignments, reads,
                        vicOrig.getPreviousOrInitial().variableInfoInClosure());
                VariableInfoContainer newVic = new VariableInfoContainerImpl(variable, vicOrig.variableNature(),
                        Either.right(initial), null, vicOrig.hasMerge());
                ((VariableDataImpl) variableData).put(variable, newVic);
            }
            if (variable instanceof FieldReference fr2) {
                Expression newSvScope = suffix(fullScope, fr2.scope());
                recursivelyAdd(append, fr2.scope(), fullScope, fr2, newSvScope, svFieldInfo, newList, variableData,
                        statementIndex);
            }
        }
    }

    /*
    if all = a.b.c.d, and
    prefix = a.b.c, result should be d
    prefix = a.b, result should be c.d
     */
    private Expression suffix(Expression all, Expression prefix) {
        if (all instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            This newThis = runtime.newThis(fr.fieldInfo().typeInfo().asParameterizedType());
            Expression scope = runtime.newVariableExpressionBuilder().setVariable(newThis).setSource(ve.source()).build();
            Expression move = fr.scope();
            while (!move.equals(prefix)) {
                if (move instanceof VariableExpression ve2 && ve2.variable() instanceof FieldReference fr2) {
                    FieldReference intermediaryFr = runtime.newFieldReference(fr2.fieldInfo(), scope, fr.parameterizedType());
                    scope = runtime.newVariableExpressionBuilder().setSource(ve2.source()).setVariable(intermediaryFr).build();
                    move = fr2.scope();
                }
            }
            FieldReference newFr = runtime.newFieldReference(fr.fieldInfo(), scope, fr.parameterizedType());
            return runtime.newVariableExpressionBuilder().setVariable(newFr).setSource(all.source()).build();
        }
        throw new UnsupportedOperationException();
    }


    public Map<Variable, Expression> checkCaseForBuilder(MethodCall mc,
                                                         ParameterizedType thisType,
                                                         Expression substituteForThis,
                                                         Map<Variable, Expression> values,
                                                         Map<Variable, StaticValues> assignments) {

        // case for builder()

        StaticValues sv = mc.methodInfo().analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        if (sv != null && sv.expression() instanceof ConstructorCall cc && cc.constructor() != null) {
            Map<Variable, Expression> existingMap = new HashMap<>(values);
            for (Map.Entry<Variable, StaticValues> e : assignments.entrySet()) {
                if (e.getValue().expression() != null) {
                    existingMap.putIfAbsent(e.getKey(), e.getValue().expression());
                }
            }
            // do a mapping of svObject.values() to the fields to which the parameters of the constructor call link
            return staticValuesInCaseOfABuilder(cc, thisType, substituteForThis, existingMap);
        }

        return null;
    }

    private Map<Variable, Expression> staticValuesInCaseOfABuilder(ConstructorCall cc,
                                                                   ParameterizedType thisType,
                                                                   Expression substituteForThis,
                                                                   Map<Variable, Expression> existingMap) {
        Map<Variable, Expression> svObjectValues = new HashMap<>();
        for (ParameterInfo pi : cc.constructor().parameters()) {
            StaticValues svPi = pi.analysis().getOrDefault(STATIC_VALUES_PARAMETER, NONE);
            Expression arg = cc.parameterExpressions().get(pi.index());

            // builder situation
            if (arg instanceof VariableExpression veArg
                && svPi.expression() instanceof VariableExpression ve
                && ve.variable() instanceof FieldReference fr) {
                // replace the value in svObject.values() to this one...

                // array components, see TestStaticValuesRecord,6
                if (fr.parameterizedType().arrays() > 0
                    && pi.parameterizedType().arrays() == fr.parameterizedType().arrays()) {
                    // can we add components of the array?
                    for (Map.Entry<Variable, Expression> entry : existingMap.entrySet()) {
                        if (entry.getKey() instanceof DependentVariable dv) {
                            Variable thisVar = runtime.newThis(thisType);
                            VariableExpression thisVarVe = runtime.newVariableExpressionBuilder().setVariable(thisVar)
                                    .setSource(substituteForThis.source()).build();
                            TranslationMap tm = runtime.newTranslationMapBuilder().put(substituteForThis, thisVarVe).build();
                            Variable arrayVarTm = ((VariableExpression) dv.arrayExpression().translate(tm)).variable();
                            if (arrayVarTm.equals(veArg.variable())) {
                                DependentVariable newDv = runtime.newDependentVariable(ve, dv.indexExpression());
                                svObjectValues.put(newDv, entry.getValue());
                            }
                        }
                    }
                } else {
                    // whole objects
                    for (Map.Entry<Variable, Expression> entry : existingMap.entrySet()) {
                        // entry.getKey() = new Loop.LoopDataImpl.Builder().body
                        // veArg.variable = (this of Builder).body
                        // substitute for this = new Loop.LoopDataImpl.Builder() -> this
                        // fr = (this of LoopDataImpl).body
                        Variable thisVar = runtime.newThis(substituteForThis.parameterizedType());
                        VariableExpression thisVarVe = runtime.newVariableExpressionBuilder().setVariable(thisVar)
                                .setSource(substituteForThis.source()).build();
                        TranslationMap tm = runtime.newTranslationMapBuilder().put(substituteForThis, thisVarVe).build();
                        VariableExpression keyVe = runtime.newVariableExpression(entry.getKey());
                        Variable keyTm =((VariableExpression)keyVe.translate(tm)).variable();
                        if (veArg.variable().equals(keyTm)) {
                            svObjectValues.put(fr, entry.getValue());
                        }
                    }
                }
            }
        }
        return svObjectValues;
    }
}
