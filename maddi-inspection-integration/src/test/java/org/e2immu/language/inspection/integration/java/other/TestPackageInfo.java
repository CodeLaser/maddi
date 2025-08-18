package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestPackageInfo {
    @Test
    public void test() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources(InputConfigurationImpl.MAVEN_TEST)
                .addRestrictSourceToPackages("org.e2immu.language.inspection.integration.java.importhelper.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(ToolChain.CLASSPATH_JUNIT)
                .addClassPath(ToolChain.CLASSPATH_INTELLIJ_LANG)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        ParseResult parseResult = javaInspector.parse(JavaInspectorImpl.FAIL_FAST).parseResult();
        TypeInfo packageInfo = parseResult
                .findType("org.e2immu.language.inspection.integration.java.importhelper.package-info");
        assertNotNull(packageInfo);
        assertTrue(packageInfo.typeNature().isPackageInfo());
        String printed = javaInspector.print2(packageInfo);

        assertTrue(printed.startsWith("@Docstrings( {"));
    }
}
