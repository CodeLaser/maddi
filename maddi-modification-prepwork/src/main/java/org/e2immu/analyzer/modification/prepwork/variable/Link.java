package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;


public interface Link extends Comparable<Link> {
    boolean containsVirtualFields();

    Variable from();

    LinkNature linkNature();

    Link replaceSubsetSuperset(Variable modified);

    Variable to();

    @Override
    default int compareTo(@NotNull Link o) {
        int c = from().compareTo(o.from());
        if (c != 0) return c;
        int d = to().compareTo(o.to());
        if (d != 0) return d;
        return Long.compare(linkNature().rank(), o.linkNature().rank());
    }

    boolean toIsIntermediateVariable();

    Link translate(TranslationMap translationMap);

    Link translateFrom(TranslationMap translationMap);
}
