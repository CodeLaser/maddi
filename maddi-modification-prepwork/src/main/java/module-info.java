module org.e2immu.analyzer.modification.prepwork {
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.impl;
    requires org.e2immu.language.cst.io;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.util.external.support;
    requires org.e2immu.util.internal.graph;
    requires org.e2immu.util.internal.util;
    requires org.e2immu.analyzer.modification.common;

    requires org.slf4j;
    requires org.jetbrains.annotations;
    requires org.e2immu.language.inspection.integration;

    exports org.e2immu.analyzer.modification.prepwork;
    exports org.e2immu.analyzer.modification.prepwork.callgraph;
    exports org.e2immu.analyzer.modification.prepwork.escape;
    exports org.e2immu.analyzer.modification.prepwork.variable;
    exports org.e2immu.analyzer.modification.prepwork.variable.impl;
    exports org.e2immu.analyzer.modification.prepwork.io;

}