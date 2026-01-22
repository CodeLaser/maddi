module org.e2immu.analyzer.aapi.parser {
    requires ch.qos.logback.classic;
    requires org.e2immu.analyzer.modification.common;
    requires org.e2immu.analyzer.modification.prepwork;
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.inspection.api;
    requires org.e2immu.language.inspection.integration;
    requires org.e2immu.language.inspection.resource;
    requires org.e2immu.util.external.support;
    requires org.e2immu.util.internal.util;
    requires org.slf4j;
    requires org.e2immu.language.cst.impl;

    exports org.e2immu.analyzer.aapi.parser;
}