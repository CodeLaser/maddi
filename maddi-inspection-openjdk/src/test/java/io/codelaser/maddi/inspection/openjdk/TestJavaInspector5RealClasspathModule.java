package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.support.SetOnce;
import io.codelaser.maddi.util.StringUtil;
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

import static io.codelaser.maddi.inspection.resource.SourceSetImpl.sourceSetModuleOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector5RealClasspathModule {

    private JavaInspector javaInspector;
    private SourceSet cstApi;

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet annotations = sourceSetModuleOf(NotNull.class, javaBase);
        SourceSet maddiAnnotation = sourceSetModuleOf(io.codelaser.maddi.annotation.Immutable.class, javaBase);
        SourceSet maddiSupport = sourceSetModuleOf(SetOnce.class, javaBase, maddiAnnotation);
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

        TypeInfo element = parseResult.findType("io.codelaser.maddi.cst.api.element.Element");
        assertTrue(element.isInterface());
    }

    // With ignoreModule the runner compiles everything in the unnamed module, so javac drops module-info.java and
    // produces no ModuleInfo. Refactorings still need the module descriptor, so the inspector falls back to the
    // home-made parser (JavaInspectorImpl.parseModuleInfoDescriptor). Before that fallback, moduleInfo(cstApi) was
    // null here; now it is populated with the same exports as the javac path above.
    @Test
    public void test2IgnoreModuleStillGetsDescriptor() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).setIgnoreModule(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        ModuleInfo moduleInfo = parseResult.moduleInfo(cstApi);
        assertNotNull(moduleInfo, "module descriptor must be parsed even under ignoreModule");
        assertEquals(13, moduleInfo.exports().size());
    }

    /*
    print2 renders a compilation unit through the real formatter, and a module-info.java is a compilation unit with
    a module declaration and no types. It used to emit nothing of the module at all: the directives answered
    print() with null, ModuleInfo.print() gave the bare name, and CompilationUnitPrinterImpl only walked types().

    This is the round trip a refactoring lever needs -- parse a descriptor, change a directive, print it back --
    so it is asserted against the real file rather than a fixture.
     */
    @Test
    public void test3Print2RendersTheModuleDeclaration() throws IOException {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).setIgnoreModule(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();
        ModuleInfo moduleInfo = parseResult.moduleInfo(cstApi);

        String printed = javaInspector.print2(moduleInfo.compilationUnit());
        String onDisk = Files.readString(Path.of("../maddi-cst-api/src/main/java/module-info.java"));

        // non-vacuity: normalise() must not be comparing two empty strings, and the body must really be there
        assertTrue(printed.contains("module io.codelaser.maddi.cst.api {"), printed);
        assertTrue(printed.contains("requires org.jetbrains.annotations;"), printed);
        assertTrue(printed.contains("exports io.codelaser.maddi.cst.api.element;"), printed);

        assertEquals(normalise(onDisk), normalise(printed));
    }

    /*
    The out-of-sync read: a module-info.java of a project that is not in this parse at all -- note that no parse()
    runs in this test. A build-unit split has to extend a qualified export in exactly such a sibling, whose module
    would otherwise have to be edited with a regular expression.

    The two properties an edit needs are asserted rather than the mere fact that something came back: a `file:` URI
    on the compilation unit (Editor.openForEditing resolves the file from it, and refuses anything else), and a
    Source on every directive (that range is what a replacement is written over).
     */
    @Test
    public void test4ParseModuleInfoByPathWithoutAParse() {
        ModuleInfo moduleInfo = javaInspector.parseModuleInfo(
                Path.of("../maddi-cst-api/src/main/java/module-info.java"));

        assertNotNull(moduleInfo);
        assertEquals("io.codelaser.maddi.cst.api", moduleInfo.name());
        assertEquals(13, moduleInfo.exports().size(), "the same descriptor the two parsing paths produce");

        assertTrue(moduleInfo.compilationUnit().uri().toString().startsWith("file:"),
                "an edit resolves the file from this URI: " + moduleInfo.compilationUnit().uri());
        for (ModuleInfo.Exports exports : moduleInfo.exports()) {
            assertNotNull(exports.source(), exports.packageName());
            assertTrue(exports.source().beginLine() > 0, exports.packageName() + " has no line");
        }
    }

    @Test
    public void test4bNotAModuleInfoIsNullRatherThanAFailure() {
        // a best-effort read of a file outside the analysed tree must never fail a run
        assertNull(javaInspector.parseModuleInfo(Path.of("../maddi-cst-api/src/main/java/does-not-exist.java")));
        assertNull(javaInspector.parseModuleInfo(
                Path.of("../maddi-cst-api/src/main/java/io/codelaser/maddi/cst/api/element/Element.java")),
                "an ordinary compilation unit is not a module declaration");
    }

    // the file carries a licence header the descriptor does not model; compare the declaration itself
    private static String normalise(String s) {
        int module = s.indexOf("module ");
        return s.substring(module < 0 ? 0 : module).replaceAll("[ \t]+", " ").strip();
    }
}
