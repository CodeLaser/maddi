module io.codelaser.maddi.ide.daemon {
    requires io.codelaser.maddi.modification.analyzer;
    // AnalyzerException, to report the types prep isolated (likelier now that a partial parse is analysed)
    requires io.codelaser.maddi.modification.common;
    requires io.codelaser.maddi.modification.prepwork;
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
