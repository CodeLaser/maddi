module io.codelaser.maddi.aapi.parser {
    requires ch.qos.logback.classic;
    requires io.codelaser.maddi.modification.common;
    requires io.codelaser.maddi.modification.prepwork;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.resource;
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.util;
    requires org.slf4j;
    requires io.codelaser.maddi.cst.impl;

    exports io.codelaser.maddi.aapi.parser;
}