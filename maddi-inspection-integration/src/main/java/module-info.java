/*
Integrates all CST definition, parsing, and inspection modules.
 */
module io.codelaser.maddi.inspection.integration {
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.util;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.impl;
    requires io.codelaser.maddi.cst.io;
    requires io.codelaser.maddi.cst.print;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.parser;
    requires io.codelaser.maddi.inspection.resource;
    requires io.codelaser.maddi.java.bytecode;
    requires io.codelaser.maddi.java.parser;

    requires org.slf4j;
    // used by DetectJREs, for MacOS
    requires java.xml;
    requires io.codelaser.maddi.graph;

    exports io.codelaser.maddi.inspection.integration;
}