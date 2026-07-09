package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.graph.Fact;
import org.e2immu.analyzer.modification.link.impl.linkgraph.FollowGraph;
import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
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
import java.util.stream.Stream;

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
        Set<Variable> unmarkedModifications = new HashSet<>(modifiedDuringEvaluation.keySet());
        Map<Variable, Links.Builder> newLinkedVariables = new HashMap<>();
        List<Link> toRemove = new ArrayList<>();

        Map<Variable, Set<MethodInfo>> expandedModifiedDuringEvaluation = new HashMap<>();
        for (Map.Entry<Variable, Set<MethodInfo>> entry : modifiedDuringEvaluation.entrySet()) {
            for (Variable v : followGraph.graph().allShared(entry.getKey())) {
                expandedModifiedDuringEvaluation.put(v, entry.getValue());
            }
        }

        for (VariableInfo vi : vd.variableInfoIterable(Stage.EVALUATION)) {
            toRemove.addAll(doVariableReturnRecompute(statement, vi, unmarkedModifications,
                    previouslyModified, expandedModifiedDuringEvaluation, newLinkedVariables));
        }
        /*
         toRemove now contains links that should change from ⊆, ⊇ to ~, see e.g. TestConstructor,1; TestDependent,1

         The complication is that we must update the graph, and some of the results, but not all, because some results
         are "before the modification", such as ∈ in a removeFirst() operation on a list (TestDependent,1),
         and the ⊆ to ~ is an "after the modification" operation.

         The solution here is to selectively remove the correct edges from the graph, and to rely
         on the 'best path' to solve the problem. TestDependent,2 shows the way. TestList2,2 shows a variant
         of the problem, with assignment order.
         */
        if (!toRemove.isEmpty()) {
            Set<Variable> affected = new HashSet<>();
            for (Link link : toRemove) {
                Set<Variable> set = followGraph.graph()
                        .replaceReturnAffected(link.from(), link.to(), link.linkNature(), SHARES_ELEMENTS);
                affected.add(link.from());
                affected.add(link.to());
                updateNewLinks(newLinkedVariables, link);
            }
            // assert !affected.isEmpty();
            followGraph.graph().recompute(affected, statement.source().index(), this::acceptRemoval);
        }
        Map<Variable, Links> builtNewLinkedVariables = new HashMap<>();
        int sum = newLinkedVariables.entrySet().stream().mapToInt(e -> {
            Links links = e.getValue().sort().build();
            builtNewLinkedVariables.put(e.getKey(), links);
            return links.size();
        }).sum();
        return new WriteResult(builtNewLinkedVariables, unmarkedModifications, sum);
    }

    // FIXME should be replaced by real code that goes as far as §m equivalence: anything @Dependent
    //  should be removed
    private boolean acceptRemoval(Fact<Variable, LinkNature> fact) {
        return fact.label() == IS_SUBSET_OF || fact.label() == IS_SUPERSET_OF;
    }

    private void updateNewLinks(Map<Variable, Links.Builder> newLinkedVariables, Link toUpdate) {
        Variable primary = Util.primary(toUpdate.from());
        Links.Builder builder = newLinkedVariables.computeIfAbsent(primary, _ -> new LinksImpl.Builder(primary));
        builder.replace(toUpdate, SHARES_ELEMENTS);
    }

    /*
    It is possible that 2 real variables are represented by one shared variable.
    It is equally possible that one real variable refers is composed of multiple shared variables.

    On top of that, we add the relevant modification links kept in VirtualModificationIdenticals.
    */
    private List<Link> doVariableReturnRecompute(Statement statement,
                                                 VariableInfo vi,
                                                 Set<Variable> unmarkedModifications,
                                                 Set<Variable> previouslyModified,
                                                 Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation,
                                                 Map<Variable, Links.Builder> newLinkedVariables) {
        Variable variable = vi.variable();
        unmarkedModifications.remove(variable);

        Links.Builder builder2;
        // step one, multiple real variables map onto a single shared variable
        Variable toFollow = followGraph.graph().translateForward(variable);
        Links.Builder builder1 = followGraph.followGraph(virtualFieldComputer, toFollow);
        if (toFollow != variable) {
            builder2 = new LinksImpl.Builder(variable);
            builder1.linkSet().forEach(link -> {
                VariableTranslationMap vtm = new VariableTranslationMap(runtime);
                vtm.put(toFollow, variable);
                Link translated = link.translateFrom(vtm);
                builder2.add(translated.from(), translated.linkNature(), translated.to());
            });
        } else {
            builder2 = builder1;
        }

        Links.Builder builder = new LinksImpl.Builder(builder2.primary());
        // components of the variable can also be part of the shared variables...
        for (Link link : builder2.linkSet()) {
            iterateOverShared(link.from()).forEach(from -> iterateOverShared(link.to())
                    .forEach(to -> {
                        // expanding a shared-variable rep on the to-side can surface an internal self-field link
                        // (e.g. 'choose ≻ $__sv_s1' → 'choose ≻ choose.s1') that FollowGraph's internal-reference
                        // filter would have dropped had the member not been masked by the rep. Re-apply it here.
                        if (!isInternalSelfFieldLink(from, to)) {
                            builder.add(from, link.linkNature(), to);
                        }
                    }));
        }
        // reconstruct intra-group shared-variable assignment edges (the collapse stores 'field ← param' once;
        // e.g. a record constructor's this.s1 ← 0:s1). Unlike the §m fold below we do not gate on
        // containsPrimaryOf: the summary of a both-fresh group has no external edge yet to anchor to.
        followGraph.graph().sharedAssignmentEdgeStream(variable)
                .filter(link -> !builder.contains(link.from(), link.linkNature(), link.to()))
                .forEach(link -> builder.add(link.from(), link.linkNature(), link.to()));
        // finally, modification edges
        followGraph.graph().virtualModificationEdgeStream(variable)
                .filter(link -> builder.containsPrimaryOf(link.to()))
                .filter(link -> !builder.contains(link.from(), link.linkNature(), link.to()))
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
                           && notLinkedToModifiedVirtualModification(variable, toFollow, modifiedInThisEvaluation)
                           // and other links such as ≺ IS_FIELD_OF
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
                        toRemove.add(new LinksImpl.LinkImpl(link.to(), link.linkNature().reverse(), link.from()));
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

    // mirrors the internal-reference filter in FollowGraph.followGraph: a link between two variables sharing the
    // same primary, where one is (part of) the primary itself, is an internal self-reference and not emitted.
    private static boolean isInternalSelfFieldLink(Variable from, Variable to) {
        Variable primaryFrom = Util.primary(from);
        Variable primaryTo = Util.primary(to);
        if (primaryFrom == null || !primaryFrom.equals(primaryTo)) return false;
        Variable firstRealFrom = Util.firstRealVariable(from);
        Variable firstRealTo = Util.firstRealVariable(to);
        boolean crossField = !firstRealFrom.equals(primaryFrom)
                             && !firstRealTo.equals(primaryTo)
                             && !firstRealFrom.equals(firstRealTo);
        return !crossField;
    }

    private Stream<Variable> iterateOverShared(Variable variable) {
        if (variable instanceof SharedVariable sv) {
            return sv.variables().stream();
        }
        if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
            return iterateOverShared(fr.scopeVariable())
                    .map(scope -> scope == fr.scopeVariable()
                            ? fr
                            : runtime.newFieldReference(fr.fieldInfo(),
                            runtime.newVariableExpression(scope), fr.parameterizedType()));
        }
        return Stream.of(variable);
    }

    private static boolean assignedInThisStatement(Statement statement, VariableInfo vi) {
        String index = statement.source().index();
        return vi.assignments().contains(index) && !vi.reads().indices().contains(index);
    }

    private boolean notLinkedToModifiedVirtualModification(Variable variable,
                                                           Variable toFollow,
                                                           Map<Variable, Set<MethodInfo>> modifiedVariablesAndTheirCause) {
        return followGraph.graph().eqVariables(variable).noneMatch(modifiedVariablesAndTheirCause::containsKey);
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
