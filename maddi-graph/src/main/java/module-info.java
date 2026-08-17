module io.codelaser.maddi.graph {
    requires org.slf4j;
    requires org.jgrapht.core;
    requires org.jgrapht.io;

    requires io.codelaser.maddi.support;

    exports io.codelaser.maddi.graph;
    exports io.codelaser.maddi.graph.op;
    exports io.codelaser.maddi.graph.analyzer;
    exports io.codelaser.maddi.graph.util;
}