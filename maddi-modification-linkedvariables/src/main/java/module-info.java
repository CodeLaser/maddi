module org.e2immu.analyzer.modification.linkedvariables {
    requires org.e2immu.analyzer.modification.common;
    requires org.e2immu.analyzer.modification.prepwork;
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.io;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.parser;
    requires org.e2immu.util.external.support;
    requires org.e2immu.util.internal.graph;
    requires org.e2immu.util.internal.util;
    requires org.slf4j;

    exports org.e2immu.analyzer.modification.linkedvariables;
    exports org.e2immu.analyzer.modification.linkedvariables.impl;
    exports org.e2immu.analyzer.modification.linkedvariables.graph;
    exports org.e2immu.analyzer.modification.linkedvariables.io;
    exports org.e2immu.analyzer.modification.linkedvariables.staticvalues;
    exports org.e2immu.analyzer.modification.linkedvariables.lv;

}