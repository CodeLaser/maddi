module io.codelaser.maddi.run.openjdkmain {
    requires io.codelaser.maddi.callgraph;
    requires io.codelaser.maddi.analysis.api;
    requires io.codelaser.maddi.run.config;
    requires io.codelaser.maddi.run.rewire;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.impl;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.openjdk;
    requires io.codelaser.maddi.inspection.resource;
    requires io.codelaser.maddi.graph;
    requires io.codelaser.maddi.util;

    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires org.apache.commons.cli;
    requires org.slf4j;

    exports io.codelaser.maddi.run.openjdkmain;
    exports io.codelaser.maddi.run.openjdkmain.javac;
}
