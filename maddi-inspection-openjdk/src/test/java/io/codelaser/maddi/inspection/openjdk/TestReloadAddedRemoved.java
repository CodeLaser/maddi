package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.run.rewire.ReparseOracle;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Files added and removed between two parses: {@link JavaInspector#reloadSources} reports them
 * ({@link JavaInspector.ReloadResult#added()}, {@link JavaInspector.ReloadResult#removed()}), and the next re-parse
 * scans the new files and forgets the types of the removed ones. The caller makes the changed types and the users of
 * a removed type INVALID. Every case is compared with a cold parse of the same files ({@link ReparseOracle}).
 */
public class TestReloadAddedRemoved {

    @Language("java")
    private static final String BASE = """
            package a;
            public class Base {
                public String name() { return "base"; }
            }
            """;
    @Language("java")
    private static final String HELPER = """
            package a;
            public class Helper {
                public int help() { return 42; }
            }
            """;
    @Language("java")
    private static final String USER = """
            package a;
            public class User {
                public int use(Helper h) { return h.help(); }
            }
            """;
    @Language("java")
    private static final String USER_OF_MOVED = """
            package a;
            import b.Helper;
            public class User {
                public int use(Helper h) { return h.help(); }
            }
            """;
    @Language("java")
    private static final String HELPER_MOVED = """
            package b;
            public class Helper {
                public int help() { return 42; }
            }
            """;
    @Language("java")
    private static final String EXTRA = """
            package a;
            public class Extra {
                public int extra(Base base) { return base.name().length(); }
            }
            """;
    @Language("java")
    private static final String DEP_USER = """
            package c;
            import a.Helper;
            public class DepUser {
                public int twice(Helper h) { return 2 * h.help(); }
            }
            """;

    @TempDir
    Path root;

    @DisplayName("a new file in a set with no other change: scanned, everything else kept")
    @Test
    public void newFile() throws IOException {
        Path src = root.resolve("src");
        write(src, "a/Base.java", BASE);
        write(src, "a/Helper.java", HELPER);
        SourceSet main = set("main", src, null);
        InputConfiguration configuration = configuration(main);
        JavaInspector inspector = inspector(configuration);
        ParseResult pr1 = parse(inspector);
        TypeInfo base1 = pr1.findType("a.Base");

        write(src, "a/Extra.java", EXTRA);
        JavaInspector.ReloadResult rr = inspector.reloadSources(configuration, Map.of());
        assertEquals(Set.of(), rr.sourceHasChanged());
        assertEquals(Set.of("a/Extra.java"), rr.added().stream().map(sf -> relative(src, sf.uri().getPath()))
                .collect(Collectors.toSet()));
        assertEquals(Set.of(), rr.removed());
        ParseResult pr2 = reparse(inspector, rr, Map.of());

        TypeInfo extra = pr2.findType("a.Extra");
        assertNotNull(extra, "the new file is parsed");
        assertSame(base1, pr2.findType("a.Base"), "Base is kept");
        assertSame(base1, extra.findUniqueMethod("extra", 1).parameters().getFirst().parameterizedType().typeInfo(),
                "Extra resolves the kept Base");
        assertAsCold(inspector, pr2, main);
    }

    @DisplayName("a type moved to another package of its set: the old one is gone, its user resolves the new one")
    @Test
    public void movedWithinSet() throws IOException {
        Path src = root.resolve("src");
        write(src, "a/Base.java", BASE);
        write(src, "a/Helper.java", HELPER);
        write(src, "a/User.java", USER);
        SourceSet main = set("main", src, null);
        InputConfiguration configuration = configuration(main);
        JavaInspector inspector = inspector(configuration);
        ParseResult pr1 = parse(inspector);
        TypeInfo helper1 = pr1.findType("a.Helper");

        Files.delete(src.resolve("a/Helper.java"));
        write(src, "b/Helper.java", HELPER_MOVED);
        write(src, "a/User.java", USER_OF_MOVED);
        JavaInspector.ReloadResult rr = inspector.reloadSources(configuration, Map.of());
        assertEquals(Set.of("a.User"), names(rr.sourceHasChanged()));
        assertEquals(Set.of(helper1), rr.removed());
        assertEquals(1, rr.added().size());
        ParseResult pr2 = reparse(inspector, rr, Map.of());

        assertNull(pr2.findType("a.Helper"), "the moved type is gone from the result");
        assertNull(inspector.compiledTypesManager().get("a.Helper", null), "and from the registry");
        TypeInfo helper2 = pr2.findType("b.Helper");
        assertNotNull(helper2);
        assertSame(helper2, pr2.findType("a.User").findUniqueMethod("use", 1).parameters().getFirst()
                .parameterizedType().typeInfo());
        assertAsCold(inspector, pr2, main);
    }

    @DisplayName("a type moved into the set that depends on its own, under the same name: its user there finds it")
    @Test
    public void movedIntoAnotherSet() throws IOException {
        Path mainSrc = root.resolve("main-src");
        write(mainSrc, "a/Base.java", BASE);
        write(mainSrc, "a/Helper.java", HELPER);
        Path depSrc = root.resolve("dep-src");
        write(depSrc, "c/DepUser.java", DEP_USER);
        Path mainClasses = compile(List.of(mainSrc.resolve("a/Base.java"), mainSrc.resolve("a/Helper.java")),
                root.resolve("main-classes"));
        SourceSet main = set("main", mainSrc, mainClasses);
        SourceSet dep = set("dep", depSrc, null, main);
        InputConfiguration configuration = configuration(main, dep);
        JavaInspector inspector = inspector(configuration);
        ParseResult pr1 = parse(inspector);
        TypeInfo base1 = pr1.findType("a.Base");

        // the move, and the build that follows it: main's classes no longer hold Helper
        Files.delete(mainSrc.resolve("a/Helper.java"));
        write(depSrc, "a/Helper.java", HELPER);
        deleteRecursively(mainClasses);
        compile(List.of(mainSrc.resolve("a/Base.java")), mainClasses);
        JavaInspector.ReloadResult rr = inspector.reloadSources(configuration, Map.of());
        assertEquals(Set.of("a.Helper"), names(rr.removed()));
        assertEquals(1, rr.added().size());
        assertEquals(Set.of(), rr.sourceHasChanged());
        // the user of the removed type is INVALID: it must resolve the name again
        ParseResult pr2 = reparse(inspector, rr, Map.of("c.DepUser", INVALID));

        TypeInfo helper2 = pr2.findType("a.Helper");
        assertNotNull(helper2);
        assertEquals("dep", helper2.compilationUnit().sourceSet().name(), "Helper now lives in dep");
        assertSame(helper2, pr2.findType("c.DepUser").findUniqueMethod("twice", 1).parameters().getFirst()
                .parameterizedType().typeInfo());
        assertSame(base1, pr2.findType("a.Base"), "Base is kept");
        assertAsCold(inspector, pr2, main, dep);
    }

    @DisplayName("a file deleted while its type has users in its set and another: they fail as in a cold parse")
    @Test
    public void deletedWithUsers() throws IOException {
        Path mainSrc = root.resolve("main-src");
        write(mainSrc, "a/Base.java", BASE);
        write(mainSrc, "a/Helper.java", HELPER);
        write(mainSrc, "a/User.java", USER);
        Path depSrc = root.resolve("dep-src");
        write(depSrc, "c/DepUser.java", DEP_USER);
        Path mainClasses = compile(List.of(mainSrc.resolve("a/Base.java"), mainSrc.resolve("a/Helper.java"),
                mainSrc.resolve("a/User.java")), root.resolve("main-classes"));
        SourceSet main = set("main", mainSrc, mainClasses);
        SourceSet dep = set("dep", depSrc, null, main);
        InputConfiguration configuration = configuration(main, dep);
        JavaInspector inspector = inspector(configuration);
        parse(inspector);

        Files.delete(mainSrc.resolve("a/Helper.java"));
        deleteRecursively(mainClasses);
        compile(List.of(mainSrc.resolve("a/Base.java")), mainClasses);
        JavaInspector.ReloadResult rr = inspector.reloadSources(configuration, Map.of());
        assertEquals(Set.of("a.Helper"), names(rr.removed()));
        Summary warm = reparseSummary(inspector, rr, Map.of("a.User", INVALID, "c.DepUser", INVALID));
        JavaInspector coldInspector = inspector(configuration);
        Summary cold = coldInspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(false).build());
        // the name no longer finds the removed source type; what it finds is what a cold parse registers for a name
        // that does not resolve, a stub
        TypeInfo warmHelper = inspector.compiledTypesManager().get("a.Helper", null);
        TypeInfo coldHelper = coldInspector.compiledTypesManager().get("a.Helper", null);
        assertEquals(coldHelper == null, warmHelper == null);
        if (warmHelper != null) {
            assertNull(warmHelper.compilationUnit().sourceSet(), "not a source type: " + warmHelper.compilationUnit());
            assertNull(coldHelper.compilationUnit().sourceSet());
        }

        // not fail-fast, so javac's errors come as warnings, and the run carries on over them
        assertFalse(errorFiles(warm).isEmpty(), "the users of a removed type do not resolve it");
        assertEquals(errorFiles(cold), errorFiles(warm), "the same files fail as in a cold parse");
        ParseResult pr2 = warm.parseResultIgnoringErrors();
        assertNull(pr2.findType("a.Helper"));
        assertEquals(names(cold.parseResultIgnoringErrors().primaryTypes()), names(pr2.primaryTypes()));
    }

    // ---- helpers

    private static Set<String> names(java.util.Collection<TypeInfo> types) {
        return types.stream().map(TypeInfo::fullyQualifiedName).collect(Collectors.toSet());
    }

    private static Set<String> errorFiles(Summary summary) {
        return java.util.stream.Stream.concat(summary.parseExceptions().stream(), summary.parseWarnings().stream())
                .map(pe -> pe.uri() == null ? "?" : Path.of(pe.uri().getPath())
                .getFileName().toString()).collect(Collectors.toSet());
    }

    private static String relative(Path root, String path) {
        return root.toAbsolutePath().normalize().relativize(Path.of(path).toAbsolutePath().normalize()).toString();
    }

    private static void assertAsCold(JavaInspector warm, ParseResult warmResult, SourceSet... sourceSets) {
        List<String> stale = ReparseOracle.staleReferences(warmResult);
        assertTrue(stale.isEmpty(), "stale references: " + stale);
        JavaInspector cold = inspector(configuration(sourceSets));
        ParseResult coldResult = parse(cold);
        List<String> differences = ReparseOracle.differences(warm, warmResult, null, cold, coldResult, null);
        assertTrue(differences.isEmpty(), String.join("\n", differences));
    }

    private static SourceSet set(String name, Path src, Path classes, SourceSet... dependencies) {
        return new SourceSetImpl.Builder().setName(name)
                .setSourceDirectories(List.of(src))
                .setUri((classes == null ? src : classes).toUri())
                .setDependencies(List.of(dependencies))
                .build();
    }

    private static InputConfiguration configuration(SourceSet... sourceSets) {
        return new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSets)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
    }

    private static JavaInspector inspector(InputConfiguration configuration) {
        try {
            JavaInspector inspector = new JavaInspectorImpl(true, false);
            inspector.initialize(configuration);
            return inspector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ParseResult parse(JavaInspector inspector) {
        return inspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    /* the reload's changed types INVALID, as are the types named; the rest UNCHANGED */
    private static ParseResult reparse(JavaInspector inspector, JavaInspector.ReloadResult rr,
                                       Map<String, JavaInspector.InvalidationState> states) {
        Summary summary = reparseSummary(inspector, rr, states);
        assertFalse(summary.haveErrors(), () -> "unexpected parse errors: " + summary.parseExceptions());
        return summary.parseResult();
    }

    private static Summary reparseSummary(JavaInspector inspector, JavaInspector.ReloadResult rr,
                                          Map<String, JavaInspector.InvalidationState> states) {
        JavaInspector.Invalidated invalidated = ti -> rr.sourceHasChanged().contains(ti) ? INVALID
                : states.getOrDefault(ti.fullyQualifiedName(), UNCHANGED);
        return inspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(false).setInvalidated(invalidated).build());
    }

    private static void write(Path sourceRoot, String relative, String content) throws IOException {
        Path file = sourceRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private static Path compile(List<Path> files, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(null, fm, null, List.of(), null, units).call(), "could not compile " + files);
        }
        return outputDir;
    }
}
