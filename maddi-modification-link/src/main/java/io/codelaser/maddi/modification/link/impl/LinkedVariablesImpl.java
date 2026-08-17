package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.modification.prepwork.Util;
import io.codelaser.maddi.modification.prepwork.variable.LinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public record LinkedVariablesImpl(Map<Variable, Links> links) implements LinkedVariables {

    // NOTE (2026-08-01, bistability investigation, docs/eventual-info-hierarchy.md): an FQN-sorted
    // canonical constructor was tried here and REVERTED — it deterministically re-labels the §-face
    // indices and fails TestForEachLambda's ~/∩ pairing pins (and did not resolve the 24↔10 dogfood
    // bistability). If iteration order is canonicalized here again, those pins must be re-derived
    // together with the face-minting order.
    public final static LinkedVariables EMPTY = new LinkedVariablesImpl(Map.of());

    @Override
    public boolean isEmpty() {
        return links.isEmpty();
    }

    @Override
    @NotNull
    public Iterator<Map.Entry<Variable, Links>> iterator() {
        return links.entrySet().iterator();
    }

    @Override
    public LinkedVariables merge(LinkedVariables other) {
        if (this.isEmpty()) return other;
        if (other.isEmpty()) return this;
        HashMap<Variable, Links> map = new HashMap<>(links);
        other.map().forEach((v, l)->map.merge(v, l, Links::merge));
        // deliberately NOT Map.copyOf: the JDK's immutable maps iterate in a per-JVM SALTED order,
        // while a HashMap keyed by Variable (FQN-based hashCode) iterates identically across runs
        return new LinkedVariablesImpl(map);
    }

    @Override
    public @NotNull String toString() {
        return links.entrySet().stream()
                .map(e -> Util.simpleName(e.getKey()) + ": " + e.getValue())
                .sorted()
                .collect(Collectors.joining("; "));
    }

    @Override
    public Map<Variable, Links> map() {
        return links;
    }
}
