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

package io.codelaser.maddi.callgraph;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.support.SetOnce;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import io.codelaser.maddi.support.SetOnceMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static io.codelaser.maddi.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static io.codelaser.maddi.inspection.resource.SourceSetImpl.sourceSetOf;

public class CommonTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);
    protected static final String ABX = "a.b.X";
    protected static final JavaInspector.ParseOptions ALLOW_COMPILATION_ERRORS
            = new JavaInspector.ParseOptions.Builder().build();

    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected final String[] extraClassPath;
    protected boolean openJdkParser;
    // under the openjdk parser, single-file parses (parseSingleFileInSourceSet) must use a source set that is
    // registered in the input configuration and carries the classpath dependencies. Tests that parse external
    // files in their own (per-directory) source sets override openJdkExtraSourceSetNames(); the built, registered
    // source sets are then available here by name.
    protected final SetOnceMap<String, SourceSet> openJdkSourceSetsByName = new SetOnceMap<>();

    // names of extra (per-directory) source sets a subclass needs registered for openjdk single-file parsing
    protected List<String> openJdkExtraSourceSetNames() {
        return List.of();
    }

    protected CommonTest() {
        this(new String[]{});
    }

    protected CommonTest(String... extraClassPath) {
        this.extraClassPath = extraClassPath;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
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
        openJdkParser = true;
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet orgSlf4j = sourceSetOf(org.slf4j.Logger.class, javaBase);
        SourceSet annotations = sourceSetOf(NotNull.class, javaBase);
        SourceSet maddiSupport = sourceSetOf(SetOnce.class, javaBase);
        SourceSet junitJupiter = sourceSetOf(Assertions.class, javaBase);
        List<SourceSet> dependencies = List.of(javaBase, orgSlf4j, annotations, maddiSupport, junitJupiter);
        SourceSet sources = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/"))
                .setDependencies(dependencies)
                .build();
        InputConfigurationImpl.Builder icBuilder = new InputConfigurationImpl.Builder()
                .addSourceSets(sources)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiSupport, orgSlf4j, annotations, junitJupiter);
        // a test may need extra, per-directory source sets for openjdk single-file parsing; each must be
        // registered in the input configuration and carry the classpath dependencies, and they must be distinct
        // so that identically-named types in different directories do not collide (types are keyed by
        // (fqn, source set)).
        for (String extraName : openJdkExtraSourceSetNames()) {
            SourceSet extraSet = new SourceSetImpl.Builder().setName(extraName).setUri(URI.create("file:/"))
                    .setDependencies(dependencies)
                    .build();
            openJdkSourceSetsByName.put(extraName, extraSet);
            icBuilder.addSourceSets(extraSet);
        }
        // extra JDK modules requested by a test (e.g. TestCloneBenchMethodHistogram needs java.desktop, java.sql,
        // ...); maddiParser() adds these too. Without them the clone-bench files do not resolve and the parse
        // records exceptions, which makes Summary.parseResult() throw.
        for (String extra : extraClassPath) {
            icBuilder.addClassPath(extra);
        }
        InputConfiguration inputConfiguration = icBuilder.build();
        javaInspector = new io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        sources.computePriorityDependencies();
        openJdkSourceSetsByName.valueStream().forEach(SourceSet::computePriorityDependencies);
    }

    private void maddiParser() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.MADDI_SUPPORT)
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
