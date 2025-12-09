package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.ExpandHelper.*;
import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public record ExpandReturnValueLinks(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandReturnValueLinks.class);

    /*
     Prepares the links of the return value for the outside world:
     - find as many links to fields and parameters
     - remove (intermediate) links to local variables
     */
    public Links go(ReturnVariable returnVariable, Links links, LinkedVariables extra, VariableData vd) {
        Variable primary = links.primary();
        if (primary == null) return LinksImpl.EMPTY;
        TranslationMap tm = runtime.newTranslationMapBuilder().put(primary, returnVariable).build();

        Map<Variable, Map<Variable, LinkNature>> graph = makeGraph(links, extra, vd);
        LOGGER.debug("Return graph: {}", graph);

        Links.Builder rvBuilder = followGraph(graph, primary, tm, false);

        if (containsNoLocalVariable(primary)) {
            rvBuilder.add(LinkNature.IS_IDENTICAL_TO, primary);
        }
        return rvBuilder.build();
    }

    private static Map<Variable, Map<Variable, LinkNature>> makeGraph(Links links,
                                                                      LinkedVariables extra,
                                                                      VariableData vd) {
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        Stream<Link> stream = Stream.concat(links.links().stream(),
                extra.map().values().stream().flatMap(l -> l.links().stream()));
        stream.forEach(link -> mergeEdge(graph, link.from(), link.linkNature(), link.to()));

        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                vLinks.links().forEach(l -> mergeEdge(graph, l.from(), l.linkNature(), l.to()));
            }
        });

        return graph;
    }

}
