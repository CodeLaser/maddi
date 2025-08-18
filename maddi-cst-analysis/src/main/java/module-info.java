module org.e2immu.language.cst.analysis {
    requires org.e2immu.util.external.support;
    requires transitive org.e2immu.language.cst.api;
    requires org.slf4j;

    exports org.e2immu.language.cst.impl.analysis;
}