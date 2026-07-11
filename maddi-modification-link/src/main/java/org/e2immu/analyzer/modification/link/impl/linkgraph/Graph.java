package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.graph.Fact;
import org.e2immu.analyzer.modification.link.impl.graph.IncrementalFixpointEngine;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;

public class Graph {
    private final Runtime runtime;
    private final IncrementalFixpointEngine<Variable, LinkNature> engine;
    private final VirtualModificationIdenticals virtualModificationIdenticals = new VirtualModificationIdenticals();
    private final SharedVariables sharedVariables;

    public Graph(Runtime runtime, IncrementalFixpointEngine<Variable, LinkNature> engine) {
        this.engine = engine;
        this.sharedVariables = new SharedVariables(runtime);
        this.runtime = runtime;
    }

    public Collection<Variable> allShared(Variable variable) {
        return sharedVariables.allShared(variable);
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

    public Stream<Variable> eqVariables(Variable variable) {
      return  virtualModificationIdenticals.equivalentStream(variable);
    }

    public void removeEquivalence(Set<Variable> allToRemove2) {
        virtualModificationIdenticals.remove(allToRemove2);
    }

    private boolean invalidEdge(Variable from, LinkNature label, Variable to) {
        if (Util.isVirtualModification(from) != Util.isVirtualModification(to)) return true;
        // an edge between a real variable and a virtual (hidden-content) field is legitimate for element/membership
        // (∈ ∋) and for assignment of the content itself (← →): a value read out of a container's hidden content is
        // assigned from it, e.g. 'X x = optional.orElseGet(...)' yields 'x ← optional.§x'. Other natures between a
        // real and a virtual endpoint remain malformed.
        return Util.virtual(from) != Util.virtual(to)
               && label != LinkNatureImpl.CONTAINS_AS_MEMBER
               && label != LinkNatureImpl.IS_ELEMENT_OF
               && label != LinkNatureImpl.IS_ASSIGNED_FROM
               && label != LinkNatureImpl.IS_ASSIGNED_TO;
    }

    public Stream<Link> sharedAssignmentEdgeStream(Variable primary) {
        return sharedVariables.assignmentEdgeStream(primary);
    }

    public Stream<Link> virtualModificationEdgeStream(Variable primary) {
        Set<Variable> variables = virtualModificationIdenticals.variablesPartOf(primary);
        Map<Variable, VirtualModificationIdenticals.Group> groups = variables.stream()
                .collect(Collectors.toUnmodifiableMap(v -> v, virtualModificationIdenticals::members));
        return groups.entrySet().stream().flatMap(e -> e.getValue().expand(e.getKey()));
    }


    public void clear(Variable variable, String statementIndex) {
        sharedVariables.remove(variable);
        Set<Variable> set = Set.of(variable);
        if (engine.removeVertices(set)) {
            engine.recompute(set, statementIndex, _ -> true);
        }
    }

    // diagnostic: NOSV=1 in the environment disables the shared-variable collapse (assignments stay first-class
    // edges), so the O(N^2) part-of link explosion is not bounded. Used to demonstrate what sv prevents.
    private static final boolean NOSV = System.getenv("NOSV") != null;

    boolean mergeEdgeBi(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        if (invalidEdge(from, linkNature, to)) return false;
        if (linkNature.isIdenticalTo() && Util.isVirtualModification(from)) {
            return virtualModificationIdenticals.add(from, linkNature, to);
        }
        // only collapse whole-object assignment aliases; a hidden-content virtual field ('x ← optional.§x' — x is a
        // copy of the content, not an alias of the container) stays a first-class edge so 'x ← optional.§x' survives.
        if (!NOSV && linkNature.isAssignedFrom() && !(to instanceof MarkerVariable)
            && !Util.virtual(from) && !Util.virtual(to)) {
            boolean fromInGroups = sharedVariables.isKnown(from);
            if (fromInGroups) {
                // reassignment: we must remove 'from' if present in any shared variable
                sharedVariables.remove(from);
                // TODO what with fromInGraph?
            }
            SharedVariable sv = sharedVariables.isAssignedFrom(from, to);
            Set<Variable> fromInGraph = isKnownInGraph(from);
            Set<Variable> toInGraph = isKnownInGraph(to);
            if (sv == null) {
                assert fromInGraph.isEmpty() && toInGraph.isEmpty()
                        : from + " and " + to + " should already have been removed; they're in the same equivalance group";
                return false;
            }
            if (!fromInGraph.isEmpty()) {
                transformToSharedVariable(from, fromInGraph, sv, statementIndex);
                if (!toInGraph.isEmpty()) {
                    engine.removeVertices(Set.of(to));
                }
            } else if (!toInGraph.isEmpty()) {
                transformToSharedVariable(to, toInGraph, sv, statementIndex);
            }
            return true;
        }
        Variable tFrom = sharedVariables.translateForward(from);
        Variable tTo = sharedVariables.translateForward(to);
        if (tFrom.equals(tTo)) {
            // distinct 'from' and 'to' both translate to the same shared-variable representative (they are in one
            // assignment group): the edge would be a self-loop, which the engine forbids (Fact asserts
            // source != target). Nothing to link; just make sure the vertex exists. Mirrors the from.equals(to)
            // guard at the top of this method, now applied after translateForward.
            return engine.addVertex(tFrom);
        }
        return engine.addSymmetricEdge(tFrom, tTo, linkNature, statementIndex) > 0;
    }

    private Set<Variable> isKnownInGraph(Variable variable) {
        return engine.vertices().stream()
                .filter(v -> Util.variableAndScopes(v).anyMatch(variable::equals))
                .collect(Collectors.toUnmodifiableSet());
    }


    private void transformToSharedVariable(Variable variable,
                                           Set<Variable> variablesInGraph,
                                           SharedVariable sharedVariable,
                                           String statementIndex) {
        var forwardLinksList = variablesInGraph
                .stream()
                .map(v -> new AbstractMap.SimpleEntry<>(v, engine.edges(v)))
                .toList();
        engine.removeVertices(variablesInGraph);
        engine.addVertex(sharedVariable);
        for (Map.Entry<Variable, Iterable<Map.Entry<Variable, LinkNature>>> forwardLinks : forwardLinksList) {
            VariableTranslationMap vtm = new VariableTranslationMap(runtime);
            vtm.put(variable, sharedVariable);
            Variable newFrom = vtm.translateVariableRecursively(forwardLinks.getKey());
            for (Map.Entry<Variable, LinkNature> link : forwardLinks.getValue()) {
                // re-homing a member's edges onto the shared representative can make the source coincide with the
                // target (the edge pointed at another member of the same group); skip the resulting self-loop,
                // which the engine forbids (Fact asserts source != target).
                if (newFrom.equals(link.getKey())) continue;
                engine.addSymmetricEdge(newFrom, link.getKey(), link.getValue(), statementIndex);
            }
        }
        engine.recompute(Set.of(sharedVariable), statementIndex, _ -> true);
    }

    public Variable translateForward(Variable variable) {
        return sharedVariables.translateForward(variable);
    }

    // The inverse of translateForward for extraction: expand a graph vertex whose scope chain contains a
    // shared-variable rep back to its member forms (a rep as the whole vertex, or nested in a field scope such
    // as '$__sv_list1.§$s'). Mirrors WriteLinksAndModification.iterateOverShared. A vertex with no rep maps to
    // itself.
    public Stream<Variable> expandRepToMembers(Variable variable) {
        if (variable instanceof SharedVariable sv) {
            return sv.variables().stream();
        }
        if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
            return expandRepToMembers(fr.scopeVariable())
                    .map(scope -> scope.equals(fr.scopeVariable())
                            ? fr
                            : runtime.newFieldReference(fr.fieldInfo(),
                            runtime.newVariableExpression(scope), fr.parameterizedType()));
        }
        if (variable instanceof DependentVariable dv && dv.arrayVariable() != null) {
            // an array-indexed rep, e.g. '$__sv_g[0]' -> 'g[0]'; rebuild the access on the expanded array member.
            return expandRepToMembers(dv.arrayVariable())
                    .map(arr -> arr.equals(dv.arrayVariable())
                            ? dv
                            : runtime.newDependentVariable(runtime.newVariableExpression(arr), dv.indexExpression()));
        }
        return Stream.of(variable);
    }

    // rebuild 'variable' with every occurrence of 'from' in its scope chain replaced by 'to'
    // (e.g. rehome(a.list1.§$s, a, $__sv_return) = $__sv_return.list1.§$s).
    public Variable rehome(Variable variable, Variable from, Variable to) {
        VariableTranslationMap vtm = new VariableTranslationMap(runtime);
        vtm.put(from, to);
        return vtm.translateVariableRecursively(variable);
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
