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
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
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

    public Set<Variable> derivedShared(Variable variable) {
        return sharedVariables.derivedShared(variable);
    }

    public Set<Variable> assignmentSources(Variable variable) {
        return sharedVariables.assignmentSources(variable);
    }

    // return variables that were assigned a fresh, unanalyzable object ('return new URL(...)') whose
    // reduced intermediate never entered the graph; handleReturnVariable adds the '← $_v' marker for them
    private final Set<Variable> freshObjectReturns = new HashSet<>();

    public void markFreshObjectReturn(Variable returnVariable) {
        freshObjectReturns.add(returnVariable);
    }

    public boolean isFreshObjectReturn(Variable returnVariable) {
        return freshObjectReturns.contains(returnVariable);
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

    public Stream<VirtualModificationIdenticals.Group> eqGroups(Variable variable) {
        return virtualModificationIdenticals.groupsOf(variable);
    }

    /*
    §m-directional inheritance (consumption-aware; see catalogue): §m ≡ facts are routed into VMI, never the
    graph, so the closure cannot compose 'r.§m ≡ rr.§m' with the graph edge 'rr.§m → 0:in.§m'. For the strict-≡
    (no ☷ pass) groups of 'owner', read each sibling's closure and return the §m-to-§m facts rehomed onto
    owner's face. The CALLER decides when these enter a builder: they must be added AFTER the modification
    decision and the ⊇→~ rewrite collection — emitted earlier, they leak into verdicts (the reverted VMIFP
    experiment: ⊇→~ fired, 'newly created cannot be modified' nearly flipped).
     */
    public java.util.List<Link> vmiDirectionalFacts(Variable owner) {
        java.util.List<Link> result = new ArrayList<>();
        virtualModificationIdenticals.groupsOf(owner).forEach(group -> {
            if (!group.linkNature().pass().isEmpty()) return;
            Variable face = group.members().stream()
                    .filter(v -> owner.equals(Util.firstRealVariable(v)))
                    .findFirst().orElse(null);
            if (face == null) return;
            for (Variable sib : group.members()) {
                if (!sib.equals(face) && containsVariable(sib)) {
                    closureStream(sib).forEach(entry -> {
                        if (Util.isVirtualModification(entry.getKey())
                            && !entry.getKey().equals(face)
                            && !group.members().contains(entry.getKey())) {
                            result.add(new LinksImpl.LinkImpl(face, entry.getValue(), entry.getKey()));
                        }
                    });
                }
            }
        });
        return result;
    }

    public void removeEquivalence(Set<Variable> allToRemove2) {
        virtualModificationIdenticals.remove(allToRemove2);
    }

    private boolean invalidEdge(Variable from, LinkNature label, Variable to) {
        if (Util.isVirtualModification(from) != Util.isVirtualModification(to)) return true;
        if (Util.virtual(from) == Util.virtual(to)) return false;
        // an edge between a real variable and a virtual (hidden-content) field is legitimate for element/membership
        // (∈ ∋) and for assignment of the content itself (← →): a value read out of a container's hidden content is
        // assigned from it, e.g. 'X x = optional.orElseGet(...)' yields 'x ← optional.§x'.
        if (label == LinkNatureImpl.CONTAINS_AS_MEMBER
            || label == LinkNatureImpl.IS_ELEMENT_OF
            || label == LinkNatureImpl.IS_ASSIGNED_FROM
            || label == LinkNatureImpl.IS_ASSIGNED_TO) return false;
        // The owner ≻ own-virtual-field spine (the old engine's AddEdge.addField added it unconditionally): a
        // variable genuinely contains its own hidden content. These edges are load-bearing: the varargs fan-out
        // 'target.§is ~ collection.§is ≺ collection ∈ collections.§iss' closes to 'target.§is ∩ collections.§iss'
        // only through them. The general real↔virtual ≺/≻ ban (a graph-size reduction) stays for CROSS-variable
        // containment, which is malformed.
        if (System.getenv("NOSPINE") != null) return true;
        if (label == LinkNatureImpl.CONTAINS_AS_FIELD && Util.virtual(to) && from.equals(fieldScopeRoot(to))) {
            return false;
        }
        if (label == LinkNatureImpl.IS_FIELD_OF && Util.virtual(from) && to.equals(fieldScopeRoot(from))) {
            return false;
        }
        return true;
    }

    public Stream<Link> sharedAssignmentEdgeStream(Variable primary) {
        List<Link> result = new ArrayList<>();
        sharedVariables.assignmentEdgeStream(primary).forEach(link -> {
            result.add(link);
            // Field-level mirrors of a reconstructed whole-object assignment: the collapse hides the ← edge from
            // the engine, so the field projections the old engine's sub-propagation derived (combine.§is ←
            // target.§is for 'return target'; setI.i ← this.i for a fluent setter) never arise. The group's field
            // vertices live re-keyed on the rep; project each onto both endpoints of the intra-group link.
            Variable from = link.from(), to = link.to();
            Variable repFrom = sharedVariables.translateForward(from);
            if (System.getenv("NOMIRROR") == null
                && repFrom instanceof SharedVariable && repFrom.equals(sharedVariables.translateForward(to))) {
                for (Variable v : engine.vertices()) {
                    if (!v.equals(repFrom) && Util.isPartOf(repFrom, v)) {
                        Variable fromSub = rehome(v, repFrom, from);
                        Variable toSub = rehome(v, repFrom, to);
                        result.add(new LinksImpl.LinkImpl(fromSub, link.linkNature(), toSub));
                    }
                }
                // A field of the SOURCE face may itself be collapsed into a DIFFERENT group ('this.i' lives in
                // {this.i, 0:i}), so its vertex sits under that group's rep and is invisible above. Project such
                // member-fields of the source onto the recipient: 'setI ← this' mirrors to 'setI.i ← this.i'
                // (source knowledge transfers to the recipient, techniques §1.2; never the reverse).
                if (link.linkNature().isAssignedFrom()) {
                    sharedVariables.memberFieldsOf(to).forEach(m ->
                            result.add(new LinksImpl.LinkImpl(rehome(m, to, from), link.linkNature(), m)));
                }
            }
        });
        return result.stream();
    }

    public boolean isPureAssignmentSource(Variable variable) {
        return sharedVariables.isPureAssignmentSource(variable);
    }

    public Stream<Link> virtualModificationEdgeStream(Variable primary) {
        Set<Variable> variables = virtualModificationIdenticals.variablesPartOf(primary);
        Map<Variable, VirtualModificationIdenticals.Group> groups = variables.stream()
                .collect(Collectors.toUnmodifiableMap(v -> v, virtualModificationIdenticals::members));
        Stream<Link> own = groups.entrySet().stream().flatMap(e -> e.getValue().expand(e.getKey()));
        // §m knowledge of an assignment SOURCE transfers to the recipient: 'return zs' collapses {return, zs}, and
        // zs.§m ≡ 0:in.§m (subList returns a view) must surface as return.§m ≡ 0:in.§m on the return's summary.
        // The VMI members are keyed on the source (primary(zs.§m) = zs), so variablesPartOf(primary) misses them;
        // rehome each source-face member onto the primary. Only the SOURCE direction transfers (see
        // isPureAssignmentSource for why the reverse must not).
        List<Link> inherited = new ArrayList<>();
        for (Variable face : sharedVariables.assignmentSources(primary)) {
            for (Variable v : virtualModificationIdenticals.variablesPartOf(face)) {
                Variable rehomed = rehome(v, face, primary);
                virtualModificationIdenticals.members(v).expand(rehomed).forEach(inherited::add);
            }
        }
        return Stream.concat(own, inherited.stream());
    }


    public void clear(Variable variable, String statementIndex) {
        sharedVariables.remove(variable);
        // remove the variable AND every graph vertex whose scope chain contains it (its virtual fields 'v.§f',
        // array accesses 'v.f[i]', ...): with the owner≻own-virtual-field spine each variable owns such vertices,
        // and leaving them orphaned (scope pointing at a removed variable) pollutes the graph and later closures.
        // materializeWitnessOrphans (inside removeVertices) first preserves knowledge between SURVIVORS whose
        // witnesses routed through any of these.
        Set<Variable> set;
        if (System.getenv("NODESC") != null) {
            set = Set.of(variable);
        } else {
            set = new HashSet<>(isKnownInGraph(variable));
            set.add(variable);
        }
        if (engine.removeVertices(set)) {
            engine.recompute(set, statementIndex, _ -> true);
        }
    }

    // diagnostic: NOSV=1 in the environment disables the shared-variable collapse (assignments stay first-class
    // edges), so the O(N^2) part-of link explosion is not bounded. Used to demonstrate what sv prevents.
    private static final boolean NOSV = System.getenv("NOSV") != null;

    boolean mergeEdgeBi(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        if (System.getenv("TRACEVAR") != null
            && (from.toString().contains(System.getenv("TRACEVAR")) || to.toString().contains(System.getenv("TRACEVAR")))) {
            System.out.println("TRACE mergeEdgeBi " + statementIndex + ": " + from + " " + linkNature + " " + to);
        }
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
            if (fromInGroups && !(from instanceof org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable)
                && sharedVariables.isReassignment(from, statementIndex)) {
                // genuine reassignment ('from' was assigned at an earlier statement, now assigned again): drop its
                // old group membership. A second value assigned in the SAME statement (multi-valued 'm = cond ? a : b'
                // -> 'm ← a' and 'm ← b') is NOT a reassignment; keep 'from' so both sources join one group.
                // A ReturnVariable is exempt: a second 'return' statement is a merge over paths, not a
                // reassignment — removing it abandons the group rep carrying the first path's knowledge.
                sharedVariables.remove(from);
                // TODO what with fromInGraph?
            } else if (System.getenv("NORSRC") == null
                       && fromInGroups && sharedVariables.isSourceAtOtherStatement(from, statementIndex)) {
                // 'from' was a pure SOURCE in its group ('method ← 1:num' at an earlier statement) and is now
                // being assigned ('num = amb'): its past source-participation stays valid for the OLD value, but
                // the new value must not join the group as an alias. Keep the edge as a plain graph edge onto the
                // rep: extraction then yields both 'method ← 1:num' (intra-group) and 'method ← 0:amb' (rep edge).
                Variable tFromR = sharedVariables.translateForward(from);
                Variable tToR = sharedVariables.translateForward(to);
                if (tFromR.equals(tToR)) return engine.addVertex(tFromR);
                return engine.addSymmetricEdge(tFromR, tToR, linkNature, statementIndex) > 0;
            }
            SharedVariable sv = sharedVariables.isAssignedFrom(from, to, statementIndex);
            SharedVariable mergedAway = sharedVariables.consumeLastMergedAway();
            if (mergedAway != null && sv != null) {
                // two existing groups were bridged: re-key the discarded rep's graph vertices onto the survivor
                Set<Variable> inGraph = isKnownInGraph(mergedAway);
                if (!inGraph.isEmpty()) {
                    transformToSharedVariable(mergedAway, inGraph, sv, statementIndex);
                }
                return true;
            }
            Set<Variable> fromInGraph = isKnownInGraph(from);
            Set<Variable> toInGraph = isKnownInGraph(to);
            if (sv == null) {
                assert fromInGraph.isEmpty() && toInGraph.isEmpty()
                        : from + " and " + to + " should already have been removed; they're in the same equivalance group";
                return false;
            }
            if (!fromInGraph.isEmpty()) {
                transformToSharedVariable(from, fromInGraph, sv, statementIndex);
            }
            // re-key the to-side as well (recomputed: the from-pass may already have consumed shared vertices).
            // Removing just the bare 'to' vertex instead left its scope-descendants ('to.§$$s' with its edges)
            // orphaned in the graph — the next edge on the same pair then hit the sv==null assert — and silently
            // deleted the to-side knowledge instead of re-keying it onto the rep.
            Set<Variable> toInGraphAfter = isKnownInGraph(to);
            if (!toInGraphAfter.isEmpty()) {
                transformToSharedVariable(to, toInGraphAfter, sv, statementIndex);
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
                // translate BOTH endpoints: an edge between two members of the group (e.g. the owner ≻ own-virtual-
                // field spine 'target ≻ target.§is') must be re-keyed on both sides ('$__sv ≻ $__sv.§is'); leaving
                // the to-side untranslated resurrects the removed member vertex and produces a half-translated edge
                // ('$__sv ≻ target.§is') that invalidEdge then rightly drops — severing the spine at every collapse.
                Variable newTo = System.getenv("NOBOTH") != null ? link.getKey()
                        : vtm.translateVariableRecursively(link.getKey());
                // re-homing a member's edges onto the shared representative can make the source coincide with the
                // target (the edge pointed at another member of the same group); skip the resulting self-loop,
                // which the engine forbids (Fact asserts source != target).
                if (newFrom.equals(newTo)) continue;
                if (System.getenv("TRACEVAR") != null
                    && (newFrom.toString().contains(System.getenv("TRACEVAR"))
                        || newTo.toString().contains(System.getenv("TRACEVAR")))) {
                    System.out.println("TRACE transform " + variable + "->" + sharedVariable + " re-add: "
                                       + newFrom + " " + link.getValue() + " " + newTo);
                }
                engine.addSymmetricEdge(newFrom, newTo, link.getValue(), statementIndex);
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
            // keep the original element type: a member may be statically Object-typed (a downcast slot such as
            // 'ld.variables[1]' grouped with 'float[][] matrix'), where recomputing via copyWithOneFewerArrays fails.
            return expandRepToMembers(dv.arrayVariable())
                    .map(arr -> arr.equals(dv.arrayVariable())
                            ? dv
                            : runtime.newDependentVariable(runtime.newVariableExpression(arr), dv.indexExpression(),
                            dv.parameterizedType()));
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

    public String printShared(Function<Variable, String> variablePrinter) {
        return sharedVariables.print(variablePrinter);
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
                                               LinkNature newLinkNature,
                                               String skipStatementIndex) {
        return engine.replaceReturnAffected(from, to, currentLinkNature, newLinkNature, skipStatementIndex);
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
