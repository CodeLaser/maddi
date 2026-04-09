package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.linkgraph.FollowGraph;
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

import java.util.*;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

class WriteLinksAndModification {
    private final Runtime runtime;
    private final VirtualFieldComputer virtualFieldComputer;
    private final FollowGraph followGraph;

    WriteLinksAndModification(JavaInspector javaInspector,
                              VirtualFieldComputer virtualFieldComputer,
                              FollowGraph followGraph) {
        this.runtime = javaInspector.runtime();
        this.virtualFieldComputer = virtualFieldComputer;
        this.followGraph = followGraph;
    }

    record WriteResult(Map<Variable, Links> newLinks, Set<Variable> modifiedOutsideVariableData, int newLinksSize) {
    }

    @NotNull WriteResult go(Statement statement,
                            VariableData vd,
                            Set<Variable> previouslyModified,
                            Map<Variable, Set<MethodInfo>> modifiedDuringEvaluation) {
        int infiniteLoopProtection = 0;
        while (true) {
            Set<Variable> unmarkedModifications = new HashSet<>(modifiedDuringEvaluation.keySet());
            Map<Variable, Links.Builder> newLinkedVariables = new HashMap<>();
            List<Link> toRemove = new ArrayList<>();

            for (VariableInfo vi : vd.variableInfoIterable(Stage.EVALUATION)) {
                toRemove.addAll(doVariableReturnRecompute(statement, vi, unmarkedModifications,
                        previouslyModified, modifiedDuringEvaluation, newLinkedVariables));
            }
            // toRemove now contains links that should change from ⊆, ⊇ to ~
            // when empty, we can complete the building process, and return a result
            if (toRemove.isEmpty()) {
                Map<Variable, Links> builtNewLinkedVariables = new HashMap<>();
                int sum = newLinkedVariables.entrySet().stream().mapToInt(e -> {
                    Links links = e.getValue().build();
                    builtNewLinkedVariables.put(e.getKey(), links);
                    return links.size();
                }).sum();
                return new WriteResult(builtNewLinkedVariables, unmarkedModifications, sum);
            }
            // when not empty, we should remove and recompute the links, and try again
            // see e.g. TestConstructor,1
            Set<Variable> affected = new HashSet<>();
            for (Link link : toRemove) {
                // not only convert ⊆ to ~
                Set<Variable> set = followGraph.graph()
                        .replaceReturnAffected(link.from(), link.to(), link.linkNature(), SHARES_ELEMENTS);
                affected.addAll(set);
                // but also the reverse link ⊇ to ~
                Set<Variable> set2 = followGraph.graph()
                        .replaceReturnAffected(link.to(), link.from(), link.linkNature().reverse(), SHARES_ELEMENTS);
                affected.addAll(set2);
            }
            assert !affected.isEmpty();
            followGraph.graph().recompute(affected, statement.source().index());
            if (infiniteLoopProtection++ > 5) throw new UnsupportedOperationException();
        }
    }

    private List<Link> doVariableReturnRecompute(Statement statement,
                                                 VariableInfo vi,
                                                 Set<Variable> unmarkedModifications,
                                                 Set<Variable> previouslyModified,
                                                 Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation,
                                                 Map<Variable, Links.Builder> newLinkedVariables) {
        Variable variable = vi.variable();
        unmarkedModifications.remove(variable);

        Links.Builder builder = followGraph.followGraph(virtualFieldComputer, variable);
        followGraph.graph().equivalentEdgesStream(variable)
                .sorted(Link::compareTo)
                .forEach(link -> builder.add(link.from(), link.linkNature(), link.to()));
        List<Link> toRemove = new ArrayList<>();
        if (variable instanceof ReturnVariable rv) {
            // return variables will always be complete
            handleReturnVariable(rv, builder);
        } else {
            boolean unmodified =
                    variable.isIgnoreModifications()
                    ||
                    !previouslyModified.contains(variable)
                    && (assignedInThisStatement(statement, vi)
                        || !modifiedInThisEvaluation.containsKey(variable)
                           // all the §m links
                           && notLinkedToModified(builder, modifiedInThisEvaluation));
            builder.removeIf(WriteLinksAndModification::notInLinkedVariables);

            if (variable instanceof This) {
                // only keep direct links for "this", the others are replicated in its fields
                builder.removeIf(l -> !(l.from() instanceof This));
            }
            Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
            vi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, newValue);

            if (!unmodified) {
                // ⊆, ⊇ become ~ after a modification
                builder.linkSet().forEach(link -> {
                    if (link.linkNature() == IS_SUBSET_OF || link.linkNature() == IS_SUPERSET_OF) {
                        toRemove.add(link);
                    }
                });
            }
        }
        if (newLinkedVariables.put(variable, builder) != null) {
            throw new UnsupportedOperationException("Each real variable must be a primary");
        }
        return toRemove;
    }

    private static boolean notInLinkedVariables(Link l) {
        return Util.lvPrimaryOrNull(l.to()) instanceof IntermediateVariable
               || l.to() instanceof MarkerVariable mv && mv.isConstant()
                  && !(l.linkNature().equals(IS_ASSIGNED_FROM) || l.linkNature().equals(CONTAINS_AS_MEMBER))
               || l.from() instanceof MarkerVariable mvf && mvf.isConstant()
                  && !(l.linkNature().equals(IS_ASSIGNED_TO) || l.linkNature().equals(IS_ELEMENT_OF));
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
