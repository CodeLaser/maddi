package org.e2immu.util.internal.graph.op;

import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.*;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Stream;

public class DijkstraShortestPath {

    private final Connection initialConnection;
    private final LongFunction<String> distancePrinter;
    private final Function<Integer, String> nodePrinter;

    public record Accept(boolean accept, Connection next) {
    }

    public interface Connection {
        Accept next(Function<Integer, String> nodePrinter, int from, int to, Connection connection);

        Connection merge(Connection connection);
    }

    public class DC implements Comparable<DC> {
        private final long dist;
        private final Connection connection;

        public DC(long dist, Connection connection) {
            this.dist = dist;
            this.connection = connection;
        }

        @Override
        public int compareTo(DC o) {
            return Long.compare(dist, o.dist);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DC dc && dist == dc.dist;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(dist);
        }

        public DC merge(DC alt) {
            if (connection == null) return alt;
            return new DC(dist, connection.merge(alt.connection));
        }

        public Connection connection() {
            return connection;
        }

        public long dist() {
            return dist;
        }

        @Override
        public String toString() {
            String distance = distancePrinter == null ? "" + dist : distancePrinter.apply(dist);
            return "DC[" + distance + ", " + connection + "]";
        }
    }

    public record DCP(long dist, Connection connection) {
        // this = the new edge
        private Accept accept(Function<Integer, String> nodePrinter, int from, int to, Connection current) {
            if (this.connection == null) return new Accept(true, current);
            return this.connection.next(nodePrinter, from, to, current);
        }
    }

    public static class DCPEntry implements Map.Entry<Integer, DijkstraShortestPath.DCP> {
        int variable;
        DCP dcp;

        public DCPEntry(Map.Entry<Integer, Long> e) {
            variable = e.getKey();
            dcp = new DCP(e.getValue(), null);
        }

        @Override
        public Integer getKey() {
            return variable;
        }

        @Override
        public DijkstraShortestPath.DCP getValue() {
            return dcp;
        }

        @Override
        public DijkstraShortestPath.DCP setValue(DijkstraShortestPath.DCP value) {
            throw new UnsupportedOperationException();
        }
    }

    /*
    the vertices are numbered 0...n-1
     */
    public interface EdgeProvider {
        Stream<Map.Entry<Integer, DCP>> edges(int i);
    }

    /* from wikipedia:

1  function Dijkstra(Graph, source):
2      dist[source] ← 0                           // Initialization
3
4      create vertex priority queue Q
5
6      for each vertex v in Graph.Vertices:
7          if v ≠ source
8              dist[v] ← INFINITY                 // Unknown distance from source to v
9              prev[v] ← UNDEFINED                // Predecessor of v
10
11         Q.add_with_priority(v, dist[v])
12
13
14     while Q is not empty:                      // The main loop
15         u ← Q.extract_min()                    // Remove and return best vertex
16         for each neighbor v of u:              // Go through all v neighbors of u
17             alt ← dist[u] + Graph.Edges(u, v)
18             if alt < dist[v]:
19                 dist[v] ← alt
20                 prev[v] ← u
21                 Q.decrease_priority(v, alt)
22
23     return dist, prev
     */


    private final DC NO_PATH = new DC(Long.MAX_VALUE, null);

    public DijkstraShortestPath(Connection initialConnection,
                                Function<Integer, String> nodePrinter,
                                LongFunction<String> distancePrinter) {
        this.initialConnection = initialConnection;
        this.distancePrinter = distancePrinter;
        this.nodePrinter = nodePrinter;
    }

    public DijkstraShortestPath() {
        this(new Connection() {
            @Override
            public Accept next(Function<Integer, String> nodePrinter, int from, int to, Connection connection) {
                return new Accept(true, null);
            }

            @Override
            public Connection merge(Connection connection) {
                return this;
            }
        }, i -> "" + i, null);
    }

    public long[] shortestPath(int numVertices, EdgeProvider edgeProvider, int sourceVertex) {
        DC[] dcs = shortestPathDC(numVertices, edgeProvider, sourceVertex);
        return Arrays.stream(dcs).mapToLong(DC::dist).toArray();
    }

    public DC[] shortestPathDC(int numVertices,
                               EdgeProvider edgeProvider,
                               int sourceVertex) {
        DC[] dist = new DC[numVertices]; // dist[source]<-0 implicit

        // https://en.wikipedia.org/wiki/Priority_queue
        // current implementation from org.jheaps library, recursively included in JGraphT
        // https://en.wikipedia.org/wiki/Pairing_heap,
        PairingHeap<DC, Integer> priorityQueue = new PairingHeap<>(); // default comparator
        List<AddressableHeap.Handle<DC, Integer>> handles = new ArrayList<>(numVertices);

        for (int i = 0; i < numVertices; i++) {
            DC dc;
            if (i != sourceVertex) {
                dc = NO_PATH;
            } else {
                dc = new DC(0, initialConnection);
            }
            dist[i] = dc;
            handles.add(priorityQueue.insert(dc, i));
        }
        while (!priorityQueue.isEmpty()) {
            int u = priorityQueue.deleteMin().getValue();

            edgeProvider.edges(u).forEach(edge -> {
                int v = edge.getKey();

                DC d = dist[u];
                DC alt;
                if (d == NO_PATH) {
                    alt = NO_PATH;
                } else {
                    DCP edgeValue = edge.getValue();
                    Accept a = edgeValue.accept(nodePrinter, u, v, d.connection);
                    if (!a.accept) {
                        alt = NO_PATH;
                    } else {
                        if (a.next != null) {
                            alt = new DC(d.dist + edgeValue.dist, a.next);
                        } else {
                            // allow no connection
                            alt = new DC(d.dist + edgeValue.dist, initialConnection);
                        }
                    }
                }
                if (alt.dist < dist[v].dist) {
                    dist[v] = alt;
                    AddressableHeap.Handle<DC, Integer> handle = handles.get(v);
                    handle.decreaseKey(alt);
                } else if (alt.dist == dist[v].dist) {
                    dist[v] = dist[v].merge(alt);
                }
            });
        }
        return dist;
    }
}
