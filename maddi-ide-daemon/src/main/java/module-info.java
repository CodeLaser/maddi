module io.codelaser.maddi.ide.daemon {
    requires io.codelaser.maddi.modification.analyzer;
    requires io.codelaser.maddi.modification.prepwork;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.openjdk;
    requires io.codelaser.maddi.inspection.resource;
    requires io.codelaser.maddi.support;

    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports io.codelaser.maddi.ide.daemon;
}
