module org.e2immu.analyzer.run.openjdkmain {
    requires org.e2immu.analyzer.aapi.parser;
    requires org.e2immu.analyzer.modification.analyzer;
    requires org.e2immu.analyzer.modification.common;
    requires org.e2immu.analyzer.modification.link;
    requires org.e2immu.analyzer.modification.prepwork;
    requires org.e2immu.analyzer.run.config;
    requires org.e2immu.analyzer.run.rewire;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.impl;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.openjdk;
    requires org.e2immu.language.inspection.resource;
    requires org.e2immu.util.internal.graph;
    requires org.e2immu.util.internal.util;

    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires org.apache.commons.cli;
    requires org.slf4j;

    exports org.e2immu.analyzer.run.openjdkmain;
    exports org.e2immu.analyzer.run.openjdkmain.javac;
}
