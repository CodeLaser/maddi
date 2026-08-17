module io.codelaser.maddi.java.bytecode {
    requires io.codelaser.maddi.support;
    requires io.codelaser.maddi.cst.api;
    requires io.codelaser.maddi.inspection.api;

    requires org.slf4j;
    requires org.objectweb.asm;
    requires io.codelaser.maddi.util;

    exports io.codelaser.maddi.bytecode.java;
    exports io.codelaser.maddi.bytecode.java.asm;
}