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
import org.e2immu.language.java.openjdk.InputConfigurationSupport;
import org.e2immu.support.SetOnce;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.e2immu.language.java.openjdk.InputConfigurationSupport.sourceSetOf;

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

    protected void openJdkParser() throws URISyntaxException, IOException {
        SourceSet javaBase = InputConfigurationSupport.javaBase();
        SourceSet orgSlf4j = sourceSetOf(org.slf4j.Logger.class, javaBase);
        SourceSet annotations = sourceSetOf(NotNull.class, javaBase);
        SourceSet maddiSupport = sourceSetOf(SetOnce.class, javaBase);
        SourceSet junitJupiter = sourceSetOf(Assertions.class, javaBase);
        SourceSet sources = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase, orgSlf4j, annotations, maddiSupport, junitJupiter))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sources)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiSupport, orgSlf4j, annotations, junitJupiter)
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
