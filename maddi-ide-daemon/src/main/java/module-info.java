module io.codelaser.maddi.ide.daemon {
    requires io.codelaser.maddi.callgraph;
    // the modification analysis, as a service: the implementation is provided at run time (maddi-run-analysis)
    requires io.codelaser.maddi.analysis.api;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.openjdk;
    requires io.codelaser.maddi.inspection.resource;
    // the mixed Java+Kotlin parse, and the realm the Kotlin front end is loaded in (automatic modules: Kotlin)
    requires io.codelaser.maddi.inspection.mixed;
    requires io.codelaser.maddi.kotlin.api;
    requires io.codelaser.maddi.kotlin.realm;
    requires io.codelaser.maddi.support;

    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports io.codelaser.maddi.ide.daemon;
}
