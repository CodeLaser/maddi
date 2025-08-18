module org.e2immu.language.inspection.parser {
    requires org.e2immu.util.external.support;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.util.internal.util;

    requires org.slf4j;
    requires org.e2immu.util.internal.graph;

    exports org.e2immu.language.inspection.impl.parser;
}
