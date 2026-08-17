module io.codelaser.maddi.inspection.api {
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.util;

    requires org.slf4j;

    exports io.codelaser.maddi.inspection.api.integration;
    exports io.codelaser.maddi.inspection.api.parser;
    exports io.codelaser.maddi.inspection.api.resource;
    exports io.codelaser.maddi.inspection.api.util;
}