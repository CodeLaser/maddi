package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public record LinksImpl(List<Link> links) implements Links {
    public static final Links EMPTY = new LinksImpl(List.of());

    public LinksImpl(Variable from, LinkNature linkNature, Variable to) {
        this(List.of(new LinkImpl(from, linkNature, to)));
    }

    @Override
    public @NotNull Iterator<Link> iterator() {
        return links.iterator();
    }

    @Override
    public @NotNull String toString() {
        return links.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}
