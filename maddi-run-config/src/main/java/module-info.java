module org.e2immu.analyzer.run.config {
    requires org.e2immu.analyzer.aapi.parser;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.impl;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.resource;
    requires org.e2immu.util.internal.graph;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports org.e2immu.analyzer.run.config;
    exports org.e2immu.analyzer.run.config.compile;
    exports org.e2immu.analyzer.run.config.report;
    exports org.e2immu.analyzer.run.config.util;
}
