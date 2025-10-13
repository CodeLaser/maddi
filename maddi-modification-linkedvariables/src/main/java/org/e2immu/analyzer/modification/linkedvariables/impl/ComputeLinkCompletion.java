package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.linkedvariables.graph.Cache;
import org.e2immu.analyzer.modification.linkedvariables.graph.ShortestPath;
import org.e2immu.analyzer.modification.linkedvariables.graph.WeightedGraph;
import org.e2immu.analyzer.modification.linkedvariables.graph.impl.GraphCacheImpl;
import org.e2immu.analyzer.modification.linkedvariables.graph.impl.WeightedGraphImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.linkedvariables.staticvalues.StaticValuesHelper;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.MODIFIED_FI_COMPONENTS_VARIABLE;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

/*
Given a number of links, build a graph, and compute the shortest path between all combinations,
to be stored in the VariableInfo objects.

The computeModified() method propagates modification through the graph, using vi.unmodified() to block
traversal of edges and typeIsNotImmutable() to allow them.

 */
class ComputeLinkCompletion {
    private static final Logger LOGGER = LoggerFactory.getLogger("graph-algorithm");

    private final Cache cache = new GraphCacheImpl(100);
    private final StaticValuesHelper staticValuesHelper;
    private final AnalysisHelper analysisHelper;

    ComputeLinkCompletion(AnalysisHelper analysisHelper, StaticValuesHelper staticValuesHelper) {
        this.analysisHelper = analysisHelper;
        this.staticValuesHelper = staticValuesHelper;
    }

    class Builder {
        private final WeightedGraph weightedGraph = new WeightedGraphImpl(cache);
        private final Set<Variable> modifiedInEval = new HashSet<>();
        private final Map<FieldReference, Boolean> modifiedFunctionalComponents = new HashMap<>();
        private final Map<Variable, Set<TypeInfo>> casts = new HashMap<>();

        private final Map<Variable, List<StaticValues>> staticValues = new HashMap<>();

        void addLinkEvaluation(EvaluationResult evaluationResult, VariableData destination) {
            for (Map.Entry<Variable, LinkedVariables> entry : evaluationResult.links().entrySet()) {
                VariableInfoImpl vi = (VariableInfoImpl) destination.variableInfo(entry.getKey());
                addLink(entry.getValue(), vi);
            }
            for (Map.Entry<Variable, StaticValues> entry : evaluationResult.assignments().entrySet()) {
                VariableInfoImpl vi = (VariableInfoImpl) destination.variableInfo(entry.getKey());
                addAssignment(vi.variable(), entry.getValue());
            }
            this.modifiedInEval.addAll(evaluationResult.modified());
            this.modifiedFunctionalComponents.putAll(evaluationResult.modifiedFunctionalComponents());
        }

        void addCasts(Map<Variable, Set<ParameterizedType>> casts) {
            casts.forEach((variable, set) -> {
                Set<TypeInfo> typeInfos = this.casts.computeIfAbsent(variable, v -> new HashSet<>());
                set.forEach(pt -> {
                    TypeInfo e = pt.bestTypeInfo();
                    if (e != null) typeInfos.add(e);
                });
            });
        }

        void addAssignment(Variable variable, StaticValues value) {
            staticValues.computeIfAbsent(variable, l -> new ArrayList<>()).add(value);
        }

        void addLink(LinkedVariables linkedVariables, VariableInfoImpl destinationVi) {
            weightedGraph.addNode(destinationVi.variable(), linkedVariables.variables());
        }

        public void write(VariableData variableData, Stage stage,
                          VariableData previous, Stage stageOfPrevious,
                          String statementIndex, Source source) {
            writeLinksAndModification(variableData, stage, previous, stageOfPrevious);
            writeAssignments(variableData, stage, previous, stageOfPrevious, statementIndex, source);
            writeCasts(variableData, stage, previous, stageOfPrevious);
        }

        private void writeCasts(VariableData variableData,
                                Stage stage,
                                VariableData previous,
                                Stage stageOfPrevious) {
            if (previous != null) {
                previous.variableInfoStream(stageOfPrevious).forEach(vi -> {
                    if (variableData.isKnown(vi.variable().fullyQualifiedName())) {
                        Value.SetOfTypeInfo set = vi.analysis().getOrDefault(VariableInfoImpl.DOWNCAST_VARIABLE,
                                ValueImpl.SetOfTypeInfoImpl.EMPTY);
                        if (!set.typeInfoSet().isEmpty()) {
                            casts.computeIfAbsent(vi.variable(), v -> new HashSet<>()).addAll(set.typeInfoSet());
                        }
                    }
                });
            }
            this.casts.forEach((v, set) -> {
                VariableInfoContainer vic = variableData.variableInfoContainerOrNull(v.fullyQualifiedName());
                VariableInfoImpl vii = (VariableInfoImpl) vic.best(stage);
                vii.analysis().setAllowControlledOverwrite(VariableInfoImpl.DOWNCAST_VARIABLE,
                        new ValueImpl.SetOfTypeInfoImpl(Set.copyOf(set)));
            });
        }

        private void writeAssignments(VariableData variableData,
                                      Stage stage,
                                      VariableData previous,
                                      Stage stageOfPrevious,
                                      String statementIndex,
                                      Source source) {
            staticValuesHelper.recursivelyAddAssignmentsAtScopeLevel(staticValues, source, variableData, statementIndex);
            if (previous != null) {
                // copy previous assignment data into the map, but only for variables that are known to the current one
                // (some variables disappear after a statement, e.g. pattern variables)
                previous.variableInfoStream(stageOfPrevious)
                        .forEach(vi -> {
                            if (variableData.isKnown(vi.variable().fullyQualifiedName()) && vi.staticValues() != null) {
                                addAssignment(vi.variable(), vi.staticValues());
                            }
                        });
            }
            for (Map.Entry<Variable, List<StaticValues>> entry : staticValues.entrySet()) {
                Variable variable = entry.getKey();
                VariableInfoContainer vic = variableData.variableInfoContainerOrNull(variable.fullyQualifiedName());
                VariableInfoImpl vii = (VariableInfoImpl) vic.best(stage);
                StaticValues merge = entry.getValue().stream().reduce(StaticValuesImpl.NONE, StaticValues::merge);
                vii.staticValuesSet(merge);
            }
        }

        private void writeLinksAndModification(VariableData variableData, Stage stage,
                                               VariableData previous, Stage stageOfPrevious) {
            if (previous != null) {
                // copy previous link data into the graph, but only for variables that are known to the current one
                // (some variables disappear after a statement, e.g. pattern variables)
                previous.variableInfoStream(stageOfPrevious).forEach(vi -> {
                    if (variableData.isKnown(vi.variable().fullyQualifiedName()) && vi.linkedVariables() != null) {
                        Map<Variable, LV> map = new HashMap<>();
                        vi.linkedVariables().stream()
                                .filter(e -> variableData.isKnown(e.getKey().fullyQualifiedName()))
                                .forEach(e -> map.put(e.getKey(), e.getValue()));
                        weightedGraph.addNode(vi.variable(), map);
                    }
                });
            }
            LOGGER.debug("WG: {}", weightedGraph);

            // ensure that all variables known at this stage, are present
            variableData.variableInfoStream(stage).forEach(vi ->
                    weightedGraph.addNode(vi.variable(), Map.of()));

            WeightedGraph wgForModification = weightedGraph.copyForModification();
            ShortestPath shortestPathForMod = wgForModification.shortestPath();
            Set<Variable> modifying = computeModified(previous, stageOfPrevious, modifiedInEval, shortestPathForMod);

            ShortestPath shortestPath = weightedGraph.shortestPath();
            Map<Variable, Map<Variable, Boolean>> mfiComponentMaps = computeMFIComponents(previous, stageOfPrevious,
                    modifiedFunctionalComponents, shortestPath);

            for (Variable variable : shortestPath.variables()) {
                Map<Variable, LV> links = shortestPath.links(variable, null);

                VariableInfoContainer vic = variableData.variableInfoContainerOrNull(variable.fullyQualifiedName());
                if (vic != null && vic.has(stage)) {
                    VariableInfoImpl vii = (VariableInfoImpl) vic.best(stage);
                    LinkedVariables linkedVariables = LinkedVariablesImpl.of(links).remove(Set.of(variable));
                    vii.initializeLinkedVariables(LinkedVariablesImpl.NOT_YET_SET);
                    if (links.isEmpty()) {
                        vii.setLinkedVariables(LinkedVariablesImpl.EMPTY);
                    } else {
                        vii.setLinkedVariables(linkedVariables);
                    }
                    if (!vii.analysis().haveAnalyzedValueFor(VariableInfoImpl.UNMODIFIED_VARIABLE)) {
                        boolean unmodified = !modifying.contains(variable);
                        vii.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, ValueImpl.BoolImpl.from(unmodified));
                    }
                    Map<Variable, Boolean> mfiComponents = mfiComponentMaps.get(vii.variable());
                    if (mfiComponents != null
                        && !mfiComponents.isEmpty()
                        && !vii.analysis().haveAnalyzedValueFor(MODIFIED_FI_COMPONENTS_VARIABLE)) {
                        // do a controlled overwrite: as the iterations go, fewer variables become modified
                        // so we allow to overwrite with less
                        vii.analysis().setAllowControlledOverwrite(MODIFIED_FI_COMPONENTS_VARIABLE,
                                new ValueImpl.VariableBooleanMapImpl(mfiComponents));
                    }
                } // is possible: artificially created break variable (see e.g. TestBreakVariable)
            }
        }

        private Map<Variable, Map<Variable, Boolean>> computeMFIComponents
                (VariableData previous, Stage stageOfPrevious,
                 Map<FieldReference, Boolean> modifiedFunctionalComponents,
                 ShortestPath shortestPath) {
            Map<Variable, Map<Variable, Boolean>> mapForAllVariables = new HashMap<>();
            modifiedFunctionalComponents.forEach((fr, b) -> recursivelyAddTo(mapForAllVariables, fr, b));
            if (previous != null) {
                for (Variable variable : shortestPath.variables()) {
                    VariableInfoContainer vicPrev = previous.variableInfoContainerOrNull(variable.fullyQualifiedName());
                    if (vicPrev != null) {
                        VariableInfo vi = vicPrev.best(stageOfPrevious);
                        if (vi != null) {
                            Value.VariableBooleanMap map = vi.analysis().getOrDefault(MODIFIED_FI_COMPONENTS_VARIABLE,
                                    ValueImpl.VariableBooleanMapImpl.EMPTY);
                            mergeMFIMaps(mapForAllVariables, vi.variable(), map.map());
                        }
                    }
                }
            }
            boolean change = true;
            while (change) {
                change = false;
                for (Variable variable : shortestPath.variables()) {
                    Map<Variable, LV> links = shortestPath.links(variable, null);
                    for (Map.Entry<Variable, LV> e : links.entrySet()) {
                        Variable to = e.getKey();
                        if (to != variable && mapForAllVariables.containsKey(to) && e.getValue().isStaticallyAssignedOrAssigned()) {
                            change |= mergeMFIMaps(mapForAllVariables, variable, mapForAllVariables.get(to));
                        }
                    }
                }
            }
            return mapForAllVariables;
        }

        /*
        when presented with r.function->true, we'll add r.function->true in r

        when presented with s.r.function->true, we'll add r.function->true in s.r and s.r.function->true in s

        when presented with t.s.r.function->true, we add 3:
            r.function->true in t.s.r
            s.r.function->true in t.s
            t.s.r.function->true in t

         this mechanism is very similar to the one in recursivelyAdd() for static values.
         FOR NOW, we'll only add when the variable already exists. There is, I believe, no point to have the
         intermediaries.
         */
        private void recursivelyAddTo(Map<Variable, Map<Variable, Boolean>> mapForAllVariables, FieldReference fr, Boolean b) {
            Variable base = fr;
            while (base instanceof FieldReference fr2 && fr2.scope() instanceof VariableExpression ve) {
                base = ve.variable();
            }
            mapForAllVariables.put(base, Map.of(fr, b));
        }

        private static boolean mergeMFIMaps(Map<Variable, Map<Variable, Boolean>> mapForAllVariables,
                                            Variable v,
                                            Map<Variable, Boolean> map) {
            Map<Variable, Boolean> inMap = mapForAllVariables.get(v);
            Map<Variable, Boolean> newMap = mapForAllVariables.merge(v, map, (m1, m2) -> {
                Map<Variable, Boolean> res = new HashMap<>(m1);
                for (Map.Entry<Variable, Boolean> e : m2.entrySet()) {
                    Boolean b = res.put(e.getKey(), e.getValue());
                    if (b != null && (boolean)b != e.getValue()) throw new UnsupportedOperationException();
                }
                return Map.copyOf(res);
            });
            return !Objects.equals(inMap, newMap);
        }


        /*
        note that we do immutable checks again, because of downcasts of immutable objects into immutable HC objects
        (e.g. an object array containing String objects)
         */
        private Set<Variable> computeModified(VariableData previous,
                                              Stage stageOfPrevious,
                                              Set<Variable> modifiedInEval,
                                              ShortestPath shortestPath) {
            Set<Variable> modified = new HashSet<>();
            for (Variable v : modifiedInEval) {
                if (isNotImmutable(v)) modified.add(v);
            }
            if (previous != null) {
                for (Variable variable : shortestPath.variables()) {
                    VariableInfoContainer vicPrev = previous.variableInfoContainerOrNull(variable.fullyQualifiedName());
                    if (vicPrev != null) {
                        VariableInfo vi = vicPrev.best(stageOfPrevious);
                        if (vi != null && !vi.isUnmodified()) {
                            if (isNotImmutable(vi.variable())) {
                                modified.add(vi.variable());
                            }
                        }
                    }
                }
            }
            // because we have no completeness of the graph at the moment, we iterate
            boolean change = true;
            while (change) {
                change = false;
                Set<Variable> newModified = new HashSet<>(modified);
                for (Variable variable : modified) {
                    Map<Variable, LV> links = shortestPath.links(variable, null);
                    for (Map.Entry<Variable, LV> e : links.entrySet()) {
                        Variable to = e.getKey();
                        if (to != variable && e.getValue().propagateModification()) {
                            if (isNotImmutable(to)) {
                                change |= newModified.add(to);
                            }
                        }
                    }
                }
                modified.addAll(newModified);
            }
            return modified;
        }

        /*
        immutableHC is passed on, because those types can be downcast to mutable.
         */
        private boolean isNotImmutable(Variable variable) {
            Value.Immutable immutable = analysisHelper.typeImmutable(variable.parameterizedType());
            return !immutable.isImmutable();
        }
    }
}
