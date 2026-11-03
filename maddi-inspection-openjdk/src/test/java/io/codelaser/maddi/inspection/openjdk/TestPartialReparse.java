package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
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

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The per-file re-parse within a source set ({@code JavaInspectorImpl.reparse}, {@code SourceSetAction.PARTIAL}):
 * javac is handed the changed files, the rest of the set sits on its source path, unchanged types are kept or
 * rewired. See {@code docs/design/incremental-and-overlay.md} §5.
 * <p>
 * Every case ends with {@link ReparseOracle}, the corpus driver's oracle (§8 of that document): no stale reference
 * anywhere in the result, and the same printed compilation units and the same variables read as a cold parse of the
 * same files in a fresh inspector.
 */
public class TestPartialReparse {

    @Language("java")
    private static final String BASE = """
            package a;
            public class Base {
                public String name() { return "base"; }
            }
            """;

    @Language("java")
    private static final String BASE_EDITED = """
            package a;
            public class Base {
                public String name() { return "base" + suffix(); }
                public String suffix() { return "!"; }
            }
            """;

    // independent of Base
    @Language("java")
    private static final String HELPER = """
            package a;
            public class Helper {
                public int help() { return 42; }
            }
            """;

    // uses Base; Base does not use it
    @Language("java")
    private static final String USER = """
            package a;
            public class User {
                private final Base base = new Base();
                public String use() { return base.name(); }
            }
            """;

    // Left and Right refer to each other
    @Language("java")
    private static final String LEFT = """
            package b;
            public class Left {
                public Right right() { return new Right(); }
                public int value() { return 1; }
            }
            """;

    @Language("java")
    private static final String LEFT_EDITED = """
            package b;
            public class Left {
                public Right right() { return new Right(); }
                public int value() { return 2; }
            }
            """;

    @Language("java")
    private static final String RIGHT = """
            package b;
            public class Right {
                public int leftValue(Left left) { return left.value(); }
            }
            """;

    @TempDir
    Path root;

    // ---- one source set

    private Path src;
    private SourceSet main;

    private void oneSourceSet() throws IOException {
        src = root.resolve("src");
        write(src, "a/Base.java", BASE);
        write(src, "a/Helper.java", HELPER);
        write(src, "a/User.java", USER);
        write(src, "b/Left.java", LEFT);
        write(src, "b/Right.java", RIGHT);
        main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(src))
                .setUri(src.toUri())
                .build();
    }

    @DisplayName("Base INVALID: Helper is kept, User (same set) is rewired onto the new Base")
    @Test
    public void testChangedFileOnly() throws IOException {
        oneSourceSet();
        JavaInspector inspector = inspector(main);
        ParseResult pr1 = parse(inspector);
        TypeInfo base1 = pr1.findType("a.Base");
        TypeInfo helper1 = pr1.findType("a.Helper");
        TypeInfo user1 = pr1.findType("a.User");
        TypeInfo left1 = pr1.findType("b.Left");

        write(src, "a/Base.java", BASE_EDITED);
        ParseResult pr2 = reparse(inspector, Map.of("a.Base", INVALID, "a.User", REWIRE));

        TypeInfo base2 = pr2.findType("a.Base");
        assertNotSame(base1, base2);
        assertNotSame(base1.compilationUnit(), base2.compilationUnit(), "re-parsed: a new compilation unit");
        assertNotNull(base2.findUniqueMethod("suffix", 0), "the edit is in");

        assertSame(helper1, pr2.findType("a.Helper"), "Helper shares Base's set, but it is unchanged: kept");
        assertSame(left1, pr2.findType("b.Left"), "kept");

        TypeInfo user2 = pr2.findType("a.User");
        assertNotSame(user1, user2, "User refers to Base: rewired");
        assertSame(user1.compilationUnit(), user2.compilationUnit(), "rewired, not re-parsed");

        assertEquals(5, pr2.primaryTypes().size());
        assertNoStaleReferences(pr2);
        assertPrintsAsCold(inspector, pr2, main);
    }

    /*
     The case a same-set rewire must survive: Left changed and is scanned while Right, which refers back to it, still
     has its old object. Left's scanned object therefore points at the old Right; Right is rewired afterwards onto
     the new Left. Unless Left goes through the rewire too, the two end up pointing past each other.
     */
    @DisplayName("Left INVALID, Right REWIRE, referring to each other: both point at each other's live object")
    @Test
    public void testMutualReference() throws IOException {
        oneSourceSet();
        JavaInspector inspector = inspector(main);
        ParseResult pr1 = parse(inspector);
        TypeInfo helper1 = pr1.findType("a.Helper");

        write(src, "b/Left.java", LEFT_EDITED);
        ParseResult pr2 = reparse(inspector, Map.of("b.Left", INVALID, "b.Right", REWIRE));

        TypeInfo left2 = pr2.findType("b.Left");
        TypeInfo right2 = pr2.findType("b.Right");
        assertTrue(left2.findUniqueMethod("value", 0).methodBody().toString().contains("2"), "the edit is in");
        assertSame(right2, left2.findUniqueMethod("right", 0).returnType().typeInfo(),
                "Left must reach the live Right");
        assertSame(left2, right2.findUniqueMethod("leftValue", 1).parameters().getFirst().parameterizedType()
                .typeInfo(), "Right must reach the live Left");
        assertSame(helper1, pr2.findType("a.Helper"), "kept");
        assertSame(left2, inspector.compiledTypesManager().get("b.Left", main), "the registry hands out the live Left");

        assertNoStaleReferences(pr2);
        assertPrintsAsCold(inspector, pr2, main);
    }

    @DisplayName("two partial re-parses in a row: the second starts from the first one's objects")
    @Test
    public void testTwoInARow() throws IOException {
        oneSourceSet();
        JavaInspector inspector = inspector(main);
        parse(inspector);

        write(src, "a/Base.java", BASE_EDITED);
        reparse(inspector, Map.of("a.Base", INVALID, "a.User", REWIRE));
        write(src, "b/Left.java", LEFT_EDITED);
        ParseResult pr3 = reparse(inspector, Map.of("b.Left", INVALID, "b.Right", REWIRE));

        assertNotNull(pr3.findType("a.Base").findUniqueMethod("suffix", 0), "the first edit stays in");
        assertNoStaleReferences(pr3);
        assertPrintsAsCold(inspector, pr3, main);
    }

    @DisplayName("every type INVALID: the set is re-scanned in full, as before")
    @Test
    public void testAllInvalidRescans() throws IOException {
        oneSourceSet();
        JavaInspector inspector = inspector(main);
        ParseResult pr1 = parse(inspector);

        ParseResult pr2 = reparse(inspector, Map.of(), _ -> INVALID);
        for (TypeInfo t1 : pr1.primaryTypes()) {
            assertNotSame(t1, pr2.findType(t1.fullyQualifiedName()));
        }
        assertNoStaleReferences(pr2);
    }

    // ---- what a rewire must carry besides code: comments, javadoc and its links, annotations, type-use annotations

    @Language("java")
    private static final String MARKER = """
            package c;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
            public @interface Marker {
            }
            """;

    @Language("java")
    private static final String DOC = """
            package c;
            /** Uses {@link Other}; see {@link Doc#run()}. */
            @Marker
            public class Doc {
                // a field comment
                @Marker
                private final Other other = new Other();

                /** Runs {@link Other#value()}. */
                // a method comment
                @Marker
                public int run() { return other.value(); }

                public @Marker String typeUse(@Marker String s) { return s; }
            }
            """;

    @Language("java")
    private static final String OTHER = """
            package c;
            /** {@link Doc} uses me. */
            public class Other {
                public int value() { return 1; }
            }
            """;

    /*
     Doc is rewired on every edit of Other, so a rewire that adds a declaration's comments or annotations a second
     time, drops its javadoc, or leaves a link on the previous generation shows up after a few rounds, and a leak
     with it: each generation reachable from the next.
     */
    @DisplayName("comments, javadoc links and annotations survive repeated rewires, once and pointing at live objects")
    @Test
    public void testDeclarationsSurviveRepeatedRewires() throws IOException {
        Path cSrc = root.resolve("c-src");
        write(cSrc, "c/Marker.java", MARKER);
        write(cSrc, "c/Doc.java", DOC);
        write(cSrc, "c/Other.java", OTHER);
        SourceSet set = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(cSrc)).setUri(cSrc.toUri()).build();
        JavaInspector inspector = inspector(set);
        parse(inspector);
        ParseResult pr = null;
        for (int round = 1; round <= 3; round++) {
            write(cSrc, "c/Other.java", "// round " + round + "\n" + OTHER);
            pr = reparse(inspector, Map.of("c.Other", INVALID, "c.Doc", REWIRE));
            assertNoStaleReferences(pr);
            assertPrintsAsCold(inspector, pr, set);
        }
        TypeInfo doc = pr.findType("c.Doc");
        assertNotNull(doc.javaDoc(), "a rewired type keeps its javadoc");
        assertEquals(1, doc.findUniqueMethod("run", 0).comments().stream()
                .filter(c -> c.comment().contains("a method comment")).count(), "once, after three rewires");
    }

    // ---- type-use annotations whose annotation type comes from a class file

    @Language("java")
    private static final String LIB_NOT_NULL = """
            package lib;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.CLASS)
            @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
            public @interface NotNull {
            }
            """;

    @Language("java")
    private static final String TYPE_USE_USER = """
            package u;
            import lib.NotNull;
            public class User {
                @NotNull private final String id = "x";
                public @NotNull String id(@NotNull String s) { return s; }
            }
            """;

    @Language("java")
    private static final String TYPE_USE_EARLY = """
            package u;
            public class Early {
                public int length() { return new User().id.length() + new User().id("y").length(); }
            }
            """;

    /*
     fernflower: org.jetbrains.annotations.NotNull applies to a declaration and to a type, and comes from a jar. A
     member built from javac's symbol, because a unit scanned earlier referred to it, had it on its type as well;
     one built from the source tree had it on the declaration only. A cold parse scans Early first and builds User's
     members from symbols; a per-file re-parse of User scans User alone, from the tree. Both must give one answer:
     the declaration's, as the tree path always has (a type copy printed '@NotNull public final @NotNull String').
     */
    @DisplayName("a declaration-and-type-use annotation sits on the declaration, whichever path built the member")
    @Test
    public void testTypeUseAnnotationFromClassFile() throws IOException {
        Path libSrc = root.resolve("lib-src");
        write(libSrc, "lib/NotNull.java", LIB_NOT_NULL);
        Path libClasses = compile(List.of(libSrc.resolve("lib/NotNull.java")), root.resolve("lib-classes"), List.of());
        SourceSet lib = new SourceSetImpl.Builder().setName("lib").setUri(libClasses.toUri())
                .setExternalLibrary(true).build();
        Path uSrc = root.resolve("u-src");
        write(uSrc, "u/Early.java", TYPE_USE_EARLY);
        write(uSrc, "u/User.java", TYPE_USE_USER);
        SourceSet set = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(uSrc))
                .setUri(uSrc.toUri()).setDependencies(List.of(lib)).build();
        JavaInspector inspector = inspector(List.of(lib), set);
        assertDeclarationOnly(parse(inspector).findType("u.User"), "cold");

        write(uSrc, "u/User.java", "// edited\n" + TYPE_USE_USER);
        ParseResult pr = reparse(inspector, Map.of("u.User", INVALID, "u.Early", REWIRE));
        assertDeclarationOnly(pr.findType("u.User"), "re-parsed");
        assertNoStaleReferences(pr);
        JavaInspector cold = inspector(List.of(lib), set);
        List<String> differences = ReparseOracle.differences(inspector, pr, null, cold, parse(cold), null);
        assertTrue(differences.isEmpty(), String.join("\n", differences));
    }

    private static void assertDeclarationOnly(TypeInfo user, String world) {
        var field = user.getFieldByName("id", true);
        assertEquals(1, field.annotations().size(), world + ": the field's declaration annotation");
        assertEquals(0, field.type().annotations().size(), world + ": not on the field's type as well");
        var method = user.findUniqueMethod("id", 1);
        assertEquals(1, method.annotations().size(), world + ": the method's declaration annotation");
        assertEquals(0, method.returnType().annotations().size(), world + ": not on the return type as well");
        assertEquals(0, method.parameters().getFirst().parameterizedType().annotations().size(),
                world + ": not on the parameter's type as well");
    }

    // ---- class literals

    @Language("java")
    private static final String LITERALS = """
            package k;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @interface Uses { Class<?> value(); }
            @Uses(Target.class)
            public class Literals {
                private final Class<?> target = Target.class;
                @Uses(Literals.class)
                public Class<?> self() { return Literals.class; }
                public String name() { return Target.class.getName() + target; }
            }
            """;

    @Language("java")
    private static final String LITERAL_TARGET = """
            package k;
            public class Target {
            }
            """;

    /*
     fernflower: 'EdgeType.class' in a field initializer, 'Fernflower.class' in Fernflower's own method,
     '@ArgumentsSource(Provider.class)', '@MagicConstant(valuesFromClass = TypeAnnotation.class)'. A class literal
     is a constant, and a constant's rewire returned itself: the literal kept the type it was parsed against.
     */
    @DisplayName("class literals, in code and in annotations, follow the rewire")
    @Test
    public void testClassLiteralsAreRewired() throws IOException {
        Path kSrc = root.resolve("k-src");
        write(kSrc, "k/Literals.java", LITERALS);
        write(kSrc, "k/Target.java", LITERAL_TARGET);
        SourceSet set = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(kSrc))
                .setUri(kSrc.toUri()).build();
        JavaInspector inspector = inspector(set);
        parse(inspector);
        for (int round = 1; round <= 2; round++) {
            write(kSrc, "k/Target.java", "// round " + round + "\n" + LITERAL_TARGET);
            ParseResult pr = reparse(inspector, Map.of("k.Target", INVALID, "k.Literals", REWIRE));
            assertNoStaleReferences(pr);
            assertPrintsAsCold(inspector, pr, set);
        }
    }

    // ---- three source sets: main <- mid <- top

    @Language("java")
    private static final String MID = """
            package m;
            import a.Base;
            public class Mid {
                public String fromBase(Base base) { return base.name(); }
            }
            """;

    @Language("java")
    private static final String TOP = """
            package t;
            import m.Mid;
            public class Top {
                public String top(Mid mid) { return mid.fromBase(new a.Base()); }
            }
            """;

    @Language("java")
    private static final String TOP_EDITED = """
            package t;
            import m.Mid;
            public class Top {
                public String top(Mid mid) { return "top: " + mid.fromBase(new a.Base()); }
            }
            """;

    /*
     Across sets, the same shape as testMutualReference: Top changed and is scanned while Mid, which it refers to and
     which is REWIRE because it refers to the changed Base, still has its old object. Before the per-set test in
     reparse, every re-scanned type was only seeded, and Top kept pointing at the replaced Mid.
     */
    @DisplayName("Base and Top INVALID, Mid (in between) REWIRE: Top reaches the rewired Mid")
    @Test
    public void testRescannedTypeReachesRewiredTypeOfAnotherSet() throws IOException {
        Path mainSrc = root.resolve("main-src");
        write(mainSrc, "a/Base.java", BASE);
        Path midSrc = root.resolve("mid-src");
        write(midSrc, "m/Mid.java", MID);
        Path topSrc = root.resolve("top-src");
        write(topSrc, "t/Top.java", TOP);
        Path mainClasses = compile(List.of(mainSrc.resolve("a/Base.java")), root.resolve("main-classes"), List.of());
        Path midClasses = compile(List.of(midSrc.resolve("m/Mid.java")), root.resolve("mid-classes"),
                List.of(mainClasses));

        SourceSet mainSet = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(mainSrc)).setUri(mainClasses.toUri()).build();
        SourceSet midSet = new SourceSetImpl.Builder().setName("mid")
                .setSourceDirectories(List.of(midSrc)).setUri(midClasses.toUri())
                .setDependencies(List.of(mainSet)).build();
        SourceSet topSet = new SourceSetImpl.Builder().setName("top")
                .setSourceDirectories(List.of(topSrc)).setUri(topSrc.toUri())
                .setDependencies(List.of(midSet, mainSet)).build();
        JavaInspector inspector = inspector(mainSet, midSet, topSet);
        parse(inspector);

        write(topSrc, "t/Top.java", TOP_EDITED);
        ParseResult pr2 = reparse(inspector, Map.of("a.Base", INVALID, "m.Mid", REWIRE, "t.Top", INVALID));

        TypeInfo mid2 = pr2.findType("m.Mid");
        TypeInfo top2 = pr2.findType("t.Top");
        assertSame(mid2, top2.findUniqueMethod("top", 1).parameters().getFirst().parameterizedType().typeInfo(),
                "Top must reach the rewired Mid, not the object it replaced");
        assertNoStaleReferences(pr2);
        assertPrintsAsCold(inspector, pr2, mainSet, midSet, topSet);
    }

    // ---- the oracle: ReparseOracle, the corpus driver's

    private static void assertNoStaleReferences(ParseResult parseResult) {
        List<String> stale = ReparseOracle.staleReferences(parseResult);
        assertTrue(stale.isEmpty(), "stale references: " + stale);
    }

    private static void assertPrintsAsCold(JavaInspector warm, ParseResult warmResult, SourceSet... sourceSets) {
        JavaInspector cold = inspector(sourceSets);
        ParseResult coldResult = parse(cold);
        List<String> differences = ReparseOracle.differences(warm, warmResult, null, cold, coldResult, null);
        assertTrue(differences.isEmpty(), String.join("\n", differences));
    }

    // ---- set-up

    private static JavaInspector inspector(SourceSet... sourceSets) {
        return inspector(List.of(), sourceSets);
    }

    private static JavaInspector inspector(List<SourceSet> classPathParts, SourceSet... sourceSets) {
        try {
            JavaInspector inspector = new JavaInspectorImpl(true, false);
            InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                    .addSourceSets(sourceSets)
                    .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                    .addClassPathParts(classPathParts)
                    .build();
            inspector.initialize(inputConfiguration);
            return inspector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ParseResult parse(JavaInspector inspector) {
        return inspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    /** Re-parse; a type not named is UNCHANGED. */
    private static ParseResult reparse(JavaInspector inspector, Map<String, JavaInspector.InvalidationState> states) {
        return reparse(inspector, states, ti -> states.getOrDefault(ti.fullyQualifiedName(), UNCHANGED));
    }

    private static ParseResult reparse(JavaInspector inspector, Map<String, JavaInspector.InvalidationState> ignored,
                                       JavaInspector.Invalidated invalidated) {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true)
                .setFailFast(true)
                .setInvalidated(invalidated)
                .build();
        return inspector.parse(Map.of(), options).parseResult();
    }

    private static void write(Path sourceRoot, String relative, String content) throws IOException {
        Path file = sourceRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static Path compile(List<Path> files, Path outputDir, List<Path> classPath) throws IOException {
        Files.createDirectories(outputDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            fm.setLocation(StandardLocation.CLASS_PATH, classPath.stream().map(Path::toFile).toList());
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(null, fm, null, List.of(), null, units).call(), "could not compile " + files);
        }
        return outputDir;
    }
}
