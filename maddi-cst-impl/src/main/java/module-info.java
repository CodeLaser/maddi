module io.codelaser.maddi.cst.impl {
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.cst.analysis;
    requires io.codelaser.maddi.util;
    requires org.slf4j;
    requires org.jetbrains.annotations;

    exports io.codelaser.maddi.cst.impl.element;
    exports io.codelaser.maddi.cst.impl.expression;
    exports io.codelaser.maddi.cst.impl.expression.eval;
    exports io.codelaser.maddi.cst.impl.expression.util;
    exports io.codelaser.maddi.cst.impl.info;
    exports io.codelaser.maddi.cst.impl.output;
    exports io.codelaser.maddi.cst.impl.runtime;
    exports io.codelaser.maddi.cst.impl.statement;
    exports io.codelaser.maddi.cst.impl.translate;
    exports io.codelaser.maddi.cst.impl.type;
    exports io.codelaser.maddi.cst.impl.variable;
}