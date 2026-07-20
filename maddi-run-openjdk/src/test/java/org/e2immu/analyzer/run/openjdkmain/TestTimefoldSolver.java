package org.e2immu.analyzer.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;

@Tag("slow")
public class TestTimefoldSolver {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (multi-GB of captured output over 50k+ elements x iterations)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("org.e2immu.analyzer.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    private static void assumeCorpus() {
        Assumptions.assumeTrue(Files.exists(TestOssCorpus.config("timefold-solver")),
                "requires the timefold-solver corpus checkout with its locally generated input configuration");
    }

    @Test
    public void test() throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + TestOssCorpus.config("timefold-solver")
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk"
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }

    @Disabled("one of the two tests is fine in normal circumstances")
    @Test
    public void test2() throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + TestOssCorpus.ROOT.resolve("timefold-solver/inputConfiguration2.json")
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk"
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }
}
