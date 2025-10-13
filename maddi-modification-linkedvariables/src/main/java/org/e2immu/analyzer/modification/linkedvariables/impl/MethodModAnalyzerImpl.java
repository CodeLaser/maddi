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
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.common.defaults.ShallowMethodAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.MethodModAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.linkedvariables.staticvalues.StaticValuesHelper;
import org.e2immu.analyzer.modification.prepwork.hcs.ComputeHCS;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.*;
import static org.e2immu.language.cst.api.analysis.Value.SetOfTypeInfo;
import static org.e2immu.language.cst.api.analysis.Value.VariableBooleanMap;
import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.*;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.*;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;

public class MethodModAnalyzerImpl extends CommonAnalyzerImpl implements MethodModAnalyzer, ModAnalyzerForTesting {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodModAnalyzerImpl.class);
    private static final Logger LOGGER_GRAPH = LoggerFactory.getLogger("graph-algorithm");

    private final Runtime runtime;
    private final ComputeLinkCompletion computeLinkCompletion;
    private final ShallowMethodAnalyzer shallowMethodAnalyzer;
    private final GetSetHelper getSetHelper;
    private final StaticValuesHelper staticValuesHelper;
    private final boolean trackObjectCreations;

    public MethodModAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration) {
        super(configuration);
        this.runtime = runtime;
        staticValuesHelper = new StaticValuesHelper(runtime);
        shallowMethodAnalyzer = new ShallowMethodAnalyzer(runtime, Element::annotations);
        computeLinkCompletion = new ComputeLinkCompletion(new AnalysisHelper(), staticValuesHelper); // has a cache, we want this to be stable
        this.getSetHelper = new GetSetHelper(runtime);
        this.trackObjectCreations = configuration.trackObjectCreations();
    }

    private record OutputImpl(List<AnalyzerException> analyzerExceptions, Set<MethodInfo> waitForMethods,
                              Set<TypeInfo> waitForIndependenceOfTypes,
                              Map<String, Integer> infoHistogram) implements Output {
    }

    @Override
    public Output go(MethodInfo methodInfo, boolean activateCycleBreaking) {
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(activateCycleBreaking);
        try {
            methodAnalyzer.doMethod(methodInfo);
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception/error analyzing {} @line {}", methodInfo, bestSourceLog(methodInfo), re);
            if (configuration.storeErrors()) {
                if (!(re instanceof AnalyzerException)) {
                    methodAnalyzer.analyzerExceptions.add(new AnalyzerException(methodInfo, re));
                }
            } else throw re;
        }
        return new OutputImpl(methodAnalyzer.analyzerExceptions, methodAnalyzer.waitForMethods,
                methodAnalyzer.waitForIndependenceOfTypes, methodAnalyzer.infoHistogram);
    }

    private static String bestSourceLog(MethodInfo methodInfo) {
        Block methodBody = methodInfo.methodBody();
        return methodBody == null || methodBody.source() == null ? "?" : methodBody.source().compact2();
    }

    class MethodAnalyzer implements InternalMethodModAnalyzer {
        private final List<AnalyzerException> analyzerExceptions = new LinkedList<>();
        private final Set<MethodInfo> waitForMethods = new HashSet<>();
        private final Set<TypeInfo> waitForIndependenceOfTypes = new HashSet<>();
        private final Map<String, Integer> infoHistogram = new HashMap<>();
        private final boolean activateCycleBreaking;
        private final ExpressionAnalyzer expressionAnalyzer;

        MethodAnalyzer(boolean activateCycleBreaking) {
            this.activateCycleBreaking = activateCycleBreaking;
            this.expressionAnalyzer = new ExpressionAnalyzer(runtime, this, staticValuesHelper);
        }

        public void doMethod(MethodInfo methodInfo) {
            LOGGER.debug("Mod: do method {}", methodInfo);
            ComputeHCS.safeHcsMethod(runtime, methodInfo);
            assert methodInfo.parameters().stream()
                    .allMatch(pi -> pi.analysis().haveAnalyzedValueFor(HCS_PARAMETER))
                    : "Method with a parameter without HCS: " + methodInfo;

            if (methodInfo.isAbstract()) {
                // NOTE: the shallow analyzers only write out non-default values
                shallowMethodAnalyzer.analyze(methodInfo);
                // TODO consider moving this into the shallow analyzer!
                getSetHelper.copyStaticValuesForGetSet(methodInfo);
            } else {
                Block methodBody = methodInfo.methodBody();
                if (LOGGER.isDebugEnabled() && methodBody.source() != null) {
                    LOGGER.debug("Mod:   method body @line {}", methodBody.source().compact2());
                }
                VariableData variableData = doBlock(methodInfo, methodBody, null);
                if (variableData != null) {
                    copyFromVariablesIntoMethod(methodInfo, variableData);
                } // else: can be null for empty synthetic constructors, for example
            }
        }

        private void copyFromVariablesIntoMethod(MethodInfo methodInfo, VariableData variableData) {
            Map<Variable, Boolean> modifiedComponentsMethod = new HashMap<>();
            boolean allFieldsUnmodified = true;
            for (VariableInfo vi : variableData.variableInfoStream().toList()) {
                Variable v = vi.variable();
                if (v instanceof ParameterInfo pi && pi.methodInfo() == methodInfo) {
                    copyFromVariablesIntoMethodPi(variableData, vi, pi);
                } else if (v instanceof ReturnVariable && methodInfo.hasReturnValue()) {
                    LinkedVariables linkedVariables = vi.linkedVariables();
                    if (linkedVariables != null) {
                        LinkedVariables filteredLvs = linkedVariables.remove(vv -> vv instanceof LocalVariable);
                        if (filteredLvs != null) {
                            methodInfo.analysis().setAllowControlledOverwrite(LINKED_VARIABLES_METHOD, filteredLvs);
                        }
                    }
                    StaticValues staticValues = vi.staticValues();
                    if (staticValues != null) {
                        StaticValues filtered = staticValues.remove(vv -> vv instanceof LocalVariable);
                        methodInfo.analysis().setAllowControlledOverwrite(STATIC_VALUES_METHOD, filtered);
                    }
                } else if (v instanceof This ||
                           (v instanceof FieldReference fr && (fr.scopeIsRecursivelyThis() || fr.isStatic()))
                           && fr.fieldInfo().analysis().getOrDefault(IGNORE_MODIFICATIONS_FIELD, FALSE).isFalse()
                           || vi.isVariableInClosure()) {
                    boolean modification = vi.analysis().getOrDefault(UNMODIFIED_VARIABLE, FALSE).isFalse();
                    boolean assignment = !vi.assignments().isEmpty();
                    if ((modification || assignment) && !methodInfo.isConstructor()) {
                        allFieldsUnmodified = false;
                        if (v instanceof FieldReference || vi.isVariableInClosure()) {
                            modifiedComponentsMethod.put(v, true);
                        }
                    }
                    if (vi.isVariableInClosure()) {
                        VariableData vd = vi.variableInfoInClosure();
                        VariableInfo outerVi = vd.variableInfo(vi.variable().fullyQualifiedName());
                        try {
                            outerVi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE,
                                    ValueImpl.BoolImpl.from(!modification));
                        } catch (RuntimeException re) {
                            LOGGER.error("Overwrite error variable in closure {}", outerVi.variable());
                            throw re;
                        }
                    }
                }
                if (methodInfo.isConstructor()
                    && v instanceof FieldReference fr && fr.scopeIsThis() && fr.fieldInfo().isPropertyFinal()) {
                    StaticValues sv = vi.staticValues();
                    if (sv != null && sv.expression() instanceof VariableExpression ve
                        && ve.variable() instanceof ParameterInfo pi) {
                        VariableExpression veFr = runtime.newVariableExpressionBuilder()
                                .setVariable(fr).setSource(ve.source())
                                .build();
                        StaticValues newSv = new StaticValuesImpl(null, veFr, false, Map.of());
                        pi.analysis().setAllowControlledOverwrite(STATIC_VALUES_PARAMETER, newSv);

                        pi.analysis().setAllowControlledOverwrite(PARAMETER_ASSIGNED_TO_FIELD,
                                new ValueImpl.AssignedToFieldImpl(Set.of(fr.fieldInfo())));

                    }
                }
                if (v instanceof This && !methodInfo.hasReturnValue()) {
                    StaticValues staticValues = vi.staticValues();
                    if (staticValues != null) {
                        StaticValues filtered = staticValues.remove(vv -> vv instanceof LocalVariable);
                        methodInfo.analysis().setAllowControlledOverwrite(STATIC_VALUES_METHOD, filtered);
                    }
                }
            }
            if (!methodInfo.isConstructor()) {
                methodInfo.analysis().setAllowControlledOverwrite(NON_MODIFYING_METHOD,
                        ValueImpl.BoolImpl.from(allFieldsUnmodified));
            }
            methodInfo.analysis().setAllowControlledOverwrite(MODIFIED_COMPONENTS_METHOD,
                    new ValueImpl.VariableBooleanMapImpl(modifiedComponentsMethod));
        }

        private void copyFromVariablesIntoMethodPi(VariableData variableData, VariableInfo vi, ParameterInfo pi) {
            LinkedVariables linkedVariables = vi.linkedVariables();
            if (linkedVariables != null) {
                LinkedVariables filteredLvs = linkedVariables.remove(vv -> vv instanceof LocalVariable);
                if (filteredLvs != null) {
                    pi.analysis().setAllowControlledOverwrite(LINKED_VARIABLES_PARAMETER, filteredLvs);
                }
            }
            if (vi.isUnmodified() && linkedVariables != null
                && linkedVariables.variableStream().noneMatch(v ->
                    v instanceof FieldReference fr && fr.scopeIsRecursivelyThis())) {
                pi.analysis().setAllowControlledOverwrite(UNMODIFIED_PARAMETER, TRUE);
            } // when linked to a field, we must wait for the field to be declared unmodified...

            // IMPORTANT: we also store this in case of !modified; see TestLinkCast,2
            // a parameter can be of type Object, not modified, even though, via casting, its hidden content is modified

            Map<Variable, Boolean> modifiedComponents = computeModifiedComponents(variableData, pi);
            if (!modifiedComponents.isEmpty()) {
                VariableBooleanMap vbm = translateVariableBooleanMapToThisScope(pi, modifiedComponents);
                pi.analysis().setAllowControlledOverwrite(MODIFIED_COMPONENTS_PARAMETER, vbm);
            }

            VariableBooleanMap mfi = vi.analysis().getOrNull(VariableInfoImpl.MODIFIED_FI_COMPONENTS_VARIABLE,
                    ValueImpl.VariableBooleanMapImpl.class);
            if (mfi != null && !mfi.map().isEmpty()) {
                VariableBooleanMap thisScope = translateVariableBooleanMapToThisScope(pi, mfi.map());
                pi.analysis().setAllowControlledOverwrite(MODIFIED_FI_COMPONENTS_PARAMETER, thisScope);
            }

            SetOfTypeInfo casts = vi.analysis().getOrDefault(VariableInfoImpl.DOWNCAST_VARIABLE, ValueImpl.SetOfTypeInfoImpl.EMPTY);
            if (!casts.typeInfoSet().isEmpty()) {
                pi.analysis().setAllowControlledOverwrite(DOWNCAST_PARAMETER, casts);
            }
        }

        private VariableBooleanMap translateVariableBooleanMapToThisScope(ParameterInfo pi, Map<Variable, Boolean> vbm) {
            This thisVar = runtime.newThis(pi.parameterizedType().typeInfo().asParameterizedType());
            Map<Variable, Boolean> map = vbm.entrySet().stream().collect(Collectors
                    .toUnmodifiableMap(e -> replaceScope(e.getKey(), thisVar), Map.Entry::getValue));
            return new ValueImpl.VariableBooleanMapImpl(map);
        }

        private Variable replaceScope(Variable v, Variable newScope) {
            Variable frScope = v.fieldReferenceBase();
            if (frScope != null) {
                TranslationMap tm = runtime.newTranslationMapBuilder().put(frScope, newScope).build();
                return tm.translateVariableRecursively(v);
            }
            return v;
        }

        // step 1: modification check on fields of parameter
        // step 2: modification check on variables who have been assigned to a field of a parameter
        private Map<Variable, Boolean> computeModifiedComponents(VariableData variableData, ParameterInfo pi) {
            if (pi.parameterizedType().isTypeParameter()) return Map.of();
            Stream<Variable> step1 = variableData.variableInfoStream()
                    .filter(vi -> vi.variable() instanceof FieldReference fr && fr.scopeIsRecursively(pi))
                    .filter(vi -> !vi.isUnmodified())
                    .map(VariableInfo::variable);
            Stream<Variable> step2 = variableData.variableInfoStream()
                    .filter(vi -> !vi.isUnmodified())
                    .map(vi -> {
                        StaticValues sv = vi.staticValues();
                        if (sv != null && sv.expression() instanceof VariableExpression ve && ve.variable().scopeIsRecursively(pi)) {
                            // this catches fields of a parameter, and array indexing in that parameter
                            return ve.variable();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull);
            return Stream.concat(step1, step2)
                    .distinct() // we could be in both!
                    .collect(Collectors.toUnmodifiableMap(v -> v, v -> true));
        }

        // NOTE: the doBlocks, doBlock methods, and parts of doStatement, have been copied from prepwork.MethodAnalyzer
        // NOTE: order is important, to provide stability in eventual json output
        private LinkedHashMap<String, VariableData> doBlocks(MethodInfo methodInfo,
                                                             Statement parentStatement,
                                                             VariableData vdOfParent) {
            Stream<Block> blockStream;
            if (parentStatement instanceof TryStatement ts && !(ts.resources().isEmpty())) {
                Block.Builder bb = runtime.newBlockBuilder();
                bb.addStatements(ts.resources());
                bb.addStatements(ts.block().statements());
                blockStream = Stream.concat(Stream.of(bb.build()), ts.otherBlocksStream());
            } else {
                blockStream = parentStatement.subBlockStream();
            }
            return blockStream
                    .filter(b -> !b.isEmpty())
                    .collect(Collectors.toMap(
                            block -> block.statements().getFirst().source().index(),
                            block -> doBlock(methodInfo, block, vdOfParent),
                            (vd1, vd2) -> {
                                throw new UnsupportedOperationException();
                            },
                            LinkedHashMap::new));
        }

        @Override
        public VariableData doBlock(MethodInfo methodInfo,
                                    Block block,
                                    VariableData vdOfParent) {
            VariableData previous = vdOfParent;
            boolean first = true;
            for (Statement statement : block.statements()) {
                try {
                    previous = doStatement(methodInfo, statement, previous, first);
                } catch (Throwable re) {
                    LOGGER.error("Have error analyzing statement {}, {}, in method {}",
                            statement, statement.source(), methodInfo);
                    throw re;
                }
                if (first) first = false;
            }
            return previous;
        }

        @Override
        public VariableData doStatement(MethodInfo methodInfo,
                                        Statement statement,
                                        VariableData previous,
                                        boolean first) {
            LOGGER_GRAPH.debug("Method {} statement {}", methodInfo, statement.source());
            Stage stageOfPrevious = first ? Stage.EVALUATION : Stage.MERGE;
            VariableData vd = VariableDataImpl.of(statement);
            assert vd != null : "No variable data in " + statement + " source " + statement.source();
            ComputeLinkCompletion.Builder clcBuilder = computeLinkCompletion.new Builder();

            if (statement instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv -> {
                    if (!lv.assignmentExpression().isEmpty()) {
                        EvaluationResult evaluationResult = expressionAnalyzer.linkEvaluation(methodInfo, previous,
                                stageOfPrevious, lv.assignmentExpression());
                        VariableInfoImpl vi = (VariableInfoImpl) vd.variableInfo(lv);
                        clcBuilder.addLink(evaluationResult.linkedVariables(), vi);
                        clcBuilder.addLinkEvaluation(evaluationResult, vd);
                        StaticValues staticValues = evaluationResult.gatherAllStaticValues(runtime);
                        clcBuilder.addAssignment(vi.variable(), staticValues);
                        clcBuilder.addCasts(evaluationResult.casts());
                    }
                });
            } else if (statement instanceof ExpressionAsStatement
                       || statement instanceof ReturnStatement
                       || statement instanceof YieldStatement
                       || statement instanceof IfElseStatement
                       || statement instanceof LoopStatement
                       || statement instanceof SynchronizedStatement
                       || statement instanceof ThrowStatement
                       || statement instanceof SwitchStatementOldStyle
                       || statement instanceof SwitchStatementNewStyle
            ) {
                EvaluationResult evaluationResult = expressionAnalyzer.linkEvaluation(methodInfo, previous, stageOfPrevious,
                        statement.expression());

                if (statement instanceof ReturnStatement) {
                    ReturnVariable rv = new ReturnVariableImpl(methodInfo);
                    VariableInfoImpl vi = (VariableInfoImpl) vd.variableInfo(rv);
                    clcBuilder.addLink(evaluationResult.linkedVariables(), vi);
                    StaticValues sv = evaluationResult.staticValues();
                    // see TestStaticValuesAssignment: when returning this, we add the static values of the fields
                    if (sv.expression() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        Map<Variable, Expression> map =
                                previous == null ? Map.of() :
                                        previous.variableInfoStream(stageOfPrevious)
                                                .filter(vi2 -> vi2.variable() instanceof FieldReference fr && fr.scopeIsRecursivelyThis())
                                                .filter(vi2 -> vi2.staticValues() != null && vi2.staticValues().expression() != null)
                                                .collect(Collectors.toUnmodifiableMap(VariableInfo::variable, vi2 -> vi2.staticValues().expression()));
                        if (!map.isEmpty()) {
                            clcBuilder.addAssignment(rv, new StaticValuesImpl(null, sv.expression(), false, map));
                        } else {
                            clcBuilder.addAssignment(rv, sv);
                        }
                    } else {
                        clcBuilder.addAssignment(rv, sv);
                    }
                } else if (statement instanceof ForEachStatement forEach) {
                    LinkedVariables lvs;
                    VariableInfoImpl vi = (VariableInfoImpl) vd.variableInfo(forEach.initializer().localVariable());
                    lvs = computeForEachLinkedVariables(methodInfo, previous, forEach, stageOfPrevious);
                    clcBuilder.addLink(lvs, vi);
                }
                clcBuilder.addLinkEvaluation(evaluationResult, vd);
                clcBuilder.addCasts(evaluationResult.casts());
            } else if (statement instanceof ExplicitConstructorInvocation eci) {
                eci.parameterExpressions().forEach(expression -> {
                    EvaluationResult er = expressionAnalyzer.linkEvaluation(methodInfo, previous, stageOfPrevious, expression);
                    clcBuilder.addLinkEvaluation(er, vd);
                    clcBuilder.addCasts(er.casts());
                });
            } // else: resources of Try statement are handled in doBlocks
            clcBuilder.write(vd, Stage.EVALUATION, previous, stageOfPrevious, statement.source().index(), statement.source());

            if (statement.hasSubBlocks()) {
                LinkedHashMap<String, VariableData> lastOfEachSubBlock = doBlocks(methodInfo, statement, vd);
                if (!lastOfEachSubBlock.isEmpty()) {
                    vd.variableInfoContainerStream().forEach(vic -> {
                        if (vic.hasMerge()) {
                            mergeBlocks(vic, lastOfEachSubBlock, vd);
                        }
                    });
                }
            }
            return vd;
        }

        private LinkedVariables computeForEachLinkedVariables(MethodInfo methodInfo,
                                                              VariableData previous,
                                                              ForEachStatement forEach,
                                                              Stage stageOfPrevious) {
            LinkedVariables lvs;
            TypeInfo iterator = runtime.getFullyQualified(Iterator.class, false);
            TypeInfo iterableType = runtime.getFullyQualified(Iterable.class, false);
            if (iterator == null || iterableType == null) {
                lvs = EMPTY;
            } else {
                Source source = forEach.expression().source();
                ParameterizedType initType = forEach.initializer().localVariable().parameterizedType();
                MethodInfo iterableIterator = iterableType.findUniqueMethod("iterator", 0);
                ParameterizedType concreteIteratorType = runtime.newParameterizedType(iterator,
                        List.of(initType.ensureBoxed(runtime)));
                MethodCall mcIterator = runtime.newMethodCallBuilder()
                        .setSource(source)
                        .setObject(forEach.expression())
                        .setMethodInfo(iterableIterator)
                        .setParameterExpressions(List.of())
                        .setConcreteReturnType(concreteIteratorType)
                        .build();
                MethodInfo iteratorNext = iterator.findUniqueMethod("next", 0);
                MethodCall mc = runtime.newMethodCallBuilder()
                        .setSource(source)
                        .setObject(mcIterator)
                        .setMethodInfo(iteratorNext)
                        .setParameterExpressions(List.of(runtime.newInt(List.of(), source, 0)))
                        .setConcreteReturnType(initType)
                        .build();
                EvaluationResult ev2 = expressionAnalyzer.linkEvaluation(methodInfo, previous, stageOfPrevious, mc);
                lvs = ev2.linkedVariables();
            }
            return lvs;
        }

        private static void mergeBlocks(VariableInfoContainer vic, Map<String, VariableData> lastOfEachSubBlock, VariableData vd) {
            Variable variable = vic.variable();
            VariableInfoImpl merge = (VariableInfoImpl) vic.best();

            LinkedVariables reducedLv = computeLinkedVariablesMerge(lastOfEachSubBlock, variable, vd);
            merge.initializeLinkedVariables(LinkedVariablesImpl.NOT_YET_SET);
            merge.setLinkedVariables(reducedLv);

            StaticValues reducedSv = computeStaticValuesMerge(lastOfEachSubBlock, variable);
            merge.staticValuesSet(reducedSv);

            boolean unmodified = lastOfEachSubBlock.values().stream()
                    .map(lastVd -> lastVd.variableInfoContainerOrNull(variable.fullyQualifiedName()))
                    .filter(Objects::nonNull)
                    .map(VariableInfoContainer::best)
                    .allMatch(vi -> vi.analysis().getOrDefault(UNMODIFIED_VARIABLE, FALSE).isTrue());
            merge.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, ValueImpl.BoolImpl.from(unmodified));

            Map<Variable, Boolean> map = lastOfEachSubBlock.values().stream()
                    .map(lastVd -> lastVd.variableInfoContainerOrNull(variable.fullyQualifiedName()))
                    .filter(Objects::nonNull)
                    .map(VariableInfoContainer::best)
                    .flatMap(vi -> vi.analysis().getOrDefault(MODIFIED_FI_COMPONENTS_VARIABLE,
                            ValueImpl.VariableBooleanMapImpl.EMPTY).map().entrySet().stream())
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue,
                            (b1, b2) -> b1 || b2)); // TODO is this the correct merge function?
            merge.analysis().setAllowControlledOverwrite(MODIFIED_FI_COMPONENTS_VARIABLE, new ValueImpl.VariableBooleanMapImpl(map));
            Set<TypeInfo> combinedCasts = lastOfEachSubBlock.values().stream()
                    .map(lastVd -> lastVd.variableInfoContainerOrNull(variable.fullyQualifiedName()))
                    .filter(Objects::nonNull)
                    .map(VariableInfoContainer::best)
                    .flatMap(vi -> vi.analysis().getOrDefault(DOWNCAST_VARIABLE, ValueImpl.SetOfTypeInfoImpl.EMPTY).typeInfoSet().stream())
                    .collect(Collectors.toUnmodifiableSet());
            if (!combinedCasts.isEmpty()) {
                merge.analysis().setAllowControlledOverwrite(DOWNCAST_VARIABLE, new ValueImpl.SetOfTypeInfoImpl(combinedCasts));
            }
        }

        private static StaticValues computeStaticValuesMerge(Map<String, VariableData> lastOfEachSubBlock, Variable variable) {
            List<StaticValues> staticValuesList = lastOfEachSubBlock.values().stream()
                    .map(lastVd -> lastVd.variableInfoContainerOrNull(variable.fullyQualifiedName()))
                    .filter(Objects::nonNull)
                    .map(VariableInfoContainer::best)
                    .map(VariableInfo::staticValues)
                    .filter(Objects::nonNull)
                    .toList();
            return staticValuesList.stream().reduce(NONE, StaticValues::merge);
        }

        private static LinkedVariables computeLinkedVariablesMerge(Map<String, VariableData> lastOfEachSubBlock,
                                                                   Variable variable,
                                                                   VariableData vdMerge) {
            List<LinkedVariables> linkedVariablesList = lastOfEachSubBlock.values().stream()
                    .map(lastVd -> lastVd.variableInfoContainerOrNull(variable.fullyQualifiedName()))
                    .filter(Objects::nonNull)
                    .map(VariableInfoContainer::best)
                    .map(VariableInfo::linkedVariables)
                    .filter(Objects::nonNull)
                    .toList();
            return linkedVariablesList.stream().reduce(EMPTY, LinkedVariables::merge).remove(v -> !vdMerge.isKnown(v.fullyQualifiedName()));
        }

        @Override
        public boolean trackObjectCreations() {
            return trackObjectCreations;
        }
    }

    /*
    there is no real need to analyze per primary type
     */

    @Override
    public List<AnalyzerException> go(List<Info> analysisOrder) {
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(false);
        for (Info info : analysisOrder) {
            try {
                if (info instanceof MethodInfo mi) {
                    methodAnalyzer.doMethod(mi);
                }
            } catch (Exception | AssertionError problem) {
                LOGGER.error("Caught exception/error analyzing {}: {}", info, problem.getMessage());
                if (configuration.storeErrors()) {
                    methodAnalyzer.analyzerExceptions.add(new AnalyzerException(info, problem));
                    String errorMessage = Objects.requireNonNullElse(problem.getMessage(), "<no message>");
                    String fullMessage = "ANALYZER ERROR: " + errorMessage;
                    info.analysis().set(ANALYZER_ERROR, new ValueImpl.MessageImpl(fullMessage));
                } else {
                    throw problem;
                }
            }
        }
        return methodAnalyzer.analyzerExceptions;
    }
}
