package org.e2immu.analyzer.modification.link.impl.graph;

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

public class EquivalenceGroup {
    public String print(Function<Variable, String> variablePrinter) {
        return groups.entrySet().stream()
                .map(e -> e.getKey() + ": "
                          + e.getValue().linkNature + " "
                          + e.getValue().members.stream().sorted(Variable::compareTo)
                                  .map(variablePrinter)
                                  .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

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
    private final Map<Variable, Integer> memberToGroup = new HashMap<>();

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

    public Group members(Variable variable) {
        Integer groupId = memberToGroup.get(variable);
        if (groupId == null) return EMPTY;
        Group group = groups.get(groupId);
        assert group != null && !group.members.isEmpty();
        return group;
    }

    public Set<Variable> variablesPartOf(Variable primary) {
        return memberToGroup.keySet().stream().filter(v -> primary.equals(Util.primary(v)))
                .collect(Collectors.toUnmodifiableSet());
    }

    private enum AddResultEnum {
        NEW, ADD, MERGE, NONE;
    }

    public record AddResult(AddResultEnum result, Variable representativeToRemove) {
        public boolean isAddOrNew() {
            return result == AddResultEnum.ADD || result == AddResultEnum.NEW;
        }

        public boolean isMerge() {
            return result == AddResultEnum.MERGE;
        }
    }

    private static final AddResult AR_NEW = new AddResult(AddResultEnum.NEW, null);
    private static final AddResult AR_ADD = new AddResult(AddResultEnum.ADD, null);
    private static final AddResult AR_NONE = new AddResult(AddResultEnum.NONE, null);

    public AddResult add(Variable v1, LinkNature linkNature, Variable v2) {
        Integer g1 = memberToGroup.get(v1);
        Integer g2 = memberToGroup.get(v2);
        if (g1 == null && g2 == null) {
            int newGroupId = groupIdProvider.incrementAndGet();
            Set<Variable> set = new LinkedHashSet<>();
            set.add(v1);
            set.add(v2);
            groups.put(newGroupId, new Group(linkNature, set));
            memberToGroup.put(v1, newGroupId);
            memberToGroup.put(v2, newGroupId);
            return AR_NEW;
        }
        if (g1 == null) {
            groups.get(g2).members.add(v1);
            memberToGroup.put(v1, g2);
            return AR_ADD;
        }
        if (g2 == null) {
            groups.get(g1).members.add(v2);
            memberToGroup.put(v2, g1);
            return AR_ADD;
        }
        int g1i = g1;
        int g2i = g2;
        if (g1i == g2i) {
            return AR_NONE;
        }
        // merge g2 into g1
        Group group2 = groups.get(g2);
        Variable representative = group2.members.stream().findFirst().orElseThrow();
        groups.get(g1).members.addAll(group2.members);
        groups.remove(g2);
        group2.members.forEach(v -> memberToGroup.put(v, g1));
        return new AddResult(AddResultEnum.MERGE, representative);
    }

    public Variable representative(Variable v) {
        Integer gId = memberToGroup.get(v);
        if (gId != null) {
            return groups.get(gId).members.stream().findFirst().orElseThrow();
        }
        return v;
    }
}
