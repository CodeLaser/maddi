package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The per-file re-parse within a source set ({@code JavaInspectorImpl.reparse}, {@code SourceSetAction.PARTIAL}):
 * javac is handed the changed files, the rest of the set sits on its source path, unchanged types are kept or
 * rewired. See {@code docs/design/incremental-and-overlay.md} §5.
 * <p>
 * Every case ends with the same two checks, the oracle of §8 of that document:
 * <ol>
 *     <li>no reference anywhere in the re-parsed result reaches an object that is not the live one for its type
 *     (the registry's, the parse result's): a stale reference is exactly what a wrong rewire leaves behind, and it
 *     is invisible to any check by name or by equality, both being fqn + source set;</li>
 *     <li>every compilation unit prints exactly as a cold parse of the same files, in a fresh inspector, prints
 *     it.</li>
 * </ol>
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

    // ---- the oracle

    /**
     * Every type a reference reaches, if it is one of the parse's own source types, must be the object the parse
     * result holds for it, by identity. Walks the type references of every primary type, and in every method body
     * and field initializer the owners of the methods called, the constructors invoked and the fields read.
     */
    private static void assertNoStaleReferences(ParseResult parseResult) {
        List<String> stale = new ArrayList<>();
        Set<String> ours = parseResult.primaryTypes().stream().map(TypeInfo::fullyQualifiedName)
                .collect(Collectors.toUnmodifiableSet());
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            Function<TypeInfo, Void> check = reached -> {
                if (reached == null) return null;
                TypeInfo primary = reached.primaryType();
                if (primary != null && ours.contains(primary.fullyQualifiedName())
                    && parseResult.findType(primary.fullyQualifiedName()) != primary) {
                    stale.add(primaryType.fullyQualifiedName() + " -> " + reached.fullyQualifiedName());
                }
                return null;
            };
            primaryType.typesReferenced(_ -> true).forEach(tr -> check.apply(tr.typeInfo()));
            primaryType.recursiveSubTypeStream().forEach(type -> {
                check.apply(type.parentClass() == null ? null : type.parentClass().typeInfo());
                type.interfacesImplemented().forEach(pt -> check.apply(pt.typeInfo()));
                type.constructorAndMethodStream().forEach(mi -> {
                    check.apply(mi.returnType().typeInfo());
                    mi.parameters().forEach(pi -> check.apply(pi.parameterizedType().typeInfo()));
                    mi.methodBody().visit(e -> visit(e, check));
                });
                for (FieldInfo fieldInfo : type.fields()) {
                    check.apply(fieldInfo.type().typeInfo());
                    if (fieldInfo.initializer() != null) fieldInfo.initializer().visit(e -> visit(e, check));
                }
            });
        }
        assertTrue(stale.isEmpty(), "stale references: " + stale);
    }

    private static boolean visit(Element element, Function<TypeInfo, Void> check) {
        switch (element) {
            case MethodCall mc -> check.apply(mc.methodInfo().typeInfo());
            case ConstructorCall cc when cc.constructor() != null -> check.apply(cc.constructor().typeInfo());
            case VariableExpression ve when ve.variable() instanceof FieldReference fr ->
                    check.apply(fr.fieldInfo().owner());
            default -> {
            }
        }
        return true;
    }

    /**
     * Every compilation unit prints exactly as a cold parse of the same files, in a fresh inspector, prints it; and
     * every method body and field initializer reads the same variables, by fully qualified name, in the same order, with
     * the same field scopes written. The second half sees what printing hides: a rewire that turned an implicit
     * {@code v} into an explicit {@code this.v} printed identically, but the codec and the Kotlin printer tell them
     * apart.
     */
    private static void assertPrintsAsCold(JavaInspector warm, ParseResult warmResult, SourceSet... sourceSets) {
        JavaInspector cold = inspector(sourceSets);
        ParseResult coldResult = parse(cold);
        assertEquals(printed(cold, coldResult), printed(warm, warmResult));
        assertEquals(variablesRead(coldResult), variablesRead(warmResult));
    }

    private static Map<String, List<String>> variablesRead(ParseResult parseResult) {
        Map<String, List<String>> map = new TreeMap<>();
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            primaryType.recursiveSubTypeStream().forEach(type -> {
                type.constructorAndMethodStream().forEach(mi -> map.put(mi.fullyQualifiedName(),
                        variablesRead(mi.methodBody())));
                for (FieldInfo fieldInfo : type.fields()) {
                    if (fieldInfo.initializer() != null) {
                        map.put(fieldInfo.fullyQualifiedName(), variablesRead(fieldInfo.initializer()));
                    }
                }
            });
        }
        return map;
    }

    private static List<String> variablesRead(Element element) {
        List<String> list = new ArrayList<>();
        element.visit(e -> {
            if (e instanceof VariableExpression ve) {
                // whether a field's scope was written is not in its name, and the Java printer prints both alike;
                // the codec, the Kotlin and C# printers and the isolation tools read it
                String scope = ve.variable() instanceof FieldReference fr && !fr.isDefaultScope() ? " (scope written)"
                        : "";
                list.add(ve.variable().fullyQualifiedName() + scope);
            }
            return true;
        });
        return list;
    }

    private static Map<String, String> printed(JavaInspector inspector, ParseResult parseResult) {
        Map<String, String> map = new TreeMap<>();
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            map.put(primaryType.fullyQualifiedName(), inspector.print2(primaryType.compilationUnit()));
        }
        return map;
    }

    // ---- set-up

    private static JavaInspector inspector(SourceSet... sourceSets) {
        try {
            JavaInspector inspector = new JavaInspectorImpl(true, false);
            InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                    .addSourceSets(sourceSets)
                    .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
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
