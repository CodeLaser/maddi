module org.e2immu.language.cst.impl {
    requires org.e2immu.util.external.support;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.cst.analysis;
    requires org.e2immu.util.internal.util;
    requires org.slf4j;

    exports org.e2immu.language.cst.impl.element;
    exports org.e2immu.language.cst.impl.expression;
    exports org.e2immu.language.cst.impl.expression.eval;
    exports org.e2immu.language.cst.impl.expression.util;
    exports org.e2immu.language.cst.impl.info;
    exports org.e2immu.language.cst.impl.output;
    exports org.e2immu.language.cst.impl.runtime;
    exports org.e2immu.language.cst.impl.statement;
    exports org.e2immu.language.cst.impl.translate;
    exports org.e2immu.language.cst.impl.type;
    exports org.e2immu.language.cst.impl.variable;
}