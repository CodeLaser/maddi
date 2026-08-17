module io.codelaser.maddi.cst.io {
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.analysis;
    requires org.slf4j;

    exports io.codelaser.maddi.cst.io;
    exports org.parsers.json;
    exports org.parsers.json.ast;
}
