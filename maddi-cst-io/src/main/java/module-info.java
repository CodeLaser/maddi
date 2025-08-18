module org.e2immu.language.cst.io {
    requires org.e2immu.util.external.support;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.analysis;
    requires org.slf4j;

    exports org.e2immu.language.cst.io;
    exports org.parsers.json;
    exports org.parsers.json.ast;
}
