package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.analysis.Value;

import java.util.List;

public interface Links extends Iterable<Link>, Value {

    default Link theObjectItself() {
        if (links().isEmpty()) return null;
        Link first = links().getFirst();
        return first.from() == null ? first : null;
    }

    default boolean isEmpty() {
        return links().isEmpty();
    }

    List<Link> links();

    Links merge(Links links);
}
