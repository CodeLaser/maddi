module org.e2immu.analyzer.modification.common {
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.util.external.support;
    requires org.e2immu.util.internal.graph;
    requires org.e2immu.util.internal.util;
    requires org.slf4j;

    exports org.e2immu.analyzer.modification.common;
    exports org.e2immu.analyzer.modification.common.defaults;
    exports org.e2immu.analyzer.modification.common.getset;
}