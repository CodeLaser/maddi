module io.codelaser.maddi.run.main {
    requires io.codelaser.maddi.aapi.parser;
    requires io.codelaser.maddi.modification.analyzer;
    requires io.codelaser.maddi.modification.prepwork;
    requires io.codelaser.maddi.run.config;
    requires io.codelaser.maddi.run.rewire;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.impl;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.integration;
    requires io.codelaser.maddi.inspection.resource;
    requires io.codelaser.maddi.graph;
    requires io.codelaser.maddi.util;

    requires ch.qos.logback.classic;
    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires org.apache.commons.cli;
    requires org.slf4j;

    exports io.codelaser.maddi.run.main;
}
