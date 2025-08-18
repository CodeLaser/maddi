module org.e2immu.analyzer.aapi.archive {
    requires ch.qos.logback.classic;
    requires java.datatransfer;
    requires java.desktop;
    requires java.net.http;
    requires java.xml;
    requires org.e2immu.util.external.support;
    requires org.junit.jupiter.api;
    requires org.slf4j;

    exports org.e2immu.analyzer.aapi.archive.v2.jdk;
    exports org.e2immu.analyzer.aapi.archive.v2.libs.log;
    exports org.e2immu.analyzer.aapi.archive.v2.libs.test;
}