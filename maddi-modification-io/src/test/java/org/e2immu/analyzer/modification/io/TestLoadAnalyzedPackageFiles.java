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

package org.e2immu.analyzer.modification.io;

import ch.qos.logback.classic.Level;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NOT_NULL;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NULLABLE;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

public class TestLoadAnalyzedPackageFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestLoadAnalyzedPackageFiles.class);

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.modification")).setLevel(Level.DEBUG);
    }

    @DisplayName("using files")
    @Test
    public void test1() throws IOException {
        List<String> classPath = List.of(
                "jmod:java.base", "jmod:java.xml", "jmod:java.net.http",
                "jmod:java.desktop", "jmod:java.datatransfer",
                JAR_WITH_PATH_PREFIX + "org/e2immu/support",
                JAR_WITH_PATH_PREFIX + "org/slf4j",
                JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic",
                JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api",
                JAR_WITH_PATH_PREFIX + "ch/qos/logback/core/spi",
                JAR_WITH_PATH_PREFIX + "org/apiguardian/api",
                JAR_WITH_PATH_PREFIX + "org/opentest4j"
        );
        JavaInspectorImpl javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder();
        classPath.forEach(inputConfigurationBuilder::addClassPath);
        inputConfigurationBuilder.addSources("none");
        javaInspector.initialize(inputConfigurationBuilder.build());

        LoadAnalyzedPackageFiles loadAnalyzedPackageFiles = new LoadAnalyzedPackageFiles(javaInspector.mainSources());
        String jdk = ToolChain.mapJreShortNameToAnalyzedPackageShortName(ToolChain.currentJre().shortName());
        File jdkDir = new File("../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk/" + jdk);
        LOGGER.info("JDK dir is {}", jdkDir);
        assertTrue(jdkDir.isDirectory());
        int countJdk = loadAnalyzedPackageFiles.goDir(javaInspector, jdkDir);
        assertTrue(countJdk > 1);

        File libDir = new File("../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/libs");
        LOGGER.info("Lib dir is {}", libDir);
        assertTrue(libDir.isDirectory());
        int countLib = loadAnalyzedPackageFiles.goDir(javaInspector, libDir);
        assertTrue(countLib > 0);

        doTests(javaInspector);
    }

    @DisplayName("using resource:")
    @Test
    public void test2() throws IOException {
        JavaInspectorImpl javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(ToolChain.CLASSPATH_SLF4J_LOGBACK)
                .addClassPath(ToolChain.CLASSPATH_JUNIT)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addSources("none");
        javaInspector.initialize(inputConfiguration.build());

        LoadAnalyzedPackageFiles loadAnalyzedPackageFiles = new LoadAnalyzedPackageFiles(javaInspector.mainSources());
        int count = loadAnalyzedPackageFiles.go(javaInspector, List.of(ToolChain.currentJdkAnalyzedPackages(),
                ToolChain.commonLibsAnalyzedPackages()));
        assertTrue(count > 1);
        doTests(javaInspector);
    }

    private static void doTests(JavaInspectorImpl javaInspector) {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().get(Object.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("toString", 0);
        // assertSame(TRUE, methodInfo.analysis().getOrDefault(CONTAINER_METHOD, FALSE));
        assertSame(NOT_NULL, methodInfo.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertFalse(methodInfo.isModifying());
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
/*
        TypeInfo hashMap = javaInspector.compiledTypesManager().get(HashMap.class);

        // subtype not present:
        TypeInfo sub = hashMap.findSubType("EntryIterator");
        assertNull(sub.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.class));

        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        Assertions.assertEquals("0=E", list.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.class).detailedSortedTypes());
        MethodInfo listAdd = list.findUniqueMethod("add", 1);
        ParameterInfo listAdd0 = listAdd.parameters().getFirst();
        Assertions.assertEquals("0=*", listAdd0.analysis().getOrNull(HiddenContentSelector.HCS_PARAMETER,
                HiddenContentSelector.class).detailed());*/
    }
}
