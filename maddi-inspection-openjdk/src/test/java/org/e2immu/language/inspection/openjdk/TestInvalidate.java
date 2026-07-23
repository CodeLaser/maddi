package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
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

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Re-parsing with an {@code Invalidated}, at the granularity this inspector works in: <b>the source set</b>, because
 * that is javac's unit.
 * <ul>
 *     <li>a source set holding an INVALID type is re-scanned in full — so its <em>unchanged</em> types are rebuilt
 *     too, which is where this deliberately differs from the in-house inspector's per-file re-parse;</li>
 *     <li>a source set that only needs REWIRE is not re-scanned: its types are copied onto the new objects, keeping
 *     their compilation units;</li>
 *     <li>an untouched source set keeps its very objects.</li>
 * </ul>
 * Two source sets on disk, because that is the only way one can depend on another here: a dependent source set
 * resolves through javac's class path, i.e. against the <em>compiled</em> artifact of the set it depends on (cf.
 * TestJavaInspector6MultiProject). Hence main is compiled to a class directory first.
 */
public class TestInvalidate {

    private static final String BASE_FQN = "a.b.Base";
    private static final String HELPER_FQN = "a.b.Helper";
    private static final String USER_FQN = "c.d.User";

    @Language("java")
    private static final String BASE = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
            }
            """;

    // in the same source set as Base, and independent of it
    @Language("java")
    private static final String HELPER = """
            package a.b;
            public class Helper {
                public int help() { return 42; }
            }
            """;

    // in a source set that depends on main
    @Language("java")
    private static final String USER = """
            package c.d;
            import a.b.Base;
            import java.util.function.Supplier;
            public class User {
                public String use(Base base) { return base.name(); }
                public Supplier<String> supplier(Base base) {
                    return new Supplier<String>() {
                        @Override
                        public String get() { return base.name(); }
                    };
                }
            }
            """;

    @TempDir
    Path root;

    private JavaInspector javaInspector;
    private SourceSet main;
    private SourceSet dependent;

    @BeforeEach
    public void before() throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a/b"));
        Files.writeString(mainSrc.resolve("Base.java"), BASE);
        Files.writeString(mainSrc.resolve("Helper.java"), HELPER);
        Path depSrc = Files.createDirectories(root.resolve("dep-src/c/d"));
        Files.writeString(depSrc.resolve("User.java"), USER);

        // 'dependent' resolves a.b.Base through javac's class path, so main must exist as bytecode
        Path mainClasses = Files.createDirectories(root.resolve("main-classes"));
        compile(List.of(mainSrc.resolve("Base.java"), mainSrc.resolve("Helper.java")), mainClasses);

        main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(mainClasses.toUri())
                .build();
        dependent = new SourceSetImpl.Builder().setName("dependent")
                .setSourceDirectories(List.of(root.resolve("dep-src")))
                .setUri(root.resolve("dep-src").toUri())
                .setDependencies(List.of(main))
                .build();

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
    }

    private InputConfiguration inputConfiguration() {
        return new InputConfigurationImpl.Builder()
                .addSourceSets(main, dependent)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
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

    // no in-memory sources: the inspector walks each source set's directories
    private ParseResult parseAll() {
        return javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    private ParseResult reparse(JavaInspector.Invalidated invalidated) {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true)
                .setInvalidated(invalidated)
                .build();
        return javaInspector.parse(Map.of(), options).parseResult();
    }

    @DisplayName("all UNCHANGED: nothing is re-scanned, every object survives")
    @Test
    public void testAllUnchanged() {
        ParseResult pr1 = parseAll();
        assertEquals(3, pr1.primaryTypes().size(), "expected Base, Helper, User; got " + pr1.primaryTypes());

        ParseResult pr2 = reparse(_ -> UNCHANGED);
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            assertSame(pt1, pr2.findType(pt1.fullyQualifiedName()), pt1.fullyQualifiedName() + " must be kept");
        }
    }

    @DisplayName("all INVALID: every source set is re-scanned, every object is new")
    @Test
    public void testAllInvalid() {
        ParseResult pr1 = parseAll();

        ParseResult pr2 = reparse(_ -> INVALID);
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            assertNotNull(pt2, pt1.fullyQualifiedName() + " must come back");
            assertNotSame(pt1, pt2);
            assertEquals(pt1.fullyQualifiedName(), pt2.fullyQualifiedName());
            assertNotSame(pt1.compilationUnit(), pt2.compilationUnit(), "re-scanned: a new compilation unit");
            assertEquals(pt1.compilationUnit().sourceSet().name(), pt2.compilationUnit().sourceSet().name());
        }
    }

    @DisplayName("the reload exposes its rewire as a read-only InfoMapView, mapping old REWIRE objects to new")
    @Test
    public void testInfoMapViewExposed() {
        ParseResult pr1 = parseAll();
        TypeInfo user1 = pr1.findType(USER_FQN);
        assertNotNull(user1);

        ParseResult pr2 = reparse(ti -> switch (ti.simpleName()) {
            case "Base" -> INVALID;
            case "Helper" -> UNCHANGED;
            case "User" -> REWIRE;
            default -> throw new UnsupportedOperationException(ti.fullyQualifiedName());
        });
        TypeInfo user2 = pr2.findType(USER_FQN);
        assertNotSame(user1, user2);

        org.e2immu.language.cst.api.info.InfoMapView view = javaInspector.lastRewireInfoMap();
        assertNotNull(view, "User was rewired, so the reload must expose a view");
        assertSame(user2, view.typeInfo(user1), "the exposed view maps the old User to the rewired one");
        assertTrue(view.rewiredTypes().contains(user2), "rewiredTypes includes the rewired User");
        // the view maps members too, which is what an outside-reload analysis carry needs
        assertSame(user2.findUniqueMethod("use", 1),
                view.methodInfo(user1.findUniqueMethod("use", 1)), "the view maps members");
    }

    @DisplayName("rescan-only (no REWIRE) still exposes a view mapping old rescanned objects to new")
    @Test
    public void testRescanOnlyExposesView() {
        ParseResult pr1 = parseAll();
        TypeInfo base1 = pr1.findType(BASE_FQN);
        TypeInfo helper1 = pr1.findType(HELPER_FQN);
        assertNotNull(base1);
        assertNotNull(helper1);

        // main (Base, Helper) is re-scanned; dependent (User) is KEEP -- nothing is REWIRE. Before, this exposed no
        // view (toRewire empty); now it exposes the rescanned old->new mapping, which the same-source-set carry needs.
        ParseResult pr2 = reparse(ti -> switch (ti.simpleName()) {
            case "Base", "Helper" -> INVALID;
            case "User" -> UNCHANGED;
            default -> throw new UnsupportedOperationException(ti.fullyQualifiedName());
        });
        TypeInfo base2 = pr2.findType(BASE_FQN);
        assertNotSame(base1, base2, "Base was re-scanned");

        org.e2immu.language.cst.api.info.InfoMapView view = javaInspector.lastRewireInfoMap();
        assertNotNull(view, "a rescanned set exposes a view even with nothing to rewire");
        // the old rescanned type resolves to the new one by fqn+source-set equality (InfoMapImpl seeds rebuilt types)
        assertSame(base2, view.typeInfo(base1), "the view maps the old rescanned Base to the new one");
        assertSame(pr2.findType(HELPER_FQN), view.typeInfo(helper1), "and old Helper to the new one");
        // members too, which the analysis carry rewires references through
        assertSame(base2.findUniqueMethod("name", 0), view.methodInfo(base1.findUniqueMethod("name", 0)),
                "the view maps rescanned members");
    }

    @DisplayName("Base INVALID, User REWIRE: main is re-scanned, dependent is rewired, and Helper is rebuilt with it")
    @Test
    public void testInvalidAndRewire() {
        ParseResult pr1 = parseAll();
        TypeInfo base1 = pr1.findType(BASE_FQN);
        TypeInfo helper1 = pr1.findType(HELPER_FQN);
        TypeInfo user1 = pr1.findType(USER_FQN);
        assertNotNull(base1);
        assertNotNull(helper1);
        assertNotNull(user1);

        ParseResult pr2 = reparse(ti -> switch (ti.simpleName()) {
            case "Base" -> INVALID;
            case "Helper" -> UNCHANGED;   // ... but it shares a source set with Base
            case "User" -> REWIRE;
            default -> throw new UnsupportedOperationException(ti.fullyQualifiedName());
        });
        assertEquals(3, pr2.primaryTypes().size());

        TypeInfo base2 = pr2.findType(BASE_FQN);
        assertNotSame(base1, base2, "Base changed: re-scanned");
        assertNotSame(base1.compilationUnit(), base2.compilationUnit());

        // the point of source-set granularity: Helper is UNCHANGED, but it lives in a re-scanned source set
        TypeInfo helper2 = pr2.findType(HELPER_FQN);
        assertNotSame(helper1, helper2,
                "Helper is unchanged, but its source set was re-scanned: at this granularity it is rebuilt too");

        // User is rewired, not re-scanned: a new object on the SAME compilation unit
        TypeInfo user2 = pr2.findType(USER_FQN);
        assertNotSame(user1, user2, "User depends on Base: it must be rewired onto the new object");
        assertSame(user1.compilationUnit(), user2.compilationUnit(),
                "rewired, not re-parsed: rewirePhase0 reuses the compilation unit (hence its fingerprint)");

        // the whole point of REWIRE: the copy must reach the NEW Base, not the object it replaced
        assertSame(base1, typeOfUseParameter(user1), "sanity: the original User referenced the original Base");
        assertSame(base2, typeOfUseParameter(user2),
                "the rewired User must reference the re-scanned Base, not the stale one");
    }

    /**
     * The property REWIRE exists for — {@code InvalidationState}'s own words: "the type isn't changed at all, but it
     * accesses invalidated (and hence re-parsed, NEW) type info objects".
     * <p>
     * It used to fail, here and in the in-house inspector alike: {@code InfoMapImpl.typeInfo(t)} returns {@code t}
     * unchanged when {@code t.primaryType()} is not a key of its map, and both inspectors built that map from the
     * REWIRE set alone — so the re-parsed types were not keys, and every reference to them passed through stale. The
     * fix seeds the map with the rebuilt types; a lookup of the old object then finds the new one, equality being
     * fqn + source set.
     */
    @DisplayName("a rewired type reaches the re-parsed object through every reference, not just the direct one")
    @Test
    public void testRewiredTypeReferencesTheNewObject() {
        ParseResult pr1 = parseAll();
        TypeInfo base1 = pr1.findType(BASE_FQN);

        ParseResult pr2 = reparse(ti -> switch (ti.simpleName()) {
            case "Base" -> INVALID;
            case "Helper" -> UNCHANGED;
            case "User" -> REWIRE;
            default -> throw new UnsupportedOperationException(ti.fullyQualifiedName());
        });
        TypeInfo base2 = pr2.findType(BASE_FQN);
        TypeInfo user2 = pr2.findType(USER_FQN);
        assertNotSame(base1, base2);

        // the parameter's type
        assertSame(base2, typeOfUseParameter(user2));
        // and the method it calls on it: base.name() must resolve to the re-parsed Base's method
        MethodInfo name = base2.findUniqueMethod("name", 0);
        assertSame(base2, name.typeInfo());
        assertTrue(user2.findUniqueMethod("use", 1).methodBody().toString().contains("name"),
                "sanity: User.use calls base.name()");
        // nothing anywhere in the rewired User may still reach the stale Base
        assertFalse(referencesType(user2, base1), "the rewired User still reaches the stale Base");
        assertTrue(referencesType(user2, base2), "the rewired User must reach the re-parsed Base");
    }

    /** Does any type reference inside {@code typeInfo} point at exactly {@code target} (by identity)? */
    private static boolean referencesType(TypeInfo typeInfo, TypeInfo target) {
        return typeInfo.constructorAndMethodStream().anyMatch(mi ->
                mi.parameters().stream().anyMatch(pi -> pi.parameterizedType().typeInfo() == target)
                || mi.returnType() != null && mi.returnType().typeInfo() == target);
    }


    /**
     * The single-instance invariant, across a rewire: one {@code TypeInfo} per (FQN, source set), the one the
     * registries hand out.
     * <p>
     * It used to fail for the types phase 3 rewires <em>on demand</em>. Rewiring {@code User} produces a new
     * {@code c.d.User.$0} — {@code ConstructorCallImpl.rewire} calls {@code InfoMap.typeInfoRecurseAllPhases} on the
     * anonymous class — but the registries were re-pointed by walking the rewired type through
     * {@code recursiveSubTypeStream()}, which lists only the DECLARED subtypes ({@code user.subTypes()} is empty
     * here). So they kept answering with the object that was replaced, while the live CST held another. Local
     * classes and lambdas are rewired the same way and were lost the same way. The fix asks the InfoMap what it
     * built ({@code rewiredTypes()}) instead of re-deriving it.
     */
    @DisplayName("a rewired anonymous type replaces the old one in the registry")
    @Test
    public void testRewiredAnonymousTypeIsRegistered() {
        parseAll();
        ParseResult pr2 = reparse(ti -> switch (ti.simpleName()) {
            case "Base" -> INVALID;
            case "Helper" -> UNCHANGED;
            default -> REWIRE;
        });
        TypeInfo anonymousInBody = anonymousClassIn(pr2.findType(USER_FQN), "supplier");
        assertNotNull(anonymousInBody);
        assertEquals("c.d.User.$0", anonymousInBody.fullyQualifiedName());
        assertSame(anonymousInBody, javaInspector.compiledTypesManager().get("c.d.User.$0", dependent),
                "the registry must hand out the anonymous type that the rewired body actually holds");
    }

    /** The anonymous class created inside {@code typeInfo}'s method {@code methodName}, or null. */
    private static TypeInfo anonymousClassIn(TypeInfo typeInfo, String methodName) {
        java.util.concurrent.atomic.AtomicReference<TypeInfo> found = new java.util.concurrent.atomic.AtomicReference<>();
        typeInfo.findUniqueMethod(methodName, 1).methodBody().visit(e -> {
            if (found.get() == null && e instanceof ConstructorCall cc && cc.anonymousClass() != null) {
                found.set(cc.anonymousClass());
            }
            return found.get() == null;
        });
        return found.get();
    }

    /** The type of the single parameter of {@code User.use(Base)}. */
    private static TypeInfo typeOfUseParameter(TypeInfo user) {
        return user.findUniqueMethod("use", 1).parameters().getFirst().parameterizedType().typeInfo();
    }

    @DisplayName("only the dependent source set is rewired: main is untouched")
    @Test
    public void testRewireOnly() {
        ParseResult pr1 = parseAll();
        TypeInfo base1 = pr1.findType(BASE_FQN);
        TypeInfo user1 = pr1.findType(USER_FQN);

        ParseResult pr2 = reparse(ti -> "User".equals(ti.simpleName()) ? REWIRE : UNCHANGED);
        assertEquals(3, pr2.primaryTypes().size());

        assertSame(base1, pr2.findType(BASE_FQN), "main is untouched: kept as is");
        TypeInfo user2 = pr2.findType(USER_FQN);
        assertNotSame(user1, user2);
        assertSame(user1.compilationUnit(), user2.compilationUnit());
    }

    @DisplayName("the registry points at the rewired object, not the stale one")
    @Test
    public void testRegistryUpdated() {
        parseAll();
        ParseResult pr2 = reparse(ti -> "User".equals(ti.simpleName()) ? REWIRE : UNCHANGED);
        TypeInfo user2 = pr2.findType(USER_FQN);

        assertSame(user2, javaInspector.compiledTypesManager().get(USER_FQN, dependent),
                "the CompiledTypesManager must resolve to the rewired object");
    }
}
