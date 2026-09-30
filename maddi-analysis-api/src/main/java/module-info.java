module io.codelaser.maddi.analysis.api {
    requires transitive io.codelaser.maddi.cst.api;
    requires transitive io.codelaser.maddi.inspection.api;
    requires transitive io.codelaser.maddi.graph;
    requires transitive io.codelaser.maddi.callgraph;
    requires transitive io.codelaser.maddi.util;
    requires io.codelaser.maddi.support;

    exports io.codelaser.maddi.analysis.api;

    // AnalysisEngines calls ServiceLoader.load(AnalysisEngine.class): without this directive the loader
    // returns an EMPTY list on the module path, which AnalysisEngines would report as a missing jar
    uses io.codelaser.maddi.analysis.api.AnalysisEngine;
}
