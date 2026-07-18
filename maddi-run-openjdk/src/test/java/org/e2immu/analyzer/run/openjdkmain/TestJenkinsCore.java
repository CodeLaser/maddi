package org.e2immu.analyzer.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Jenkins core (enterprise/DI shape: mutable JavaBeans, Descriptor hierarchies, generated localizer
 * sources; 30k elements). Green since 2026-07-18 (certified fixpoint, ~12.5min at PARALLEL defaults).
 * Known residue: 10 test files dropped at scan (Messages-class resolution from the test source set).
 */
public class TestJenkinsCore {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("org.e2immu.analyzer.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    private static final String CONFIG = "/Users/bnaudts/git/test-oss/jenkins/inputConfiguration.json";

    @Test
    public void test() throws IOException, ParseException {
        Assumptions.assumeTrue(Files.exists(Path.of(CONFIG)),
                "requires the jenkins corpus checkout with its generated input configuration");
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + CONFIG
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk"
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }
}
