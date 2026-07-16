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

public class TestLangchain4j {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
        // corpus-scale noise (multi-GB of captured output over 50k+ elements x iterations)
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("org.e2immu.analyzer.modification.link.impl.linkgraph.RedundantLinks")).setLevel(Level.ERROR);
    }

    @Test
    public void test() throws IOException, ParseException {
        Assumptions.assumeTrue(Files.exists(Path.of("/Users/bnaudts/git/test-oss/langchain4j")),
                "requires the langchain4j corpus checkout (paths baked into the input configuration)");
        int exitValue = Main.execute(new String[]{
                "--input-configuration=./src/test/resources/inputConfiguration/langchain4j.json"
                //,"--parallel"
                , "--analysis-steps=modification"
                //,"--debug=memory",
        });
        assertEquals(Main.EXIT_OK, exitValue);
    }
}
