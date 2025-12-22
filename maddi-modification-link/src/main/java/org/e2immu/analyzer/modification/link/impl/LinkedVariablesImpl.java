package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public record LinkedVariablesImpl(Map<Variable, Links> links) implements LinkedVariables {
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
