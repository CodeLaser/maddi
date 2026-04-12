package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.graph.Fact;
import org.e2immu.analyzer.modification.link.impl.graph.IncrementalFixpointEngine;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;

public class Graph {
    private final IncrementalFixpointEngine<Variable, LinkNature> engine;
    private final VirtualModificationIdenticals virtualModificationIdenticals = new VirtualModificationIdenticals();
    private final SharedVariables sharedVariables;

    public Graph(Runtime runtime, IncrementalFixpointEngine<Variable, LinkNature> engine) {
        this.engine = engine;
        this.sharedVariables = new SharedVariables(runtime);
    }

    public SharedVariables sharedVariables() {
        return sharedVariables;
    }

    public IncrementalFixpointEngine<Variable, LinkNature> engine() {
        return engine;
    }

    public boolean containsVariable(Variable primary) {
        return engine.vertices().contains(primary);
    }

    public Iterable<Map.Entry<Variable, Map<Variable, LinkNature>>> edges() {
        return engine.edges();
    }

    public Iterable<Map.Entry<Variable, Map<Variable, LinkNature>>> edgesWithEquivalence() {
        List<Map.Entry<Variable, Map<Variable, LinkNature>>> res = new LinkedList<>();
        engine.edges().forEach(res::add);
        virtualModificationIdenticals.edges().forEach(res::add);
        return res;
    }

    public Stream<Variable> eqVariables() {
        return virtualModificationIdenticals.variables();
    }

    public void removeEquivalence(Set<Variable> allToRemove2) {
        virtualModificationIdenticals.remove(allToRemove2);
    }

    private boolean invalidEdge(Variable from, LinkNature label, Variable to) {
        if (Util.isVirtualModification(from) != Util.isVirtualModification(to)) return true;
        return Util.virtual(from) != Util.virtual(to)
               && label != LinkNatureImpl.CONTAINS_AS_MEMBER
               && label != LinkNatureImpl.IS_ELEMENT_OF;
    }

    public Stream<Link> equivalentEdgesStream(Variable primary) {
        Set<Variable> variables = virtualModificationIdenticals.variablesPartOf(primary);
        Map<Variable, VirtualModificationIdenticals.Group> groups = variables.stream()
                .collect(Collectors.toUnmodifiableMap(v -> v, virtualModificationIdenticals::members));
        return groups.entrySet().stream().flatMap(e -> e.getValue().expand(e.getKey()));
    }


    boolean mergeEdgeBi(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        if (invalidEdge(from, linkNature, to)) return false;
        if (linkNature.isIdenticalTo() && Util.isVirtualModification(from)) {
            return virtualModificationIdenticals.add(from, linkNature, to);
        }
        if (linkNature.isAssignedFrom() && !(from instanceof ReturnVariable) &&  !(to instanceof MarkerVariable)) {
            return sharedVariables.isAssignedFrom(from, to);
        }
        Variable tFrom = sharedVariables.translateForward(from);
        Variable tTo = sharedVariables.translateForward(to);
        return engine.addSymmetricEdge(tFrom, tTo, linkNature, statementIndex) > 0;
    }

    public String printEquivalence(Function<Variable, String> variablePrinter) {
        return virtualModificationIdenticals.print(variablePrinter);
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
        return engine.print();
    }

    public String printClosure() {
        return engine.printClosure();
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

    public void recompute(Set<Variable> affected,
                          String statementIndex,
                          Predicate<Fact<Variable, LinkNature>> acceptRemoval) {
        engine.recompute(affected, statementIndex, acceptRemoval);
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
