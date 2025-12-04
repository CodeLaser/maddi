package org.e2immu.analyzer.modification.link;

import java.util.List;

public interface Links extends Iterable<Link> {

    default Link first() {
        return links().isEmpty() ? null : links().getFirst();
    }

    default boolean isEmpty() {
        return links().isEmpty();
    }

    List<Link> links();

}
