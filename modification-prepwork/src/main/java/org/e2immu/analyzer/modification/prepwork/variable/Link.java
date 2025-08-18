package org.e2immu.analyzer.modification.prepwork.variable;

import java.util.Map;

public interface Link {
    Link correctTo(Map<Indices, Indices> correctionMap);

    Link merge(Link l2);

    Link prefixTheirs(int index);

    Indices to();
    boolean mutable();
}
