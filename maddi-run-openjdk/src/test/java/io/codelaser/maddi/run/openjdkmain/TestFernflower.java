package io.codelaser.maddi.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;

/**
 * The SMALL corpus (~500 source files vs timefold's ~3,500 types): fast full-chain feedback for engine
 * work (parallelism A/Bs, worklist experiments) where a timefold round costs ~40 minutes.
 */
@Tag("slow")
public class TestFernflower {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (multi-GB of captured output over many elements x iterations)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("io.codelaser.maddi.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    private static void assumeCorpus() {
        Assumptions.assumeTrue(Files.exists(TestOssCorpus.config("fernflower")),
                "requires the fernflower corpus checkout with its locally generated input configuration");
    }

    @Test
    public void test() throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + TestOssCorpus.config("fernflower")
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk"
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }
}
