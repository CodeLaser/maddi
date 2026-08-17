module io.codelaser.maddi.java.parser {
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.util;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;

    requires org.slf4j;

    exports io.codelaser.maddi.parser.java;
    exports io.codelaser.maddi.parser.java.util;
    exports org.parsers.java;
    exports org.parsers.java.ast;
}