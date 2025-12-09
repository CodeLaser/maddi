package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.e2immu.analyzer.modification.link.impl.ExpandHelper.followGraph;
import static org.e2immu.analyzer.modification.link.impl.ExpandHelper.mergeEdge;
import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public record ExpandLocal(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandLocal.class);

    /*
    Write the links in terms of variables that we have, removing the temporarily created ones.
    Ensure that we use the variables and not their virtual fields.
    Ensure that v1 == v2 also means that v1.ts == v2.ts, v1.$m == v2.$m, so that these connections can be made.
    Filtering out ? links is done in followGraph.
     */
    public Map<Variable, Links> go(Map<Variable, Links> linkedVariables, VariableData vd) {
        Map<Variable, Map<Variable, LinkNature>> graph = makeBiDirectionalGraph(linkedVariables, vd);
        LOGGER.debug("Bi-directional graph: {}", graph);
        Map<Variable, Links> newLinkedVariables = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links.Builder piBuilder = followGraph(graph, vi.variable(), null, true);
            //piBuilder.removeIf(link -> !vd.isKnown(link.to().fullyQualifiedName()));
            newLinkedVariables.put(vi.variable(), piBuilder.build());
        });
        return newLinkedVariables;
    }

    private Map<Variable, Map<Variable, LinkNature>> makeBiDirectionalGraph(Map<Variable, Links> linkedVariables,
                                                                            VariableData vd) {
        Map<Variable, Set<Variable>> subs = new HashMap<>();
        linkedVariables.values().forEach(links -> links.links().forEach(l -> {
            if (!l.from().equals(links.primary()))
                subs.computeIfAbsent(links.primary(), _ -> new HashSet<>()).add(l.from());
        }));
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                vLinks.links().forEach(l -> addToGraph(l, graph, subs));
            }
        });
        linkedVariables.values().forEach(links -> links.links().forEach(l -> addToGraph(l, graph, subs)));
        return graph;
    }

    private void addToGraph(Link l, Map<Variable, Map<Variable, LinkNature>> graph, Map<Variable, Set<Variable>> subs) {
        mergeEdge(graph, l.from(), l.linkNature(), l.to());
        mergeEdge(graph, l.to(), l.linkNature().reverse(), l.from());
        if (l.linkNature() == LinkNature.IS_IDENTICAL_TO) {
            Set<Variable> subsOfFrom = subs.get(l.from());
            if (subsOfFrom != null) {
                subsOfFrom.forEach(s ->
                        mergeEdge(graph, s, LinkNature.IS_IDENTICAL_TO, makeComparableSub(l.from(), s, l.to())));
            }
            Set<Variable> subsOfTo = subs.get(l.to());
            if (subsOfTo != null) {
                subsOfTo.forEach(s ->
                        mergeEdge(graph, makeComparableSub(l.to(), s, l.from()), LinkNature.IS_IDENTICAL_TO, s));
            }
        }
    }

    private Variable makeComparableSub(Variable base, Variable sub, Variable target) {
        if (sub instanceof FieldReference fr && base.equals(fr.scopeVariable())) {
            return runtime.newFieldReference(fr.fieldInfo(), runtime.newVariableExpression(target), fr.fieldInfo().type());
        }
        throw new UnsupportedOperationException("More complex subbing, to be implemented");
    }
}
