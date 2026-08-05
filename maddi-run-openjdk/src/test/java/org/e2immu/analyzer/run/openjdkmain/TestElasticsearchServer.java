package org.e2immu.analyzer.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The elasticsearch server sources: the large-method stress corpus (the work ceiling's degradation
 * bucket lives here — see LinkComputerImpl's WORK_REPORT notes). Not on the certified proving-ground
 * list; this driver exists for A/B and capacity runs. Historically OOM'd at 8G: run with
 * TESTXMX=24G or more.
 */
@Tag("slow")
public class TestElasticsearchServer {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (see TestTimefoldSolver)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("org.e2immu.analyzer.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    private static void assumeCorpus() {
        Assumptions.assumeTrue(Files.exists(TestOssCorpus.config("elasticsearch-server")),
                "requires the elasticsearch corpus checkout with its locally generated input configuration");
    }

    @Test
    public void test() throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + TestOssCorpus.config("elasticsearch-server")
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk"
        });
        // EXIT_ANALYSER_ERROR is expected on this corpus: one source file trips a javac ERROR that the
        // openjdk inspector files as a parse warning (PassThroughFieldSource; 2026-08-05: exactly 1 of
        // ~4,800 compilation units), and analysis runs to completion regardless. Crash-class exits
        // (internal exception, parser, inspection, IO) still fail the test.
        assertTrue(exitValue == Main.EXIT_OK || exitValue == Main.EXIT_ANALYSER_ERROR,
                "unexpected exit " + exitValue);
    }
}
