package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.e2immu.analyzer.modification.link.impl.ExpandHelper.followGraph;
import static org.e2immu.analyzer.modification.link.impl.ExpandHelper.makeGraph;
import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public record ExpandLocal(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandLocal.class);

    /*
    Write the links in terms of variables that we have, removing the temporarily created ones.
    Ensure that we use the variables and not their virtual fields.
    Ensure that v1 == v2 also means that v1.ts == v2.ts, v1.$m == v2.$m, so that these connections can be made.
    Filtering out ? links is done in followGraph.
     */
    public Map<Variable, Links> go(Map<Variable, Links> lvIn, VariableData previousVd, VariableData vd) {
        // copy everything into lv
        Map<Variable, Links> linkedVariables;
        if (previousVd == null) {
            linkedVariables = lvIn;
        } else {
            linkedVariables = new HashMap<>(lvIn);
            previousVd.variableInfoStream().forEach(vi -> {
                Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
                if (vLinks != null) {
                    linkedVariables.merge(vLinks.primary(), vLinks, Links::merge);
                }
            });
        }
        Map<Variable, Map<Variable, LinkNature>> graph = makeGraph(runtime, linkedVariables, true);
        LOGGER.debug("Bi-directional graph: {}", graph);
        Map<Variable, Links> newLinkedVariables = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links.Builder piBuilder = followGraph(graph, vi.variable(), null, true);
            piBuilder.removeIf(link -> link.toIntermediateVariable());
            newLinkedVariables.put(vi.variable(), piBuilder.build());
        });
        return newLinkedVariables;
    }

}
