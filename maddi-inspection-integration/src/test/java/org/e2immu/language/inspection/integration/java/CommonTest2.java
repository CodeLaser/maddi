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

package org.e2immu.language.inspection.integration.java;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;


public abstract class CommonTest2 {
    protected JavaInspector javaInspector;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    public Map<String, String> sourcesByURIString(Map<String, String> sourcesByFqn) {
        return sourcesByFqn.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
    }

    public ParseResult init(Map<String, String> sourcesByFqn) throws IOException {
        return init(sourcesByFqn, Map.of());
    }

    public ParseResult init(Map<String, String> sourcesByFqn, Map<String, String> testSourcesByFqn)
            throws IOException {
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);
        Map<String, String> testSourcesByURIString = sourcesByURIString(testSourcesByFqn);

        InputConfiguration inputConfiguration = makeInputConfiguration(sourcesByURIString, testSourcesByURIString);
        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration);
        Map<String,String> combined = new HashMap<>(sourcesByURIString);
        combined.putAll(testSourcesByURIString);
        return javaInspector.parse(combined,
                        new JavaInspectorImpl.ParseOptionsBuilder().setFailFast(true).setDetailedSources(true).build())
                .parseResult();
    }

    public InputConfiguration makeInputConfiguration(Map<String, String> sourcesByURIString,
                                                     Map<String, String> testSourcesByURIString) {
        InputConfiguration.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                // NOTE: no access to ToolChain here; this is rather exceptional
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        sourcesByURIString.keySet().forEach(inputConfigurationBuilder::addSources);
        testSourcesByURIString.keySet().forEach(inputConfigurationBuilder::addTestSources);
        return inputConfigurationBuilder.build();
    }
}
