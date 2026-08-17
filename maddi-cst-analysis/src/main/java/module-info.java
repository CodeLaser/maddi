module io.codelaser.maddi.cst.analysis {
    requires io.codelaser.maddi.support;
    requires transitive io.codelaser.maddi.cst.api;
    requires org.slf4j;

    exports io.codelaser.maddi.cst.impl.analysis;
}