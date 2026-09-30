/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.run.main;

import ch.qos.logback.classic.Level;
import io.codelaser.maddi.inspection.integration.ToolChain;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.shallow")).setLevel(Level.DEBUG);
    }

    @Disabled
    @Test
    public void test() throws IOException {
        File sourceDir = new File("../internal-util/src/main/java");
        assertTrue(sourceDir.isDirectory(), "Absolute = " + sourceDir.getAbsolutePath());

        Main.main(new String[]{
                "--debug=classpath",
                "--classpath=" + String.join(":", InputConfigurationImpl.DEFAULT_MODULES),
                "--classpath=" + String.join(":", ToolChain.CLASSPATH_JUNIT),
                "--classpath=" + String.join(":", ToolChain.CLASSPATH_SLF4J_LOGBACK),
                "--source=" + sourceDir.getPath(),
                "--analysis-results-dir=build/maddi",
                "--preload-analysis-results-dirs=../maddi-aapi-archive/src/main/resources/json",
        });

        File output = new File("build/maddi/OrgE2ImmuUtilInternalUtil.json");
        String content = Files.readString(output.toPath());
        String expected = """
                [{"fqn": "Tio.codelaser.maddi.util.GetSetHelper", "data":{"hct":{"E":true}}},
                {"fqn": "Tio.codelaser.maddi.util.ThrowingBiConsumer", "data":{"hct":{"E":true,"M":2,0:"S",1:"T"}}},
                {"fqn": "Tio.codelaser.maddi.util.StringUtil", "data":{"hct":{"E":true}}},
                {"fqn": "Tio.codelaser.maddi.util.Trie", "data":{"hct":{"E":true,"M":1,0:"T"}}},
                {"fqn": "Mio.codelaser.maddi.util.Trie.recursivelyVisit(8)", "data":{"hct":{1:"T"}}},
                {"fqn": "Mio.codelaser.maddi.util.MapUtil.compareMaps(0)", "data":{"hct":{0:"T",1:"D"}}},
                {"fqn": "Mio.codelaser.maddi.util.MapUtil.compareKeys(1)", "data":{"hct":{0:"T"}}}]\
                """;
        assertEquals(expected, content);
    }
}
