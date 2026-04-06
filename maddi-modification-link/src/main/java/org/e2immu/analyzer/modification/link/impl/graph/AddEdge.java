package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_FIELD_OF;

public class AddEdge {
    private static boolean mergeEdgeSingle(Map<Variable, Map<Variable, LinkNature>> graph,
                                           Variable from,
                                           LinkNature linkNature,
                                           Variable to) {
        if (from.equals(to)) return false; // safety measure, is technically possible
        Map<Variable, LinkNature> edges = graph.get(from);
        boolean change = false;
        if (edges == null) {
            edges = new HashMap<>();
            graph.put(from, edges);
            change = true;
        }
        LinkNature prev = edges.put(to, linkNature);
        if (prev == null) {
            change = true;
        } else {
            LinkNature combined = prev.combine(linkNature);
            if (combined != prev) {
                edges.put(to, combined);
            }
        }
        return change;
    }

    static boolean mergeEdgeBi(Map<Variable, Map<Variable, LinkNature>> graph, Edge edge) {
        boolean change = mergeEdgeSingle(graph, edge.from(), edge.linkNature(), edge.to());
        LinkNature ln2 = edge.linkNature().reverse();
        if (ln2 != edge.linkNature()) {
            change |= mergeEdgeSingle(graph, edge.to(), ln2, edge.from());
        }
        return change;
    }

    private static Variable fieldScopeRoot(Variable v) {
        if (v instanceof FieldReference fr) {
            if (fr.scopeVariable() instanceof This) return v;
            if (fr.scopeVariable() != null) return fieldScopeRoot(fr.scopeVariable());
        }
        return v;
    }

    private static boolean addField(Map<Variable, Map<Variable, LinkNature>> graph, Variable from, Variable primary) {
        if (!from.equals(primary) && !(primary instanceof This)
            && from instanceof FieldReference && primary.equals(fieldScopeRoot(from))) {
            boolean change = mergeEdgeSingle(graph, primary, CONTAINS_AS_FIELD, from);
            change |= mergeEdgeSingle(graph, from, IS_FIELD_OF, primary);
            return change;
        }
        return false;
    }

    static boolean simpleAddToGraph(Map<Variable, Map<Variable, LinkNature>> graph, Edge edge) {
        return simpleAddToGraph(graph, edge.from(), edge.linkNature(), edge.to());
    }

    static boolean simpleAddToGraph(Map<Variable, Map<Variable, LinkNature>> graph,
                                    Variable lFrom, LinkNature linkNature, Variable lTo) {
        boolean change = mergeEdgeSingle(graph, lFrom, linkNature, lTo);
        Variable primary = Util.primary(lFrom);
        change |= addField(graph, lFrom, primary);

        // other direction
        change |= mergeEdgeSingle(graph, lTo, linkNature.reverse(), lFrom);
        Variable toPrimary = Util.primary(lTo);
        change |= addField(graph, lTo, toPrimary);
        return change;
    }

}
