package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Stream;


public interface Link extends Comparable<Link> {
    boolean containsVirtualFields();

    Variable from();

    LinkNature linkNature();

    Variable to();

    /** true when this DIRECT link was produced through a declared-type mediator (a cast, or an
     * instanceof/switch pattern binding). Runtime value flow holds; declared-type coupling does NOT
     * (the mediator re-mediates the type) — declared-type consumers must skip mediated links.
     * Provenance is only guaranteed on directly-produced links; closure/reconstruction products
     * currently drop it (task #39 step 1; threading through reconstruction is the remaining step). */
    default boolean mediated() {
        return false;
    }

    @NotNull String toString(Set<Variable> modified);

    @Override
    default int compareTo(@NotNull Link o) {
        int c = from().compareTo(o.from());
        if (c != 0) return c;
        int d = to().compareTo(o.to());
        if (d != 0) return d;
        return Long.compare(linkNature().rank(), o.linkNature().rank());
    }

    Link translate(TranslationMap translationMap);

    Link translateFrom(TranslationMap translationMap);

    default Stream<ParameterInfo> parameterStream() {
        return Stream.concat(from().variableStreamDescend(), to().variableStreamDescend())
                .filter(v -> v instanceof ParameterInfo)
                .map(v -> (ParameterInfo) v);
    }
}
