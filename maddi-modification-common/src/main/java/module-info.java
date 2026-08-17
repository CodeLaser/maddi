module io.codelaser.maddi.modification.common {
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.graph;
    requires io.codelaser.maddi.util;
    requires org.slf4j;
    requires io.codelaser.maddi.cst.print;

    exports io.codelaser.maddi.modification.common;
    exports io.codelaser.maddi.modification.common.defaults;
    exports io.codelaser.maddi.modification.common.getset;
    exports io.codelaser.maddi.modification.common.util;
}