package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;

/*
 Ported from the pre-sv engine (openjdk branch, graph/RedundantLinks): a cross-variable, per-statement
 transitive-redundancy suppressor that did not survive the sv big-bang port. One instance lives for the duration
 of one WriteLinksAndModification.go() call (one statement); the guards accumulate over the per-variable
 extraction loop.

 For each nature GROUP (⊆/⊇/~ together, ← alone, → alone, ∈, ∋, ∩/≤/≥, ≺/≻/≈), a guard maps from → {to} of links
 already emitted for earlier variables in this statement. When a later variable's builder links to a target that
 is transitively REACHABLE through the guard from one of its other link targets, that link is redundant: keep the
 nearest hop, drop the origin. E.g. once 'stream.§xs ⊆ 0:in.§xs' has been emitted, 'stream1' keeps
 'stream1.§xs ⊆ stream.§xs' and drops the transitive 'stream1.§xs ⊆ 0:in.§xs'.
 */
public class RedundantLinks {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedundantLinks.class);

    // the guards are filled up as more calls to redundantLinks follow
    private final Map<LinkNature, Map<Variable, Set<Variable>>> completionGuard = new HashMap<>();

    /*
    FIXME (inherited from the original)
        this algorithm is not "stable" in the sense that the 'completions' map is overwritten for certain
        variables. Removing this overwrite (change it into a merge) produces wrong results (too many redundant vars)
        see TestStream,1
     */
    public void redundantLinks(Links.Builder builder) {
        Map<Variable, Set<Variable>> completions = new HashMap<>();
        builder.forEach(link -> {
            LinkNature key = key(link.linkNature());
            if (key != null) {
                Map<Variable, Set<Variable>> completionGuardForLn
                        = completionGuard.computeIfAbsent(key, _ -> new LinkedHashMap<>());
                Set<Variable> completion = completion(completionGuardForLn, link.to());
                Set<Variable> prev = completions.put(link.to(), completion);
                if (prev != null && !prev.isEmpty()) {
                    LOGGER.warn("Overwrite value of {}: {} -> {}", link.to(), prev, completion);
                }
            }
        });
        Set<Variable> redundantTo = new HashSet<>();
        for (Map.Entry<Variable, Set<Variable>> entry : completions.entrySet()) {
            redundantTo.addAll(entry.getValue());
        }
        builder.forEach(link -> {
            if (completions.containsKey(link.to())) {
                LinkNature key = key(link.linkNature());
                if (key != null) {
                    Map<Variable, Set<Variable>> completionGuardForLn
                            = completionGuard.computeIfAbsent(key, _ -> new LinkedHashMap<>());
                    Set<Variable> toSet = completionGuardForLn.get(link.to());
                    if (toSet == null || !toSet.contains(link.from())) {
                        completionGuardForLn.computeIfAbsent(link.from(), _ -> new HashSet<>())
                                .add(link.to());
                    }
                }
            }
        });
        builder.removeIf(link -> redundantTo.contains(link.to())
                                 // see TestModificationFunctional,2b
                                 && !(link.to() instanceof FunctionalInterfaceVariable)
                                 && !(link.to() instanceof AppliedFunctionalInterfaceVariable));
    }

    private static Set<Variable> completion(Map<Variable, Set<Variable>> graph, Variable start) {
        Set<Variable> result = new HashSet<>();
        completion(graph, result, start);
        return result;
    }

    private static void completion(Map<Variable, Set<Variable>> graph, Set<Variable> result, Variable start) {
        Set<Variable> targets = graph.get(start);
        if (targets != null) {
            for (Variable target : targets) {
                if (result.add(target)) {
                    completion(graph, result, target);
                }
            }
        }
    }

    // compute completions per group of link natures.
    private static LinkNature key(LinkNature ln) {
        if (SHARES_ELEMENTS.equals(ln) || IS_SUBSET_OF.equals(ln) || IS_SUPERSET_OF.equals(ln)) {
            return SHARES_ELEMENTS;
        }
        if (IS_ASSIGNED_FROM.equals(ln)) {
            return IS_ASSIGNED_FROM;
        }
        // see TestList,2 why IS_ASSIGNED_FROM cannot be merged with IS_ASSIGNED_TO
        if (IS_ASSIGNED_TO.equals(ln)) {
            return IS_ASSIGNED_TO;
        }
        if (IS_ELEMENT_OF.equals(ln)) {
            return IS_ELEMENT_OF;
        }
        // see TestMap,1b why CONTAINS_AS_MEMBER cannot be merged with IS_ELEMENT_OF
        if (CONTAINS_AS_MEMBER.equals(ln)) {
            return CONTAINS_AS_MEMBER;
        }
        if (OBJECT_GRAPH_OVERLAPS.equals(ln) || IS_IN_OBJECT_GRAPH.equals(ln) || OBJECT_GRAPH_CONTAINS.equals(ln)) {
            return OBJECT_GRAPH_OVERLAPS;
        }
        if (SHARES_FIELDS.equals(ln) || IS_FIELD_OF.equals(ln) || CONTAINS_AS_FIELD.equals(ln)) {
            return SHARES_FIELDS;
        }
        return null;
    }
}
