package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.VirtualFieldTranslationMap;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinksImpl implements Links {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinksImpl.class);

    public static final Links EMPTY = new LinksImpl(null);
    public static final Property LINKS = new PropertyImpl("links", EMPTY);
    public static final String LAMBDA = "Λ";

    // is not intermediate, can survive local linking
    public static final String FUNCTIONAL_INTERFACE_VARIABLE = "$_fi";
    public static final String CONSTANT_VARIABLE = "$_ce";

    // see toIsIntermediateVariable()
    public static final String INTERMEDIATE_VARIABLE = "$__";
    public static final String INTERMEDIATE_RETURN_VARIABLE = "$__rv";
    public static final String INTERMEDIATE_LOCAL_VARIABLE = "$__l";
    public static final String INTERMEDIATE_CONDITIONAL_VARIABLE = "$__ic";
    public static final String INTERMEDIATE_CONSTRUCTOR_VARIABLE = "$__c";

    private final Variable primary;
    private final List<Link> linkSet;

    // constructor to bypass the non-null requirement for the primary
    public LinksImpl(Variable variable) {
        this.primary = variable;
        this.linkSet = List.of();
    }

    public LinksImpl(Variable primary, List<Link> linkSet) {
        this.primary = Objects.requireNonNull(primary);
        this.linkSet = Objects.requireNonNull(linkSet);
    }

    @Override
    public Variable primary() {
        return primary;
    }

    @Override
    public Iterable<Link> linkSet() {
        return linkSet;
    }

    @Override
    public Links removeIfFromTo(Predicate<Variable> predicate) {
        return new LinksImpl(primary, linkSet.stream()
                .filter(l -> Stream.concat(l.from().variableStreamDescend(),
                        l.to().variableStreamDescend()).noneMatch(predicate))
                .toList());
    }

    @Override
    public Links removeIfTo(Predicate<Variable> toPredicate) {
        return new LinksImpl(primary, linkSet.stream()
                .filter(l -> l.to().variableStreamDescend().noneMatch(toPredicate))
                .toList());
    }

    @Override
    public Stream<Link> stream() {
        return linkSet.stream();
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
        // don't sort, they have been sorted by Expand.followGraph
        return linkSet.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    @Override
    public Links merge(Links links) {
        return new LinksImpl(primary, Stream.concat(this.linkSet.stream(), links.stream())
                .toList());
    }

    public static class Builder implements Links.Builder {
        private final Variable primary;
        private final List<Link> links = new ArrayList<>();

        public Builder(Variable primary) {
            this.primary = primary;
        }

        public Builder(Links existing) {
            this.primary = existing.primary();
            existing.forEach(links::add);
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
        public void prepend(LinkNature linkNature, Variable to) {
            links.addFirst(new LinkImpl(primary, linkNature, to));
        }

        @Override
        public Links.Builder addAll(Links other) {
            assert primary.equals(other.primary());
            other.linkSet().forEach(links::add);
            return this;
        }

        @Override
        public boolean contains(Variable from, LinkNature reverse, Variable to) {
            return this.links.stream().anyMatch(l -> l.from().equals(from)
                                                     && l.linkNature().equals(reverse)
                                                     && l.to().equals(to));
        }

        @Override
        public void removeIf(Predicate<Link> linkPredicate) {
            links.removeIf(linkPredicate);
        }

        @Override
        public void removeIfFromTo(Predicate<Variable> predicate) {
            links.removeIf(l -> Stream.concat(l.from().variableStreamDescend(),
                    l.to().variableStreamDescend()).anyMatch(predicate));
        }

        @Override
        public void replaceSubsetSuperset(Variable modified) {
            links.replaceAll(l -> l.replaceSubsetSuperset(modified));
        }

        @Override
        public @NotNull Iterator<Link> iterator() {
            return links.iterator();
        }

        public Links build() {
            return new LinksImpl(primary, List.copyOf(links));
        }
    }

    // private so that we can ensure that only the links builder can make link objects
    public record LinkImpl(Variable from, LinkNature linkNature, Variable to) implements Link {

        public LinkImpl {
            assert from != null;
            assert to != null;
            assert linkNature != null;
            assert doNotStackMOnTopOfVirtualField(from);
            assert doNotStackMOnTopOfVirtualField(to);
        }

        private static boolean doNotStackMOnTopOfVirtualField(Variable v) {
            return !(v instanceof FieldReference fr && "§m".equals(fr.fieldInfo().name())
                     && fr.scopeVariable() instanceof FieldReference fr2 && fr2.fieldInfo().name().startsWith("§"));
        }

        @Override
        public boolean toIsIntermediateVariable() {
            LocalVariable lv = Util.lvPrimaryOrNull(to);
            return lv != null && lv.simpleName().startsWith(INTERMEDIATE_VARIABLE);
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
            if (linkNature.multiplySymbols()) {
                int fromArrays = from.parameterizedType().arrays();
                int toArrays = to.parameterizedType().arrays();
                int numSymbols = Math.max(1, Math.abs(fromArrays - toArrays));
                ln = linkNature.toString().repeat(numSymbols);
            } else {
                ln = linkNature.toString();
            }
            String lambda = to.parameterizedType().isFunctionalInterface() ? LAMBDA : "";
            return org.e2immu.analyzer.modification.prepwork.Util.simpleName(from) + ln + lambda + Util.simpleName(to);
        }

        @Override
        public Link translate(TranslationMap translationMap) {
            Variable tFrom = translationMap.translateVariableRecursively(from);
            Variable tTo = translationMap.translateVariableRecursively(to);
            return new LinkImpl(tFrom, linkNature, tTo);
        }

        @Override
        public Link translateFrom(TranslationMap translationMap) {
            Variable tFrom = translationMap.translateVariableRecursively(from);
            return new LinkImpl(tFrom, linkNature, to);
        }

        @Override
        public Link replaceSubsetSuperset(Variable modified) {
            LinkNature ln2 = linkNature.replaceSubsetSuperset();
            if (ln2 != linkNature && (Util.primary(from).equals(modified) || Util.primary(to).equals(modified))) {
                LOGGER.info("Change {} -> {} for {} because of modification on {}", linkNature, ln2, this, modified);
                return new LinkImpl(from, ln2, to);
            }
            return this;
        }

        @Override
        public boolean containsVirtualFields() {
            return from instanceof FieldReference fr && Util.virtual(fr)
                   || to instanceof FieldReference fr2 && Util.virtual(fr2);
        }
    }

    @Override
    public Links changePrimaryTo(Runtime runtime, Variable newPrimary) {
        TranslationMap newPrimaryTm = null;
        Links.Builder builder = new LinksImpl.Builder(newPrimary);
        for (Link link : linkSet) {
            if (link.from().equals(primary)) {
                builder.add(link.linkNature(), link.to());
            } else {
                // links that are not 'primary'
                if (newPrimaryTm == null) {
                    newPrimaryTm = runtime.newTranslationMapBuilder().put(primary, newPrimary).build();
                }
                Variable fromTranslated = newPrimaryTm.translateVariableRecursively(link.from());
                builder.add(fromTranslated, link.linkNature(), link.to());
            }
        }
        return builder.build();
    }

    @Override
    public Links translate(TranslationMap translationMap) {
        if (primary == null) return LinksImpl.EMPTY;
        Variable newPrimary = translationMap.translateVariableRecursively(primary);
        if (newPrimary == null) return LinksImpl.EMPTY;
        if (translationMap instanceof VirtualFieldTranslationMap vfTm) {
            return new LinksImpl(newPrimary,
                    linkSet.stream().flatMap(l -> translateCorrect(vfTm, l)).toList());
        }
        return new LinksImpl(newPrimary,
                linkSet.stream().map(l -> l.translate(translationMap)).toList());
    }

    @Override
    public Links translateFrom(TranslationMap translationMap) {
        if (primary == null) return LinksImpl.EMPTY;
        Variable newPrimary = translationMap.translateVariableRecursively(primary);
        if (newPrimary == null) return LinksImpl.EMPTY;
        if (translationMap instanceof VirtualFieldTranslationMap vfTm) {
            return new LinksImpl(newPrimary,
                    linkSet.stream().flatMap(l -> translateFromCorrect(vfTm, l)).toList());
        }
        return new LinksImpl(newPrimary,
                linkSet.stream().map(l -> l.translateFrom(translationMap)).toList());
    }

    private Stream<Link> translateCorrect(VirtualFieldTranslationMap translationMap, Link link) {
        Link tLink = link.translate(translationMap);
        // upgrade: orElseGet≡this.§t ==> orElseGet≡this.§xs ==> orElseGet.§xs⊆this.§xs
        // upgrade: 0:key≡this.§kv.§k ==> 0:xs≡this.§xsys.§xs ==> 0:xs.§xs⊆this.§xsys.§xs
        if (link.linkNature().isIdenticalTo() && Util.isPrimary(tLink.from())
            && Util.hasVirtualFields(tLink.from())
            && link.to().parameterizedType().arrays() == 0
            && tLink.to().parameterizedType().arrays() > 0
            && tLink.to() instanceof FieldReference fr) {
            return translationMap.upgrade(link, tLink, fr);
        }
        return Stream.of(tLink);
    }

    private Stream<Link> translateFromCorrect(VirtualFieldTranslationMap translationMap, Link link) {
        Link tLink = link.translateFrom(translationMap);
        // upgrade: orElseGet≡this.§t ==> orElseGet≡this.§xs ==> orElseGet.§xs⊆this.§xs
        // upgrade: 0:key≡this.§kv.§k ==> 0:xs≡this.§xsys.§xs ==> 0:xs.§xs⊆this.§xsys.§xs
        if (link.linkNature().isIdenticalTo() && Util.isPrimary(tLink.from())
            && Util.hasVirtualFields(tLink.from())
            && link.to().parameterizedType().arrays() == 0
            && tLink.to().parameterizedType().arrays() > 0
            && tLink.to() instanceof FieldReference fr) {
            return translationMap.upgrade(link, tLink, fr);
        }
        return Stream.of(tLink);
    }

    @Override
    public List<Variable> primaryAssigned() {
        return linkSet.stream()
                .filter(l -> l.linkNature().isIdenticalTo())
                .map(Link::to)
                .filter(Util::isPrimary)
                .toList();
    }

    // if primary is a, make a collection of links, with the level below a as the new primaries
    // e.g. a.b.c -> x becomes b.c -> x in primary b
    @Override
    public Iterable<Links> removeThisAsPrimary() {
        assert primary instanceof This;
        Map<Variable, Builder> builders = new HashMap<>();
        for (Link link : this) {
            Variable newPrimary = Util.oneBelowThis(link.from());
            if (!(newPrimary instanceof This)) {
                builders.computeIfAbsent(newPrimary, _ -> new Builder(newPrimary))
                        .add(link.from(), link.linkNature(), link.to());
            } // else: ignore
        }
        return builders.values().stream().map(Builder::build).toList();
    }

    @Override
    public boolean containsVirtualFields() {
        if (primary instanceof FieldReference fr && Util.virtual(fr)) return true;
        return linkSet.stream().anyMatch(Link::containsVirtualFields);
    }
}
