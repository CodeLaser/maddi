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
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
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
