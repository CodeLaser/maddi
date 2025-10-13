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

package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.linkedvariables.lv.*;
import org.e2immu.analyzer.modification.linkedvariables.staticvalues.StaticValuesHelper;
import org.e2immu.analyzer.modification.prepwork.hcs.ComputeHCS;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hcs.IndexImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ObjectCreationVariableImpl;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.EMPTY;
import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER;
import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.*;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_METHOD;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.NO_VALUE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;

class ExpressionAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionAnalyzer.class);

    private final Runtime runtime;
    private final GenericsHelper genericsHelper;
    private final AnalysisHelper analysisHelper;
    private final LinkHelperFunctional linkHelperFunctional;
    private final ApplyGetSetTranslation applyGetSetTranslation;
    private final StaticValuesHelper staticValuesHelper;
    private final InternalMethodModAnalyzer analyzer; // for recursion in switch expression

    public ExpressionAnalyzer(Runtime runtime, InternalMethodModAnalyzer analyzer, StaticValuesHelper staticValuesHelper) {
        this.runtime = runtime;
        this.genericsHelper = new GenericsHelperImpl(runtime);
        this.analysisHelper = new AnalysisHelper();
        this.linkHelperFunctional = new LinkHelperFunctional(runtime, analysisHelper);
        this.applyGetSetTranslation = new ApplyGetSetTranslation(runtime);
        this.staticValuesHelper = staticValuesHelper;
        this.analyzer = analyzer;
    }

    public EvaluationResult linkEvaluation(MethodInfo currentMethod,
                                           VariableData variableDataPrevious,
                                           Stage stageOfPrevious,
                                           Expression expression) {
        Expression translated = expression.translate(applyGetSetTranslation);
        EvaluationResult er = new Internal(currentMethod, variableDataPrevious, stageOfPrevious).eval(translated);
        if (expression instanceof MethodCall mc && translated instanceof Assignment a) {
            // setter, we want to set the method call's LINKED_VARIABLES_ARGUMENTS
            List<LinkedVariables> list;
            if (mc.parameterExpressions().size() == 1) {
                list = List.of(er.linkedVariables());
            } else if (mc.parameterExpressions().size() == 2) {
                list = List.of(LinkedVariablesImpl.EMPTY, er.linkedVariables());
            } else throw new UnsupportedOperationException();
            mc.analysis().setAllowControlledOverwrite(LinkedVariablesImpl.LINKED_VARIABLES_ARGUMENTS,
                    new LinkedVariablesImpl.ListOfLinkedVariablesImpl(list));
        }
        return er;
    }

    private class Internal {
        final MethodInfo currentMethod;
        final VariableData variableDataPrevious;
        final Stage stageOfPrevious;

        Internal(MethodInfo currentMethod, VariableData variableDataPrevious, Stage stageOfPrevious) {
            this.currentMethod = currentMethod;
            this.variableDataPrevious = variableDataPrevious;
            this.stageOfPrevious = stageOfPrevious;
        }

        EvaluationResult eval(Expression expression) {
            return eval(expression, null);
        }

        EvaluationResult eval(Expression expression, ParameterizedType forwardType) {
            if (expression instanceof VariableExpression ve) {
                Variable v = ve.variable();

                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                EvaluationResult scope;
                EvaluationResult index;
                if (v instanceof FieldReference fr) {
                    scope = eval(fr.scope());
                    index = EvaluationResult.EMPTY;
                } else if (v instanceof DependentVariable dv) {
                    scope = eval(dv.arrayExpression());
                    index = eval(dv.indexExpression(), runtime.intParameterizedType());
                } else {
                    scope = EvaluationResult.EMPTY;
                    index = EvaluationResult.EMPTY;
                }
                // important: only the links explicitly copied to variables are retained by this statement,
                // because we'll be using '.setLinkedVariables(lvs)' a little later. The result must (and will) be copied
                // in linkedVariablesOfVariableExpression
                builder.merge(scope);
                builder.merge(index);

                LinkedVariables lvs = linkedVariablesOfVariableExpression(ve, v, scope, index, forwardType, builder);
                // do we have a static value for 'v'? this static value can be in the current evaluation forward (NYI)
                // or in the previous statement
                Expression svExpression = inferStaticValues(ve);
                StaticValues svs = StaticValuesImpl.of(svExpression);
                StaticValues svsVar = StaticValuesImpl.from(variableDataPrevious, stageOfPrevious, v);
                return builder
                        .setStaticValues(svs.merge(svsVar))
                        .merge(v, lvs) // make sure the link isn't lost: write it out separately
                        .setLinkedVariables(lvs)
                        .build();
            }
            if (expression instanceof Assignment assignment) {
                EvaluationResult evalValue = eval(assignment.value());
                EvaluationResult evalTarget = eval(assignment.target());

                EvaluationResult.Builder builder = new EvaluationResult.Builder()
                        .merge(evalValue)
                        .merge(evalTarget)
                        .merge(assignment.variableTarget(), evalValue.linkedVariables())
                        .setLinkedVariables(evalValue.linkedVariables())
                        .merge(assignment.variableTarget(), evalValue.staticValues())
                        .setStaticValues(evalValue.staticValues());
                LinkedVariables withoutVt = evalTarget.linkedVariables()
                        .remove(v -> v.equals(assignment.variableTarget()));
                if (!withoutVt.isEmpty()) {
                    builder.merge(assignment.variableTarget(), withoutVt);
                }
                //   setStaticValuesForVariableHierarchy(assignment, evalValue, builder);
                if (assignment.variableTarget() instanceof FieldReference fr && fr.scopeVariable() != null) {
                    markModified(fr.scopeVariable(), builder);
                } else if (assignment.variableTarget() instanceof DependentVariable dv && dv.arrayVariable() != null) {
                    markModified(dv.arrayVariable(), builder);
                }
                return builder.build();
            }
            if (expression instanceof MethodCall mc) {
                return linkEvaluationOfMethodCall(currentMethod, mc, forwardType);
            }
            if (expression instanceof ConstructorCall cc) {
                // important: check anonymous type first, it can have constructor != null
                EvaluationResult result;
                if (cc.anonymousClass() != null) {
                    result = linkEvaluationOfAnonymousClass(currentMethod, cc);
                } else if (cc.constructor() != null) {
                    result = linkEvaluationOfConstructorCall(currentMethod, cc);
                } else {
                    result = EvaluationResult.EMPTY;
                }
                if (analyzer.trackObjectCreations()) {
                    // this variable exists in the current variable data, which we don't have access to at the moment
                    // so we create it again. (this is not a problem for 'normal variables', as they already live in the CST)
                    ObjectCreationVariable ocv = new ObjectCreationVariableImpl(currentMethod, cc.source().compact(),
                            cc.parameterizedType());
                    LinkedVariables linkedVariables = LinkedVariablesImpl.of(ocv, LVImpl.LINK_ASSIGNED);
                    return new EvaluationResult.Builder()
                            .merge(result)
                            .setLinkedVariables(result.linkedVariables().merge(linkedVariables))
                            .build();
                }
                return result;
            }
            if (expression instanceof MethodReference mr) {
                return linkEvaluationOfMethodReference(currentMethod, mr);
            }
            if (expression instanceof Lambda lambda) {
                return linkEvaluationOfLambda(lambda);
            }

            // direct assignment, if there is a pattern variable. empty otherwise
            if (expression instanceof InstanceOf io) {
                if (io.patternVariable() != null) {
                    EvaluationResult evalValue = eval(io.expression());
                    EvaluationResult.Builder builder = new EvaluationResult.Builder();
                    if (io.expression() instanceof VariableExpression ve) {
                        builder.addCast(ve.variable(), io.testType());
                    }
                    return builder
                            .merge(evalValue)
                            .merge(io.patternVariable().localVariable(), evalValue.linkedVariables())
                            .setLinkedVariables(evalValue.linkedVariables())
                            .build();
                }
                return EvaluationResult.EMPTY;
            }

            // pass-through, except when narrowing the cast on primitives >> result can be completely unrelated
            if (expression instanceof Cast c) {
                if (narrowingCast(c.expression().parameterizedType(), c.parameterizedType())) {
                    return EvaluationResult.EMPTY;
                }
                EvaluationResult er = eval(c.expression(), c.parameterizedType());
                if (c.expression() instanceof VariableExpression ve) {
                    return new EvaluationResult.Builder().addCast(ve.variable(), c.parameterizedType()).merge(er).build();
                }
                return er;
            }
            if (expression instanceof EnclosedExpression c) {
                return eval(c.expression());
            }

            // trivial aggregation
            if (expression instanceof ArrayInitializer ai) {
                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                List<EvaluationResult> evaluationResults = ai.expressions().stream().map(this::eval).toList();
                evaluationResults.forEach(builder::merge);

                LinkedVariables reduced = evaluationResults.stream().map(EvaluationResult::linkedVariables)
                        .reduce(EMPTY, LinkedVariables::merge);
                return builder.setLinkedVariables(reduced).setStaticValues(NOT_COMPUTED).build();
            }
            if (expression instanceof InlineConditional ic) {
                EvaluationResult leCondition = eval(ic.condition());
                EvaluationResult leIfTrue = eval(ic.ifTrue());
                EvaluationResult leIfFalse = eval(ic.ifFalse());
                EvaluationResult.Builder b = new EvaluationResult.Builder().merge(leCondition).merge(leIfTrue).merge(leIfFalse);
                LinkedVariables merge = leIfTrue.linkedVariables().merge(leIfFalse.linkedVariables());
                return b.setLinkedVariables(merge).setStaticValues(NOT_COMPUTED).build();
            }
            if (expression instanceof ConstantExpression<?> ce) {
                assert ce.source() != null;
                StaticValues sv = StaticValuesImpl.of(ce);
                return new EvaluationResult.Builder().setLinkedVariables(EMPTY).setStaticValues(sv).build();
            }
            if (expression instanceof BinaryOperator bo) {
                EvaluationResult lhs = eval(bo.lhs());
                EvaluationResult rhs = eval(bo.rhs());
                return new EvaluationResult.Builder().merge(lhs).merge(rhs)
                        .setStaticValues(NOT_COMPUTED).setLinkedVariables(EMPTY).build();
            }
            if (expression instanceof UnaryOperator uo) {
                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                builder.merge(eval(uo.expression())).setStaticValues(NOT_COMPUTED).setLinkedVariables(EMPTY);
                return builder.build();
            }
            if (expression instanceof ArrayLength al) {
                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                builder.merge(eval(al.scope())).setStaticValues(NOT_COMPUTED).setLinkedVariables(EMPTY);
                return builder.build();
            }
            if (expression instanceof And and) {
                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                and.expressions().forEach(e -> builder.merge(eval(e)));
                return builder.setStaticValues(NOT_COMPUTED).setLinkedVariables(EMPTY).build();
            }
            if (expression instanceof Or or) {
                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                or.expressions().forEach(e -> builder.merge(eval(e)));
                return builder.setStaticValues(NOT_COMPUTED).setLinkedVariables(EMPTY).build();
            }
            if (expression instanceof SwitchExpression switchExpression) {
                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                builder.merge(eval(switchExpression.selector()));
                for (SwitchEntry switchEntry : switchExpression.entries()) {
                    if (switchEntry.statement() instanceof Block block) {
                        analyzer.doBlock(currentMethod, block, variableDataPrevious);
                    } else {
                        analyzer.doStatement(currentMethod, switchEntry.statement(), variableDataPrevious, true);
                    }
                }
                // TODO what else?
                return builder.setStaticValues(NOT_COMPUTED).setLinkedVariables(EMPTY).build();
            }
            if (expression instanceof CommaExpression ce) {
                EvaluationResult.Builder builder = new EvaluationResult.Builder();
                for (Expression e : ce.expressions()) {
                    builder.merge(eval(e));
                }
                return builder.build();
            }
            if (expression == null
                || expression instanceof EmptyExpression
                || expression instanceof TypeExpression) {
                return EvaluationResult.EMPTY;
            }
            throw new UnsupportedOperationException("expression " + expression.getClass());
        }

        private boolean narrowingCast(ParameterizedType from, ParameterizedType to) {
            return from.isPrimitiveExcludingVoid()
                   && to.isPrimitiveExcludingVoid()
                   && !from.equals(to)
                   && runtime.widestType(from, to).equals(from);
        }

        private LinkedVariables linkedVariablesOfVariableExpression(VariableExpression ve,
                                                                    Variable variable,
                                                                    EvaluationResult scope,
                                                                    EvaluationResult index,
                                                                    ParameterizedType forwardType,
                                                                    EvaluationResult.Builder builder) {
            Map<Variable, LV> map = new HashMap<>();
            map.put(variable, LVImpl.LINK_ASSIGNED);
            Variable dependentVariable;
            ParameterizedType fieldType;
            int fieldIndex;
            TypeInfo linkedScopeWithoutVariable;
            if (variable instanceof FieldReference fr) {
                if (fr.scope() instanceof VariableExpression sv && !(sv.variable() instanceof This)) {
                    dependentVariable = fr.scopeVariable();
                    fieldIndex = fr.fieldInfo().indexInType();
                    fieldType = fr.fieldInfo().type();
                    linkedScopeWithoutVariable = null;
                } else if (!(fr.scope() instanceof VariableExpression) && !scope.linkedVariables().isEmpty()) {
                    // see TestLinkToReturnValueListGet,2
                    linkedScopeWithoutVariable = fr.scope().parameterizedType().bestTypeInfo();
                    dependentVariable = null;
                    fieldIndex = fr.fieldInfo().indexInType();
                    fieldType = fr.fieldInfo().type();
                } else {
                    linkedScopeWithoutVariable = null;
                    dependentVariable = null;
                    fieldIndex = -1; // irrelevant
                    fieldType = null;
                }
            } else if (variable instanceof DependentVariable dv) {
                dependentVariable = dv.arrayVariable();
                linkedScopeWithoutVariable = null;
                if (dv.indexVariable() != null) {
                    builder.merge(dv.indexVariable(), index.linkedVariables());
                }
                fieldIndex = dv.indexExpression() instanceof IntConstant ic ? ic.constant()
                        : IndexImpl.UNSPECIFIED_MODIFICATION;
                fieldType = dv.arrayExpression().parameterizedType().copyWithOneFewerArrays();
            } else {
                linkedScopeWithoutVariable = null;
                dependentVariable = null;
                fieldIndex = -1; // irrelevant
                fieldType = null;
            }
            if (dependentVariable != null) {
                // at this point, we only link to the dependent variable, and do not do the recursion down to
                // its transitive closure. That is work for the graph algorithm in ComputeLinkCompletion
                LV lvToScope = scope.linkedVariables().value(dependentVariable);
                TypeInfo dependentVariableBestType = dependentVariable.parameterizedType().bestTypeInfo();
                LV lv = linkToDependentVariable(ve, variable, forwardType, dependentVariableBestType,
                        lvToScope, fieldIndex, fieldType);
                if (lv != null) map.put(dependentVariable, lv);
            } else if (linkedScopeWithoutVariable != null) {
                for (Map.Entry<Variable, LV> entry : scope.linkedVariables()) {
                    LV lv = linkToDependentVariable(ve, variable, forwardType, linkedScopeWithoutVariable,
                            entry.getValue(), fieldIndex, fieldType);
                    if (lv != null) {
                        LOGGER.debug("lv is {}", lv);
                        map.put(entry.getKey(), lv);
                    }
                }
            }
            // adding existing linked variables is in general not necessary.
            // we do add them in case of functional interfaces, see TestLinkFunctional,5(m3)
            // because method LinkHelperFunctional expects them
            if (variable.parameterizedType().isFunctionalInterface() &&
                variableDataPrevious != null && variableDataPrevious.isKnown(variable.fullyQualifiedName())) {
                VariableInfo viPrev = variableDataPrevious.variableInfo(variable, stageOfPrevious);
                viPrev.linkedVariables().stream()
                        .filter(e -> !(e.getKey() instanceof This))
                        .forEach(e -> map.put(e.getKey(), e.getValue()));
            }
            return LinkedVariablesImpl.of(map);
        }

        private LV linkToDependentVariable(VariableExpression ve,
                                           Variable v,
                                           ParameterizedType forwardType,
                                           TypeInfo dependentVariableBestType,
                                           LV currentLink,
                                           int fieldIndex,
                                           ParameterizedType fieldType) {
            Immutable immutable = analysisHelper.typeImmutable(ve.parameterizedType());
            Immutable immutableForward = forwardType == null ? immutable : analysisHelper.typeImmutable(forwardType);
            if (immutableForward.isImmutable()) {
                return null; // NO LINK!
            }
            boolean isMutable = immutableForward.isMutable();
            Indices targetIndices;
            Indices targetModificationArea;
            Indices sourceModificationArea;
            if (isMutable) {
                targetModificationArea = new IndicesImpl(fieldIndex);
                sourceModificationArea = IndicesImpl.ALL_INDICES;
            } else {
                targetModificationArea = IndicesImpl.NO_MODIFICATION_INDICES;
                sourceModificationArea = IndicesImpl.NO_MODIFICATION_INDICES;
            }
            if (v instanceof DependentVariable) {
                targetIndices = new IndicesImpl(0);
            } else if (v instanceof FieldReference fr) {
                assert dependentVariableBestType != null : "The unbound type parameter does not have any fields";
                HiddenContentTypes hct = dependentVariableBestType.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
                ParameterizedType fieldType2 = replaceFieldType(fieldType, dependentVariableBestType, fr.scopeVariable());
                Integer i = hct.indexOf(fieldType2);
                if (i != null) {
                    targetIndices = new IndicesImpl(i);
                } else {
                    targetIndices = null;
                }
            } else throw new UnsupportedOperationException();
            Map<Indices, Link> linkMap;
            if (targetIndices == null) {
                linkMap = Map.of();
                assert isMutable;
            } else {
                linkMap = Map.of(IndicesImpl.ALL_INDICES, new LinkImpl(targetIndices, isMutable));
            }

            // recursion
            Indices targetModificationAreaRecursion;
            if (currentLink.isDependent() && isMutable && targetModificationArea.haveValue()) {
                targetModificationAreaRecursion = targetModificationArea.prepend(currentLink.links().modificationAreaTarget());
            } else {
                targetModificationAreaRecursion = targetModificationArea;
            }

            // create the link, and add it
            Links links = new LinksImpl(linkMap, sourceModificationArea, targetModificationAreaRecursion);
            LV lv;
            if (isMutable && !currentLink.isCommonHC()) {
                lv = LVImpl.createDependent(links);
            } else {
                lv = LVImpl.createHC(links);
            }
            return lv;
        }

        /*
        situation: fieldType is T, TP#0 in M.
        we need the HCT index of T in R, which holds an M object: record R<T>(M<T> a) {}
        we must translate T as TP#0 in M to the T as TP#0 in R. There obviously is a relation.

        FIXME needs generalizing, works in TestLinkModificationArea,test1c but probably not wider
         */
        private ParameterizedType replaceFieldType(ParameterizedType fieldType, TypeInfo targetType, Variable depVar) {
            TypeParameter typeParameter = fieldType.typeParameter();
            if (typeParameter != null) {
                if (!typeParameter.isMethodTypeParameter() && notOwnedBy(typeParameter, targetType)) {
                    TypeInfo owner = typeParameter.getOwner().getLeft();
                    assert targetType.typeHierarchyExcludingJLOStream().anyMatch(ti -> owner == ti);
                    // we must map E #0List -> E #0 ArrayList
                    Map<NamedType, ParameterizedType> map = new GenericsHelperImpl(runtime).mapInTermsOfParametersOfSuperType(targetType, owner.asParameterizedType());
                    if (map != null) {
                        ParameterizedType pt = map.get(typeParameter);
                        if (pt != null) {
                            return pt;
                        }
                    }
                }
                if (depVar instanceof FieldReference fr && notOwnedBy(typeParameter, targetType)) {
                    // see TestLinkToReturnValueListGet,2. List E from getter -> ArrayList E, concrete type
                    FieldInfo fieldInTargetType = fr.fieldInfo();
                    ParameterizedType ptFTT = fieldInTargetType.type();
                    if (!ptFTT.parameters().isEmpty()) {
                        return ptFTT.parameters().getFirst();
                    }
                }
            }
            return fieldType;
        }

        private boolean notOwnedBy(TypeParameter typeParameter, TypeInfo targetType) {
            if (typeParameter.getOwner().isLeft()) return !targetType.equals(typeParameter.getOwner().getLeft());
            throw new UnsupportedOperationException("NYI");
        }

        private Expression inferStaticValues(VariableExpression ve) {
            if (variableDataPrevious != null) {
                Variable variable = ve.variable();
                VariableInfoContainer vicV = variableDataPrevious.variableInfoContainerOrNull(variable.fullyQualifiedName());
                if (vicV != null) {
                    VariableInfo viV = vicV.best(stageOfPrevious);
                    StaticValues svV = viV.staticValues();
                    if (svV != null && svV.expression() != null) {
                        return svV.expression();
                    }
                }
                if (variable instanceof DependentVariable dv
                    && dv.arrayExpression() instanceof VariableExpression av
                    && av.variable() instanceof FieldReference fr && fr.scopeVariable() != null) {
                    // r.variable[0]
                    VariableInfoContainer vicSv = variableDataPrevious.variableInfoContainerOrNull(fr.scopeVariable()
                            .fullyQualifiedName());
                    if (vicSv != null) {
                        VariableInfo viV = vicSv.best(stageOfPrevious);
                        StaticValues svV = viV.staticValues();
                        if (svV != null) {
                            Map<Variable, Expression> valueMap = new HashMap<>(svV.values());
                            ParameterizedType targetType = fr.scope().parameterizedType();
                            Map<Variable, Expression> completed = augmentWithImplementation(targetType, svV, valueMap);
                            FieldReference newFr = runtime.newFieldReference(fr.fieldInfo());
                            TypeInfo frBestType = newFr.parameterizedType().bestTypeInfo();
                            boolean isList = frBestType != null
                                             && newFr.parameterizedType().arrays() == 0
                                             && "java.util.List".equals(frBestType.fullyQualifiedName());
                            ParameterizedType newPt = isList ? newFr.parameterizedType().copyWithArrays(1)
                                    : newFr.parameterizedType();
                            DependentVariable newDv = runtime.newDependentVariable(runtime.newVariableExpression(newFr),
                                    dv.indexExpression(), newPt);
                            Expression valueForField = completed.get(newDv);
                            if (valueForField != null) {
                                return valueForField;
                            }
                        }
                    }
                } else if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
                    // r.variable
                    VariableInfoContainer vicSv = variableDataPrevious.variableInfoContainerOrNull(fr.scopeVariable()
                            .fullyQualifiedName());
                    if (vicSv != null) {
                        VariableInfo viV = vicSv.best(stageOfPrevious);
                        StaticValues svV = viV.staticValues();
                        if (svV != null) {
                            Map<Variable, Expression> valueMap = new HashMap<>(svV.values());
                            ParameterizedType targetType = fr.scope().parameterizedType();
                            Map<Variable, Expression> completed = augmentWithImplementation(targetType, svV, valueMap);
                            Expression valueForField = completed.get(runtime.newFieldReference(fr.fieldInfo()));
                            if (valueForField != null) {
                                return valueForField;
                            }
                        }
                    }
                }
            }
            return ve;
        }

        private EvaluationResult linkEvaluationOfAnonymousClass(MethodInfo currentMethod, ConstructorCall cc) {
            TypeInfo anonymousTypeInfo = cc.anonymousClass();

            MethodInfo sami = anonymousTypeImplementsFunctionalInterface(anonymousTypeInfo);
            if (sami == null) return EvaluationResult.EMPTY;

            ParameterizedType cft = anonymousTypeInfo.interfacesImplemented().getFirst();
            Value.Independent indepOfMethod = sami.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT);
            HiddenContentSelector hcsMethod = sami.analysis().getOrNull(HCS_METHOD, HiddenContentSelector.class);
            EvaluationResult evaluationResultObject = eval(cc.object());
            LinkedVariables lvsObject = evaluationResultObject.linkedVariables();
            ParameterizedType concreteReturnType = sami.returnType();
            List<Value.Independent> independentOfParameters = sami.parameters().stream()
                    .map(pi -> pi.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT))
                    .toList();
            List<HiddenContentSelector> hcsParameters = sami.parameters().stream()
                    .map(pi -> pi.analysis().getOrNull(HCS_PARAMETER, HiddenContentSelector.class))
                    .toList();
            List<LinkedVariables> lvsParams = sami.parameters().stream()
                    .map(pi -> pi.analysis().getOrDefault(LINKED_VARIABLES_PARAMETER, EMPTY))
                    .toList();
            List<ParameterizedType> parameterTypes = sami.parameters().stream()
                    .map(ParameterInfo::parameterizedType)
                    .toList();
            LinkedVariables lvs = linkHelperFunctional.functional(currentMethod.primaryType(), indepOfMethod, hcsMethod,
                    lvsObject, concreteReturnType, independentOfParameters, hcsParameters, parameterTypes, lvsParams, cft);
            return new EvaluationResult.Builder().setLinkedVariables(lvs).build();
        }

        private MethodInfo anonymousTypeImplementsFunctionalInterface(TypeInfo typeInfo) {
            if (!typeInfo.parentClass().isJavaLangObject()) return null;
            if (!typeInfo.interfacesImplemented().isEmpty()) {
                if (typeInfo.interfacesImplemented().size() > 1) return null;
                if (!typeInfo.interfacesImplemented().getFirst().isFunctionalInterface()) return null;
            }
            List<MethodInfo> methods = typeInfo.methods();
            if (methods.size() != 1) return null;
            return methods.getFirst();
        }

        private EvaluationResult linkEvaluationOfLambda(Lambda lambda) {
            LinkHelperFunctional.LambdaResult lr = LinkHelperFunctional.lambdaLinking(runtime, lambda.methodInfo());
            LinkedVariables lvsBeforeRemove;
            if (lambda.methodInfo().isModifying()) {
                lvsBeforeRemove = lr.mergedLinkedToParameters();
            } else {
                lvsBeforeRemove = lr.linkedToReturnValue();
            }
            LinkedVariables lvs = lvsBeforeRemove == null ? EMPTY
                    : lvsBeforeRemove.remove(v -> removeFromLinkedVariables(lambda.methodInfo(), v));
            return new EvaluationResult.Builder().setLinkedVariables(lvs).build();
        }

        private boolean removeFromLinkedVariables(MethodInfo lambdaMethod, Variable v) {
            return v instanceof ParameterInfo pi && lambdaMethod.equals(pi.methodInfo())
                   || v instanceof FieldReference fr && someScopeIsParameterOf(fr, lambdaMethod);
        }

        private static boolean someScopeIsParameterOf(FieldReference fr, MethodInfo methodInfo) {
            Variable sv = fr.scopeVariable();
            if (sv instanceof ParameterInfo pi && methodInfo.equals(pi.methodInfo())) return true;
            if (sv instanceof FieldReference fr2) return someScopeIsParameterOf(fr2, methodInfo);
            return false;
        }

        private EvaluationResult linkEvaluationOfMethodReference(MethodInfo currentMethod, MethodReference mr) {
            EvaluationResult scopeResult = eval(mr.scope());
            Value.Independent independentOfMethod = mr.methodInfo().analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT);
            HiddenContentSelector hcsMethod = ComputeHCS.safeHcsMethod(runtime, mr.methodInfo());

            ParameterizedType typeOfReturnValue = mr.concreteReturnType();
            List<ParameterizedType> concreteParameterTypes = mr.concreteParameterTypes();
            List<Value.Independent> independentOfParameters = mr.methodInfo().parameters().stream()
                    .map(pi -> pi.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT))
                    .toList();
            List<HiddenContentSelector> hcsParameters = mr.methodInfo().parameters().stream()
                    .map(pi -> pi.analysis().getOrNull(HCS_PARAMETER, HiddenContentSelector.class))
                    .toList();

            LinkedVariables linkedVariablesOfObject = scopeResult.linkedVariables();
            LinkedVariables lvs = linkHelperFunctional.functional(currentMethod.primaryType(),
                    independentOfMethod, hcsMethod, linkedVariablesOfObject,
                    typeOfReturnValue, independentOfParameters, hcsParameters, concreteParameterTypes,
                    List.of(linkedVariablesOfObject), mr.parameterizedType());
            return new EvaluationResult.Builder().merge(scopeResult)
                    .setLinkedVariables(lvs)
                    .setStaticValues(StaticValuesImpl.of(mr))
                    .build();
        }

        private EvaluationResult linkEvaluationOfConstructorCall(MethodInfo currentMethod, ConstructorCall cc) {
            EvaluationResult.Builder builder = new EvaluationResult.Builder();

            List<EvaluationResult> evaluationResults = cc.parameterExpressions().stream().map(this::eval).toList();
            evaluationResults.forEach(builder::merge);
            if (analyzer.trackObjectCreations() && !evaluationResults.isEmpty()) {
                List<LinkedVariables> links = evaluationResults.stream().map(EvaluationResult::linkedVariables).toList();
                LinkedVariables.ListOfLinkedVariables list = new LinkedVariablesImpl.ListOfLinkedVariablesImpl(links);
                cc.analysis().setAllowControlledOverwrite(LinkedVariablesImpl.LINKED_VARIABLES_ARGUMENTS, list);
            }

            LinkHelper linkHelper = new LinkHelper(runtime, genericsHelper, analysisHelper, currentMethod,
                    cc.constructor());
            LinkHelper.FromParameters from = linkHelper.linksInvolvingParameters(cc.parameterizedType(),
                    null, cc.parameterExpressions(), evaluationResults);
            LOGGER.debug("links involving parameters: {}", from);

            Map<Variable, Expression> map = new HashMap<>();
            for (ParameterInfo pi : cc.constructor().parameters()) {
                StaticValues svPi = pi.analysis().getOrDefault(STATIC_VALUES_PARAMETER, NONE);
                if (svPi.expression() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                    map.put(fr, cc.parameterExpressions().get(pi.index()));
                }
            }
            StaticValues staticValues = new StaticValuesImpl(cc.parameterizedType(), cc, false, Map.copyOf(map));
            return builder
                    .setStaticValues(staticValues)
                    .setLinkedVariables(from.intoObject().linkedVariables())
                    .build();
        }

        private EvaluationResult linkEvaluationOfMethodCall(MethodInfo currentMethod,
                                                            MethodCall mc,
                                                            ParameterizedType forwardType) {
            // FIXME maybe we should add more? do this in a more structured way? This seems to be the best place, however
            MethodInfo methodInfo = mc.methodInfo();
            if (methodInfo.isSynthetic() && !methodInfo.analysis().haveAnalyzedValueFor(NON_MODIFYING_METHOD)) {
                if ("values".equals(methodInfo.name()) && methodInfo.typeInfo().typeNature() == runtime.typeNatureEnum()) {
                    methodInfo.analysis().set(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.TRUE);
                }
            }
            EvaluationResult.Builder builder = new EvaluationResult.Builder();

            EvaluationResult leObject = eval(mc.object());
            builder.merge(leObject);

            List<EvaluationResult> leParams = mc.parameterExpressions().stream().map(this::eval).toList();
            if (analyzer.trackObjectCreations() && !leParams.isEmpty()) {
                List<LinkedVariables> links = leParams.stream().map(EvaluationResult::linkedVariables).toList();
                LinkedVariables.ListOfLinkedVariables list = new LinkedVariablesImpl.ListOfLinkedVariablesImpl(links);
                mc.analysis().setAllowControlledOverwrite(LinkedVariablesImpl.LINKED_VARIABLES_ARGUMENTS, list);
            }
            leParams.forEach(builder::merge);

            methodCallLinks(currentMethod, mc, builder, leObject, leParams, forwardType);
            methodCallModified(mc, builder);
            methodCallStaticValue(mc, builder, leObject, leParams);

            return builder.build();
        }

        private void methodCallStaticValue(MethodCall mc, EvaluationResult.Builder builder, EvaluationResult leObject,
                                           List<EvaluationResult> leParams) {
            // identity method
            if (mc.methodInfo().isIdentity()) {
                builder.setStaticValues(leParams.getFirst().staticValues());
                return;
            }

            assert mc.methodInfo().analysis().getOrDefault(GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY).field() == null
                    : "Should have been filtered out!";

            StaticValues svm = mc.methodInfo().analysis().getOrDefault(STATIC_VALUES_METHOD, NONE);
            if (mc.methodInfo().hasReturnValue()) {

                // fluent method that sets values, but not technically a setter
                if (mc.methodInfo().isFluent() && svm.expression() instanceof VariableExpression ve
                    && ve.variable() instanceof This) {
                    StaticValues sv = makeSvFromMethodCall(mc, leObject, svm);
                    builder.setStaticValues(sv);
                    // we also add to the object, in case we're not picking up on the result (method2)
                    if (mc.object() instanceof VariableExpression veObject && !(veObject.variable() instanceof This)) {
                        builder.merge(veObject.variable(), sv);
                    }
                    return;
                }

                // builders
                Map<Variable, Expression> svObjectValues = staticValuesHelper.checkCaseForBuilder(mc,
                        mc.concreteReturnType(),
                        leObject.staticValues().expression(),
                        leObject.staticValues().values(),
                        leObject.assignments());
                if (svObjectValues != null) {
                    StaticValues sv = new StaticValuesImpl(svm.type(), leObject.staticValues().expression(),
                            false, svObjectValues);
                    builder.setStaticValues(sv);
                    return;
                }

                // factory methods
                if (mc.methodInfo().isFactoryMethod() && mc.methodInfo().analysis()
                        .getOrDefault(IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.MUTABLE).isAtLeastImmutableHC()) {
                    StaticValues sv = new StaticValuesImpl(svm.type(), mc, false, Map.of());
                    builder.setStaticValues(sv);
                    return;
                }

                builder.setStaticValues(NOT_COMPUTED);
            } else {
                if (mc.object() instanceof VariableExpression ve && !(ve.variable() instanceof This)) {
                    StaticValues sv = makeSvFromMethodCall(mc, leObject, svm);
                    builder.merge(ve.variable(), sv);
                }
            }
        }


        private StaticValues makeSvFromMethodCall(MethodCall mc, EvaluationResult leObject, StaticValues svm) {
            Map<Variable, Expression> map = new HashMap<>();
            for (Map.Entry<Variable, Expression> entry : svm.values().entrySet()) {
                Variable variable;
                if (entry.getKey() instanceof DependentVariable dv
                    && dv.indexVariable() instanceof ParameterInfo pi
                    && pi.methodInfo() == mc.methodInfo()) {
                    //dv: this.objects[i]; mc arguments: 0, set -> objects[0]=set
                    Expression concreteIndex = mc.parameterExpressions().get(pi.index());
                    variable = runtime.newDependentVariable(dv.arrayExpression(), concreteIndex);
                } else {
                    variable = entry.getKey();
                }
                if (entry.getValue() instanceof VariableExpression vve
                    && vve.variable() instanceof ParameterInfo pi && pi.methodInfo() == mc.methodInfo()) {
                    map.put(variable, mc.parameterExpressions().get(pi.index()));
                }
            }
            Expression expression;
            if (mc.object() instanceof MethodCall mco && mco.methodInfo().isFluent()) {
                expression = leObject.staticValues().expression();
                map.putAll(leObject.staticValues().values());
            } else {
                expression = null;
            }
            return new StaticValuesImpl(svm.type(), expression, false, Map.copyOf(map));
        }

        private interface PropagateData {
            void accept(Expression mapKey, boolean mapValue, Map<Variable, Expression> svMap);
        }

        private void methodCallModified(MethodCall mc, EvaluationResult.Builder builder) {
            if (mc.object() instanceof VariableExpression ve) {
                boolean modifying = mc.methodInfo().analysis().getOrDefault(NON_MODIFYING_METHOD, FALSE).isFalse();
                if (ve.variable().parameterizedType().isFunctionalInterface()
                    && ve.variable() instanceof FieldReference fr
                    && !fr.isStatic() && !(fr.scopeVariable() instanceof This)) {
                    builder.addModifiedFunctionalInterfaceComponent(fr, modifying);
                } else if (modifying) {
                    markModified(ve.variable(), builder);
                    propagateMethodComponents(mc, builder);
                }
            }
            for (ParameterInfo pi : mc.methodInfo().parameters()) {
                if (pi.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE).isFalse()) {
                    if (pi.isVarArgs()) {
                        for (int i = mc.methodInfo().parameters().size() - 1; i < mc.parameterExpressions().size(); i++) {
                            Expression pe = mc.parameterExpressions().get(i);
                            handleModifiedParameter(builder, pe);
                        }
                    } else {
                        Expression pe = mc.parameterExpressions().get(pi.index());
                        handleModifiedParameter(builder, pe);
                    }
                }
                /*
                When a method has been passed on, and that method turns out to be modifying, we make the method
                reference scope modified as well. If that scope was 'this', the current method will become modifying too.
                On top of that, if the parameters of this method have modified components, we check if we have
                a value for these components, so that we can propagate their modification too.
                 */
                propagateComponents(MODIFIED_FI_COMPONENTS_PARAMETER, mc, pi,
                        (e, mapValue, map) -> {
                            if (e instanceof MethodReference mr) {
                                if (mapValue) {
                                    propagateModificationOfObject(mr, builder);
                                    for (ParameterInfo mrPi : mr.methodInfo().parameters()) {
                                        propagateModificationOfParameter(builder, pi, map, mrPi);
                                    }
                                } else {
                                    ensureNotModifying(mr, builder);
                                }
                            }
                        });
                propagateComponents(MODIFIED_COMPONENTS_PARAMETER, mc, pi,
                        (e, mapValue, map) -> {
                            if (e instanceof VariableExpression ve2 && mapValue) {
                                markModified(ve2.variable(), builder);
                            }
                        });
            }
        }

        private void propagateModificationOfParameter(EvaluationResult.Builder builder,
                                                      ParameterInfo pi,
                                                      Map<Variable, Expression> map,
                                                      ParameterInfo mrPi) {
            VariableBooleanMap modComp = mrPi.analysis().getOrDefault(MODIFIED_COMPONENTS_PARAMETER,
                    ValueImpl.VariableBooleanMapImpl.EMPTY);
            if (!modComp.isEmpty()) {
                TranslationMap tm = runtime.newTranslationMapBuilder()
                        .put(mrPi, runtime.newThis(pi.parameterizedType()))
                        .build();
                for (Map.Entry<Variable, Boolean> entry : modComp.map().entrySet()) {
                    if (entry.getValue()) {
                        // modified component
                        Variable translated = tm.translateVariableRecursively(entry.getKey());
                        Expression value = map.get(translated);
                        if (value instanceof VariableExpression ve) {
                            markModified(ve.variable(), builder);
                        }
                        LOGGER.debug("Have translated variable {}", translated);
                    }
                }
            }
        }

        private void handleModifiedParameter(EvaluationResult.Builder builder, Expression pe) {
            if (pe instanceof VariableExpression ve) {
                markModified(ve.variable(), builder);
            } else if (pe instanceof MethodReference mr) {
                propagateModificationOfObject(mr, builder);
            }
        }

        private void propagateMethodComponents(MethodCall mc, EvaluationResult.Builder builder) {
            VariableBooleanMap modifiedComponents = mc.methodInfo().analysis().getOrNull(MODIFIED_COMPONENTS_METHOD,
                    ValueImpl.VariableBooleanMapImpl.class);
            if (modifiedComponents != null
                && mc.object() instanceof VariableExpression ve
                // we must check for 'this', to eliminate simple assignments
                && !(ve.variable() instanceof This)) {
                for (Map.Entry<Variable, Boolean> entry : modifiedComponents.map().entrySet()) {
                    This thisInSv = runtime.newThis(mc.object().parameterizedType().typeInfo().asParameterizedType());
                    TranslationMap tm = runtime.newTranslationMapBuilder().put(thisInSv, ve.variable()).build();
                    Variable v = tm.translateVariableRecursively(entry.getKey());
                    markModified(v, builder);
                }
            }
        }

        /*
        what if entry.getKey() == ri.objects[index]
        ri == pi, the parameter
        index is the second parameter

        we need to reach this.objects[0]
        for that reason, we add all the parameters to the translation map. See TestStaticValuesModification,4.
         */
        private void propagateComponents(Property property,
                                         MethodCall mc,
                                         ParameterInfo pi,
                                         PropagateData consumer) {
            VariableBooleanMap modifiedComponents = pi.analysis().getOrNull(property,
                    ValueImpl.VariableBooleanMapImpl.class);
            if (modifiedComponents != null) {
                Expression pe = mc.parameterExpressions().get(pi.index());
                if (pe instanceof VariableExpression ve) {
                    if (variableDataPrevious != null) {
                        VariableInfoContainer vicOrNull = variableDataPrevious
                                .variableInfoContainerOrNull(ve.variable().fullyQualifiedName());
                        if (vicOrNull != null) {
                            StaticValues svParam = vicOrNull.best(stageOfPrevious).staticValues();
                            if (svParam != null) {
                                for (Map.Entry<Variable, Boolean> entry : modifiedComponents.map().entrySet()) {
                                    Map<Variable, Expression> completedMap = completeMap(svParam, variableDataPrevious,
                                            stageOfPrevious);
                                    Map<Variable, Expression> augmented = augmentWithImplementation(pi.parameterizedType(),
                                            svParam, completedMap);
                                    Variable afterArgumentExpansion = expandArguments(mc, pi, entry.getKey());
                                    Expression e = augmented.get(afterArgumentExpansion);
                                    consumer.accept(e, entry.getValue(), augmented);
                                }
                            }
                        }
                    }
                }
            }
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
                                                                    StaticValues svParam,
                                                                    Map<Variable, Expression> completedMap) {
            Map<Variable, Expression> completedAndAugmentedWithImplementation;
            if (svParam.type() != null && !svParam.type().equals(targetType)) {
                assert targetType.isAssignableFrom(runtime, svParam.type());
                completedAndAugmentedWithImplementation = new HashMap<>(completedMap);
                completedMap.forEach((v, e) -> {
                    Variable tv = liftVariable(v);
                    if (tv != v && !completedAndAugmentedWithImplementation.containsKey(tv)) {
                        completedAndAugmentedWithImplementation.put(tv, e);
                    }
                });
            } else {
                completedAndAugmentedWithImplementation = completedMap;
            }
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

        private Map<Variable, Expression> completeMap(StaticValues svParam,
                                                      VariableData variableDataPrevious,
                                                      Stage stageOfPrevious) {
            Map<Variable, Expression> result = new HashMap<>(svParam.values());
            while (true) {
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
            }
            return result;
        }

        private void ensureNotModifying(MethodReference mr, EvaluationResult.Builder builder) {
            // TODO
        }

        private void propagateModificationOfObject(MethodReference mr, EvaluationResult.Builder builder) {
            if (mr.methodInfo().isModifying() && mr.scope() instanceof VariableExpression ve) {
                markModified(ve.variable(), builder);
            }
        }

        private void markModified(Variable variable, EvaluationResult.Builder builder) {
            ParameterizedType type = variable.parameterizedType();
            TypeInfo typeInfo = type.typeInfo();
            boolean isNotImmutable = type.arrays() > 0 || typeInfo == null || !typeInfo.analysis()
                    .getOrDefault(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isImmutable();
            if (isNotImmutable || isSyntheticObjectUsedInMethodComponentModification(variable)) {
                builder.addModified(variable);
                if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
                    markModified(fr.scopeVariable(), builder);
                } else if (variable instanceof DependentVariable dv && dv.arrayVariable() != null) {
                    markModified(dv.arrayVariable(), builder);
                }
            }
        }

        private static boolean isSyntheticObjectUsedInMethodComponentModification(Variable variable) {
            return variable.parameterizedType().isJavaLangObject()
                   && variable instanceof FieldReference fr
                   && fr.fieldInfo().isSynthetic();
        }

        private void methodCallLinks(MethodInfo currentMethod,
                                     MethodCall mc,
                                     EvaluationResult.Builder builder,
                                     EvaluationResult leObject,
                                     List<EvaluationResult> leParams,
                                     ParameterizedType forwardType) {
            LinkHelper linkHelper = new LinkHelper(runtime, genericsHelper, analysisHelper, currentMethod,
                    mc.methodInfo());
            ParameterizedType objectType = mc.methodInfo().isStatic() ? null : mc.object().parameterizedType();
            ParameterizedType concreteReturnType = forwardType == null ? mc.concreteReturnType() : forwardType;

            List<LinkedVariables> linkedVariablesOfParameters = leParams.stream()
                    .map(EvaluationResult::linkedVariables).toList();

            // from parameters to object
            LinkHelper.FromParameters fp = linkHelper.linksInvolvingParameters(objectType, concreteReturnType,
                    mc.parameterExpressions(), leParams);
            LinkedVariables linkedVariablesOfObjectFromParams = fp.intoObject().linkedVariables();
            if (mc.object() instanceof VariableExpression ve) {
                builder.merge(ve.variable(), linkedVariablesOfObjectFromParams);
            }
            builder.merge(fp.intoObject());

            // in between parameters (A)
            linkHelper.crossLink(leObject.linkedVariables(), linkedVariablesOfObjectFromParams, builder);

            // from object to return value
            LinkedVariables lvsResult1;
            if (objectType == null) lvsResult1 = EMPTY;
            else {
                lvsResult1 = linkHelper.linkedVariablesMethodCallObjectToReturnType(mc, objectType, leObject.linkedVariables(),
                        linkedVariablesOfParameters, concreteReturnType);
                if (!mc.analysis().haveAnalyzedValueFor(LinkedVariablesImpl.LINKS_TO_OBJECT)) {
                    mc.analysis().set(LinkedVariablesImpl.LINKS_TO_OBJECT, lvsResult1);
                }
            }
            // merge from param to object and from object to return value
            LinkedVariables lvsResult2 = fp.intoResult() == null ? lvsResult1
                    : lvsResult1.merge(fp.intoResult().linkedVariables());
            builder.setLinkedVariables(lvsResult2);
        }
    }
}
