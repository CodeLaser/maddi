package org.e2immu.analyzer.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestLangchain4j {


    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
    }

    // External corpus (/Users/.../test-oss/langchain4j) not present on this machine. It previously false-passed via
    // the silent-exit-0 bug (now fixed); with correct exit codes the missing corpus yields a non-zero exit, and
    // Main.main's System.exit then crashes the test JVM. Re-enable where the corpus exists (prefer Main.execute).
    @Disabled("external langchain4j corpus not present; see comment")
    @Test
    public void test() throws IOException {
        Main.main(new String[]{
                "--input-configuration=./src/test/resources/inputConfiguration/langchain4j.json"
                //,"--parallel"
                , "--analysis-steps=prep"
                //,"--debug=memory",
        });
    }
}