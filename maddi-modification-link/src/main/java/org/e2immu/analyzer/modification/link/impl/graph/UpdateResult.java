package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.Set;

public record UpdateResult<V>(Set<V> affectedVertices, int newFacts, int removedEdges) {
}