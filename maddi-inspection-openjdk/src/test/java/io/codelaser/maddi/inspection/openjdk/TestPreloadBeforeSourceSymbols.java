package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>The source-and-jar duplicate-interfaces defect</b> — {@code docs/handoff-source-and-jar-duplicate-interfaces.md}.
 * Found by parsing the whole CodeLaser tree as one project: five of {@code maddi-annotation}'s twenty-seven types
 * died at {@code commit()} with {@code "Extending multiple identical interfaces"}.
 * <p>
 * The hermetic shape is this one. A package is <b>preloaded from the class path</b>, and one of its class-file
 * types is annotated with an annotation the parse holds <b>as source</b>. Resolving that annotation reaches
 * {@code ClassSymbolScanner.lazilyLoadPrimaryTypeFromClassFile}, which builds a {@code TypeInfo} in the
 * <em>current source set</em> (the symbol is a source symbol, so {@code ensureSourceSet} attributes it there) and
 * runs its whole setup block on it. {@code ScanCompilationUnit.visitClass} then <b>adopts</b> that very
 * {@code TypeInfo} — same FQN, same source set — and adds the hierarchy a second time. An {@code @interface}
 * implicitly implements {@code java.lang.annotation.Annotation}, so its interface list is never empty and the
 * distinctness assert in {@code TypeInspectionImpl} is what notices.
 * <p>
 * ⛔ <b>WHY THE EXISTING GUARD DID NOT FIRE.</b> {@code isSourceSymbol} answered from
 * {@code topLevelClassSymbolsOfSources} alone, and {@code ScanCompilationUnits.scan()} published that map
 * <em>after</em> the preload — so during the preload the guard read {@code null}, and a null map returned
 * {@code false}: "not a source symbol", for a symbol that was a source symbol all along. It failed open exactly
 * once per parse, in the first source set (the preload runs only there), which is why the failure set looked
 * arbitrary. ⭐ It was not: the five {@code maddi-annotation} types that died are exactly the five maddi
 * annotations that occur in the preloaded package {@code io.codelaser.jfocus.transform.support} —
 * {@code @NotModified}, {@code @Independent}, {@code @GetSet}, {@code @Fluent}, {@code @Modified}. The other
 * twenty-two are never resolved during the preload, so nothing writes them twice.
 * <p>
 * ⚠ <b>THE JAR ON THE CLASS PATH IS NOT WHAT DOES IT.</b> The original write-up read the shape as one FQN
 * reachable both as source and as bytecode. It is simpler than that, and this fixture holds no artifact for
 * {@code a.b.*} at all: the jar carries only package {@code p}, so javac has nothing but the sources for
 * {@code a.b.Marker} and {@code a.b.Impl} and hands the scanner a <em>source</em> symbol both times.
 * <p>
 * Two independent repairs, and this test pins the second: {@code scan()} now publishes the source symbols before
 * the preload, and {@code isSourceSymbol} no longer depends on that timing — it asks javac (a symbol with a
 * source file and no class file was entered from source). {@code ScanCompilationUnit.continueType} additionally
 * claims the type on the shared registry, so a class-file load in either direction cannot build the hierarchy a
 * second time.
 * <p>
 * The annotation half is asserted too: the same hole appended the type's own {@code @Target} a second time, and
 * nothing asserts distinctness on annotations, so that half was silent — a wrong answer, not a crash.
 */
public class TestPreloadBeforeSourceSymbols {

    /** Held as SOURCE by the parse. An {@code @interface}: implicitly implements {@code Annotation}. */
    @Language("java")
    private static final String MARKER = """
            package a.b;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            @Target(ElementType.TYPE)
            public @interface Marker { }
            """;

    /** Lives in the preloaded class-path package, and carries the annotation above. */
    @Language("java")
    private static final String USED = """
            package p;
            @a.b.Marker
            public class Used { }
            """;

    /** A plain class with an {@code implements} clause, to show the defect is not about annotations. */
    @Language("java")
    private static final String IFACE = """
            package a.b;
            public interface Iface { }
            """;

    @Language("java")
    private static final String IMPL = """
            package a.b;
            public class Impl implements Iface { }
            """;

    /** Also in the preloaded package: names {@code a.b.Impl}, which the parse holds as source. */
    @Language("java")
    private static final String USER = """
            package p;
            public class User { public a.b.Impl impl; }
            """;

    /**
     * Compile all four types, then jar ONLY the {@code p} package: {@code a.b.*} must NOT be on the class path,
     * so javac resolves those names from the source set and hands the scanner a <em>source</em> symbol — which is
     * the whole point.
     */
    private static Path compilePackagePJar(Path tmp) throws IOException {
        Path src = tmp.resolve("src");
        Path classes = Files.createDirectories(tmp.resolve("classes"));
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("a.b.Marker", MARKER);
        sources.put("a.b.Iface", IFACE);
        sources.put("a.b.Impl", IMPL);
        sources.put("p.Used", USED);
        sources.put("p.User", USER);
        List<String> files = new java.util.ArrayList<>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path javaFile = src.resolve(e.getKey().replace('.', '/') + ".java");
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, e.getValue());
            files.add(javaFile.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        List<String> args = new java.util.ArrayList<>(List.of("-d", classes.toString()));
        args.addAll(files);
        int rc = compiler.run(null, null, null, args.toArray(String[]::new));
        assertEquals(0, rc, "the class-path artifact must compile");

        Path jar = tmp.resolve("preloaded-p.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar));
             Stream<Path> walk = Files.walk(classes.resolve("p"))) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                jos.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                jos.write(Files.readAllBytes(file));
                jos.closeEntry();
            }
        }
        return jar;
    }

    @DisplayName("a preloaded class-path type annotated from source must not build that source type's hierarchy")
    @Test
    public void preloadedTypeReferencingSource() throws Exception {
        Path tmp = Files.createTempDirectory("preload-source-");
        try {
            Path jar = compilePackagePJar(tmp);

            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet preloaded = new SourceSetImpl.Builder()
                    .setName("preloaded-p.jar").setUri(jar.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();
            SourceSet setA = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/a/"))
                    .setDependencies(List.of(javaBase, preloaded)).build();

            InputConfiguration ic = new InputConfigurationImpl.Builder()
                    .addClassPathParts(javaBase, preloaded)
                    .addSourceSets(setA)
                    .build();
            JavaInspector javaInspector = new JavaInspectorImpl();
            javaInspector.initialize(ic);
            javaInspector.preload("p");

            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(setA, Map.of("a.b.Marker", MARKER, "a.b.Iface", IFACE, "a.b.Impl", IMPL));

            Summary summary = javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
            assertEquals(List.of(), summary.parseExceptions().stream().map(String::valueOf).toList(),
                    "the parse must not drop a compilation unit");

            TypeInfo marker = javaInspector.compiledTypesManager().get("a.b.Marker", setA);
            assertNotNull(marker, "a.b.Marker must be in the CST");
            assertDistinct(marker.interfacesImplemented(), "a.b.Marker's interfaces");
            assertEquals(List.of("java.lang.annotation.Annotation"), fqns(marker.interfacesImplemented()));
            // the silent half: the type's own @Target used to be appended a second time, and nothing asserts on it
            List<String> markerAnnotations = marker.annotations().stream()
                    .map(AnnotationExpression::typeInfo).map(TypeInfo::fullyQualifiedName).sorted().toList();
            assertEquals(List.of("java.lang.annotation.Target"), markerAnnotations,
                    "@Target must be present exactly once");

            TypeInfo impl = javaInspector.compiledTypesManager().get("a.b.Impl", setA);
            assertNotNull(impl, "a.b.Impl must be in the CST");
            assertDistinct(impl.interfacesImplemented(), "a.b.Impl's interfaces");
            assertEquals(List.of("a.b.Iface"), fqns(impl.interfacesImplemented()));
        } finally {
            deleteRecursively(tmp);
        }
    }

    private static List<String> fqns(List<ParameterizedType> types) {
        return types.stream().map(p -> p.typeInfo().fullyQualifiedName()).sorted().toList();
    }

    private static void assertDistinct(List<ParameterizedType> types, String what) {
        assertEquals(fqns(types).stream().distinct().toList(), fqns(types), what + " must not be duplicated");
    }

    private static void deleteRecursively(Path tmp) throws IOException {
        try (Stream<Path> walk = Files.walk(tmp)) {
            walk.sorted((x, y) -> y.getNameCount() - x.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
