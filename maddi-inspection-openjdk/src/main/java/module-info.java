module org.e2immu.language.inspection.openjdk {
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.impl;
    requires org.e2immu.language.cst.print;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.resource;
    requires org.e2immu.language.java.openjdk;
    requires org.e2immu.util.internal.graph;

    requires java.compiler;
    requires jdk.compiler;
    requires org.jetbrains.annotations;
    requires org.slf4j;

    exports org.e2immu.language.inspection.openjdk;
}
