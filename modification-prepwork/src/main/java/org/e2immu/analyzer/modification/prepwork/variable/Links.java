package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.util.internal.graph.op.DijkstraShortestPath;

import java.util.Map;

public interface Links extends DijkstraShortestPath.Connection {
    Links ensureNoModification();

    Map<Indices, Link> map();

    Indices modificationAreaSource();

    Indices modificationAreaTarget();

    Links mineToTheirs(Links links);

    Links reverse();

    String toString(int hc);

    Links theirsToTheirs(Links links);
}
