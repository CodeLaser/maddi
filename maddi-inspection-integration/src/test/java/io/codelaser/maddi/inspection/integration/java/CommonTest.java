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

package io.codelaser.maddi.inspection.integration.java;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.codelaser.maddi.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;


public abstract class CommonTest {
    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected final boolean allowCreationOfStubTypes;

    protected CommonTest() {
        this(false);
    }

    protected CommonTest(boolean allowCreationOfStubTypes) {
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("io.codelaser.maddi.cst.impl")).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = new JavaInspectorImpl(false, allowCreationOfStubTypes);
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources(InputConfigurationImpl.MAVEN_TEST)
                .addRestrictSourceToPackages("io.codelaser.maddi.inspection.integration.java.importhelper.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                // Two entries since the 0.9.1 split: the annotations moved to maddi-annotation while
                // Either/SetOnce stayed in maddi-support. These are hardcoded output directories, so
                // nothing propagates them automatically -- omitting the first makes every parse that
                // touches an annotation fail with "Cannot find type Docstrings".
                .addClassPath("../maddi-annotation/build/classes/java/main")
                .addClassPath("../maddi-support/build/classes/java/main")
                // NOTE: no access to ToolChain here; this is rather exceptional
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/assertj/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/springframework/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/springframework/test")
                .addClassPath(JAR_WITH_PATH_PREFIX + "lombok")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/mockito")
                .build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.parse(new JavaInspector.ParseOptions.Builder()
                .setFailFast(true)
                .setLombok(false)
                .setDetailedSources(true).build());
        javaInspector.javaBase().computePriorityDependencies();
        runtime = javaInspector.runtime();
    }
}
