module org.e2immu.analyzer.ide.daemon {
    requires org.e2immu.analyzer.modification.analyzer;
    requires org.e2immu.analyzer.modification.prepwork;
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.openjdk;
    requires org.e2immu.language.inspection.resource;
    requires org.e2immu.util.external.support;

    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports org.e2immu.analyzer.ide.daemon;
}
