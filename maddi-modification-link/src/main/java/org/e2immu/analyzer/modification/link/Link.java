package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;


public interface Link extends Comparable<Link> {
    Variable from();

    LinkNature linkNature();

    Variable to();

    @Override
    default int compareTo(@NotNull Link o) {
        int c = from().compareTo(o.from());
        if (c != 0) return c;
        int d = to().compareTo(o.to());
        if (d != 0) return d;
        return Long.compare(linkNature().longValue(), o.linkNature().longValue());
    }
}
