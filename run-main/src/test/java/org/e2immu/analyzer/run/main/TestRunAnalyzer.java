package org.e2immu.analyzer.run.main;

import ch.qos.logback.classic.Level;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class TestRunAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRunAnalyzer.class);

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
    }

    @Test
    public void test() throws IOException {
        File sourceDir = new File("../../util-internal/e2immu-internal-util/src/main/java");
        assertTrue(sourceDir.isDirectory(), "Absolute = " + sourceDir.getAbsolutePath());

        Main.main(new String[]{
                "--debug=classpath",
                "--classpath=" + String.join(":", InputConfigurationImpl.DEFAULT_MODULES),
                "--classpath=" + String.join(":", ToolChain.CLASSPATH_JUNIT),
                "--classpath=" + String.join(":", ToolChain.CLASSPATH_SLF4J_LOGBACK),
                "--source=" + sourceDir.getPath(),
                "--analysis-results-dir=build/e2immu",
                "--analyzed-annotated-api-dir=../../analyzer-shallow/e2immu-shallow-aapi/src/main/resources/json",
        });

        File output = new File("build/e2immu/OrgE2ImmuUtilInternalUtil.json");
        String content = Files.readString(output.toPath());
        String expected = """
                [{"fqn": "Torg.e2immu.util.internal.util.GetSetHelper", "data":{"hct":{"E":true}}},
                {"fqn": "Torg.e2immu.util.internal.util.ThrowingBiConsumer", "data":{"hct":{"E":true,"M":2,0:"S",1:"T"}}},
                {"fqn": "Torg.e2immu.util.internal.util.StringUtil", "data":{"hct":{"E":true}}},
                {"fqn": "Torg.e2immu.util.internal.util.Trie", "data":{"hct":{"E":true,"M":1,0:"T"}}},
                {"fqn": "Morg.e2immu.util.internal.util.Trie.recursivelyVisit(8)", "data":{"hct":{1:"T"}}},
                {"fqn": "Morg.e2immu.util.internal.util.MapUtil.compareMaps(0)", "data":{"hct":{0:"T",1:"D"}}},
                {"fqn": "Morg.e2immu.util.internal.util.MapUtil.compareKeys(1)", "data":{"hct":{0:"T"}}}]\
                """;
        assertEquals(expected, content);
    }
}
