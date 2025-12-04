package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.Links;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public record LinksImpl(List<Link> links) implements Links {
    public static final Links EMPTY = new LinksImpl(List.of());

    @Override
    public @NotNull Iterator<Link> iterator() {
        return links.iterator();
    }

    @Override
    public @NotNull String toString() {
        return links.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}
