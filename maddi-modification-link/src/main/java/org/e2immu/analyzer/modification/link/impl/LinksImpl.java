package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LinksImpl(Variable primary, List<Link> links) implements Links {
    public static final Links EMPTY = new LinksImpl(null, List.of());
    public static final Property LINKS = new PropertyImpl("links", EMPTY);

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
        return new LinksImpl(primary, Stream.concat(this.links.stream(), links.links().stream())
                .distinct()
                .sorted((l1, l2) -> {
                    boolean l1IsPrimary = primary.equals(l1.from());
                    boolean l2IsPrimary = primary.equals(l2.from());
                    if (l1IsPrimary && !l2IsPrimary) return -1;
                    if (l2IsPrimary && !l1IsPrimary) return 1;
                    return l1.from().compareTo(l2.from());
                })
                .toList());
    }

    public static class Builder implements Links.Builder {
        private final Variable primary;
        private final List<Link> links = new ArrayList<>();

        public Builder(Variable primary) {
            this.primary = primary;
        }

        public Builder add(LinkNature linkNature, Variable to) {
            links.add(new LinkImpl(primary, linkNature, to));
            return this;
        }

        public Builder add(Variable from, LinkNature linkNature, Variable to) {
            assert assertIsPartOfPrimary(from);
            links.add(new LinkImpl(from, linkNature, to));
            return this;
        }

        private boolean assertIsPartOfPrimary(Variable from) {
            if (primary.equals(from)) return true;
            if (from instanceof DependentVariable dv) return assertIsPartOfPrimary(dv.arrayVariableBase());
            if (from instanceof FieldReference fr) return primary.equals(fr.fieldReferenceBase());
            return false;
        }

        public Links build() {
            return new LinksImpl(primary, List.copyOf(links));
        }
    }

    private record LinkImpl(Variable from, LinkNature linkNature, Variable to) implements Link {

        public LinkImpl {
            assert from != null;
            assert to != null;
            assert linkNature != null;
        }

        @Override
        public @NotNull String toString() {
            return simpleVar(from) + linkNature + simpleVar(to);
        }
    }

    private static String simpleVar(Variable variable) {
        if (variable instanceof ParameterInfo pi) {
            return pi.index() + ":" + pi.name();
        }
        return variable.toString();
    }

    // used by LVC
    @Override
    public Links changePrimaryTo(Variable newPrimary, TranslationMap tm) {
        Links.Builder builder = new LinksImpl.Builder(newPrimary);
        for (Link link : links) {
            if (link.from().equals(primary)) {
                Variable translated = tm == null ? link.to() : tm.translateVariableRecursively(link.to());
                builder.add(link.linkNature(), translated);
            } else throw new UnsupportedOperationException("NYI");
        }
        return builder.build();
    }
}
