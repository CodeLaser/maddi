package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The class-file dependency between source sets, and the two answers to it.
 * <p>
 * javac resolves a dependent source set's references through the <em>class files</em> of the set it depends on (see
 * {@code JavaInspectorImpl.createTask}), so a class output that was never built, cleaned away, or left one edit
 * behind silently costs us the compilation units that reference it. Here: that it is now reported (always), and that
 * pointing the inspector at a generated-classes directory removes the dependency altogether.
 */
public class TestGeneratedClassOutput {

    private static final String BASE_FQN = "a.b.Base";
    private static final String USER_FQN = "c.d.User";

    @Language("java")
    private static final String BASE = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
            }
            """;

    // Base with a member that the class file compiled from the ORIGINAL Base does not have
    @Language("java")
    private static final String BASE_V2 = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
                public String greet() { return "hello"; }
            }
            """;

    @Language("java")
    private static final String USER = """
            package c.d;
            import a.b.Base;
            public class User {
                public String use(Base base) { return base.name(); }
            }
            """;

    // only compiles against BASE_V2
    @Language("java")
    private static final String USER_V2 = """
            package c.d;
            import a.b.Base;
            public class User {
                public String use(Base base) { return base.greet(); }
            }
            """;

    @TempDir
    Path root;

    private Path mainSrc;
    private Path mainClasses;
    private SourceSet main;
    private SourceSet dependent;
    private JavaInspector inspector; // the one the last parse(...) used, for post-parse probing

    private void setup(String base, String user) throws IOException {
        mainSrc = Files.createDirectories(root.resolve("main-src/a/b"));
        Files.writeString(mainSrc.resolve("Base.java"), base);
        Path depSrc = Files.createDirectories(root.resolve("dep-src/c/d"));
        Files.writeString(depSrc.resolve("User.java"), user);
        mainClasses = Files.createDirectories(root.resolve("main-classes"));

        main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(mainClasses.toUri())
                .build();
        dependent = new SourceSetImpl.Builder().setName("dependent")
                .setSourceDirectories(List.of(root.resolve("dep-src")))
                .setUri(root.resolve("dep-classes").toUri())
                .setDependencies(List.of(main))
                .build();
    }

    /** @param generatedClasses when not null, the directory maddi compiles the source sets into itself */
    private Summary parse(Path generatedClasses) throws IOException {
        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        inspector = javaInspector;
        javaInspector.setGeneratedClassesDirectory(generatedClasses);
        javaInspector.initialize(new InputConfigurationImpl.Builder()
                .addSourceSets(main, dependent)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build());
        // accumulate rather than fail fast: a source set that does not resolve must degrade, which is exactly the
        // silent behaviour under test
        return javaInspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder().setFailFast(false).build());
    }

    private static void compile(List<Path> files, Path outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(null, fm, null, List.of(), null, units).call(),
                    "could not compile the main source set");
        }
    }

    /** The warnings that name the 'main' source set's class output. */
    private static List<String> classOutputWarnings(Summary summary) {
        return summary.parseWarnings().stream().map(Throwable::getMessage)
                .filter(m -> m != null && m.contains("resolves its references into 'main'"))
                .toList();
    }

    /**
     * Whether the parse produced this type. Read off the {@link Summary} rather than through {@code parseResult()}:
     * a stale class file can make a drop a hard <em>error</em> ("Inspection of a.b.Base has already been committed",
     * javac having loaded from the class file a type we already committed from source), and {@code parseResult()}
     * refuses to hand out a result once there are parse errors.
     */
    private static boolean holds(Summary summary, String fqn) {
        return summary.types().stream().anyMatch(t -> fqn.equals(t.fullyQualifiedName()));
    }

    private static boolean generated(Path generatedClasses, String fqn) throws IOException {
        if (!Files.isDirectory(generatedClasses)) return false;
        String tail = fqn.replace('.', '/') + ".class";
        try (Stream<Path> walk = Files.walk(generatedClasses)) {
            return walk.anyMatch(p -> p.toString().replace('\\', '/').endsWith(tail));
        }
    }

    @DisplayName("class output never built: reported, and the dependent type is lost")
    @Test
    public void testMissingClassOutputIsReported() throws IOException {
        setup(BASE, USER);
        // mainClasses exists but is empty: 'main' was parsed from source, never compiled

        Summary summary = parse(null);

        List<String> warnings = classOutputWarnings(summary);
        assertEquals(1, warnings.size(), "expected exactly one class-output warning, got " + warnings);
        assertTrue(warnings.getFirst().contains("neither a class file nor a source file"), warnings.getFirst());
        assertTrue(warnings.getFirst().contains(BASE_FQN), warnings.getFirst());

        assertTrue(holds(summary, BASE_FQN), "main is parsed from source, so Base is there");
        assertFalse(holds(summary, USER_FQN), "User cannot resolve Base, so it is dropped");
    }

    @DisplayName("class output never built, generation on: no warning, and the dependent type resolves")
    @Test
    public void testGenerationReplacesTheMissingClassOutput() throws IOException {
        setup(BASE, USER);
        Path generatedClasses = root.resolve("generated");

        Summary summary = parse(generatedClasses);

        assertEquals(List.of(), classOutputWarnings(summary), "nothing to warn about: we compiled 'main' ourselves");
        assertTrue(holds(summary, USER_FQN), "User resolves against the class files we generated");
        assertTrue(generated(generatedClasses, BASE_FQN), "Base.class must be under " + generatedClasses);
        assertTrue(generated(generatedClasses, USER_FQN), "the dependent source set is generated too");
        assertEquals(List.of(), List.of(mainClasses.toFile().list()), "the build's own class output is untouched");
    }

    @DisplayName("a source directory as the class-path entry resolves implicitly, and is not reported")
    @Test
    public void testSourceDirectoryOnTheClassPathIsNotReported() throws IOException {
        setup(BASE, USER);
        // what both build plugins emit: a source set's uri() is its first SOURCE directory, never a class output.
        // javac's class path doubles as its source path, so it compiles a.b.Base implicitly and 'dependent'
        // resolves — at the price of parsing main's sources again, once per dependent set. Nothing to report.
        main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(root.resolve("main-src").toUri())
                .build();
        dependent = new SourceSetImpl.Builder().setName("dependent")
                .setSourceDirectories(List.of(root.resolve("dep-src")))
                .setUri(root.resolve("dep-classes").toUri())
                .setDependencies(List.of(main))
                .build();

        Summary summary = parse(null);

        assertEquals(List.of(), classOutputWarnings(summary));
        assertTrue(holds(summary, USER_FQN), "javac compiled a.b.Base implicitly from the class path");
    }

    @DisplayName("class output one edit behind: reported as stale, and the new member is lost")
    @Test
    public void testStaleClassOutputIsReported() throws IOException {
        setup(BASE, USER_V2);
        // compile Base as it was, then move the source on: the class file no longer has what User calls
        compile(List.of(mainSrc.resolve("Base.java")), mainClasses);
        Files.writeString(mainSrc.resolve("Base.java"), BASE_V2);
        Path classFile = mainClasses.resolve("a/b/Base.class");
        assertTrue(Files.exists(classFile));
        Files.setLastModifiedTime(mainSrc.resolve("Base.java"),
                FileTime.fromMillis(Files.getLastModifiedTime(classFile).toMillis() + 10_000));

        Summary summary = parse(null);

        List<String> warnings = classOutputWarnings(summary);
        assertEquals(1, warnings.size(), "expected exactly one class-output warning, got " + warnings);
        assertTrue(warnings.getFirst().contains("older than their source"), warnings.getFirst());
        assertFalse(holds(summary, USER_FQN), "User calls greet(), which the stale class file lacks");
    }

    @DisplayName("class output one edit behind, generation on: the dependent resolves against the current source")
    @Test
    public void testGenerationSupersedesTheStaleClassOutput() throws IOException {
        setup(BASE, USER_V2);
        compile(List.of(mainSrc.resolve("Base.java")), mainClasses);
        Files.writeString(mainSrc.resolve("Base.java"), BASE_V2);
        Path generatedClasses = root.resolve("generated");

        Summary summary = parse(generatedClasses);

        assertEquals(List.of(), classOutputWarnings(summary));
        assertTrue(holds(summary, USER_FQN), "greet() is in the class file we generated");
    }

    /**
     * The assertion the first round of these tests was missing. {@code generate()} tears the javac task's compiler
     * context down, and the inspector retains the most recent scan's task to serve
     * {@code CompiledTypesManager.getOrLoad} — the on-demand library load that
     * {@code maddi-inspection-openjdk/DESIGN-drop-javac-ast.md} §3 shows the analysis depends on. Switching
     * generation on therefore broke it with an {@code IllegalStateException} out of {@code getElements()}, exactly as
     * {@code docs/partial-reparse-rewire.md} §7.1 predicted. A source-free replacement task now serves those loads.
     */
    @DisplayName("compiled-type loading still works after generation destroyed the scan's task")
    @Test
    public void testGetOrLoadSurvivesGeneration() throws IOException {
        setup(BASE, USER);
        Summary summary = parse(root.resolve("generated"));
        assertTrue(holds(summary, USER_FQN));

        // a JDK type no preload touched, so it can only come through the lazy loader
        for (String fqn : List.of("java.util.StringJoiner", "java.util.BitSet")) {
            TypeInfo loaded = inspector.compiledTypesManager().type(fqn, null);
            assertNotNull(loaded, fqn + " must load through the source-free loader task");
            assertEquals(fqn, loaded.fullyQualifiedName());
            assertTrue(loaded.hasBeenInspected(), fqn + " must be complete, not a stub");
        }
        // and a nested type, which the loader resolves through its top-level enclosing type
        TypeInfo entry = inspector.compiledTypesManager().type("java.util.Map.Entry", null);
        assertNotNull(entry, "a nested type must load too");
    }

    @DisplayName("a type deleted between two parses does not linger in the generated directory")
    @Test
    public void testGeneratedDirectoryIsWipedPerScan() throws IOException {
        setup(BASE, USER);
        Path generatedClasses = root.resolve("generated");
        Summary first = parse(generatedClasses);
        assertTrue(holds(first, BASE_FQN));
        assertTrue(generated(generatedClasses, BASE_FQN));

        // a second, independent inspector over the same directories, with Base renamed away
        Files.delete(mainSrc.resolve("Base.java"));
        Files.writeString(mainSrc.resolve("Renamed.java"), BASE.replace("Base", "Renamed"));
        Files.writeString(root.resolve("dep-src/c/d/User.java"),
                USER.replace("Base", "Renamed").replace("Renamed base", "Renamed renamed")
                        .replace("base.name()", "renamed.name()"));

        Summary second = parse(generatedClasses);
        assertTrue(holds(second, "a.b.Renamed"));
        assertFalse(generated(generatedClasses, BASE_FQN), "the class file of the deleted type must be gone");
    }
}
