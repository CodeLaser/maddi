module io.codelaser.maddi.inspection.openjdk {
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.impl;
    requires io.codelaser.maddi.cst.print;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.parser;
    requires io.codelaser.maddi.inspection.resource;
    requires io.codelaser.maddi.java.openjdk;
    requires io.codelaser.maddi.java.parser;
    requires io.codelaser.maddi.graph;

    requires java.compiler;
    requires jdk.compiler;
    requires org.jetbrains.annotations;
    requires org.slf4j;

    exports io.codelaser.maddi.inspection.openjdk;
}
