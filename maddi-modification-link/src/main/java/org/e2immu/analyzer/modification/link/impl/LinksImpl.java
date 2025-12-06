package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LinksImpl(Variable primary, Set<Link> links) implements Links {
    public static final Links EMPTY = new LinksImpl(null, Set.of());
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
        if (isEmpty()) return "-";
        return links.stream().sorted().map(Object::toString).collect(Collectors.joining(","));
    }

    @Override
    public Links merge(Links links) {
        return new LinksImpl(primary, Stream.concat(this.links.stream(), links.links().stream())
                .collect(Collectors.toUnmodifiableSet()));
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
            assert primary(from).equals(primary);
            links.add(new LinkImpl(from, linkNature, to));
            return this;
        }

        public Links build() {
            return new LinksImpl(primary, Set.copyOf(links));
        }
    }

    public static Variable primary(Variable v) {
        if (v instanceof DependentVariable dv) return primary(dv.arrayVariableBase());
        if (v instanceof FieldReference fr) return fr.fieldReferenceBase();
        return v;
    }

    private record LinkImpl(Variable from, LinkNature linkNature, Variable to) implements Link {

        public LinkImpl {
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
            return simpleVar(from) + linkNature + simpleVar(to);
        }
    }

    static String simpleVar(Variable variable) {
        if (variable instanceof ParameterInfo pi) {
            return pi.index() + ":" + pi.name();
        }
        if (variable instanceof ReturnVariable rv) {
            return rv.methodInfo().name();
        }
        if (variable instanceof FieldReference fr) {
            String scope = fr.scopeVariable() != null ? simpleVar(fr.scopeVariable()) : fr.scope().toString();
            return scope + "." + fr.fieldInfo().name();
        }
        if (variable instanceof DependentVariable dv) {
            String index = dv.indexVariable() != null ? simpleVar(dv.indexVariable()) : dv.indexExpression().toString();
            return simpleVar(dv.arrayVariable()) + "[" + index + "]";
        }
        return variable.toString();
    }

    // used by LVC
    @Override
    public Links changePrimaryTo(Runtime runtime, Variable newPrimary, TranslationMap tm) {
        TranslationMap newPrimaryTm = null;
        Links.Builder builder = new LinksImpl.Builder(newPrimary);
        for (Link link : links) {
            if (link.from().equals(primary)) {
                Variable translated = tm == null ? link.to() : tm.translateVariableRecursively(link.to());
                builder.add(link.linkNature(), translated);
            } else {
                // links that are not 'primary'
                if (newPrimaryTm == null) {
                    newPrimaryTm = runtime.newTranslationMapBuilder().put(primary, newPrimary).build();
                }
                Variable fromTranslated = newPrimaryTm.translateVariableRecursively(link.from());
                Variable translated = tm == null ? link.to() : tm.translateVariableRecursively(link.to());
                builder.add(fromTranslated, link.linkNature(), translated);
            }
        }
        return builder.build();
    }
}
