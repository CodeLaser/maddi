package org.e2immu.util.internal.graph.analyser;

import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.gml.GmlExporter;
import org.jgrapht.nio.gml.GmlImporter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class TypeGraphIO {

    public static Map<Node, Map<Node, Long>> convertGraphToMap(Graph<Node, DefaultWeightedEdge> graph) {
        Map<Node, Map<Node, Long>> map = new LinkedHashMap<>();
        for (Node node : graph.vertexSet()) {
            Set<DefaultWeightedEdge> edges = graph.edgesOf(node);
            Map<Node, Long> edgeMap = new LinkedHashMap<>();
            for (DefaultWeightedEdge d : edges) {
                Node to = graph.getEdgeTarget(d);
                long weight = (long) graph.getEdgeWeight(d);
                edgeMap.merge(to, weight, Long::sum);
            }
            map.put(node, edgeMap);
        }
        return map;
    }

    public static void importGraph(InputStream inputStream,
                                   Graph<Node, DefaultWeightedEdge> graph) throws IOException {
        GmlImporter<Node, DefaultWeightedEdge> importer = new GmlImporter<>();
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(inputStream),
                StandardCharsets.UTF_8)) {
            importer.setVertexFactory(Node::new);
            importer.addVertexAttributeConsumer((pair, attribute) -> {
                if ("label".equals(pair.getSecond())) {
                    pair.getFirst().label = attribute.getValue();
                }
                if ("weight".equals(pair.getSecond())) {
                    pair.getFirst().weight = Long.parseLong(attribute.getValue());
                }
            });
            importer.importGraph(graph, reader);
        }
    }

    public static class Node {
        final int id;
        String label;
        long weight;

        Node(int id) {
            this.id = id;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setWeight(long weight) {
            this.weight = weight;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label + " (" + weight + ")";
        }
    }

    public static Graph<Node, DefaultWeightedEdge> createPackageGraph() {
        return GraphTypeBuilder.<Node, DefaultWeightedEdge>directed()
                .allowingMultipleEdges(false)
                .allowingSelfLoops(true)
                .edgeClass(DefaultWeightedEdge.class)
                .weighted(true)
                .buildGraph();
    }

    public static <T> void dumpGraph(File file, G<T> typeGraph) throws IOException {
        Graph<T, DefaultWeightedEdge> graph =
                GraphTypeBuilder.<T, DefaultWeightedEdge>directed()
                        .allowingMultipleEdges(false)
                        .allowingSelfLoops(true)
                        .edgeClass(DefaultWeightedEdge.class)
                        .weighted(true)
                        .buildGraph();
        for (Map.Entry<V<T>, Map<V<T>, Long>> entry : typeGraph.edges()) {
            T typeInfo = entry.getKey().t();
            if (!graph.containsVertex(typeInfo)) graph.addVertex(typeInfo);
            for (Map.Entry<V<T>, Long> e2 : entry.getValue().entrySet()) {
                T target = e2.getKey().t();
                if (!graph.containsVertex(target)) graph.addVertex(target);
                DefaultWeightedEdge e = graph.addEdge(typeInfo, target);
                graph.setEdgeWeight(e, e2.getValue());
            }
        }
        Function<T, Map<String, Attribute>> vertexAttributeProvider = (v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            V<T> vertex = typeGraph.vertex(v);
            Map<V<T>, Long> edges = typeGraph.edges(vertex);
            // weight is the sum of the outgoing edge weights
            long sum = edges == null ? 0L : edges.values().stream().mapToLong(x -> x).sum();
            map.put("weight", DefaultAttribute.createAttribute(sum));
            return map;
        };
        exportWeightedGraph(graph, vertexAttributeProvider, file);
    }

    private static <T> void exportWeightedGraph(Graph<T, DefaultWeightedEdge> graph,
                                                Function<T, Map<String, Attribute>> vertexAttributeProvider,
                                                File file) throws IOException {
        GmlExporter<T, DefaultWeightedEdge> exporter = new GmlExporter<>();
        exporter.setParameter(GmlExporter.Parameter.EXPORT_VERTEX_LABELS, true);
        exporter.setParameter(GmlExporter.Parameter.EXPORT_CUSTOM_VERTEX_ATTRIBUTES, true);
        exporter.setVertexAttributeProvider(vertexAttributeProvider);
        exporter.setParameter(GmlExporter.Parameter.EXPORT_EDGE_WEIGHTS, false);
        exporter.setParameter(GmlExporter.Parameter.EXPORT_EDGE_LABELS, false);
        exporter.setParameter(GmlExporter.Parameter.EXPORT_CUSTOM_EDGE_ATTRIBUTES, true);
        exporter.setEdgeAttributeProvider(e -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            int w = (int) graph.getEdgeWeight(e);
            map.put("weight", DefaultAttribute.createAttribute(w));
            return map;
        });
        exporter.exportGraph(graph, file);
    }
}
