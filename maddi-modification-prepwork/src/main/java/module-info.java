module io.codelaser.maddi.modification.prepwork {
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.impl;
    requires io.codelaser.maddi.cst.io;
    requires io.codelaser.maddi.inspection.api;
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.graph;
    requires io.codelaser.maddi.util;
    requires io.codelaser.maddi.modification.common;

    requires org.slf4j;
    requires org.jetbrains.annotations;

    exports io.codelaser.maddi.modification.prepwork;
    exports io.codelaser.maddi.modification.prepwork.callgraph;
    exports io.codelaser.maddi.modification.prepwork.escape;
    exports io.codelaser.maddi.modification.prepwork.variable;
    exports io.codelaser.maddi.modification.prepwork.variable.impl;
    exports io.codelaser.maddi.modification.prepwork.io;

}