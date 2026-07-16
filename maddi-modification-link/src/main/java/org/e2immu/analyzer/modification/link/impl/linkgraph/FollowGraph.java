package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public record FollowGraph(Graph graph) {
    // sorting is needed to consistently take the same direction for tests
    // queryFrom is the graph vertex we read the closure of; emitFrom is the variable the resulting link is keyed
    // on. They differ for a shared-variable rep: its field-precise member was collapsed out of the graph, so we
    // read the rep's closure but emit keyed on the member (which is what is part of the primary).
    private record FromPair(Variable queryFrom, Variable emitFrom) {
    }

    public Links.Builder followGraph(VirtualFieldComputer virtualFieldComputer, Variable primary) {
        Links.Builder builder = new LinksImpl.Builder(primary);
        if (System.getenv("SBDUMP") != null && System.getenv("SBDUMP").equals(primary.simpleName())) {
            System.out.println("SBDUMP closure with witnesses:");
            System.out.println(graph.printClosure());
        }
        // A vertex contributes when it, or one of its shared-variable-rep expansions (a rep as the whole vertex
        // or nested in a field scope, e.g. '$__sv_list1.§$s' -> 'a.list1.§$s'), is part of the primary. We read
        // the closure of the graph vertex (queryFrom) but key the emitted link on the member form (emitFrom).
        List<FromPair> fromList = new ArrayList<>();
        if (primary instanceof This) {
            if (graph.containsVariable(primary)) fromList.add(new FromPair(primary, primary));
        } else {
            // When 'primary' is itself a shared-variable rep (the extraction of a collapsed variable, e.g. a return
            // aliased by 'return a'), its group members are alternative "faces" of the same value. A vertex whose
            // rep-expansion is part of a face f (e.g. 'a.list1.§$s' part of the sibling 'a') belongs to the primary
            // too; we emit it keyed on the face->primary rehomed form ('$__sv_return.list1.§$s'), which the caller's
            // builder2 then rehomes to the real variable ('method1.list1.§$s').
            List<Variable> faces = primary instanceof SharedVariable sv
                    ? List.copyOf(sv.variables()) : List.of(primary);
            for (Variable v : graph.variables()) {
                if (Util.isPartOf(primary, v)) {
                    fromList.add(new FromPair(v, v));
                    continue;
                }
                // a shared-variable rep somewhere in v's scope chain, standing for a member part of a face (a
                // whole-vertex rep '$__sv_list1', or one nested in a field scope '$__sv_list1.§$s' -> 'a.list1.§$s').
                // The !e.equals(v) guard keeps ordinary vertices on the fast path (expandRepToMembers rebuilds
                // equal-but-distinct FieldReferences, which must not replace the original).
                graph.expandRepToMembers(v).filter(e -> !e.equals(v)).forEach(e -> {
                    for (Variable face : faces) {
                        if (Util.isPartOf(face, e)) {
                            Variable emit = face.equals(primary) ? e : graph.rehome(e, face, primary);
                            fromList.add(new FromPair(v, emit));
                            return;
                        }
                    }
                    // Derived from-pair (gate NODFP): 'e' (rv6.bodyThrowingFunction, a face of the collapsed
                    // builder chain) is not part of the primary, but a RECIPIENT group-sibling of it
                    // (td.throwingFunction ← rv6.bodyThrowingFunction) is. The vertex 'v'
                    // ($__sv_$__rv6.bodyThrowingFunction) then carries knowledge the collapse stranded on the
                    // chain rep's field face (the ←Λ$_fi method-reference edge): read v's closure, emit keyed
                    // on the recipient face. Only the source→recipient direction transfers (assignmentSources).
                    if (System.getenv("NODFP") == null) {
                        for (Variable sibling : graph.allShared(e)) {
                            if (sibling.equals(e) || !graph.assignmentSources(sibling).contains(e)) continue;
                            for (Variable face : faces) {
                                if (Util.isPartOf(face, sibling)) {
                                    Variable emit = face.equals(primary) ? sibling
                                            : graph.rehome(sibling, face, primary);
                                    fromList.add(new FromPair(v, emit));
                                    return;
                                }
                            }
                        }
                    }
                });
            }
        }
        // canonical total pre-sort: the parts-first comparator below is a PARTIAL order (and intransitive
        // across its branches), so TimSort's result depends on the input order — which comes from unordered
        // graph.variables() and flickered run-to-run (the m∩copy determinism flake: emission order feeds the
        // block-set redundancy suppression). A stable sort on top of a canonical order is deterministic.
        fromList.sort(java.util.Comparator
                .comparing((FromPair p) -> p.emitFrom().fullyQualifiedName())
                .thenComparing(p -> p.queryFrom().fullyQualifiedName()));
        // parts-first refinement: the comparator is a PARTIAL, intransitive order, and TimSort THROWS
        // ('Comparison method violates its general contract') on inputs where it detects the inconsistency
        // (first seen on the real parseq code, via the VL2O probe). A stable insertion sort tolerates the
        // partial order, is deterministic on top of the canonical pre-sort, and preserves the exact ordering
        // TimSort produced on the inputs it accepted. fromList is per-primary and small.
        insertionSortPartsFirst(fromList);

        List<LinksImpl.LinkImpl> reverseReturnFacts = new ArrayList<>();
        // Engine feature #9: composite facts TARGETING a return variable are never created
        // (acceptForComposite), so 'return run ↖ 1:r.function' exists keyed on the return vertex only.
        // For a non-return primary, read the return vertices' closures and emit the reverse
        // ('1:r.function ↗ run') keyed on the primary's face. Gate NORVREV.
        if (!(primary instanceof ReturnVariable) && System.getenv("NORVREV") == null) {
            for (Variable v : graph.variables()) {
                if (!(Util.primary(v) instanceof ReturnVariable)) continue;
                List<Variable> faces = primary instanceof SharedVariable sv
                        ? List.copyOf(sv.variables()) : List.of(primary);
                graph.closureStream(v).forEach(entry -> {
                    Variable to = entry.getKey();
                    Variable emit = null;
                    if (Util.isPartOf(primary, to)) {
                        emit = to;
                    } else {
                        for (Variable e : graph.expandRepToMembers(to).filter(e -> !e.equals(to)).toList()) {
                            for (Variable face : faces) {
                                if (Util.isPartOf(face, e)) {
                                    emit = face.equals(primary) ? e : graph.rehome(e, face, primary);
                                    break;
                                }
                            }
                            if (emit != null) break;
                        }
                    }
                    LinkNature reversed = entry.getValue().reverse();
                    if (emit != null && reversed.valid() && Util.acceptModificationLink(emit, v)) {
                        reverseReturnFacts.add(new LinksImpl.LinkImpl(emit, reversed, v));
                    }
                });
            }
        }

        // stream.§$s⊆0:in.§$s
        Set<Edge> block = new HashSet<>();
        for (FromPair fromPair : fromList) {
            Variable from = fromPair.emitFrom();
            Stream<Map.Entry<Variable, LinkNature>> all = graph.closureStream(fromPair.queryFrom());

            List<Map.Entry<Variable, LinkNature>> entries = all
                    .sorted((e1, e2) -> {
                        int c = e2.getValue().rank() - e1.getValue().rank();
                        if (c != 0) return c;
                        boolean p1 = Util.isPrimary(e1.getKey());
                        boolean p2 = Util.isPrimary(e2.getKey());
                        if (p1 && !p2) return 1;
                        if (p2 && !p1) return -1;
                        // subs first, best score first
                        return e1.getKey().fullyQualifiedName().compareTo(e2.getKey().fullyQualifiedName());
                    })
                    .toList();

            Variable primaryFrom = Util.primary(from);
            if (primaryFrom != null) {
                Variable firstRealFrom = Util.firstRealVariable(from);

                //LOGGER.debug("Entries of {}: {}", from, entries);

                for (Map.Entry<Variable, LinkNature> entry : entries) {
                    LinkNature linkNature = entry.getValue();
                    Variable to = entry.getKey();
                    Variable primaryTo = Util.primary(to);
                    if (primaryTo == null) continue;//array expression not a variable
                    Variable firstRealTo = Util.firstRealVariable(to);
                    // remove internal references (field inside primary to primary or other field in primary)
                    // see TestStaticValues1,5 for an example where s.k ← s.r.i, which requires the 2nd clause
                    if (linkNature.valid()
                        && (!primaryTo.equals(primaryFrom) ||
                            !firstRealFrom.equals(primaryFrom) &&
                            !firstRealTo.equals(primaryTo) &&
                            !firstRealFrom.equals(firstRealTo))
                        && Util.acceptModificationLink(from, to)
                        && block.add(new Edge(from, linkNature, to))) {
                        builder.add(from, linkNature, to);
                        if (linkNature.isIdenticalToOrAssignedFromTo()
                            && !(primaryTo instanceof ReturnVariable) && !(primaryFrom instanceof ReturnVariable)
                            && !Util.virtual(from)
                            && !Util.virtual(to)
                            // no §m equivalence with an opaque someValue marker ('method.§m ≡ $_v.§m' is noise)
                            && !(primaryTo instanceof MarkerVariable mv && mv.isSomeValue())
                            && virtualFieldComputer != null) {
                            VirtualFieldComputer.M2 m2 = virtualFieldComputer.addModificationFieldEquivalence(from, to);
                            LinkNature id = LinkNatureImpl.makeIdenticalTo(null);
                            if (m2 != null && !builder.contains(m2.m1(), id, m2.m2())) {
                                builder.add(m2.m1(), id, m2.m2());
                            }
                        }
                        // don't add if the reverse is already present in this builder
                        block.add(new Edge(to, linkNature.reverse(), from));
                        // when adding p.sub < q.sub, don't add p < q.sub, p.sub < q
                        Set<Variable> scopeFrom = Util.scopeVariables(from);
                        for (LinkNature lnUp : linkNature.redundantFromUp()) {
                            for (Variable sv : scopeFrom) {
                                block.add(new Edge(sv, lnUp, to));
                            }
                        }
                        Set<Variable> scopeTo = Util.scopeVariables(to);
                        for (LinkNature lnUp : linkNature.redundantToUp()) {
                            for (Variable sv : scopeTo) {
                                block.add(new Edge(from, lnUp, sv));
                            }
                        }
                        for (LinkNature lnBoth : linkNature.redundantUp()) {
                            for (Variable fromUp : scopeFrom) {
                                for (Variable toUp : scopeTo) {
                                    block.add(new Edge(fromUp, lnBoth, toUp));
                                }
                            }
                        }
                    }
                }
            }
        }
        // append the reversed return-targeted facts in canonical order (they were collected iterating
        // unordered graph.variables()); the main loop registered both directions of every emitted link
        // in 'block', so a plain add-check dedups against it
        reverseReturnFacts.sort(java.util.Comparator
                .comparing((LinksImpl.LinkImpl l) -> l.from().fullyQualifiedName())
                .thenComparing(l -> l.linkNature().rank())
                .thenComparing(l -> l.to().fullyQualifiedName()));
        for (LinksImpl.LinkImpl link : reverseReturnFacts) {
            if (block.add(new Edge(link.from(), link.linkNature(), link.to()))) {
                builder.add(link.from(), link.linkNature(), link.to());
                block.add(new Edge(link.to(), link.linkNature().reverse(), link.from()));
            }
        }
        return builder;
    }

    private static void insertionSortPartsFirst(java.util.List<FromPair> fromList) {
        for (int i = 1; i < fromList.size(); i++) {
            FromPair p = fromList.get(i);
            int j = i - 1;
            while (j >= 0 && partsFirst(fromList.get(j), p) > 0) {
                fromList.set(j + 1, fromList.get(j));
                j--;
            }
            fromList.set(j + 1, p);
        }
    }

    private static int partsFirst(FromPair p1, FromPair p2) {
        Variable v1 = p1.emitFrom();
        Variable v2 = p2.emitFrom();
        if (v1.equals(v2)) return 0;
        if (Util.isPartOf(v1, v2)) return 1;
        if (Util.isPartOf(v2, v1)) return -1;
        return Util.isPartOfComparator(v1, v2);
    }
}
