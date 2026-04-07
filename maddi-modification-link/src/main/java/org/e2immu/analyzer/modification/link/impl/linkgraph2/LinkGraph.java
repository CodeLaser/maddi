package org.e2immu.analyzer.modification.link.impl.linkgraph2;

import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public record LinkGraph(JavaInspector javaInspector,
                        Runtime runtime,
                        boolean checkDuplicateNames,
                        Graph graph,
                        MakeGraph makeGraph,
                        FollowGraph followGraph) {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkGraph.class);

    // see TestModificationParameter, a return variable with the same name as a local variable
    private static String stringForDuplicate(Variable v) {
        if (v instanceof ReturnVariable) return "rv " + v;
        return v.toString();
    }

    //-------------------------------------------------------------------------------------------------

    /*
     Write the links in terms of variables that we have, removing the temporarily created ones.
     Ensure that we use the variables and not their virtual fields.
     Ensure that v1 == v2 also means that v1.ts == v2.ts, v1.$m == v2.$m, so that these connections can be made.
     Filtering out ? links is done in followGraph.
     */
    public void compute(String statementIndex,
                        Map<Variable, Links> newLinks,
                        Set<Variable> toRemove,
                        TranslationMap replaceConstants,
                        Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation) {
        graph.remove(toRemove);
        newLinks.entrySet().stream()
                .filter(e -> !(e.getKey() instanceof This))
                .filter(e -> e.getValue().primary() != null)
                .forEach(e -> e.getValue().translate(replaceConstants).forEach(link -> {
                    graph.simpleAddToGraph(link.from(), link.linkNature(), link.to(), statementIndex);
                }));

        boolean change = true;
        int cycleProtection = 0;
        while (change) {
            ++cycleProtection;
            if (cycleProtection > 20) {
                // NOTE: there is a class that requires more than 10 cycles in the maddi code base...
                throw new UnsupportedOperationException("cycle protection");
            }
            change = makeGraph.doOneMakeGraphCycle(statementIndex, modifiedInThisEvaluation.keySet());
        }
        assert !checkDuplicateNames ||
               graph.size() == graph.variables().stream().map(LinkGraph::stringForDuplicate).distinct().count();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bi-directional graph for local:\n{}", graph.engine()
                    .printClosure(LinkGraph::vertexPrinter, Variable::compareTo));
        }
    }

    private static String vertexPrinter(Variable variable) {
        if (variable instanceof ParameterInfo pi) {
            return pi.isUnnamed() ? "_" : pi.index() + ":" + pi.name();
        }
        return variable.simpleName();
    }

    // indirection in applied functional interface variable
    // TODO this is shaky code, dedicated to TestStaticBiFunction,6
    //  it may have relevance later
    public Links indirect(Variable primary, Link link, Links links2) {
       /* Variable v = links2.primary();
        Variable pi = Util.primary(link.to());
        TranslationMap tm2 = new VariableTranslationMap(runtime).put(v, pi);
        Links links2Tm = links2.translate(tm2);
        Map<Variable, Links> linkedVariables = new HashMap<>();
        Links links = new LinksImpl(primary, List.of(link));
        linkedVariables.put(links.primary(), links);
        linkedVariables.put(links2Tm.primary(), links2Tm);
        Map<Variable, Map<Variable, LinkNature>> graph = makeGraph(linkedVariables, Set.of());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Indirection graph, primary {}:\n{}", links.primary(), printGraph(graph));
        }
        Links.Builder builder = followGraph.followGraph(null, graph, links.primary());
        builder.removeIf(l -> l.to().variableStreamDescend().anyMatch(vv -> vv instanceof ParameterInfo));
        return builder.build();*/
        return LinksImpl.EMPTY; // FIXME
    }

}
