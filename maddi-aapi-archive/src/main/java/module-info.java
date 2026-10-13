module io.codelaser.maddi.aapi.archive {
    requires ch.qos.logback.classic;
    requires java.datatransfer;
    requires java.desktop;
    requires java.logging;
    requires java.net.http;
    requires java.xml;
    requires io.codelaser.maddi.support;
    requires org.junit.jupiter.api;
    requires org.slf4j;
    // the Kotlin contracts name kotlin.Pair in a signature; nothing here runs, so it is optional
    requires static kotlin.stdlib;
    // the side-loaded vavr hints (libs/vavr) name vavr types in their signatures; parsed, never run. Not
    // exported and not packed into libs.jar: consumers preload analyzedPackageFiles/libs/vavr explicitly
    requires static io.vavr;
    // the same for the side-loaded Eclipse Collections hints (libs/eclipsecollections)
    requires static org.eclipse.collections.api;
    requires static org.eclipse.collections.impl;
    // the Guava hints (libs/guava, packed into libs.jar since 2026-10-12) name Guava types in their signatures
    requires static com.google.common;
    // the AssertJ hints (libs/assertj, packed into libs.jar like libs/test) name AssertJ types in their signatures
    requires static org.assertj.core;
    // the same for hamcrest (libs/hamcrest) and JUnit's parameterized-test arguments (libs/junitparams)
    requires static org.hamcrest;
    requires static org.junit.jupiter.params;
    requires static org.mockito; // libs/mockito
    requires static org.apache.commons.io; // libs/commonsio
    requires static com.fasterxml.jackson.databind; // libs/jackson
    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.annotation;

    exports io.codelaser.maddi.aapi.archive.jdk;
    exports io.codelaser.maddi.aapi.archive.libs.log;
    exports io.codelaser.maddi.aapi.archive.libs.support;
    exports io.codelaser.maddi.aapi.archive.libs.test;
}