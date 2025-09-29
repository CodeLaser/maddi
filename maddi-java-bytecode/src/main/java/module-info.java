module org.e2immu.language.java.bytecode {
    requires org.e2immu.util.external.support;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.language.inspection.api;

    requires org.slf4j;
    requires org.objectweb.asm;
    requires org.e2immu.util.internal.util;

    exports org.e2immu.bytecode.java;
    exports org.e2immu.bytecode.java.asm;
}