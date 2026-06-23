package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.support.SetOnce;
import org.e2immu.util.internal.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.resource.SourceSetImpl.sourceSetModuleOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector5RealClasspathModule {

    private JavaInspector javaInspector;
    private SourceSet cstApi;

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet annotations = sourceSetModuleOf(NotNull.class, javaBase);
        SourceSet maddiSupport = sourceSetModuleOf(SetOnce.class, javaBase);
        SourceSet maddiUtil = sourceSetModuleOf(StringUtil.class, javaBase);

        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java");
        assertTrue(Files.isDirectory(cstApiPath));
        cstApi = new SourceSetImpl.Builder().setName("cst-api")
                .setSourceDirectories(List.of(cstApiPath))
                .setUri(URI.create("file:/")) // not important here
                .setModule(true)
                .setDependencies(List.of(javaBase, annotations, maddiSupport, maddiUtil))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi)
                .addClassPathParts(javaBase, annotations, maddiSupport, maddiUtil)
                .build();
        assertEquals(4, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
    }

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build(); // not ignoring module here!
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        ModuleInfo moduleInfo = parseResult.moduleInfo(cstApi);
        assertEquals(13, moduleInfo.exports().size());

        TypeInfo element = parseResult.findType("org.e2immu.language.cst.api.element.Element");
        assertTrue(element.isInterface());
    }
}
