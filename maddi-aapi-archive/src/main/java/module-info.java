module io.codelaser.maddi.aapi.archive {
    requires ch.qos.logback.classic;
    requires java.datatransfer;
    requires java.desktop;
    requires java.net.http;
    requires java.xml;
    requires io.codelaser.maddi.support;
    requires org.junit.jupiter.api;
    requires org.slf4j;
    // the Kotlin contracts name kotlin.Pair in a signature; nothing here runs, so it is optional
    requires static kotlin.stdlib;

    exports io.codelaser.maddi.aapi.archive.jdk;
    exports io.codelaser.maddi.aapi.archive.libs.log;
    exports io.codelaser.maddi.aapi.archive.libs.support;
    exports io.codelaser.maddi.aapi.archive.libs.test;
}