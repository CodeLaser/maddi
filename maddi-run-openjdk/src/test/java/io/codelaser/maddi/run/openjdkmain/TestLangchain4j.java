package io.codelaser.maddi.run.openjdkmain;

import io.codelaser.maddi.util.corpus.Corpora;
import ch.qos.logback.classic.Level;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;

@Tag("slow")
public class TestLangchain4j {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (multi-GB of captured output over 50k+ elements x iterations)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("io.codelaser.maddi.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    @Test
    public void test() throws IOException, ParseException {
        // Ask the locator, do NOT test the file here: only the locator honours
        // -Dmaddi.corpus.required, which slowTest sets so that an absent corpus FAILS instead of
        // skipping. A hand-written assumeTrue reported the same green as a run that analysed the
        // whole corpus -- TestGuava skipped in 9 ms with no guava on disk and the build said
        // SUCCESSFUL (measured 2026-09-30).
        Corpora.oss("langchain4j").requireConfig();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + Corpora.oss("langchain4j").config()
                //,"--parallel"
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk"
                //,"--debug=memory",
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }
}
