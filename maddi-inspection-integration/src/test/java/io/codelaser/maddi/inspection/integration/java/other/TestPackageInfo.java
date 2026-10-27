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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.ToolChain;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPackageInfo {
    @Test
    public void test() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources(InputConfigurationImpl.MAVEN_TEST)
                .addRestrictSourceToPackages("io.codelaser.maddi.inspection.integration.java.importhelper.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(ToolChain.CLASSPATH_JUNIT)
                .addClassPath(ToolChain.CLASSPATH_INTELLIJ_LANG)
                .addClassPath(JavaInspectorImpl.MADDI_SUPPORT)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        ParseResult parseResult = javaInspector.parse(JavaInspectorImpl.FAIL_FAST).parseResult();
        TypeInfo packageInfo = parseResult
                .findType("io.codelaser.maddi.inspection.integration.java.importhelper.package-info");
        assertNotNull(packageInfo);
        assertTrue(packageInfo.typeNature().isPackageInfo());
        String printed = javaInspector.print2(packageInfo.compilationUnit());

        assertTrue(printed.startsWith("@Docstrings( {"));
    }
}
