package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.graph.EquivalenceGroup;
import org.e2immu.analyzer.modification.link.impl.graph.IncrementalFixpointEngine;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;

public class Graph {
    private final IncrementalFixpointEngine<Variable, LinkNature> engine;
    private final EquivalenceGroup equivalenceGroup = new EquivalenceGroup();

    public Graph(IncrementalFixpointEngine<Variable, LinkNature> engine) {
        this.engine = engine;
    }

    public IncrementalFixpointEngine<Variable, LinkNature> engine() {
        return engine;
    }

    public boolean containsVariable(Variable primary) {
        return engine.vertices().contains(primary);
    }

    // FIXME this does not seem to be the solution
    public Iterable<Map.Entry<Variable, Map<Variable, LinkNature>>> edgesWithEquivalence() {
        List<Map.Entry<Variable, Map<Variable, LinkNature>>> res = new LinkedList<>();
        for (Map.Entry<Variable, Map<Variable, LinkNature>> entry : engine.edges()) {
            EquivalenceGroup.Group group = equivalenceGroup.members(entry.getKey());
            if (group.members().isEmpty()) {
                res.add(entry);
            } else {
                // augment!
                Map<Variable, LinkNature> map = new HashMap<>(entry.getValue());
                group.members().forEach(v -> map.merge(v, group.linkNature(), LinkNature::best));
                res.add(new AbstractMap.SimpleEntry<>(entry.getKey(), map));
            }
        }
        return res;
    }

    private boolean invalidEdge(Variable from, LinkNature label, Variable to) {
        if (Util.isVirtualModification(from) != Util.isVirtualModification(to)) return true;
        return Util.virtual(from) != Util.virtual(to)
               && label != LinkNatureImpl.CONTAINS_AS_MEMBER
               && label != LinkNatureImpl.IS_ELEMENT_OF;
    }

    public Stream<Link> equivalentEdgesStream(Variable primary) {
        Set<Variable> variables = equivalenceGroup.variablesPartOf(primary);
        Map<Variable, EquivalenceGroup.Group> groups = variables.stream()
                .collect(Collectors.toUnmodifiableMap(v -> v, equivalenceGroup::members));
        return groups.entrySet().stream()
                .flatMap(e -> expand(Set.of(e.getKey()), e.getValue()));
    }


    private static Stream<Link> expand(Set<Variable> set1, EquivalenceGroup.Group group) {
        Stream.Builder<Link> links = Stream.builder();
        for (Variable v1 : set1) {
            for (Variable v2 : group.members()) {
                if (v1 != v2) {
                    links.accept(new LinksImpl.LinkImpl(v1, group.linkNature(), v2));
                }
            }
        }
        return links.build();
    }

    boolean mergeEdgeBi(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        if (invalidEdge(from, linkNature, to)) return false;
        if (linkNature.isIdenticalTo()) {
            EquivalenceGroup.AddResult result = equivalenceGroup.add(from, linkNature, to);
            if (result.isMerge()) {
                throw new UnsupportedOperationException("NYI");
            }
            return result.isAddOrNew();
        }
        Variable eqFrom = equivalenceGroup.representative(from);
        Variable eqTo = equivalenceGroup.representative(to);

        LinkNature rev = linkNature.reverse();
        int newFacts1 = engine.addEdge(eqFrom, eqTo, linkNature, statementIndex);
        int newFacts2 = engine.addEdge(eqTo, eqFrom, rev, statementIndex);
        return newFacts1 + newFacts2 > 0;
    }

    public String printEquivalence(Function<Variable, String> variablePrinter) {
        return equivalenceGroup.print(variablePrinter);
    }

    boolean simpleAddToGraph(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        boolean change = mergeEdgeBi(from, linkNature, to, statementIndex);
        change |= addField(from, Util.primary(from), statementIndex);
        change |= addField(to, Util.primary(to), statementIndex);
        return change;
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
            return mergeEdgeBi(primary, CONTAINS_AS_FIELD, from, statementIndex);
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
}
