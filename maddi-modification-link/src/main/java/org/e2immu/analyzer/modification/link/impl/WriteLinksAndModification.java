package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkGraph.followGraph;
import static org.e2immu.analyzer.modification.link.impl.LinkGraph.printGraph;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

record WriteLinksAndModification(JavaInspector javaInspector, Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteLinksAndModification.class);

    record WriteResult(Map<Variable, Links> newLinks, Set<Variable> modifiedOutsideVariableData) {
    }

    @NotNull WriteResult go(Statement statement,
                            VariableData vd,
                            Set<Variable> previouslyModified,
                            Map<Variable, Set<MethodInfo>> modifiedDuringEvaluation,
                            Map<LinkGraph.V, Map<LinkGraph.V, LinkNature>> graph) {

        // do the first iteration
        LoopResult lr = loopOverVd(vd, statement, graph, previouslyModified, modifiedDuringEvaluation);
        if (!lr.redo) {
            return new WriteResult(lr.newLinkedVariables, lr.unmarkedModifications);
        }

        // do a second iteration, we have changed some of the operations because of a modification
        // (⊆ becomes ~ after List.add(...) e.g. See TestConstructor,1)
        LinkGraph linkGraph = new LinkGraph(javaInspector, runtime, false);
        Map<LinkGraph.V, Map<LinkGraph.V, LinkNature>> graph2 = linkGraph.makeGraph(lr.newLinkedVariables, Set.of());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Recomputed bi-directional graph for local:\n{}", printGraph(graph2));
        }
        // first decide which variables to recompute
        String index = statement.source().index();
        Set<Variable> recompute = vd.variableInfoStream(Stage.EVALUATION)
                .filter(vi -> vi.assignments().indexOfDefinition().compareTo(index) < 0
                              && !vi.assignments().contains(index))
                .map(VariableInfo::variable)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.debug("Variables to recompute: {}", recompute);
        Map<Variable, Links> newLinkedVariables = new HashMap<>(lr.newLinkedVariables);
        for (Variable variable : recompute) {
            Links.Builder builder = followGraph(graph2, variable, modifiedDuringEvaluation.get(variable));
            builder.removeIf(l -> Util.lvPrimaryOrNull(l.to()) instanceof IntermediateVariable);
            newLinkedVariables.put(variable, builder.build());
        }
        return new WriteResult(newLinkedVariables, lr.unmarkedModifications);
    }

    private record LoopResult(boolean redo,
                              Set<Variable> unmarkedModifications,
                              Map<Variable, Links> newLinkedVariables) {
    }

    private LoopResult loopOverVd(VariableData vd,
                                  Statement statement,
                                  Map<LinkGraph.V, Map<LinkGraph.V, LinkNature>> graph,
                                  Set<Variable> previouslyModified,
                                  Map<Variable, Set<MethodInfo>> modifiedDuringEvaluation) {
        Set<Variable> unmarkedModifications = new HashSet<>(modifiedDuringEvaluation.keySet());
        Map<Variable, Links.Builder> newLinkedVariables = new HashMap<>();
        List<Link> toRemove = new ArrayList<>();
        for (VariableInfo vi : vd.variableInfoIterable(Stage.EVALUATION)) {
            toRemove.addAll(doVariableReturnRecompute(statement, graph, vi, unmarkedModifications,
                    previouslyModified, modifiedDuringEvaluation, newLinkedVariables));
        }
        for (Link link : toRemove) {
            newLinkedVariables.get(Util.primary(link.from())).removeIf(l -> l.equals(link));
        }
        Map<Variable, Links> builtNewLinkedVariables = new HashMap<>();
        newLinkedVariables.forEach((v, b) -> builtNewLinkedVariables.put(v, b.build()));
        return new LoopResult(!toRemove.isEmpty(), unmarkedModifications, builtNewLinkedVariables);
    }


    private List<Link> doVariableReturnRecompute(Statement statement,
                                                 Map<LinkGraph.V, Map<LinkGraph.V, LinkNature>> graph,
                                                 VariableInfo vi,
                                                 Set<Variable> unmarkedModifications,
                                                 Set<Variable> previouslyModified,
                                                 Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation,
                                                 Map<Variable, Links.Builder> newLinkedVariables) {
        Variable variable = vi.variable();
        unmarkedModifications.remove(variable);

        Links.Builder builder = followGraph(graph, variable, modifiedInThisEvaluation.get(variable));
        List<Link> toRemove = new ArrayList<>();
        if (variable instanceof ReturnVariable rv) {
            handleReturnVariable(rv, builder);
        } else {
            boolean unmodified = !previouslyModified.contains(variable)
                                 && (assignedInThisStatement(statement, vi)
                                     || !modifiedInThisEvaluation.containsKey(variable)
                                        && notLinkedToModified(builder, modifiedInThisEvaluation));
            builder.removeIf(l -> Util.lvPrimaryOrNull(l.to()) instanceof IntermediateVariable);

            Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
            vi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, newValue);

            if (!unmodified) {
                toRemove.addAll(builder.replaceSubsetSuperset(variable));
            }
        }
        if (newLinkedVariables.put(variable, builder) != null) {
            throw new UnsupportedOperationException("Each real variable must be a primary");
        }
        return toRemove;
    }

    private void handleReturnVariable(ReturnVariable rv, Links.Builder builder) {
        // replace all intermediates by a marker; don't worry about duplicate makers for now
        // don't bother with modifications; not relevant.
        boolean needMarker = false;
        List<Link> newLinks = new ArrayList<>();
        for (Link link : builder) {
            if (link.linkNature().isIdenticalToOrAssignedFromTo()
                && link.to() instanceof IntermediateVariable iv && iv.isNewObject()) {
                needMarker = true;
            } else if (LinkVariable.acceptForLinkedVariables(link.from())
                       && LinkVariable.acceptForLinkedVariables(link.to())) {
                newLinks.add(link);
            }
        }
        if (needMarker) {
            Variable marker = MarkerVariable.someValue(runtime, rv.methodInfo().returnType());
            newLinks.addFirst(new LinksImpl.LinkImpl(rv, IS_ASSIGNED_FROM, marker));
        }
        builder.replaceAll(newLinks);
    }

    private static boolean assignedInThisStatement(Statement statement, VariableInfo vi) {
        String index = statement.source().index();
        return vi.assignments().contains(index) && !vi.reads().indices().contains(index);
    }

    private boolean notLinkedToModified(Links.Builder builder,
                                        Map<Variable, Set<MethodInfo>> modifiedVariablesAndTheirCause) {
        for (Link link : builder) {
            Variable toPrimary = Util.primary(link.to());
            Set<MethodInfo> causesOfModification = modifiedVariablesAndTheirCause.get(toPrimary);
            if (causesOfModification != null) {
                LinkNature ln = link.linkNature();
                if (ln.isIdenticalTo() // FIXME check pass
                    && link.to() instanceof FieldReference fr
                    && VirtualFieldComputer.isVirtualModificationField(fr.fieldInfo())
                    && (ln.pass().isEmpty() || !Collections.disjoint(ln.pass(), causesOfModification))) {
                    return false;
                }
                if (ln == CONTAINS_AS_FIELD
                    || ln == SHARES_FIELDS // see impl/TestInstanceOf,2
                    || ln == CONTAINS_AS_MEMBER) {
                    return false;
                }
                // for now, we ONLY propagate through §m
               /* if (ln == IS_ASSIGNED_TO) {
                    Value.Immutable immutable = new AnalysisHelper().typeImmutable(link.to().parameterizedType());
                    return immutable.isAtLeastImmutableHC();
                }*/
            }
        }
        return true;
    }

}
