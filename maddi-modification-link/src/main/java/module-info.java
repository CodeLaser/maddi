module io.codelaser.maddi.modification.link {
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.io;
    requires io.codelaser.maddi.cst.impl;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.inspection.parser;
    requires io.codelaser.maddi.modification.prepwork;
    requires io.codelaser.maddi.support;

    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires io.codelaser.maddi.modification.common;
    requires io.codelaser.maddi.util;
    requires io.codelaser.maddi.graph;

    exports io.codelaser.maddi.modification.link;
    exports io.codelaser.maddi.modification.link.vf;
    exports io.codelaser.maddi.modification.link.impl;
    exports io.codelaser.maddi.modification.link.impl.localvar;
    exports io.codelaser.maddi.modification.link.io;
    exports io.codelaser.maddi.modification.link.impl.translate;
    exports io.codelaser.maddi.modification.link.impl.graph;
    exports io.codelaser.maddi.modification.link.impl.linkgraph;
}
