package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VirtualModificationIdenticals {

    public record Group(LinkNature linkNature, Set<Variable> members) {
        public Stream<Link> expand(Variable v1) {
            Stream.Builder<Link> links = Stream.builder();
            for (Variable v2 : members) {
                if (v1 != v2) {
                    links.accept(new LinksImpl.LinkImpl(v1, linkNature, v2));
                }
            }
            return links.build();
        }
    }

    private static final Group EMPTY = new Group(null, Set.of());

    private final AtomicInteger groupIdProvider = new AtomicInteger();
    private final Map<Integer, Group> groups = new HashMap<>();
    // a member can sit in SEVERAL groups, one per ≡-nature: the ☷ pass set is a property of the EDGE
    // ('identical except via remove()' between an iterator and its collection), and folding a pass-marked
    // pair into a strict group discarded the pass — iterating a collection (next()) then counted as a strict
    // modification of everything §m-equivalent to it (the TestMap reverse0 ⊆→~ flip).
    private final Map<Variable, Set<Integer>> memberToGroup = new HashMap<>();

    public String print(Function<Variable, String> variablePrinter) {
        return groups.entrySet().stream()
                .map(e -> e.getKey() + ": "
                          + e.getValue().linkNature + " "
                          + e.getValue().members.stream().sorted(Variable::compareTo)
                                  .map(variablePrinter)
                                  .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }


    public void remove(Set<Variable> variables) {
        memberToGroup.keySet().removeAll(variables);
        groups.values().forEach(g -> g.members.removeAll(variables));
    }

    public Iterable<Map.Entry<Variable, Map<Variable, LinkNature>>> edges() {
        Map<Variable, Map<Variable, LinkNature>> map = new HashMap<>();
        for (Group group : groups.values()) {
            for (Variable v1 : group.members) {
                Map<Variable, LinkNature> sub = new HashMap<>();
                for (Variable v2 : group.members) {
                    if (v1 != v2) sub.put(v2, group.linkNature);
                }
                map.put(v1, sub);
            }
        }
        return map.entrySet();
    }

    // all groups the member sits in (one per ≡-nature)
    public Stream<Group> groupsOfMember(Variable variable) {
        return memberToGroup.getOrDefault(variable, Set.of()).stream()
                .map(g -> groups.getOrDefault(g, EMPTY));
    }

    public Set<Variable> variablesPartOf(Variable primary) {
        return memberToGroup.keySet().stream().filter(v -> primary.equals(Util.primary(v)))
                .collect(Collectors.toUnmodifiableSet());
    }

    // pass semantics are per-EDGE: a strict ≡ and a pass-marked ☷ between the same members live in
    // DIFFERENT groups (matched on the pass set), never merged
    private static boolean sameNature(LinkNature a, LinkNature b) {
        return a.pass().equals(b.pass());
    }

    private Integer groupWithNature(Variable v, LinkNature linkNature) {
        for (Integer g : memberToGroup.getOrDefault(v, Set.of())) {
            Group group = groups.get(g);
            if (group != null && sameNature(group.linkNature, linkNature)) return g;
        }
        return null;
    }

    public boolean add(Variable v1, LinkNature linkNature, Variable v2) {
        Integer g1 = groupWithNature(v1, linkNature);
        Integer g2 = groupWithNature(v2, linkNature);
        if (g1 == null && g2 == null) {
            int newGroupId = groupIdProvider.incrementAndGet();
            Set<Variable> set = new LinkedHashSet<>();
            set.add(v1);
            set.add(v2);
            groups.put(newGroupId, new Group(linkNature, set));
            join(v1, newGroupId);
            join(v2, newGroupId);
            return true;
        }
        if (g1 == null) {
            groups.get(g2).members.add(v1);
            join(v1, g2);
            return true;
        }
        if (g2 == null) {
            groups.get(g1).members.add(v2);
            join(v2, g1);
            return true;
        }
        int g1i = g1;
        int g2i = g2;
        if (g1i == g2i) {
            return false;
        }
        // merge g2 into g1 (same nature by construction)
        Group group2 = groups.get(g2);
        groups.get(g1).members.addAll(group2.members);
        groups.remove(g2);
        for (Variable v : group2.members) {
            Set<Integer> set = memberToGroup.get(v);
            if (set != null) set.remove(g2i);
            join(v, g1i);
        }
        return true;
    }

    private void join(Variable v, int groupId) {
        memberToGroup.computeIfAbsent(v, _ -> new LinkedHashSet<>()).add(groupId);
    }

    public Stream<Variable> variables() {
        return memberToGroup.keySet().stream();
    }

    // given v, and v.§m ≡ w.§m ≡ x.§m, return v, w, x
    public Stream<Variable> equivalentStream(Variable variable) {
        Set<Integer> groups = memberToGroup.entrySet().stream()
                .filter(e -> variable.equals(Util.firstRealVariable(e.getKey())))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toUnmodifiableSet());
        return groups.stream().flatMap(g -> this.groups.getOrDefault(g, EMPTY).members.stream())
                .map(Util::firstRealVariable);
    }

    // the groups (with their nature, which carries the ☷ pass set) that 'variable' belongs to via its §m member.
    // Unlike equivalentStream, this keeps the group boundary so modification propagation can respect the pass
    // semantics ('identical except via remove()', see Iterable.iterator()).
    public Stream<Group> groupsOf(Variable variable) {
        return memberToGroup.entrySet().stream()
                .filter(e -> variable.equals(Util.firstRealVariable(e.getKey())))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .map(g -> groups.getOrDefault(g, EMPTY));
    }
}
