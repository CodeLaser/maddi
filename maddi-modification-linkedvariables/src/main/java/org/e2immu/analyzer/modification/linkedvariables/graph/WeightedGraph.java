/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.linkedvariables.graph;

import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public interface WeightedGraph {

    WeightedGraph copyForModification();

    @NotModified
    int size();

    @NotModified
    boolean isEmpty();

    @Independent(hc = true)
    @NotModified
    ShortestPath shortestPath();

    @NotModified
    void visit(@NotNull @Independent(hc = true) BiConsumer<Variable, Map<Variable, LV>> consumer);

    @Modified
    void addNode(@NotNull @Independent(hc = true) Variable v,
                 @NotNull @Independent(hc = true) Map<Variable, LV> dependsOn);
}
