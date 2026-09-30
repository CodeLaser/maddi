package io.codelaser.maddi.run.openjdkmain;

import io.codelaser.maddi.util.corpus.Corpora;
import ch.qos.logback.classic.Level;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;

@Tag("slow")
// Only READS the shared timefold checkout -- but two tests in jfocus-refactor-service (TestTimefoldDeadCode,
// TestTimefoldRandomRenames) rewrite that same tree in place, and a reader parsing a half-edited corpus fails just as
// hard as two writers colliding. Same claim, so the nightly scheduler keeps all three apart; Gradle ignores the tag.
@Tag("corpus-timefold-solver")
public class TestTimefoldSolver {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (multi-GB of captured output over 50k+ elements x iterations)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("io.codelaser.maddi.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    private static void assumeCorpus() {
        // Ask the locator, do NOT test the file here: only the locator honours
        // -Dmaddi.corpus.required, which slowTest sets so that an absent corpus FAILS instead of
        // skipping. A hand-written assumeTrue reported the same green as a run that analysed the
        // whole corpus -- TestGuava skipped in 9 ms with no guava on disk and the build said
        // SUCCESSFUL (measured 2026-09-30).
        Corpora.oss("timefold-solver").requireConfig();
    }

    @Test
    public void test() throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + Corpora.oss("timefold-solver").config()
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk"
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }

    @Disabled("one of the two tests is fine in normal circumstances")
    @Test
    public void test2() throws IOException, ParseException {
        assumeCorpus();
        int exitValue = Main.execute(new String[]{
                "--input-configuration=" + Corpora.ossRoot().resolve("timefold-solver/inputConfiguration2.json")
                , "--analysis-steps=modification"
                , "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk"
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }
}
