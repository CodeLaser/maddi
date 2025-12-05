package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkedVariablesImpl implements LinkedVariables {
    public final static LinkedVariables EMPTY = new LinkedVariablesImpl(Map.of());

    private final Map<Variable, Links> links;

    public LinkedVariablesImpl(Map<Variable, Links> links) {
        this.links = links;
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefault() {
        return EMPTY.equals(this);
    }

    @Override
    public boolean isEmpty() {
        return links.isEmpty();
    }

    @Override
    @org.jetbrains.annotations.NotNull
    public Iterator<Map.Entry<Variable, Links>> iterator() {
        return links.entrySet().iterator();
    }

    @Override
    public int size() {
        return links.size();
    }

    @Override
    public LinkedVariables merge(LinkedVariables other) {
        if (this.isEmpty()) return other;
        if (other.isEmpty()) return this;
        HashMap<Variable, Links> map = new HashMap<>(links);
        map.putAll(((LinkedVariablesImpl) other).links);
        return new LinkedVariablesImpl(map);
    }

    @Override
    public String toString() {
        return links.entrySet().stream()
                .map(e -> LinksImpl.simpleVar(e.getKey()) + ": " + e.getValue())
                .collect(Collectors.joining("; "));
    }

    @Override
    public Map<Variable, Links> map() {
        return links;
    }
}
