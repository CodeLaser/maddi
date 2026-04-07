package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.linkgraph2.FollowGraph;
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
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

class WriteLinksAndModification {
    private final JavaInspector javaInspector;
    private final Runtime runtime;
    private final VirtualFieldComputer virtualFieldComputer;
    private final FollowGraph followGraph;

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteLinksAndModification.class);

    WriteLinksAndModification(JavaInspector javaInspector,
                              VirtualFieldComputer virtualFieldComputer,
                              FollowGraph followGraph) {
        this.javaInspector = javaInspector;
        this.runtime = javaInspector.runtime();
        this.virtualFieldComputer = virtualFieldComputer;
        this.followGraph = followGraph;
    }

    record WriteResult(Map<Variable, Links> newLinks, Set<Variable> modifiedOutsideVariableData, int newLinksSize) {
    }

    @NotNull WriteResult go(Statement statement,
                            boolean lastStatement,
                            VariableData vd,
                            Set<Variable> previouslyModified,
                            Map<Variable, Set<MethodInfo>> modifiedDuringEvaluation) {

        // do the first iteration
        LoopResult lr = loopOverVd(vd, statement, lastStatement, previouslyModified, modifiedDuringEvaluation);
        assert !lr.redo;
        return new WriteResult(lr.newLinkedVariables, lr.unmarkedModifications, lr.newLinksSize);

        /* TODO
        // do a second iteration, we have changed some of the operations because of a modification
        // (⊆ becomes ~ after List.add(...) e.g. See TestConstructor,1)
        LinkGraph linkGraph = new LinkGraph(javaInspector, runtime, false, timer, followGraph);
        Map<Variable, Map<Variable, LinkNature>> graph2 = linkGraph.makeGraph(lr.newLinkedVariables, Set.of());
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
            Links.Builder builder = followGraph.followGraph(virtualFieldComputer, graph2, variable);
            builder.removeIf(l -> Util.lvPrimaryOrNull(l.to()) instanceof IntermediateVariable);
            newLinkedVariables.put(variable, builder.build());
        }
        return new WriteResult(newLinkedVariables, lr.unmarkedModifications, lr.newLinksSize);*/
    }

    private record LoopResult(boolean redo,
                              Set<Variable> unmarkedModifications,
                              Map<Variable, Links> newLinkedVariables,
                              int newLinksSize) {
    }

    private LoopResult loopOverVd(VariableData vd,
                                  Statement statement,
                                  boolean lastStatement,
                                  Set<Variable> previouslyModified,
                                  Map<Variable, Set<MethodInfo>> modifiedDuringEvaluation) {

        Set<Variable> unmarkedModifications = new HashSet<>(modifiedDuringEvaluation.keySet());
        Map<Variable, Links.Builder> newLinkedVariables = new HashMap<>();
        List<Link> toRemove = new ArrayList<>();

        // the purpose of this map is to make sure that we don't add unnecessary virtual modification links (a.§m ≡ b.§m)
        // this system depends on always processing the variables in the same order (linked hash map in VD, order of occurrence)
        // this should reduce the modification links to something below quadratic
        for (VariableInfo vi : vd.variableInfoIterable(Stage.EVALUATION)) {
            toRemove.addAll(doVariableReturnRecompute(statement, lastStatement, vi, unmarkedModifications,
                    previouslyModified, modifiedDuringEvaluation, newLinkedVariables));
        }
        for (Link link : toRemove) {
            Variable primary = Util.primary(link.from());
            Links.Builder builder = newLinkedVariables.get(primary);
            if (builder != null) builder.removeIf(l -> l.equals(link));
        }
        Map<Variable, Links> builtNewLinkedVariables = new HashMap<>();
        int sum = newLinkedVariables.entrySet().stream().mapToInt(e -> {
            Links links = e.getValue().build();
            builtNewLinkedVariables.put(e.getKey(), links);
            return links.size();
        }).sum();
        return new LoopResult(!toRemove.isEmpty(), unmarkedModifications, builtNewLinkedVariables, sum);
    }


    private List<Link> doVariableReturnRecompute(Statement statement,
                                                 boolean lastStatement,
                                                 VariableInfo vi,
                                                 Set<Variable> unmarkedModifications,
                                                 Set<Variable> previouslyModified,
                                                 Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation,
                                                 Map<Variable, Links.Builder> newLinkedVariables) {
        Variable variable = vi.variable();
        unmarkedModifications.remove(variable);

        Links.Builder builder = followGraph.followGraph(virtualFieldComputer, variable);
        List<Link> toRemove = new ArrayList<>();
        if (variable instanceof ReturnVariable rv) {
            // return variables will always be complete
            handleReturnVariable(rv, builder);
        } else {
            // in the very last statement, we want the parameters to be complete
            Set<Variable> completion;
            // FIXME
            // if (!lastStatement || !(variable instanceof ParameterInfo)) {
            //     redundantLinks.redundantLinks(builder);
            //      completion = redundantLinks.modificationLinks(builder, modifiedInThisEvaluation);
            // } else {
            completion = Set.of();
            //  }
            boolean unmodified =
                    variable.isIgnoreModifications()
                    ||
                    !previouslyModified.contains(variable)
                    && (assignedInThisStatement(statement, vi)
                        || !modifiedInThisEvaluation.containsKey(variable)
                           // all the §m links
                           && Collections.disjoint(modifiedInThisEvaluation.keySet(), completion)
                           && notLinkedToModified(builder, modifiedInThisEvaluation));
            builder.removeIf(l -> Util.lvPrimaryOrNull(l.to()) instanceof IntermediateVariable
                                  || l.to() instanceof MarkerVariable mv && mv.isConstant() && !(l.linkNature().equals(IS_ASSIGNED_FROM) || l.linkNature().equals(CONTAINS_AS_MEMBER))
                                  || l.from() instanceof MarkerVariable mvf && mvf.isConstant() && !(l.linkNature().equals(IS_ASSIGNED_TO) || l.linkNature().equals(IS_ELEMENT_OF)));

            if (variable instanceof This) {
                // only keep direct links for "this", the others are replicated in its fields
                builder.removeIf(l -> !(l.from() instanceof This));
            }
            Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
            vi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, newValue);

            if (!unmodified) {
                // ⊆, ⊇ become ~ after a modification
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
            Variable toReal = Util.firstRealVariable(link.to());
            Set<MethodInfo> causesOfModification = modifiedVariablesAndTheirCause.get(toReal);
            if (causesOfModification != null) {
                LinkNature ln = link.linkNature();
                if (ln.isIdenticalTo()
                    && link.to() instanceof FieldReference fr
                    && Util.isVirtualModificationField(fr.fieldInfo())
                    && (ln.pass().isEmpty() || !Collections.disjoint(ln.pass(), causesOfModification))) {
                    // x.§m ≡ y.§m
                    // pass = see Iterable, whose iterator() method is @Independent(hc = true, except = "remove")

                    // because we're processing the variables in order, adding to the map here provides the completion
                    modifiedVariablesAndTheirCause.put(builder.primary(), causesOfModification);
                    return false;
                }
                if (ln == CONTAINS_AS_FIELD
                    || ln == SHARES_FIELDS // see impl/TestInstanceOf,2
                    || ln == CONTAINS_AS_MEMBER) {
                    return false;
                }
                // the following rule is only valid for variables of non-abstract types (those that have no §m)
                // in particular, it is NOT valid for arrays and unbound type parameters
                if (ln == IS_ASSIGNED_TO) {
                    ParameterizedType pt = toReal.parameterizedType();
                    if (!Util.needsVirtual(pt)) {
                        Value.Immutable immutable = new AnalysisHelper().typeImmutable(pt);
                        if (!immutable.isAtLeastImmutableHC()) return false;
                    }
                }
            }
        }
        return true;
    }

}
