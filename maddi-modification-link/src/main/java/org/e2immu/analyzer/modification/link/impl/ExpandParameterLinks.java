package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.e2immu.analyzer.modification.link.impl.ExpandHelper.followGraph;
import static org.e2immu.analyzer.modification.link.impl.ExpandHelper.mergeEdge;
import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public record ExpandParameterLinks(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandParameterLinks.class);

    public List<Links> go(MethodInfo methodInfo, VariableData vd) {

        /*
        why a bidirectional graph for the parameters, and only a directional one for the return value?
        the flow is obviously linear parm -> rv, so if we want rv in function of parameters, only one
        direction is relevant. But if we want the parameters in function of the fields, we may have
        to see the reverse of param -> field. See e.g. TestList,4 (set)
         */
        Map<Variable, Map<Variable, LinkNature>> graph = makeBiDirectionalGraph(vd);
        LOGGER.debug("Bi-directional graph: {}", graph);

        List<Links> linksPerParameter = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            Links.Builder piBuilder = followGraph(graph, pi, null);
            linksPerParameter.add(piBuilder.build());
        }

        return linksPerParameter;
    }

    private static Map<Variable, Map<Variable, LinkNature>> makeBiDirectionalGraph(VariableData vd) {
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                vLinks.links().forEach(l -> {
                    mergeEdge(graph, l.from(), l.linkNature(), l.to());
                    mergeEdge(graph, l.to(), l.linkNature().reverse(), l.from());
                });
            }
        });
        return graph;
    }
}
