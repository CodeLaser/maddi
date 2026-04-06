package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record FollowGraph(Timer timer) {

    // sorting is needed to consistently take the same direction for tests
    public Links.Builder followGraph(VirtualFieldComputer virtualFieldComputer,
                                     Map<Variable, Map<Variable, LinkNature>> graph,
                                     Variable primary) {
        timer.start("follow1");
        Links.Builder builder = new LinksImpl.Builder(primary);
        List<Variable> fromList = primary instanceof This ? (graph.containsKey(primary) ? List.of(primary) : List.of())
                : graph.keySet().stream()
                .filter(v -> Util.isPartOf(primary, v))
                .sorted((v1, v2) -> {
                    if (v1.equals(v2)) return 0;
                    if (Util.isPartOf(v1, v2)) return 1;
                    if (Util.isPartOf(v2, v1)) return -1;
                    // FIXME still an issue occasionally
                    return Util.isPartOfComparator(v1, v2);
                })
                .toList();

        // stream.§$s⊆0:in.§$s
        Set<Edge> block = new HashSet<>();
        timer.end("follow1");
        for (Variable from : fromList) {
            timer.start("fixpoint");
            Map<Variable, LinkNature> all = bestPath(graph, from);
            timer.end("fixpoint");
            timer.start("follow2");
            List<Map.Entry<Variable, LinkNature>> entries = all.entrySet().stream()
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
            timer.end("follow2");

            Variable primaryFrom = Util.primary(from);
            if (primaryFrom != null) {
                timer.start("follow3");
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
                    if (linkNature.known()
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
                timer.end("follow3");
            }
        }
        return builder;
    }

    static Map<Variable, LinkNature> bestPath(Map<Variable, Map<Variable, LinkNature>> graph, Variable start) {
        Map<Variable, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNatureImpl.EMPTY,
                        LinkNature::combine);
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(LinkNatureImpl.EMPTY, LinkNature::best)));
    }

}
