module io.codelaser.maddi.run.config {
    requires io.codelaser.maddi.aapi.parser;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.impl;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.resource;
    requires io.codelaser.maddi.graph;

    // TypeUseAnnotationClosure reads the type-annotation attributes of classpath bytecode
    requires org.objectweb.asm;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports io.codelaser.maddi.run.config;
    exports io.codelaser.maddi.run.config.compile;
    exports io.codelaser.maddi.run.config.report;
    exports io.codelaser.maddi.run.config.util;
}
