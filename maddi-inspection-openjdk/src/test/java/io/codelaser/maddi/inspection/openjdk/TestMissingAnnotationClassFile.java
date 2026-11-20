package io.codelaser.maddi.inspection.openjdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * A preloaded class-path type annotated with an annotation that the parse holds only as SOURCE, on disk. The preload
 * task does not see the sources, so javac reports "class file for ann.Ann not found" and the scanner mints a stub,
 * handed to the first scan as a pending preload. Two failures followed, both on the refactor engine's 55-set load of
 * maddi itself (the server's transform-support jar carries @NotModified, @Independent, @Fluent, @Modified and
 * @GetSet; the project defines maddi-annotation as source, so the engine drops its own maddi-annotation jar):
 * <ul>
 *     <li>the scan registered the real ann.Ann over the stub, but the "copy into CTM" loop still committed the
 *     stub, from this task's symbol, the source annotation: annotation nature, no interfaces, and under -ea
 *     TypeInspectionImpl.Builder.commit's java.lang.annotation.Annotation assert threw NoSuchElementException;</li>
 *     <li>a use met before the declaration (a.Use before ann.Ann) found the stub and compared its null URI: NPE,
 *     and the compilation unit dropped.</li>
 * </ul>
 */
public class TestMissingAnnotationClassFile {

    @Language("java")
    private static final String ANN = """
            package ann;
            public @interface Ann { boolean hc() default false; }
            """;

    @Language("java")
    private static final String LIB = """
            package p;
            import ann.Ann;
            @Ann(hc = true)
            public class Lib { @Ann public void run(@Ann(hc = true) Object o) { } }
            """;

    @Language("java")
    private static final String USE = """
            package a;
            public class Use { @ann.Ann void go(p.Lib l) { l.run(null); } }
            """;

    /** Compile Ann and Lib, jar Lib only. */
    private static Path compileLibWithoutAnn(Path tmp) throws IOException {
        Path src = tmp.resolve("src");
        Path classes = Files.createDirectories(tmp.resolve("classes"));
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("ann.Ann", ANN);
        sources.put("p.Lib", LIB);
        List<String> args = new ArrayList<>(List.of("-d", classes.toString()));
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path javaFile = src.resolve(e.getKey().replace('.', '/') + ".java");
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, e.getValue());
            args.add(javaFile.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        assertEquals(0, compiler.run(null, null, null, args.toArray(String[]::new)));
        Path jar = tmp.resolve("lib.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("p/Lib.class"));
            jos.write(Files.readAllBytes(classes.resolve("p/Lib.class")));
            jos.closeEntry();
        }
        return jar;
    }

    @DisplayName("a stub minted by the preload yields to the source annotation it names")
    @Test
    public void missingAnnotation() throws Exception {
        Path tmp = Files.createTempDirectory("missing-ann-");
        try {
            Path jar = compileLibWithoutAnn(tmp);
            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet lib = new SourceSetImpl.Builder().setName("lib.jar").setUri(jar.toUri())
                    .setLibrary(true).setExternalLibrary(true).setDependencies(List.of(javaBase)).build();
            // ON DISK, as a build tool's source set: the preload task does not see these sources
            Path srcA = tmp.resolve("srcA");
            for (Map.Entry<String, String> e : Map.of("ann.Ann", ANN, "a.Use", USE).entrySet()) {
                Path javaFile = srcA.resolve(e.getKey().replace('.', '/') + ".java");
                Files.createDirectories(javaFile.getParent());
                Files.writeString(javaFile, e.getValue());
            }
            SourceSet setA = new SourceSetImpl.Builder().setName("a").setUri(srcA.toUri())
                    .setSourceDirectories(List.of(srcA))
                    .setDependencies(List.of(javaBase, lib)).build();
            InputConfiguration ic = new InputConfigurationImpl.Builder()
                    .addClassPathParts(javaBase, lib).addSourceSets(setA).build();
            JavaInspector javaInspector = new JavaInspectorImpl();
            javaInspector.initialize(ic);
            javaInspector.preload("p");

            Summary summary = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES);

            assertEquals(List.of(), summary.parseExceptions().stream().map(String::valueOf).toList(),
                    "no type may fail to commit");
            TypeInfo ann = javaInspector.compiledTypesManager().get("ann.Ann", setA);
            assertNotNull(ann, "ann.Ann must be in the CST");
            assertTrue(ann.typeNature().isAnnotation(), ann.typeNature().toString());
            assertEquals(List.of("java.lang.annotation.Annotation"), ann.interfacesImplemented().stream()
                    .map(pt -> pt.typeInfo().fullyQualifiedName()).toList());
            TypeInfo use = javaInspector.compiledTypesManager().get("a.Use", setA);
            assertNotNull(use, "a.Use must be in the CST");
            assertSame(ann, use.findUniqueMethod("go", 1).annotations().getFirst().typeInfo(),
                    "the use resolves to the source annotation, not to the stub");
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted((x, y) -> y.getNameCount() - x.getNameCount()).forEach(p -> p.toFile().delete());
            }
        }
    }
}
