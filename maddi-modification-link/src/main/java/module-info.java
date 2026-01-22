module org.e2immu.analyzer.modification.link {
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.io;
    requires org.e2immu.language.cst.impl;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.parser;
    requires org.e2immu.analyzer.modification.prepwork;
    requires org.e2immu.util.external.support;

    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires org.e2immu.analyzer.modification.common;
    requires org.e2immu.util.internal.util;

    exports org.e2immu.analyzer.modification.link;
    exports org.e2immu.analyzer.modification.link.vf;
    exports org.e2immu.analyzer.modification.link.impl;
    exports org.e2immu.analyzer.modification.link.impl.localvar;
}
