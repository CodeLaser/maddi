package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LinkGraph {
    Runtime runtime;
    boolean checkDuplicateNames;
    Graph graph;
    MakeGraph makeGraph;

    public LinkGraph(Runtime runtime,
                     boolean checkDuplicateNames,
                     Graph graph,
                     MakeGraph makeGraph) {
        this.checkDuplicateNames = checkDuplicateNames;
        this.graph = graph;
        this.makeGraph = makeGraph;
        this.runtime = runtime;
    }

    public Graph graph() {
        return graph;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkGraph.class);

    // see TestModificationParameter, a return variable with the same name as a local variable
    private static String stringForDuplicate(Variable v) {
        if (v instanceof ReturnVariable) return "rv " + v;
        if (v instanceof ParameterInfo pi) return pi.methodInfo().name() + ":" + pi.index();
        if (v instanceof DependentVariable dv && dv.indexVariable() != null) {
            return stringForDuplicate(dv.arrayVariable()) + "[" + stringForDuplicate(dv.indexVariable()) + "]";
        }
        if (v instanceof FieldReference fr && fr.scopeVariable() != null) {
            return stringForDuplicate(fr.scopeVariable()) + "." + fr.fieldInfo().name();
        }
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
        Set<Variable> allToRemove = graph.variables().stream()
                .filter(v -> toRemove.contains(Util.firstRealVariable(v)))
                .collect(Collectors.toUnmodifiableSet());
        if (!allToRemove.isEmpty()) {
            graph.remove(allToRemove);
        }
        Set<Variable> allToRemove2 = graph.eqVariables()
                .filter(v -> toRemove.contains(Util.firstRealVariable(v)))
                .collect(Collectors.toUnmodifiableSet());
        if (!allToRemove2.isEmpty()) {
            graph.removeEquivalence(allToRemove2);
        }

        Reduce r = reduceLinks(newLinks);
        if (r != null && r.clear != null) {
            graph.clear(r.clear, statementIndex);
        }
        Map<Variable, Links> newLinks2 = r == null ? newLinks : r.newLinks;
        newLinks2.entrySet()
                .stream()
                .filter(e -> !(e.getKey() instanceof This))
                .filter(e -> e.getValue().primary() != null)
                .sorted(Map.Entry.comparingByKey(Variable::compareTo))
                .forEach(e -> e.getValue().translate(replaceConstants).forEach(link -> {
                    graph.simpleAddToGraph(link.from(), link.linkNature(), link.to(), statementIndex,
                            link.mediated());
                }));

        boolean change = true;
        int cycleProtection = 0;
        while (change) {
            ++cycleProtection;
            if (cycleProtection > 30) {
                // NOTE: a class in the maddi code base needs >10 rounds, camel's generated bulk-converter
                // loaders >20. Keeping the partial graph is NOT an option (its size makes everything
                // downstream minutes-slow: measured 5s -> 30min on camel-base); the throw is caught at the
                // method level, which degrades to a SHALLOW summary (LinkComputerImpl.doMethod).
                throw new UnsupportedOperationException("cycle protection");
            }
            change = makeGraph.expandGraph(statementIndex, modifiedInThisEvaluation.keySet());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bi-directional graph for local:\n{}\n{}", graph.engine().printClosure(),
                    graph.printEquivalence(LinkGraph::vertexPrinter));
        }
        assert !checkDuplicateNames ||
               graph.size() == graph.variables().stream().map(LinkGraph::stringForDuplicate).distinct().count();
    }

    private final Set<IntermediateVariable> intermediateVariablesRemoved = new HashSet<>();

    private record Reduce(Map<Variable, Links> newLinks, Variable clear) {
    }

    private Reduce reduceLinks(Map<Variable, Links> newLinks) {
        Variable source = null; // iis ← $__c0  == target ← source
        Variable target = null;
        Links skip = null;
        VariableTranslationMap vtm = new VariableTranslationMap(runtime);
        for (Map.Entry<Variable, Links> entry : newLinks.entrySet()) {
            Links links = entry.getValue();
            if (links.size() == 1) {
                Link link = links.link(0);
                if (link.linkNature() == LinkNatureImpl.IS_ASSIGNED_FROM
                    && link.to() instanceof IntermediateVariable iv
                    && intermediateVariablesRemoved.add(iv)) {
                    source = iv;
                    target = link.from();
                    skip = links;
                    vtm.put(source, target);
                    break;
                }
            }
        }
        if (source == null) return null;
        Map<Variable, Links> res = new HashMap<>();
        for (Map.Entry<Variable, Links> entry : newLinks.entrySet()) {
            if (skip != entry.getValue()) {
                Variable v = entry.getKey() == source ? target : entry.getKey();
                Links links = entry.getValue().translate(vtm);
                res.put(v, links);
            } else if (source instanceof IntermediateVariable iv && iv.isNewObject()
                       && Util.primary(target) instanceof ReturnVariable) {
                // the reduced pair 'method ← $__c(new URL(...))' carries the FRESH-OBJECT fact: a return
                // value that is a fresh (unanalyzable) object must not end up looking like a parameter
                // identity. Record it side-band for handleReturnVariable's '← $_v' marker — NOT as a graph
                // edge: a shared $_v graph vertex acts as a closure hub (TestParSeqLinkBench 0.9s -> 3-19s).
                graph.markFreshObjectReturn(Util.primary(target));
            }
        }
        return new Reduce(res, target);
    }

    public static String vertexPrinter(Variable variable) {
        if (variable instanceof ParameterInfo pi) {
            return pi.isUnnamed() ? "_" : pi.index() + ":" + pi.name();
        }
        if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
            return vertexPrinter(fr.scopeVariable()) + "." + fr.fieldInfo().name();
        }
        return variable.simpleName();
    }

}
