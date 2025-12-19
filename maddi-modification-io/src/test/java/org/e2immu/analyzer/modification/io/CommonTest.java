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
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.MethodModAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.ModAnalyzerForTesting;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonTest {
    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected final String[] extraClassPath;
    protected ModAnalyzerForTesting modAnalyzer;
    protected PrepAnalyzer prepAnalyzer;
    protected final boolean storeErrorsInPVMap;

    protected CommonTest(String... extraClassPath) {
        this(false, extraClassPath);
    }

    protected CommonTest(boolean storeErrorsInPVMap, String... extraClassPath) {
        this.extraClassPath = extraClassPath;
        this.storeErrorsInPVMap = storeErrorsInPVMap;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("graph-algorithm")).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath(ToolChain.CLASSPATH_JUNIT)
                .addClassPath(ToolChain.CLASSPATH_SLF4J_LOGBACK);
        for (String extra : extraClassPath) {
            builder.addClassPath(extra);
        }
        builder.addSources("none");
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.util");

        new LoadAnalyzedPackageFiles(javaInspector.mainSources())
                .go(javaInspector, List.of(ToolChain.currentJdkAnalyzedPackages(),
                ToolChain.commonLibsAnalyzedPackages()));

        javaInspector.parse(JavaInspectorImpl.FAIL_FAST);
        runtime = javaInspector.runtime();
        prepAnalyzer = new PrepAnalyzer(runtime);
        IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setStoreErrors(storeErrorsInPVMap).build();
        modAnalyzer = new MethodModAnalyzerImpl(runtime, configuration);
    }

    protected List<Info> prepWork(TypeInfo typeInfo) {
        List<TypeInfo> typesLoaded = javaInspector.compiledTypesManager().typesLoaded(true);
        assertTrue(typesLoaded.stream().anyMatch(ti -> "java.util.ArrayList".equals(ti.fullyQualifiedName())));
        prepAnalyzer.initialize(typesLoaded);

        return prepAnalyzer.doPrimaryType(typeInfo);
    }
}
