package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinksImpl implements Links {
    public static final Links EMPTY = new LinksImpl();
    public static final Property LINKS = new PropertyImpl("links", EMPTY);
    public static final String LAMBDA = "Λ";
    private final Variable primary;
    private final Set<Link> linkSet;

    // private constructor to bypass the non-null requirement for the primary
    private LinksImpl() {
        this.primary = null;
        this.linkSet = Set.of();
    }

    public LinksImpl(Variable primary, Set<Link> linkSet) {
        this.primary = Objects.requireNonNull(primary);
        this.linkSet = Objects.requireNonNull(linkSet);
    }

    @Override
    public Variable primary() {
        return primary;
    }

    @Override
    public Set<Link> linkSet() {
        return linkSet;
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
    public boolean isEmpty() {
        return linkSet.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LinksImpl other)) return false;
        return Objects.equals(primary, other.primary);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(primary);
    }

    @Override
    public @NotNull Iterator<Link> iterator() {
        return linkSet.iterator();
    }

    @Override
    public @NotNull String toString() {
        if (isEmpty()) return "-";
        return linkSet.stream().map(Object::toString).sorted().collect(Collectors.joining(","));
    }

    @Override
    public Links merge(Links links) {
        return new LinksImpl(primary, Stream.concat(this.linkSet.stream(), links.linkSet().stream())
                .collect(Collectors.toUnmodifiableSet()));
    }

    public static class Builder implements Links.Builder {
        private final Variable primary;
        private final List<Link> links = new ArrayList<>();

        public Builder(Variable primary) {
            this.primary = primary;
        }

        Builder(Links existing) {
            this.primary = existing.primary();
            links.addAll(existing.linkSet());
        }

        @Override
        public Variable primary() {
            return primary;
        }

        @Override
        public Builder add(LinkNature linkNature, Variable to) {
            links.add(new LinkImpl(primary, linkNature, to));
            return this;
        }

        @Override
        public Builder add(Variable from, LinkNature linkNature, Variable to) {
            assert primary instanceof This || Util.isPartOf(primary, from);
            links.add(new LinkImpl(from, linkNature, to));
            return this;
        }

        @Override
        public Links.Builder addAll(Links other) {
            assert primary == other.primary();
            this.links.addAll(other.linkSet());
            return this;
        }

        @Override
        public void removeIf(Predicate<Link> linkPredicate) {
            links.removeIf(linkPredicate);
        }

        public Links build() {
            return new LinksImpl(primary, Set.copyOf(links));
        }
    }

    // private so that we can ensure that only the links builder can make link objects
    private record LinkImpl(Variable from, LinkNature linkNature, Variable to) implements Link {

        private LinkImpl {
            assert from != null;
            assert to != null;
            assert linkNature != null;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof LinkImpl link)) return false;
            return Objects.equals(to(), link.to()) && Objects.equals(from(), link.from());
        }

        @Override
        public int hashCode() {
            return Objects.hash(from(), to());
        }

        @Override
        public @NotNull String toString() {
            String ln;
            if (linkNature == LinkNature.IS_ELEMENT_OF || linkNature == LinkNature.CONTAINS) {
                int fromArrays = from.parameterizedType().arrays();
                int toArrays = to.parameterizedType().arrays();
                int numSymbols = Math.max(1, Math.abs(fromArrays - toArrays));
                ln = linkNature.toString().repeat(numSymbols);
            } else {
                ln = linkNature.toString();
            }
            String lambda = to.parameterizedType().isFunctionalInterface() ? LAMBDA : "";
            return Util.simpleName(from) + ln + lambda + Util.simpleName(to);
        }

        @Override
        public Link translate(TranslationMap translationMap) {
            return new LinkImpl(translationMap.translateVariableRecursively(from),
                    linkNature, translationMap.translateVariableRecursively(to));
        }
    }

    // used by LVC
    @Override
    public Links changePrimaryTo(Runtime runtime, Variable newPrimary, TranslationMap tm, IntFunction<Links> paramProvider) {
        TranslationMap newPrimaryTm = null;
        Links.Builder builder = new LinksImpl.Builder(newPrimary);
        for (Link link : linkSet) {
            if (link.from().equals(primary)) {
                Variable translated = translateHandleSupplier(tm, link, paramProvider);
                builder.add(link.linkNature(), translated);
            } else {
                // links that are not 'primary'
                if (newPrimaryTm == null) {
                    newPrimaryTm = runtime.newTranslationMapBuilder().put(primary, newPrimary).build();
                }
                Variable fromTranslated = newPrimaryTm.translateVariableRecursively(link.from());
                Variable translated = translateHandleSupplier(tm, link, paramProvider);
                builder.add(fromTranslated, link.linkNature(), translated);
            }
        }
        return builder.build();
    }

    private static Variable translateHandleSupplier(TranslationMap tm, Link link, IntFunction<Links> paramProvider) {
        if (tm == null) return link.to();
        if (link.to().parameterizedType().isFunctionalInterface()) {
            if (link.to() instanceof ParameterInfo pi) {
                Links links = paramProvider.apply(pi.index());
                // grab the to of the primary
                return links.linkSet().stream().filter(l -> l.from().equals(links.primary())).map(Link::to).findFirst().orElseThrow();
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return tm.translateVariableRecursively(link.to());
    }

    @Override
    public Links translate(TranslationMap translationMap) {
        return new LinksImpl(translationMap.translateVariableRecursively(primary),
                linkSet.stream().map(l -> l.translate(translationMap)).collect(Collectors.toUnmodifiableSet()));
    }
}
