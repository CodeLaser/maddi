package org.e2immu.analyzer.modification.link;

import java.util.List;

public interface Links extends Iterable<Link> {

    default boolean isEmpty() {
        return links().isEmpty();
    }

    List<Link> links();

}
