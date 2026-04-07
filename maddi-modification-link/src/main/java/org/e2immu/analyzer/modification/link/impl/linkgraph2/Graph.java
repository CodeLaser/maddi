package org.e2immu.analyzer.modification.link.impl.linkgraph2;

import org.e2immu.analyzer.modification.link.impl.graph2.IncrementalFixpointEngine;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;
import java.util.Set;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_FIELD_OF;

public record Graph(IncrementalFixpointEngine<Variable, LinkNature> engine) {
    public boolean containsVariable(Variable primary) {
        return engine.vertices().contains(primary);
    }

    public Iterable<Map.Entry<Variable, Map<Variable, LinkNature>>> edges() {
        return engine.edges();
    }

    private boolean mergeEdgeSingle(Variable from, LinkNature linkNature, Variable to) {
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        return engine.addEdge(from, to, linkNature).newFacts() > 0;
    }

    boolean mergeEdgeBi(Edge edge) {
        Variable from = edge.from();
        Variable to = edge.to();
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        LinkNature ln = edge.linkNature();
        LinkNature rev = edge.linkNature().reverse();
        int newFacts1 = engine.addEdge(from, to, ln).newFacts();
        int newFacts2 = engine.addEdge(to, from, rev).newFacts();
        return newFacts1 + newFacts2 > 0;
    }

    static Variable fieldScopeRoot(Variable v) {
        if (v instanceof FieldReference fr) {
            if (fr.scopeVariable() instanceof This) return v;
            if (fr.scopeVariable() != null) return fieldScopeRoot(fr.scopeVariable());
        }
        return v;
    }

    boolean addField(Variable from, Variable primary) {
        if (!from.equals(primary) && !(primary instanceof This)
            && from instanceof FieldReference && primary.equals(fieldScopeRoot(from))) {
            boolean change = mergeEdgeSingle(primary, CONTAINS_AS_FIELD, from);
            change |= mergeEdgeSingle(from, IS_FIELD_OF, primary);
            return change;
        }
        return false;
    }

    public void remove(Set<Variable> toRemove) {
        engine.removeVertices(toRemove);
    }

    boolean simpleAddToGraph(Edge edge) {
        return simpleAddToGraph(edge.from(), edge.linkNature(), edge.to());
    }

    boolean simpleAddToGraph(Variable lFrom, LinkNature linkNature, Variable lTo) {
        boolean change = mergeEdgeSingle(lFrom, linkNature, lTo);
        Variable primary = Util.primary(lFrom);
        change |= addField(lFrom, primary);

        // other direction
        change |= mergeEdgeSingle(lTo, linkNature.reverse(), lFrom);
        Variable toPrimary = Util.primary(lTo);
        change |= addField(lTo, toPrimary);
        return change;
    }

    public int size() {
        return variables().size();
    }

    Set<Variable> variables() {
        return engine.vertices();
    }

    Map<Variable, LinkNature> closure(Variable variable) {
        return engine.closure(variable);
    }
}
