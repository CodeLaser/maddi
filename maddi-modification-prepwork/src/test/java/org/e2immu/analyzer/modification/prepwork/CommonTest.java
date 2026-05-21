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

package org.e2immu.analyzer.modification.prepwork;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);
    protected static final String ABX = "a.b.X";
    protected static final JavaInspector.ParseOptions ALLOW_COMPILATION_ERRORS
            = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl.ParseOptionsBuilder().build();

    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected final String[] extraClassPath;

    protected CommonTest() {
        this(new String[]{});
    }

    protected CommonTest(String... extraClassPath) {
        this.extraClassPath = extraClassPath;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger(MethodAnalyzer.class)).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger(PrepAnalyzer.class)).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException, URISyntaxException {
        String impl = System.getProperty("maddi_parser", "maddi");
        LOGGER.info("Parsing with {}", impl);
        if ("maddi".equalsIgnoreCase(impl)) {
            maddiParser();
        } else if ("openJdk".equalsIgnoreCase(impl)) {
            openJdkParser();
        } else throw new UnsupportedEncodingException("Unknown parser " + impl);
        runtime = javaInspector.runtime();
    }

    private void openJdkParser() throws URISyntaxException, IOException {

        Path maddiSupportClasses = Path.of("../maddi-support/build/classes/java/main/");
        assertTrue(Files.isDirectory(maddiSupportClasses));
        SourceSet maddiSupport = new SourceSetImpl.Builder().setName("maddi-support")
                .setUri(maddiSupportClasses.toUri())
                .setLibrary(true).setExternalLibrary(true).build();

        URI slf4jApiUri = org.slf4j.Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet orgSlf4jApi = new SourceSetImpl.Builder().setName("slf4j-api-2.0.17.jar")
                .setUri(slf4jApiUri)
                .setExternalLibrary(true)
                .setModule(true)
                .build();

        URI annotationsUri = NotNull.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet annotations = new SourceSetImpl.Builder().setName("annotations-26.1.0.jar")
                .setUri(annotationsUri)
                .setExternalLibrary(true)
                .setModule(true)
                .build();

        URI junitJupiterApi = Test.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet junitJupiter = new SourceSetImpl.Builder().setName("junit-jupiter-api-6.0.3.jar")
                .setUri(junitJupiterApi)
                .setExternalLibrary(true)
                .setModule(true)
                .build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(org.e2immu.language.inspection.openjdk.JavaInspectorImpl.TEST_PROTOCOL_SOURCE_SET)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiSupport, orgSlf4jApi, annotations, junitJupiter)
                .build();
        javaInspector = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
    }

    private void maddiParser() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                // NOTE: no access to ToolChain
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic")
                .addClassPath(JAR_WITH_PATH_PREFIX + "io/codelaser/jfocus/transform/support")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        for (String extra : extraClassPath) {
            builder.addClassPath(extra);
        }
        builder.addSources("none");
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.parse(JavaInspectorImpl.FAIL_FAST);
        javaInspector.javaBase().computePriorityDependencies();
    }
}
