package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EquivalenceGroup {
    private final AtomicInteger groupIdProvider = new AtomicInteger();
    private final Map<Integer, Set<Variable>> groups = new HashMap<>();
    private final Map<Variable, Integer> memberToGroup = new HashMap<>();

    public Stream<Set<Variable>> groupsStream() {
        return groups.values().stream();
    }

    public Set<Variable> members(Variable variable) {
        Integer groupId = memberToGroup.get(variable);
        if (groupId == null) return Set.of();
        Set<Variable> group = groups.get(groupId);
        assert group != null && !group.isEmpty();
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

    public AddResult add(Variable v1, Variable v2) {
        Integer g1 = memberToGroup.get(v1);
        Integer g2 = memberToGroup.get(v2);
        if (g1 == null && g2 == null) {
            int newGroupId = groupIdProvider.incrementAndGet();
            Set<Variable> set = new LinkedHashSet<>();
            set.add(v1);
            set.add(v2);
            groups.put(newGroupId, set);
            memberToGroup.put(v1, newGroupId);
            memberToGroup.put(v2, newGroupId);
            return AR_NEW;
        }
        if (g1 == null) {
            groups.get(g2).add(v1);
            memberToGroup.put(v1, g2);
            return AR_ADD;
        }
        if (g2 == null) {
            groups.get(g1).add(v2);
            memberToGroup.put(v2, g1);
            return AR_ADD;
        }
        int g1i = g1;
        int g2i = g2;
        if (g1i == g2i) {
            return AR_NONE;
        }
        // merge g2 into g1
        Set<Variable> group2 = groups.get(g2);
        Variable representative = group2.stream().findFirst().orElseThrow();
        groups.get(g1).addAll(group2);
        groups.remove(g2);
        group2.forEach(v -> memberToGroup.put(v, g1));
        return new AddResult(AddResultEnum.MERGE, representative);
    }

    public Variable representative(Variable v) {
        Integer gId = memberToGroup.get(v);
        if (gId != null) {
            return groups.get(gId).stream().findFirst().orElseThrow();
        }
        return v;
    }
}
