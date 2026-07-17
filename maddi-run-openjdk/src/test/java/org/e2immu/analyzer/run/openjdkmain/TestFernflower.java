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
 * The SMALL corpus (~500 source files vs timefold's ~3,500 types): fast full-chain feedback for engine
 * work (parallelism A/Bs, worklist experiments) where a timefold round costs ~40 minutes.
 */
public class TestFernflower {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (multi-GB of captured output over many elements x iterations)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("org.e2immu.analyzer.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    private static void assumeCorpus() {
        Assumptions.assumeTrue(Files.exists(Path.of("/Users/bnaudts/git/test-oss/fernflower")),
                "requires the fernflower corpus checkout (paths baked into the input configuration)");
    }

    @Test
    public void test() throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=./src/test/resources/inputConfiguration/fernflower.json"
                , "--analysis-steps=modification"
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }
}
