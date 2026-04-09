package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.graph.IncrementalFixpointEngine;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_FIELD_OF;

public record Graph(IncrementalFixpointEngine<Variable, LinkNature> engine) {
    public boolean containsVariable(Variable primary) {
        return engine.vertices().contains(primary);
    }

    public Iterable<Map.Entry<Variable, Map<Variable, LinkNature>>> edges() {
        return engine.edges();
    }

    private boolean invalidEdge(Variable from, LinkNature label, Variable to) {
        if (Util.isVirtualModification(from) != Util.isVirtualModification(to)) return true;
        return Util.virtual(from) != Util.virtual(to)
               && label != LinkNatureImpl.CONTAINS_AS_MEMBER
               && label != LinkNatureImpl.IS_ELEMENT_OF;
    }

    private boolean mergeEdgeSingle(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        if (invalidEdge(from, linkNature, to)) return false;
        return engine.addEdge(from, to, linkNature, statementIndex) > 0;
    }

    boolean mergeEdgeBi(Edge edge, String statementIndex) {
        Variable from = edge.from();
        Variable to = edge.to();
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        LinkNature ln = edge.linkNature();
        if (invalidEdge(from, ln, to)) return false;
        LinkNature rev = edge.linkNature().reverse();
        int newFacts1 = engine.addEdge(from, to, ln, statementIndex);
        int newFacts2 = engine.addEdge(to, from, rev, statementIndex);
        return newFacts1 + newFacts2 > 0;
    }

    static Variable fieldScopeRoot(Variable v) {
        if (v instanceof FieldReference fr) {
            if (fr.scopeVariable() instanceof This) return v;
            if (fr.scopeVariable() != null) return fieldScopeRoot(fr.scopeVariable());
        }
        return v;
    }

    boolean addField(Variable from, Variable primary, String statementIndex) {
        if (!from.equals(primary) && !(primary instanceof This)
            && from instanceof FieldReference && primary.equals(fieldScopeRoot(from))) {
            boolean change = mergeEdgeSingle(primary, CONTAINS_AS_FIELD, from, statementIndex);
            change |= mergeEdgeSingle(from, IS_FIELD_OF, primary, statementIndex);
            return change;
        }
        return false;
    }

    public String print() {
        return engine.print(Graph::printForTesting, Variable::compareTo);
    }

    public String printClosure() {
        return engine.printClosure(Graph::printForTesting, Variable::compareTo);
    }

    private static String printForTesting(Variable v) {
        if (v instanceof ParameterInfo pi) return pi.index() + ":" + pi.name();
        if (v instanceof DependentVariable dv && dv.indexVariable() != null) {
            return printForTesting(dv.arrayVariable()) + "[" + printForTesting(dv.indexVariable()) + "]";
        }
        if (v instanceof FieldReference fr && fr.scopeVariable() != null) {
            return printForTesting(fr.scopeVariable()) + "." + fr.fieldInfo().name();
        }
        return v.toString();
    }

    public void recompute(Set<Variable> affected, String statementIndex) {
        engine.recompute(affected, statementIndex);
    }

    public void remove(Set<Variable> toRemove) {
        engine.removeVertices(toRemove);
    }

    public Set<Variable> replaceReturnAffected(Variable from, Variable to,
                                               LinkNature currentLinkNature,
                                               LinkNature newLinkNature) {
        return engine.replaceReturnAffected(from, to, currentLinkNature, newLinkNature);
    }

    boolean simpleAddToGraph(Edge edge, String statementIndex) {
        return simpleAddToGraph(edge.from(), edge.linkNature(), edge.to(), statementIndex);
    }

    boolean simpleAddToGraph(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        boolean change = mergeEdgeSingle(from, linkNature, to, statementIndex);
        Variable primary = Util.primary(from);
        change |= addField(from, primary, statementIndex);

        // other direction
        change |= mergeEdgeSingle(to, linkNature.reverse(), from, statementIndex);
        Variable toPrimary = Util.primary(to);
        change |= addField(to, toPrimary, statementIndex);
        return change;
    }

    public int size() {
        return variables().size();
    }

    public int sizeOfClosure() {
        return engine.sizeOfClosure();
    }
    public int sizeOfWitnesses() {
        return engine.sizeOfWitnesses();
    }

    Set<Variable> variables() {
        return engine.vertices();
    }

    Stream<Map.Entry<Variable, LinkNature>> closureStream(Variable variable) {
        return engine.successorStream(variable);
    }

    public Iterable<Map.Entry<Variable, LinkNature>> closure(Variable variable) {
        return engine.successors(variable);
    }

    public Iterable<Map.Entry<Variable, LinkNature>> successorsInGraph(Variable variable) {
        return engine.successorsInGraph(variable);
    }
}
