module io.codelaser.maddi.inspection.parser {
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.util;

    requires org.slf4j;
    requires io.codelaser.maddi.graph;

    exports io.codelaser.maddi.inspection.impl.parser;
}
