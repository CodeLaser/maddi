module org.e2immu.analyzer.modification.analyzer {
    exports org.e2immu.analyzer.modification.analyzer.impl;
    exports org.e2immu.analyzer.modification.analyzer;

    requires org.e2immu.analyzer.modification.common;
    requires org.e2immu.analyzer.modification.link;
    requires org.e2immu.analyzer.modification.prepwork;
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.util.external.support;
    requires org.slf4j;
}