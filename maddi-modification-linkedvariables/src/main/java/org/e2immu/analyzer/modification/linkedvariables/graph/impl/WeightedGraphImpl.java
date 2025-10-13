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

package org.e2immu.analyzer.modification.linkedvariables.graph.impl;

import org.e2immu.analyzer.modification.linkedvariables.graph.Cache;
import org.e2immu.analyzer.modification.linkedvariables.graph.ShortestPath;
import org.e2immu.analyzer.modification.linkedvariables.graph.WeightedGraph;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.support.Freezable;
import org.e2immu.util.internal.graph.op.DijkstraShortestPath;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl.LINK_INDEPENDENT;

public class WeightedGraphImpl extends Freezable implements WeightedGraph {

    private final Map<Variable, Node> nodeMap;
    private final Cache cache;

    // for testing only!
    public WeightedGraphImpl() {
        this(new GraphCacheImpl(10));
    }

    public WeightedGraphImpl(Cache cache) {
        nodeMap = new LinkedHashMap<>();
        this.cache = cache;
    }

    private static class Node {
        Map<Variable, LV> dependsOn;
        final Variable variable;

        private Node(Variable v) {
            variable = v;
        }
    }

    public int size() {
        return nodeMap.size();
    }

    public boolean isEmpty() {
        return nodeMap.isEmpty();
    }

    public void visit(BiConsumer<Variable, Map<Variable, LV>> consumer) {
        nodeMap.values().forEach(n -> consumer.accept(n.variable, n.dependsOn));
    }

    private Node getOrCreate(Variable v) {
        ensureNotFrozen();
        Objects.requireNonNull(v);
        return nodeMap.computeIfAbsent(v, Node::new);
    }

    public void addNode(Variable v, Map<Variable, LV> dependsOn) {
        ensureNotFrozen();
        Node node = getOrCreate(v);
        for (Map.Entry<Variable, LV> e : dependsOn.entrySet()) {
            if (node.dependsOn == null) {
                node.dependsOn = new LinkedHashMap<>();
            }
            Variable target = e.getKey();
            LV linkLevel = e.getValue();
            assert !LINK_INDEPENDENT.equals(linkLevel);
            assert !(target instanceof ReturnVariable);
            /*
             Links from the return variable are asymmetrical
             */
            LV min = node.dependsOn.merge(target, linkLevel, LV::min);
            Node n = getOrCreate(target);
            if (!(v instanceof ReturnVariable)) {
                if (n.dependsOn == null) {
                    n.dependsOn = new LinkedHashMap<>();
                }
                n.dependsOn.merge(v, min.reverse(), LV::min);
            }
        }
    }

    static Comparator<String> REVERSE_STRING_COMPARATOR = (s1, s2) -> {
        int i1 = s1.length() - 1;
        int i2 = s2.length() - 1;
        while (i1 >= 0 && i2 >= 0) {
            int c = Character.compare(s1.charAt(i1), s2.charAt(i2));
            if (c != 0) return c;
            --i1;
            --i2;
        }
        if (i1 >= 0) return 1;
        if (i2 >= 0) return -1;
        return 0;
    };

    static Comparator<Variable> REVERSE_FQN_COMPARATOR = (v1, v2) ->
            REVERSE_STRING_COMPARATOR.compare(v1.fullyQualifiedName(), v2.fullyQualifiedName());

    @Override
    public ShortestPath shortestPath() {
        int n = nodeMap.size();
        Variable[] variables = new Variable[n];
        // -- CACHE --
        int j = 0;
        for (Variable v : nodeMap.keySet()) {
            variables[j++] = v;
        }
        // we need a stable order across the variables; given the huge prefixes of parameters and fields,
        // it seems a lot faster to sort starting from the back.
        Arrays.sort(variables, REVERSE_FQN_COMPARATOR); // default: by name
        // -- CACHE --
        Map<Variable, Integer> variableIndex = new LinkedHashMap<>();
        int i = 0;
        for (Variable v : variables) {
            variableIndex.put(v, i);
            ++i;
        }
        StringBuilder sb = new StringBuilder(n * n * 5);
        Map<Integer, Map<Integer, DijkstraShortestPath.DCP>> edges = new LinkedHashMap<>();
        //CausesOfDelay delay = null;
        for (int d1 = 0; d1 < n; d1++) {
            Node node = nodeMap.get(variables[d1]);
            Map<Variable, LV> dependsOn = node.dependsOn;
            sb.append(d1);
            if (dependsOn != null && !dependsOn.isEmpty()) {

                Map<Integer, DijkstraShortestPath.DCP> edgesOfD1 = new LinkedHashMap<>();
                edges.put(d1, edgesOfD1);

                List<String> unsorted = new ArrayList<>(dependsOn.size());
                for (Map.Entry<Variable, LV> e2 : dependsOn.entrySet()) {
                    Integer d2 = variableIndex.get(e2.getKey());
                    assert d2 != null : "Variable " + e2.getKey() + " is not in " + Arrays.toString(variables);
                    LV lv = e2.getValue();

                    long d = ShortestPathImpl.toDistanceComponent(lv);
                    edgesOfD1.put(d2, new DijkstraShortestPath.DCP(d, lv.links()));

                    String cacheCode = lv.isDelayed() ? "D" : lv.minimal();
                    unsorted.add(d2 + ":" + cacheCode);
                }
                sb.append("(");
                sb.append(unsorted.stream().sorted().collect(Collectors.joining(";")));
                sb.append(")");
            } else {
                sb.append("/");
            }
        }
        String cacheKey = sb.toString();
        Cache.Hash hash = cache.createHash(cacheKey);
        ShortestPathImpl.LinkMap linkMap = (ShortestPathImpl.LinkMap)
                cache.computeIfAbsent(hash, h -> new ShortestPathImpl.LinkMap(new LinkedHashMap<>(), new AtomicInteger(), cacheKey));
        return new ShortestPathImpl(variableIndex, variables, edges, linkMap);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n");
        nodeMap.forEach((v, node) -> {
            sb.append(v).append(" --> ");
            if (node.dependsOn == null) sb.append("<none>\n");
            else sb.append(node.dependsOn.entrySet()
                    .stream().map(e -> e.getKey() + ":" + e.getValue()).sorted()
                    .collect(Collectors.joining(", "))).append("\n");
        });
        return sb.toString();
    }

    @Override
    public WeightedGraph copyForModification() {
        WeightedGraphImpl wg = new WeightedGraphImpl(cache);
        nodeMap.forEach((v, node) -> {
            Node newNode = new Node(v);
            newNode.dependsOn = node.dependsOn == null ? Map.of() :
                    node.dependsOn.entrySet().stream()
                            .filter(e -> !e.getValue().theirsContainsAll())
                            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            wg.nodeMap.put(v, newNode);
        });
        return wg;
    }
}