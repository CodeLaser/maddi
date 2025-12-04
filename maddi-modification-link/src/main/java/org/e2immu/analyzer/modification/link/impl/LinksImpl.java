package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LinksImpl(List<Link> links) implements Links {
    public static final Links EMPTY = new LinksImpl(List.of());
    public static final Property LINKS = new PropertyImpl("links", EMPTY);

    public LinksImpl(Variable from, LinkNature linkNature, Variable to) {
        this(List.of(new LinkImpl(from, linkNature, to)));
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefault() {
        return equals(EMPTY);
    }

    @Override
    public @NotNull Iterator<Link> iterator() {
        return links.iterator();
    }

    @Override
    public @NotNull String toString() {
        return links.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    @Override
    public Links merge(Links links) {
        if (links.isEmpty()) return this;
        Link otherFirst = links.theObjectItself();
        Link myFirst = theObjectItself();
        if (otherFirst != null) {
            if (myFirst != null) {
                // the other one also has a first; we'll skip that one
                return new LinksImpl(Stream.concat(this.links.stream(), links.links().stream().skip(1)).toList());
            }
            // i don't have one, so we move other first
            return new LinksImpl(Stream.concat(Stream.concat(Stream.of(otherFirst), this.links.stream()),
                    links.links().stream().skip(1)).toList());
        }
        // simply concat
        return new LinksImpl(Stream.concat(this.links.stream(), links.links().stream()).toList());
    }
}
