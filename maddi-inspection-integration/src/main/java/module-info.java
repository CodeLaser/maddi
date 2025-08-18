/*
Integrates all CST definition, parsing, and inspection modules.
 */
module org.e2immu.language.inspection.integration {
    requires org.e2immu.util.external.support;
    requires org.e2immu.util.internal.util;
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.impl;
    requires org.e2immu.language.cst.io;
    requires org.e2immu.language.cst.print;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.parser;
    requires org.e2immu.language.inspection.resource;
    requires org.e2immu.language.java.bytecode;
    requires org.e2immu.language.java.parser;

    requires org.slf4j;
    // used by DetectJREs, for MacOS
    requires java.xml;
    requires org.e2immu.util.internal.graph;

    exports org.e2immu.language.inspection.integration;
}