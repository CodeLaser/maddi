package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;

public class RedundantLinks {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedundantLinks.class);

    // the guards are filled up as more calls to redundant(Modification)Links follow
    private final Map<Variable, Set<Variable>> modificationCompletionGuard = new LinkedHashMap<>();
    private final Map<LinkNature, Map<Variable, Set<Variable>>> completionGuard = new HashMap<>();
    private final Timer timer;

    public RedundantLinks(Timer timer) {
        this.timer = timer;
    }

    /*
    FIXME
        this algorithm is not "stable" in the sense that the 'completions' map is overwritten for certain
        variables. Removing this overwrite (change it into a merge) produces wrong results (too many redundant vars)
        see TestStream,1
     */
    // concept copied from computeRedundantModificationLinks, but now for groups of links
    public void redundantLinks(Links.Builder builder) {
        timer.start("redundant1");
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
        timer.end("redundant1");
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

    // we already have v2.§m -> {v1.§m}, v3.§m->{v1.§m, v2.§m}, and now we want to add
    // v4.§m -> v3.§m, -> v1.§m, -> v2.§m.
    // we only need keep add the first link.
    public Set<Variable> modificationLinks(Links.Builder builder,
                                           Map<Variable, Set<MethodInfo>> modifiedVariablesAndTheirCause) {
        Map<Variable, Set<Variable>> completions = new HashMap<>();
        builder.forEach(link -> {
            LinkNature ln = link.linkNature();
            if (Util.isVirtualModification(link.to()) && ln.isIdenticalTo()) {
                boolean accept;
                if (ln.pass().isEmpty()) {
                    accept = true;
                } else {
                    Variable toReal = Util.firstRealVariable(link.to());
                    Set<MethodInfo> causesOfModification = modifiedVariablesAndTheirCause.get(toReal);
                    accept = causesOfModification == null || !Collections.disjoint(ln.pass(), causesOfModification);
                }
                if (accept) {
                    completions.put(link.to(), completion(modificationCompletionGuard, link.to()));
                }
            }
        });
        Set<Variable> redundantTo = new HashSet<>();
        for (Map.Entry<Variable, Set<Variable>> entry : completions.entrySet()) {
            redundantTo.addAll(entry.getValue());
        }
        builder.forEach(link -> {
            if (completions.containsKey(link.to())) {
                Set<Variable> toSet = modificationCompletionGuard.get(link.to());
                if (toSet == null || !toSet.contains(link.from())) {
                    modificationCompletionGuard.computeIfAbsent(link.from(), _ -> new HashSet<>())
                            .add(link.to());
                }
            }
        });
        builder.removeIf(link -> redundantTo.contains(link.to()));
        return redundantTo.stream().map(Util::firstRealVariable).collect(Collectors.toUnmodifiableSet());
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
