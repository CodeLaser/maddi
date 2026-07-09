package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
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
        // A vertex contributes when it, or one of its shared-variable-rep expansions (a rep as the whole vertex
        // or nested in a field scope, e.g. '$__sv_list1.§$s' -> 'a.list1.§$s'), is part of the primary. We read
        // the closure of the graph vertex (queryFrom) but key the emitted link on the member form (emitFrom).
        List<FromPair> fromList = new ArrayList<>();
        if (primary instanceof This) {
            if (graph.containsVariable(primary)) fromList.add(new FromPair(primary, primary));
        } else {
            for (Variable v : graph.variables()) {
                if (Util.isPartOf(primary, v)) {
                    fromList.add(new FromPair(v, v));
                } else {
                    // a shared-variable rep somewhere in v's scope chain, standing for a member that is part of the
                    // primary (a whole-vertex rep '$__sv_list1', or one nested in a field scope '$__sv_list1.§$s'
                    // -> 'a.list1.§$s'). Query the rep's closure, emit keyed on the member form. The !e.equals(v)
                    // guard keeps ordinary vertices on the fast path (expandRepToMembers rebuilds equal-but-distinct
                    // FieldReferences, which must not replace the original).
                    graph.expandRepToMembers(v)
                            .filter(e -> !e.equals(v) && Util.isPartOf(primary, e))
                            .forEach(e -> fromList.add(new FromPair(v, e)));
                }
            }
        }
        fromList.sort((p1, p2) -> {
            Variable v1 = p1.emitFrom();
            Variable v2 = p2.emitFrom();
            if (v1.equals(v2)) return 0;
            if (Util.isPartOf(v1, v2)) return 1;
            if (Util.isPartOf(v2, v1)) return -1;
            // FIXME still an issue occasionally
            return Util.isPartOfComparator(v1, v2);
        });

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
        return builder;
    }
}
