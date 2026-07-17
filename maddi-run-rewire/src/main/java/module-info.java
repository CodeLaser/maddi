module org.e2immu.analyzer.run.rewire {
    requires org.e2immu.analyzer.modification.prepwork;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.util.internal.graph;

    requires org.slf4j;

    exports org.e2immu.analyzer.run.rewire;
}
