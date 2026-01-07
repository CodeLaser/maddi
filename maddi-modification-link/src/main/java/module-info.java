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

    exports org.e2immu.analyzer.modification.link;
    exports org.e2immu.analyzer.modification.link.vf;
    exports org.e2immu.analyzer.modification.link.impl;
    exports org.e2immu.analyzer.modification.link.impl.localvar;
}
