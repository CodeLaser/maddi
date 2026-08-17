module io.codelaser.maddi.modification.analyzer {
    exports io.codelaser.maddi.modification.analyzer.impl;
    exports io.codelaser.maddi.modification.analyzer;
    exports io.codelaser.maddi.modification.analyzer.shadow;

    requires io.codelaser.maddi.modification.common;
    requires io.codelaser.maddi.modification.link;
    requires io.codelaser.maddi.modification.prepwork;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.graph;
    requires io.codelaser.maddi.util;
    requires org.slf4j;
    requires java.management; // AnalysisProgressFeed: heap + GC beans for long-run observability
}