package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
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

import java.util.*;

public record LinkGraph(JavaInspector javaInspector, Runtime runtime, boolean checkDuplicateNames,
                        Timer timer, FollowGraph followGraph) {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkGraph.class);

    // see TestModificationParameter, a return variable with the same name as a local variable
    private static String stringForDuplicate(Variable v) {
        if (v instanceof ReturnVariable) return "rv " + v;
        return v.toString();
    }

    public Map<Variable, Map<Variable, LinkNature>> makeGraph(Map<Variable, Links> linkedVariables,
                                                              Set<Variable> modifiedInThisEvaluation) {
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        linkedVariables.values().forEach(links -> links.forEach(
                l -> AddEdge.simpleAddToGraph(graph, l.from(), l.linkNature(), l.to())));
        boolean change = true;
        int cycleProtection = 0;
        while (change) {
            ++cycleProtection;
            if (cycleProtection > 20) {
                // NOTE: there is a class that requires more than 10 cycles in the maddi code base...
                throw new UnsupportedOperationException("cycle protection");
            }
            change = new MakeGraph(javaInspector, runtime, timer).doOneMakeGraphCycle(graph, modifiedInThisEvaluation);
        }
        assert !checkDuplicateNames ||
               graph.size() == graph.keySet().stream().map(LinkGraph::stringForDuplicate).distinct().count();
        return graph;
    }


    //-------------------------------------------------------------------------------------------------

    public static String printGraph(Map<Variable, Map<Variable, LinkNature>> graph) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Variable, Map<Variable, LinkNature>> e : graph.entrySet()) {
            for (Map.Entry<Variable, LinkNature> e2 : e.getValue().entrySet()) {
                sb.append(Util.simpleName(e.getKey())).append(" ").append(e2.getValue()).append(" ")
                        .append(Util.simpleName(e2.getKey())).append("\n");
            }
        }
        return sb.toString();
    }

    //-------------------------------------------------------------------------------------------------

    /*
     Write the links in terms of variables that we have, removing the temporarily created ones.
     Ensure that we use the variables and not their virtual fields.
     Ensure that v1 == v2 also means that v1.ts == v2.ts, v1.$m == v2.$m, so that these connections can be made.
     Filtering out ? links is done in followGraph.
     */
    public Map<Variable, Map<Variable, LinkNature>> compute(Map<Variable, Links> lvIn,
                                                            VariableData previousVd,
                                                            Stage stageOfPrevious,
                                                            VariableData vd,
                                                            TranslationMap replaceConstants,
                                                            Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation) {
        // copy everything into lv
        Map<Variable, Links> linkedVariables = new HashMap<>();
        lvIn.entrySet().stream()
                .filter(e -> !(e.getKey() instanceof This))
                .filter(e -> e.getValue().primary() != null)
                .forEach(e -> linkedVariables.put(e.getKey(), e.getValue().translate(replaceConstants)));
        if (previousVd != null) {
            previousVd.variableInfoStream(stageOfPrevious)
                    .filter(vi -> !(vi.variable() instanceof This))
                    .filter(vi -> vd.isKnown(vi.variable().fullyQualifiedName()))
                    .forEach(vi -> {
                        Links vLinks = vi.linkedVariables();
                        if (vLinks != null && vLinks.primary() != null) {
                            assert !(vLinks.primary() instanceof This);
                            Links translated = vLinks.translate(replaceConstants)
                                    .removeIfTo(v -> {
                                        Variable primary = Util.primary(v);
                                        // primary == null check: TestVarious,10
                                        return primary == null || !vd.isKnown(primary.fullyQualifiedName())
                                                                  && !LinkVariable.acceptForLinkedVariables(v);
                                    });
                            linkedVariables.merge(vLinks.primary(), translated, Links::merge);
                        }
                    });
        }

        Map<Variable, Map<Variable, LinkNature>> graph = makeGraph(linkedVariables, modifiedInThisEvaluation.keySet());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bi-directional graph for local:\n{}", printGraph(graph));
        }
        return graph;
    }

    // indirection in applied functional interface variable
    // TODO this is shaky code, dedicated to TestStaticBiFunction,6
    //  it may have relevance later
    public Links indirect(Variable primary, Link link, Links links2) {
        Variable v = links2.primary();
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
        return builder.build();
    }

}
