module io.codelaser.maddi.run.analysis {
    requires io.codelaser.maddi.analysis.api;
    requires io.codelaser.maddi.aapi.parser;
    requires io.codelaser.maddi.callgraph;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.modification.analyzer;
    requires io.codelaser.maddi.modification.common;
    requires io.codelaser.maddi.modification.link;
    requires io.codelaser.maddi.modification.prepwork;
    requires org.slf4j;

    exports io.codelaser.maddi.run.analysis;

    provides io.codelaser.maddi.analysis.api.AnalysisEngine with io.codelaser.maddi.run.analysis.AnalysisEngineImpl;
}
