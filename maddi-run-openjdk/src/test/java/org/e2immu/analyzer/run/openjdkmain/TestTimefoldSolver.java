package org.e2immu.analyzer.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTimefoldSolver {


    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
    }

    @Test
    public void test() {
        Main.main(new String[]{
                "--input-configuration=./src/test/resources/inputConfiguration/timefold-solver.json"
                , "--analysis-steps=prep"
        });
    }

    @Disabled("one of the two tests is fine in normal circumstances")
    @Test
    public void test2() {
        Main.main(new String[]{
                "--input-configuration=./src/test/resources/inputConfiguration/timefold-solver2.json"
                , "--analysis-steps=prep"
        });
    }
}