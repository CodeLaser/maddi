package org.e2immu.analyzer.modification.linkedvariables.graph.impl;

import org.e2immu.analyzer.modification.linkedvariables.graph.Cache;
import org.e2immu.analyzer.modification.linkedvariables.graph.ShortestPath;
import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.util.internal.graph.op.DijkstraShortestPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl.*;
import static org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl.NO_LINKS;

public class ShortestPathImpl implements ShortestPath {
    private static final Logger LOGGER = LoggerFactory.getLogger("graph-algorithm");

    private final Map<Variable, Integer> variableIndex;
    private final Variable[] variables;
    private final Map<Integer, Map<Integer, DijkstraShortestPath.DCP>> edges;
    private final DijkstraShortestPath dijkstraShortestPath;
    private final LinkMap linkMap;

    ShortestPathImpl(Map<Variable, Integer> variableIndex,
                     Variable[] variables,
                     Map<Integer, Map<Integer, DijkstraShortestPath.DCP>> edges,
                     LinkMap linkMap) {
        this.variables = variables;
        this.edges = edges;
        this.variableIndex = variableIndex;
        dijkstraShortestPath = new DijkstraShortestPath(NO_LINKS, this::nodePrinter, ShortestPathImpl::distancePrinter);
        this.linkMap = linkMap;
    }

    private String nodePrinter(int i) {
        return variables[i].simpleName();
    }

    static final int BITS = 15;
    static final long STATICALLY_ASSIGNED = 1L;
    static final long ASSIGNED = 1L << BITS;
    static final long DEPENDENT = 1L << (2 * BITS);
    static final long COMMON_HC = 1L << (3 * BITS);

    public static long toDistanceComponent(LV lv) {
        if (LINK_STATICALLY_ASSIGNED.equals(lv)) return STATICALLY_ASSIGNED;
        if (LINK_ASSIGNED.equals(lv)) return ASSIGNED;
        if (lv.isDependent()) return DEPENDENT;
        return COMMON_HC;
    }

    public static String distancePrinter(long l) {
        if (l == Long.MAX_VALUE) return "<no link>";
        LV lv = fromDistanceSum(l);
        assert lv != null;
        return lv.label();
    }

    // used to produce the result.
    public static LV fromDistanceSum(long l) {
        if (l == Long.MAX_VALUE) return null; // no link
        if (l < ASSIGNED) return LINK_STATICALLY_ASSIGNED;
        if (l < DEPENDENT) return LINK_ASSIGNED;
        if (l < COMMON_HC) {
            return LINK_DEPENDENT;
        }
        return LINK_COMMON_HC;
    }

    // used to produce the result.
    public static LV fromDistanceSum(DijkstraShortestPath.DC dc) {
        long l = dc.dist();
        if (l == Long.MAX_VALUE) return null; // no link
        if (l < ASSIGNED) return LINK_STATICALLY_ASSIGNED;
        if (l < DEPENDENT) return LINK_ASSIGNED;
        Links links = (Links) dc.connection();
        if (l < COMMON_HC) {
            return LVImpl.createDependent(links);
        }
        return LVImpl.createHC(links);
    }

    record Key(int start, long maxWeight) {
    }

    record LinkMap(Map<Key, LV[]> map, AtomicInteger savingsCount, String cacheKey) implements Cache.CacheElement {
        @Override
        public int savings() {
            return savingsCount.get();
        }
    }


    @Override
    public Map<Variable, LV> links(Variable v, LV maxWeight) {
        LOGGER.debug("Start in {}", v.simpleName());

        Integer startVertex = variableIndex.get(v);
        if (startVertex == null) {
            return Map.of(v, LINK_STATICALLY_ASSIGNED);
        }
        long maxWeightLong = maxWeight == null ? 0L : toDistanceComponent(maxWeight);
        Key key = new Key(startVertex, maxWeightLong);
        LV[] inMap = linkMap.map.get(key);
        LV[] shortest;
        if (inMap != null) {
            shortest = inMap;
            linkMap.savingsCount.incrementAndGet();
        } else {
            shortest = computeDijkstra(startVertex, maxWeight, maxWeightLong);
            linkMap.map.put(key, shortest);
            linkMap.savingsCount.decrementAndGet();
        }
        Map<Variable, LV> result = new HashMap<>();
        for (int j = 0; j < shortest.length; j++) {
            LV d = shortest[j];
            if (d != null) {
                result.put(variables[j], d);
            }
        }
        return result;
    }

    private LV[] computeDijkstra(int startVertex, LV maxWeight, long maxWeightLong) {
        DijkstraShortestPath.EdgeProvider edgeProvider = i -> {
            Map<Integer, DijkstraShortestPath.DCP> edgeMap = edges.get(i);
            if (edgeMap == null) return Stream.of();
            return edgeMap.entrySet().stream()
                    .filter(e -> maxWeight == null || e.getValue().dist() <= maxWeightLong);
        };
        DijkstraShortestPath.DC[] shortestDC = dijkstraShortestPath.shortestPathDC(variables.length, edgeProvider,
                startVertex);

        LV[] shortest = new LV[shortestDC.length];
        for (int i = 0; i < shortest.length; i++) {
            LV v;
            long l = shortestDC[i].dist();
            if (l == Long.MAX_VALUE) {
                v = null;
            } else {
                v = fromDistanceSum(shortestDC[i]);
            }
            shortest[i] = v;
        }
        return shortest;
    }

    @Override
    public Set<Variable> variables() {
        return variableIndex.keySet();
    }

    // for testing

    Variable variablesGet(int i) {
        return variables[i];
    }

    public String getCacheKey() {
        return linkMap.cacheKey;
    }
}

