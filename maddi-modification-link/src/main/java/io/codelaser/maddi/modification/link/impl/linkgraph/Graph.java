package io.codelaser.maddi.modification.link.impl.linkgraph;

import io.codelaser.maddi.modification.link.impl.Gate;
import io.codelaser.maddi.modification.link.impl.LinkNatureImpl;
import io.codelaser.maddi.modification.link.impl.graph.Fact;
import io.codelaser.maddi.modification.link.impl.graph.IncrementalFixpointEngine;
import io.codelaser.maddi.modification.link.impl.graph.LabeledGraph;
import io.codelaser.maddi.modification.prepwork.variable.ReturnVariable;
import io.codelaser.maddi.modification.link.impl.localvar.MarkerVariable;
import io.codelaser.maddi.modification.link.impl.localvar.SharedVariable;
import io.codelaser.maddi.modification.link.impl.translate.VariableTranslationMap;
import io.codelaser.maddi.modification.prepwork.Util;
import io.codelaser.maddi.modification.prepwork.variable.Link;
import io.codelaser.maddi.modification.prepwork.variable.LinkNature;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.codelaser.maddi.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;

public class Graph {
    private final Runtime runtime;
    private final IncrementalFixpointEngine<Variable, LinkNature> engine;
    private final VirtualModificationIdenticals virtualModificationIdenticals = new VirtualModificationIdenticals();
    private final SharedVariables sharedVariables;

    public Graph(Runtime runtime, IncrementalFixpointEngine<Variable, LinkNature> engine) {
        this.engine = engine;
        this.sharedVariables = new SharedVariables(runtime);
        this.runtime = runtime;
        // the engine's graph is still empty here, so the index sees every vertex from the start
        engine.setVertexListener(new LabeledGraph.VertexListener<>() {
            @Override
            public void vertexAdded(Variable v) {
                indexVertex(v);
            }

            @Override
            public void vertexRemoved(Variable v) {
                unindexVertex(v);
            }
        });
    }

    /*
    Vertex indexes, maintained through the engine's vertex listener. They replace full vertex scans that ran
    inside per-statement/per-variable loops — the dominant cost on long straight-line methods (review
    2026-08-05, remedy C):
    - verticesByScopePart: scope-chain member -> vertices whose variableAndScopes contain it. Answers
      isKnownInGraph (mergeEdgeBi's collapse, clear's descendant sweep), FollowGraph's fromList "part of
      primary" scan, and sharedAssignmentEdgeStream's field-mirror scan.
    - verticesWithRepInScope: vertices with a SharedVariable rep in their scope chain — exactly those for
      which expandRepToMembers is non-trivial (FollowGraph's rep-expansion branch).
    - verticesWithReturnPrimary: vertices whose primary is a ReturnVariable (FollowGraph's reverse-return
      block).
    LinkedHashSets: the sets' iteration order is the vertices' engine insertion order, i.e. the same
    relative order the replaced graph.variables() scans produced for the matching subset — emission order
    feeds rank-stable sorts downstream.
     */
    private final Map<Variable, Set<Variable>> verticesByScopePart = new HashMap<>();
    private final Set<Variable> verticesWithRepInScope = new LinkedHashSet<>();
    private final Set<Variable> verticesWithReturnPrimary = new LinkedHashSet<>();

    private void indexVertex(Variable v) {
        boolean[] rep = {false};
        Util.variableAndScopes(v).forEach(part -> {
            verticesByScopePart.computeIfAbsent(part, _ -> new LinkedHashSet<>()).add(v);
            if (part instanceof SharedVariable) rep[0] = true;
        });
        if (rep[0]) verticesWithRepInScope.add(v);
        if (Util.primary(v) instanceof ReturnVariable) verticesWithReturnPrimary.add(v);
    }

    private void unindexVertex(Variable v) {
        Util.variableAndScopes(v).forEach(part -> {
            Set<Variable> set = verticesByScopePart.get(part);
            if (set != null) {
                set.remove(v);
                if (set.isEmpty()) verticesByScopePart.remove(part);
            }
        });
        verticesWithRepInScope.remove(v);
        verticesWithReturnPrimary.remove(v);
    }

    // vertices v with base ∈ variableAndScopes(v), equivalently Util.isPartOf(base, v). Unmodifiable VIEW:
    // do not hold across graph mutations (isKnownInGraph copies for exactly that reason)
    public Set<Variable> verticesPartOf(Variable base) {
        Set<Variable> set = verticesByScopePart.get(base);
        Set<Variable> result = set == null ? Set.of() : Collections.unmodifiableSet(set);
        assert indexMatchesScan(base, result);
        return result;
    }

    public Set<Variable> verticesWithRepInScope() {
        return Collections.unmodifiableSet(verticesWithRepInScope);
    }

    public Set<Variable> verticesWithReturnPrimary() {
        return Collections.unmodifiableSet(verticesWithReturnPrimary);
    }

    /*
    Extraction-reuse dirty tracking, graph side (remedy A, review 2026-08-05). The engine tracks closure
    changes itself; everything ELSE an extraction reads — shared-variable groups, VMI groups, mediation
    pairs, pattern bindings — is recorded here, EAGERLY at the mutation site: a membership query at drain
    time sees post-mutation state, which no longer knows the old siblings.
     */
    private final Set<Variable> touchedVariables = new HashSet<>();

    // per-statement walk budget for drainDirtyVariables; exhaustion degrades to dirty-all, never wrong
    private static final long REUSE_BUDGET = Long.getLong("maddi.reuseBudget", 200_000L);

    /*
    Memo for expandRepToMembers along the drain BFS: rebuilding the member spellings allocates fresh
    FieldReferences per member per node, and dense methods re-walk the same component EVERY statement —
    66% of the remaining drain CPU after the prefix-table fix (asprof 2026-08-05, round 2). An
    expansion depends only on the vertex's structure and the rep memberships in its chain, so the memo
    is invalidated wholesale at every shared-group mutation (rare) — see clearRepExpansionCache callers.
     */
    private final Map<Variable, List<Variable>> repExpansionCache = new HashMap<>();

    private void clearRepExpansionCache() {
        repExpansionCache.clear();
    }

    private void touchVariable(Variable v) {
        touchedVariables.add(v);
    }

    private void touchAll(Collection<Variable> vs) {
        touchedVariables.addAll(vs);
    }

    /*
    The set of variables whose NEXT extraction may differ from their previous one, i.e. the union of the
    "link webs" touched since the last drain — or null when the dirt is global (a return-primary vertex
    changed: the reverse-return block in FollowGraph feeds every primary from return rows).

    The BFS closes the raw dirt over every mechanism that can carry knowledge between variables during
    extraction: closure facts (rows), structural containment (a vertex change affects all its scope
    prefixes; a variable change affects vertices spelled through it — the C index), shared-variable groups
    (reconstruction + faceKeyed/derivedFaceKeyed rehoming, reached via group co-members and the
    members-rooted-at-prefix table), and VMI groups (§m folds). Closure of the dirt over this web is what
    makes per-variable reuse sound: RedundantLinks' cross-variable guard edges come from links, links stay
    within a web, so every web is either entirely reused or entirely recomputed.
     */
    // cheap drain for the NOREUSE opt-out: empty the tracking sets without computing the closure
    public void drainRaw() {
        engine.drainTouched();
        touchedVariables.clear();
    }

    public Set<Variable> drainDirtyVariables() {
        Set<Variable> seeds = engine.drainTouched();
        seeds.addAll(touchedVariables);
        touchedVariables.clear();
        if (seeds.isEmpty()) return Set.of();
        // dedup at PUSH time: the queue stays bounded by the dirty set — a dense closure component
        // otherwise queues O(k^2) duplicate entries (heap blow-up on the bench shapes)
        Set<Variable> dirty = new HashSet<>();
        Deque<Variable> work = new ArrayDeque<>();
        // budgeted: on a saturated closure the walk is Θ(k^2) per statement and the dirty web is the
        // whole graph — reuse cannot pay off there, so bail to dirty-all (the pre-reuse behavior)
        // instead of grinding memory and CPU (the 512m test-worker OOM). Per-statement budget.
        long[] budget = {REUSE_BUDGET};
        java.util.function.Consumer<Variable> push = v -> {
            budget[0]--;
            if (dirty.add(v)) work.push(v);
        };
        seeds.forEach(push);
        while (!work.isEmpty()) {
            if (budget[0] < 0) return null; // dirty-all
            Variable d = work.pop();
            if (d instanceof ReturnVariable || Util.primary(d) instanceof ReturnVariable) {
                return null; // global dirt
            }
            engine.successorStream(d).forEach(e -> push.accept(e.getKey()));
            verticesByScopePart.getOrDefault(d, Set.of()).forEach(push);
            // prefix web: rep-expansion only when a rep actually sits in the chain — the stream-and-
            // rebuild machinery, run unconditionally, was 37% of the drain's CPU (asprof 2026-08-05);
            // the common case is a plain iterative scope walk, allocation-free
            if (hasRepInChain(d)) {
                for (Variable m : repExpansionCache.computeIfAbsent(d, v -> expandRepToMembers(v).toList())) {
                    push.accept(m);
                    Util.scopeVariables(m).forEach(push);
                }
            } else {
                Util.scopeVariables(d).forEach(push);
            }
            sharedVariables.allShared(d).forEach(push);
            // persistent prefix indexes, maintained at group-mutation time (SharedVariables/VMI); the
            // per-statement table rebuild they replace was 46% of the drain's CPU
            sharedVariables.membersRootedAt(d).forEach(push);
            for (Variable f : virtualModificationIdenticals.membersRootedAt(d)) {
                virtualModificationIdenticals.groupsOfMember(f).forEach(g -> g.members().forEach(push));
            }
        }
        return dirty;
    }

    private static boolean hasRepInChain(Variable v) {
        Variable x = v;
        while (x != null) {
            if (x instanceof SharedVariable) return true;
            x = x instanceof FieldReference fr ? fr.scopeVariable()
                    : x instanceof DependentVariable dv ? dv.arrayVariable() : null;
        }
        return false;
    }

    // assertion-only: the index must agree with the scan it replaced
    private boolean indexMatchesScan(Variable base, Set<Variable> fromIndex) {
        Set<Variable> scanned = engine.vertices().stream()
                .filter(v -> Util.variableAndScopes(v).anyMatch(base::equals))
                .collect(Collectors.toUnmodifiableSet());
        assert scanned.equals(fromIndex) : "vertex index out of sync for " + base
                                           + ": index " + fromIndex + " vs scan " + scanned;
        return true;
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

    // side-band, like freshObjectReturns: record-pattern bindings ('i instanceof R(Object o)' ⟹ o is a
    // genuine component of i). The containment filter (isInvalidFieldContainment) cannot distinguish a
    // pattern binding from an accessor-copy expansion — the distinction is made HERE, at the binding site.
    private final Map<Variable, Set<Variable>> patternBindings = new HashMap<>();

    public void markPatternBinding(Variable container, Variable binding) {
        patternBindings.computeIfAbsent(container, _ -> new HashSet<>()).add(binding);
        touchVariable(container);
        touchVariable(binding);
    }

    // 'part' (or any of its whole-object group aliases — the cast 'set' of a bound 'o') is a marked
    // pattern binding of 'container'
    public boolean isPatternBindingOrAlias(Variable container, Variable part) {
        Set<Variable> bindings = patternBindings.get(container);
        if (bindings == null) return false;
        if (bindings.contains(part)) return true;
        for (Variable alias : sharedVariables.allShared(part)) {
            if (bindings.contains(alias)) return true;
        }
        return false;
    }

    // a type-pattern alias of an existing binding ('o instanceof Set set' where o is a bound component of i):
    // the alias is the same object, so it is a component of every container o is bound in
    public void markPatternBindingAlias(Variable original, Variable alias) {
        for (Map.Entry<Variable, Set<Variable>> entry : patternBindings.entrySet()) {
            if (entry.getValue().contains(original)) {
                entry.getValue().add(alias);
                touchVariable(entry.getKey());
            }
        }
        touchVariable(original);
        touchVariable(alias);
    }

    // side-band mediation provenance (task #39): variable pairs whose direct link was produced through a
    // syntactic mediation (pattern binding, cast). Populated at insertion (simpleAddToGraph) from
    // Link.mediated(); consulted at extraction (WriteLinksAndModification) when statement-level links are
    // rebuilt — both the engine's facts and the shared-variable collapse erase the flag. Unordered pairs:
    // the engine stores both orientations of an edge. Direct pairs only; a chain composed ACROSS a
    // mediated hop is not (yet) tainted — record before consuming this for declared-type decisions.
    private final Set<Set<Variable>> mediatedPairs = new HashSet<>();

    public void markMediated(Variable a, Variable b) {
        if (!a.equals(b)) {
            mediatedPairs.add(Set.of(a, b));
            touchVariable(a);
            touchVariable(b);
        }
    }

    public boolean isMediatedPair(Variable a, Variable b) {
        return !a.equals(b) && mediatedPairs.contains(Set.of(a, b));
    }

    public void markFreshObjectReturn(Variable returnVariable) {
        freshObjectReturns.add(returnVariable);
        touchVariable(returnVariable);
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
                    // a stacked face (x.§m.§m, legal inside VMI bookkeeping) cannot be represented as a Link:
                    // skip it rather than trip the LinkImpl constructor assert (timefold constraint streams)
                    .filter(LinksImpl.LinkImpl::doNotStackMOnTopOfVirtualField)
                    .findFirst().orElse(null);
            if (face == null) return;
            for (Variable sib : group.members()) {
                if (!sib.equals(face) && containsVariable(sib)) {
                    closureStream(sib).forEach(entry -> {
                        if (Util.isVirtualModification(entry.getKey())
                            && !entry.getKey().equals(face)
                            && !group.members().contains(entry.getKey())
                            && LinksImpl.LinkImpl.doNotStackMOnTopOfVirtualField(entry.getKey())) {
                            result.add(new LinksImpl.LinkImpl(face, entry.getValue(), entry.getKey()));
                        }
                    });
                }
            }
        });
        return result;
    }

    public void removeEquivalence(Set<Variable> allToRemove2) {
        // eager: after remove() the groups no longer know these members' old siblings
        for (Variable v : allToRemove2) {
            touchVariable(v);
            virtualModificationIdenticals.groupsOfMember(v).forEach(g -> touchAll(g.members()));
        }
        virtualModificationIdenticals.remove(allToRemove2);
    }

    private boolean invalidEdge(Variable from, LinkNature label, Variable to) {
        if (Util.isVirtualModification(from) != Util.isVirtualModification(to)) return true;
        if (Util.virtual(from) == Util.virtual(to)) return false;
        // an edge between a real variable and a virtual (hidden-content) field is legitimate for element/membership
        // (∈ ∋), for assignment of the content itself (← →): a value read out of a container's hidden content is
        // assigned from it, e.g. 'X x = optional.orElseGet(...)' yields 'x ← optional.§x' — and for content
        // subset/superset against a single value (⊆ ⊇, gate NORVSUB): 'Stream.generate(() -> alt)' yields
        // 'genParam.§xs ⊆ 0:alt' (all the stream's content IS repetitions of alt).
        if (label == LinkNatureImpl.CONTAINS_AS_MEMBER
            || label == LinkNatureImpl.IS_ELEMENT_OF
            || label == LinkNatureImpl.IS_ASSIGNED_FROM
            || label == LinkNatureImpl.IS_ASSIGNED_TO) return false;
        if (!Gate.isSet("NORVSUB")
            && (label == LinkNatureImpl.IS_SUBSET_OF || label == LinkNatureImpl.IS_SUPERSET_OF)) return false;
        // The owner ≻ own-virtual-field spine (the old engine's AddEdge.addField added it unconditionally): a
        // variable genuinely contains its own hidden content. These edges are load-bearing: the varargs fan-out
        // 'target.§is ~ collection.§is ≺ collection ∈ collections.§iss' closes to 'target.§is ∩ collections.§iss'
        // only through them. The general real↔virtual ≺/≻ ban (a graph-size reduction) stays for CROSS-variable
        // containment, which is malformed.
        if (Gate.isSet("NOSPINE")) return true;
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
        sharedVariables.assignmentEdgeStream(primary, this::isMediatedPair).forEach(link -> {
            result.add(link);
            // Field-level mirrors of a reconstructed whole-object assignment: the collapse hides the ← edge from
            // the engine, so the field projections the old engine's sub-propagation derived (combine.§is ←
            // target.§is for 'return target'; setI.i ← this.i for a fluent setter) never arise. The group's field
            // vertices live re-keyed on the rep; project each onto both endpoints of the intra-group link.
            Variable from = link.from(), to = link.to();
            Variable repFrom = sharedVariables.translateForward(from);
            if (!Gate.isSet("NOMIRROR")
                && repFrom instanceof SharedVariable && repFrom.equals(sharedVariables.translateForward(to))) {
                for (Variable v : verticesPartOf(repFrom)) {
                    if (!v.equals(repFrom)) {
                        Variable fromSub = rehome(v, repFrom, from);
                        Variable toSub = rehome(v, repFrom, to);
                        // rehoming a §m member onto an endpoint that is itself x.§m stacks §m.§m — not
                        // representable as a Link; skip (same policy as vmiDirectionalFacts)
                        if (LinksImpl.LinkImpl.doNotStackMOnTopOfVirtualField(fromSub)
                            && LinksImpl.LinkImpl.doNotStackMOnTopOfVirtualField(toSub)) {
                            // a field-level mirror of a mediated whole-object link inherits the provenance
                            result.add(new LinksImpl.LinkImpl(fromSub, link.linkNature(), toSub, link.mediated()));
                        }
                    }
                }
                // A field of the SOURCE face may itself be collapsed into a DIFFERENT group ('this.i' lives in
                // {this.i, 0:i}), so its vertex sits under that group's rep and is invisible above. Project such
                // member-fields of the source onto the recipient: 'setI ← this' mirrors to 'setI.i ← this.i'
                // (source knowledge transfers to the recipient, techniques §1.2; never the reverse).
                if (link.linkNature().isAssignedFrom()) {
                    sharedVariables.memberFieldsOf(to).forEach(m -> {
                        Variable rehomed = rehome(m, to, from);
                        if (LinksImpl.LinkImpl.doNotStackMOnTopOfVirtualField(rehomed)
                            && LinksImpl.LinkImpl.doNotStackMOnTopOfVirtualField(m)) {
                            result.add(new LinksImpl.LinkImpl(rehomed, link.linkNature(), m, link.mediated()));
                        }
                    });
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
        Stream<Link> own = variables.stream()
                .flatMap(v -> virtualModificationIdenticals.groupsOfMember(v).flatMap(g -> g.expand(v)));
        // §m knowledge of an assignment SOURCE transfers to the recipient: 'return zs' collapses {return, zs}, and
        // zs.§m ≡ 0:in.§m (subList returns a view) must surface as return.§m ≡ 0:in.§m on the return's summary.
        // The VMI members are keyed on the source (primary(zs.§m) = zs), so variablesPartOf(primary) misses them;
        // rehome each source-face member onto the primary. Only the SOURCE direction transfers (see
        // isPureAssignmentSource for why the reverse must not).
        List<Link> inherited = new ArrayList<>();
        for (Variable face : sharedVariables.assignmentSources(primary)) {
            for (Variable v : virtualModificationIdenticals.variablesPartOf(face)) {
                Variable rehomed = rehome(v, face, primary);
                virtualModificationIdenticals.groupsOfMember(v)
                        .flatMap(g -> g.expand(rehomed)).forEach(inherited::add);
            }
        }
        return Stream.concat(own, inherited.stream());
    }


    public void clear(Variable variable, String statementIndex) {
        // eager: capture the group siblings AND the group's rep before the membership is severed — the
        // group's facts live on the REP vertex, so only the rep connects the BFS to the variables whose
        // rep-expansion is about to shrink (TestSimpleSharedVariable test4: this.field ∈ $__sv_copy.§$s)
        touchVariable(variable);
        Variable rep = sharedVariables.translateForward(variable);
        if (rep != variable) touchVariable(rep);
        touchAll(sharedVariables.allShared(variable));
        clearRepExpansionCache();
        sharedVariables.remove(variable);
        // remove the variable AND every graph vertex whose scope chain contains it (its virtual fields 'v.§f',
        // array accesses 'v.f[i]', ...): with the owner≻own-virtual-field spine each variable owns such vertices,
        // and leaving them orphaned (scope pointing at a removed variable) pollutes the graph and later closures.
        // materializeWitnessOrphans (inside removeVertices) first preserves knowledge between SURVIVORS whose
        // witnesses routed through any of these.
        Set<Variable> set;
        if (Gate.isSet("NODESC")) {
            set = Set.of(variable);
        } else {
            set = new HashSet<>(isKnownInGraph(variable));
            set.add(variable);
        }
        if (engine.removeVertices(set)) {
            engine.recompute(set, statementIndex, _ -> true);
        }
        // a cleared (reassigned) variable's mediation provenance is stale: the pair described the OLD value
        mediatedPairs.removeIf(pair -> {
            boolean remove = pair.stream().anyMatch(set::contains);
            if (remove) touchAll(pair); // the surviving partner's emissions change too
            return remove;
        });
    }

    // diagnostic: NOSV=1 in the environment disables the shared-variable collapse (assignments stay first-class
    // edges), so the O(N^2) part-of link explosion is not bounded. Used to demonstrate what sv prevents.
    private static final boolean NOSV = Gate.isSet("NOSV");

    boolean mergeEdgeBi(Variable from, LinkNature linkNature, Variable to, String statementIndex) {
        if (Gate.isSet("TRACEVAR")
            && (from.toString().contains(Gate.get("TRACEVAR")) || to.toString().contains(Gate.get("TRACEVAR")))) {
            System.out.println("TRACE mergeEdgeBi " + statementIndex + ": " + from + " " + linkNature + " " + to);
        }
        // boundary filter for the Options.objectGraphLinks label cut: PRODUCTION excludes ∩/≤/≥ from the
        // engine entirely, but some emitters produce them unconditionally (MakeGraph's slice ≤ base edge,
        // 'mapRight.§tts[-1] ≤ mapRight.§tts') — drop them here instead of tripping the engine's assert
        if (!engine.isValid(linkNature)) {
            return false;
        }
        if (from.equals(to)) {
            return engine.addVertex(from); // safety measure, is technically possible
        }
        if (invalidEdge(from, linkNature, to)) return false;
        if (linkNature.isIdenticalTo() && Util.isVirtualModification(from)) {
            boolean change = virtualModificationIdenticals.add(from, linkNature, to);
            // a VMI change never touches the engine, so record it here; the merged group's members all
            // gain each other's §m knowledge
            touchVariable(from);
            touchVariable(to);
            virtualModificationIdenticals.groupsOfMember(from).forEach(g -> touchAll(g.members()));
            virtualModificationIdenticals.groupsOfMember(to).forEach(g -> touchAll(g.members()));
            return change;
        }
        // only collapse whole-object assignment aliases; a hidden-content virtual field ('x ← optional.§x' — x is a
        // copy of the content, not an alias of the container) stays a first-class edge so 'x ← optional.§x' survives.
        if (!NOSV && linkNature.isAssignedFrom() && !(to instanceof MarkerVariable)
            && !Util.virtual(from) && !Util.virtual(to)) {
            // eager: capture both sides' current groups AND reps before any membership mutation below
            // (the rep vertex carries the group's facts; see clear())
            clearRepExpansionCache();
            touchVariable(from);
            touchVariable(to);
            Variable oldRepFrom = sharedVariables.translateForward(from);
            if (oldRepFrom != from) touchVariable(oldRepFrom);
            Variable oldRepTo = sharedVariables.translateForward(to);
            if (oldRepTo != to) touchVariable(oldRepTo);
            touchAll(sharedVariables.allShared(from));
            touchAll(sharedVariables.allShared(to));
            boolean fromInGroups = sharedVariables.isKnown(from);
            if (fromInGroups && !(from instanceof io.codelaser.maddi.modification.prepwork.variable.ReturnVariable)
                && sharedVariables.isReassignment(from, statementIndex)) {
                // genuine reassignment ('from' was assigned at an earlier statement, now assigned again): drop its
                // old group membership. A second value assigned in the SAME statement (multi-valued 'm = cond ? a : b'
                // -> 'm ← a' and 'm ← b') is NOT a reassignment; keep 'from' so both sources join one group.
                // A ReturnVariable is exempt: a second 'return' statement is a merge over paths, not a
                // reassignment — removing it abandons the group rep carrying the first path's knowledge.
                sharedVariables.remove(from);
                // TODO what with fromInGraph?
            } else if (!Gate.isSet("NORSRC")
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
            if (sv != null) {
                touchVariable(sv);
                touchAll(sv.variables());
            }
            if (mergedAway != null) {
                touchVariable(mergedAway);
                touchAll(mergedAway.variables());
            }
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
                // both sides are already direct members of the same group. Residual vertices spelled through a
                // member (faces added by statements AFTER it joined — e.g. the summary import of a later
                // 'v = wrap(v)' call in a reassignment-through-wrapper chain, timefold
                // ValueSelectorFactory.buildValueSelector) are legal on real code; re-key them onto the rep,
                // exactly as the sv != null path below does. (Historically an assert: "should already have been
                // removed" — the no-residue assumption only holds on the suite's simpler shapes.)
                SharedVariable group = sharedVariables.groupOf(from) != null
                        ? sharedVariables.groupOf(from) : sharedVariables.groupOf(to);
                if (group == null) return false;
                boolean change = false;
                if (!fromInGraph.isEmpty()) {
                    transformToSharedVariable(from, fromInGraph, group, statementIndex);
                    change = true;
                }
                Set<Variable> toResidue = isKnownInGraph(to);
                if (!toResidue.isEmpty()) {
                    transformToSharedVariable(to, toResidue, group, statementIndex);
                    change = true;
                }
                return change;
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
        // COPY, not view: every caller removes these vertices from the engine (clear,
        // transformToSharedVariable), which mutates the underlying index set via the listener.
        // LinkedHashSet keeps engine-insertion order (deterministic), like the view.
        return Collections.unmodifiableSet(new LinkedHashSet<>(verticesPartOf(variable)));
    }


    private void transformToSharedVariable(Variable variable,
                                           Set<Variable> variablesInGraph,
                                           SharedVariable sharedVariable,
                                           String statementIndex) {
        // FQN-sorted, NOT set-iteration order: variablesInGraph is an unmodifiable set (per-JVM SALTED
        // iteration), and the loop below re-adds each member's edges to the engine in this order — seed
        // order decides which derivation paths fire first (see IncrementalFixpointEngine.addSymmetricEdge)
        var forwardLinksList = variablesInGraph
                .stream()
                .sorted(java.util.Comparator.comparing(Variable::fullyQualifiedName))
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
                Variable newTo = Gate.isSet("NOBOTH") ? link.getKey()
                        : vtm.translateVariableRecursively(link.getKey());
                // re-homing a member's edges onto the shared representative can make the source coincide with the
                // target (the edge pointed at another member of the same group); skip the resulting self-loop,
                // which the engine forbids (Fact asserts source != target).
                if (newFrom.equals(newTo)) continue;
                if (Gate.isSet("TRACEVAR")
                    && (newFrom.toString().contains(Gate.get("TRACEVAR"))
                        || newTo.toString().contains(Gate.get("TRACEVAR")))) {
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
        return simpleAddToGraph(from, linkNature, to, statementIndex, false);
    }

    boolean simpleAddToGraph(Variable from, LinkNature linkNature, Variable to, String statementIndex,
                             boolean mediated) {
        if (mediated) markMediated(from, to);
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
        if (primary == null) return false; // e.g. an array access on an expression base: no primary
        if (!from.equals(primary) && !(primary instanceof This)
            && from instanceof FieldReference fr && primary.equals(fieldScopeRoot(from))) {
            // intermediate spine (gate NOSPINEI): a DEEP face 'entry.§xy.§x' also materializes the mid-level
            // chain 'entry ≻ entry.§xy ≻ entry.§xy.§x' — without the intermediate vertex the closure cannot
            // key facts on the mid-level face ('entry.§xy ≺ 0:optional', TestSupplier test7). The direct
            // 'entry ≻ entry.§xy.§x' fact still derives by ≻∘≻ composition.
            if (!Gate.isSet("NOSPINEI") && fr.scopeVariable() instanceof FieldReference parent
                && Util.virtual(from) && Util.virtual(parent.fieldInfo())) {
                boolean change = addField(parent, primary, statementIndex);
                change |= mergeEdgeBi(parent, CONTAINS_AS_FIELD, from, statementIndex);
                return change;
            }
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
        mediatedPairs.removeIf(pair -> {
            boolean remove = pair.stream().anyMatch(toRemove::contains);
            if (remove) touchAll(pair);
            return remove;
        });
    }

    public Set<Variable> replaceReturnAffected(Variable from, Variable to,
                                               LinkNature currentLinkNature,
                                               LinkNature newLinkNature,
                                               String skipStatementIndex,
                                               Predicate<Fact<Variable, LinkNature>> acceptRaw) {
        return engine.replaceReturnAffected(from, to, currentLinkNature, newLinkNature, skipStatementIndex,
                acceptRaw);
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
