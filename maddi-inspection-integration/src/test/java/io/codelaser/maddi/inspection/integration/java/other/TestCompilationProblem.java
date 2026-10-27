/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration.java.other;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.parsers.java.ParseException;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestCompilationProblem {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TestCompilationProblem.class);

    protected JavaInspector javaInspector;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources("src/test/resources/compilationError")
                .addRestrictSourceToPackages("a.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.MADDI_SUPPORT)
                .build();
        javaInspector.initialize(inputConfiguration);
        try {
            javaInspector.parse(new JavaInspector.ParseOptions.Builder().setFailFast(true).setDetailedSources(true).build());
        } catch (Summary.FailFastException ff) {
            Summary.ParseException e = (Summary.ParseException) ff.getCause();
            LOGGER.error("Parse exception", e.getCause() == null ? e : e.getCause());
            assertTrue(e.uri().toString().endsWith("compilationError/a/Faulty.java"));
            assertTrue(e.getMessage().contains("Encountered an error at input:4:33"));
            assertInstanceOf(ParseException.class, e.throwable());
        } catch (Exception e) {
            fail("This exception should not be raised: " + e.getClass());
        }
    }

    @Test
    public void test2FailFastFalse() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources("src/test/resources/compilationError")
                .addRestrictSourceToPackages("a.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.MADDI_SUPPORT)
                .build();
        javaInspector.initialize(inputConfiguration);
        try {
            Summary summary = javaInspector.parse(new JavaInspector.ParseOptions.Builder().setFailFast(false).setDetailedSources(true).build());
            assertEquals(1, summary.parseExceptions().size());
            Summary.ParseException e = summary.parseExceptions().getFirst();
            assertTrue(e.uri().toString().endsWith("compilationError/a/Faulty.java"));
            assertTrue(e.getMessage().contains("Encountered an error at input:4:33"));
            assertInstanceOf(ParseException.class, e.throwable());
        } catch (Exception e) {
            fail("This exception should not be raised: " + e.getClass());
        }
    }
}
