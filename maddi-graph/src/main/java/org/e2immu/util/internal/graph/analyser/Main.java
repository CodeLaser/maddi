package org.e2immu.util.internal.graph.analyser;

import org.e2immu.util.internal.graph.*;
import org.e2immu.util.internal.graph.op.BreakCycles;
import org.e2immu.util.internal.graph.op.GreedyEdgeRemoval;
import org.e2immu.util.internal.graph.op.ParallelGreedyEdgeRemoval;
import org.e2immu.util.internal.graph.op.RemoveEdgesByVertexWeight;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final String CLASSPATH = "classpath:";

    public static final String SEQUENTIAL = "sequential";
    public static final String PARALLEL = "parallel";
    public static final String VERTEX_WEIGHT = "vertexWeight";

    public static void main(String[] args) throws IOException {
        new Main().go(args);
    }

    public BreakCycles.Linearization<TypeGraphIO.Node> go(String[] args) throws IOException {
        String gmlFileName = args[0];
        try (InputStream inputStream = makeInputStream(gmlFileName)) {
            String method = args.length > 1 ? args[1] : SEQUENTIAL;
            return test(inputStream, method);
        }
    }

    private InputStream makeInputStream(String location) throws IOException {
        if (location.startsWith(CLASSPATH)) {
            String classPathLocation = location.substring(CLASSPATH.length());
            return Objects.requireNonNull(
                    this.getClass().getClassLoader().getResourceAsStream(classPathLocation),
                    "Cannot read from classpath: " + classPathLocation);
        }
        return new FileInputStream(location);
    }

    private static BreakCycles.Linearization<TypeGraphIO.Node> test(InputStream inputStream,
                                                                    String method) throws IOException {
        Graph<TypeGraphIO.Node, DefaultWeightedEdge> graph = TypeGraphIO.createPackageGraph();
        TypeGraphIO.importGraph(inputStream, graph);
        Map<TypeGraphIO.Node, Map<TypeGraphIO.Node, Long>> map = TypeGraphIO.convertGraphToMap(graph);
        G<TypeGraphIO.Node> g = ImmutableGraph.create(map);
        LOGGER.info("Have graph of {} nodes, {} edges", g.vertices().size(), g.edgeStream().count());

        BreakCycles.ActionComputer<TypeGraphIO.Node> actionComputer;
        if (VERTEX_WEIGHT.equals(method)) {
            LOGGER.info("Minimal vertex weight edge removal algorithm");
            Map<V<TypeGraphIO.Node>, Long> vertexWeights = g.incomingVertexWeight(PackedInt::longSum);
            actionComputer = new RemoveEdgesByVertexWeight<>(vertexWeights);
        } else {
            EdgePrinter<TypeGraphIO.Node> edgePrinter = m -> m == null ? "[]"
                    : m.entrySet().stream().map(e ->
                            e.getKey() + "->" + e.getValue().entrySet().stream()
                                    .map(e2 -> e2.getKey() + ":"
                                               + PackedInt.nice((int) (long) e2.getValue())).collect(Collectors.joining(",")))
                    .collect(Collectors.joining(";"));
            long limit = PackedInt.FIELD.of(1);
            EdgeIterator<TypeGraphIO.Node> edgeIterator = gg ->
                    BreakCycles.edgeIterator2(gg, Long::compareTo, limit, PackedInt::longSum);
            TimedLogger timedLogger = new TimedLogger(LOGGER, 1000L);
            if (PARALLEL.equalsIgnoreCase(method)) {
                actionComputer = new ParallelGreedyEdgeRemoval<>(edgePrinter, edgeIterator, timedLogger);
                LOGGER.info("Parallel greedy edge removal algorithm");
            } else {
                LOGGER.info("Sequential greedy edge removal algorithm");
                actionComputer = new GreedyEdgeRemoval<>(edgePrinter, edgeIterator, timedLogger);
            }
        }
        BreakCycles<TypeGraphIO.Node> bc = new BreakCycles<>(actionComputer);
        return bc.go(g);
    }

}
