package org.e2immu.analyzer.modification.linkedvariables.graph;

import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;
import java.util.Set;

public interface ShortestPath {
    Map<Variable, LV> links(@NotNull Variable v, LV maxWeight);

    Set<Variable> variables();
}
