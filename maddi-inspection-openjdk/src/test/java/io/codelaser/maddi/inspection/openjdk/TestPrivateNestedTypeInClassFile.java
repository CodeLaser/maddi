package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * ⛔⛔ <b>A PRIVATE NESTED TYPE IS SKIPPED BY THE MEMBER WALK AND STILL REACHABLE THROUGH A MEMBER'S SIGNATURE.</b>
 * {@code addMemberToType} loads private constructors and private fields — each with a comment recording why the
 * opposite was wrong — but its third branch still skips a private nested type. So the owner is COMMITTED without
 * it, and when a field's type then names that very type, {@code lazilyLoadTypeFromClassFile} reaches for
 * {@code owner.builder()} on an immutable owner:
 *
 * <pre>
 * java.lang.AssertionError: Inspection of a.b.Outer.Grant.GrantOn.GrantOnTo has already been committed
 *   at TypeInfoImpl.builder(TypeInfoImpl.java:570)
 *   at ClassSymbolScanner.lazilyLoadTypeFromClassFile(ClassSymbolScanner.java:248)
 *   ...
 *   at ClassSymbolScanner.addFieldToType(ClassSymbolScanner.java:889)
 *   at ClassSymbolScanner.loadType(ClassSymbolScanner.java:490)
 *   at ClassSymbolScanner.commitType(ClassSymbolScanner.java:1766)
 * </pre>
 *
 * <p>⚠ <b>THE THIRD INSTANCE OF ONE LESSON.</b> The constructor branch says <i>"constructors are loaded even when
 * private: a type's constructors are part of its shape"</i>; the field branch says <i>"load fields even when
 * private: like constructors (see above)"</i>. The nested-type branch is the last holdout, and it carried no
 * comment at all — two neighbours told, the third not.
 *
 * <p>⛔ <b>MEASURED ON A REAL ARTIFACT, 2026-08-12.</b> {@code jenkins-test-harness}'s
 * {@code MockAuthorizationStrategy} is exactly this shape — its {@code InnerClasses} attribute records
 * {@code private GrantOnTo of GrantOn}, and the outer type holds
 * {@code private final List<Grant.GrantOn.GrantOnTo> grantsOnTo}. One AssertionError refused the WHOLE
 * ParseResult for a 493-file source set, and the message named the victim rather than the cause.
 *
 * <p>⛔⛔ <b>READ THIS BEFORE TREATING THE TESTS BELOW AS THE FIX'S COVERAGE: THEY ARE NOT.</b> This
 * minimisation was written to reproduce the AssertionError and <b>does not</b> — it is green against the
 * UNFIXED scanner as well as the fixed one. So the shape alone (a private nested type at depth three, named by
 * the top-level type's field) is <em>not</em> sufficient; the corpus trigger additionally depends on the ORDER
 * in which types are committed, which this fixture does not control — here the chain is reached through a
 * single forced load, with the owner still mutable.
 *
 * <p>A minimisation that does not reproduce is a RESULT, not a failed attempt, and the thing not to do with it
 * is quietly assert it green and call it corpus coverage. It survives as a CONTROL: the private chain and the
 * public one must resolve identically, which is the property the fix must not break. <b>The evidence for the
 * fix is the corpus</b> — before it, the jenkins parse died on this exact type; after it, {@code test/test-classes}
 * commits all 493 primary types. Isolating the ordering trigger is still open.
 */
public class TestPrivateNestedTypeInClassFile {

    /**
     * The corpus shape, minimised: a private nested type at depth three, named by a field of the TOP-LEVEL type
     * (legal Java — private members are accessible anywhere inside the top-level enclosing class).
     */
    @Language("java")
    private static final String OUTER = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Outer {
                private final List<Grant.GrantOn.GrantOnTo> grantsOnTo = new ArrayList<>();
                public class Grant {
                    public class GrantOn {
                        private class GrantOnTo {
                            private final String sid = "s";
                            String sid() { return sid; }
                        }
                        GrantOnTo to() { return new GrantOnTo(); }
                    }
                }
            }
            """;

    /**
     * ⚠ CONTROL, in the same class file: the identical chain with a PUBLIC leaf. If the public one broke too, the
     * finding would be "depth three is unsupported" rather than "private is skipped", and the fix would be aimed
     * somewhere else entirely.
     */
    @Language("java")
    private static final String CONTROL = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Control {
                private final List<Grant.GrantOn.GrantOnTo> grantsOnTo = new ArrayList<>();
                public class Grant {
                    public class GrantOn {
                        public class GrantOnTo {
                            private final String sid = "s";
                            String sid() { return sid; }
                        }
                        GrantOnTo to() { return new GrantOnTo(); }
                    }
                }
            }
            """;

    /** The source set under parse: it only has to NAME the library types, so they get loaded and committed. */
    @Language("java")
    private static final String USER = """
            package c.d;
            import a.b.Control;
            import a.b.Outer;
            public class User {
                public String use(Outer outer, Control control) { return outer.toString() + control; }
            }
            """;

    @TempDir
    Path root;

    private JavaInspector inspector;   // for post-parse lookup of the LIBRARY types, which are not in summary.types()
    private SourceSet user;

    private Summary parseAgainstLibrary() throws IOException {
        Path libSrc = Files.createDirectories(root.resolve("lib-src/a/b"));
        Files.writeString(libSrc.resolve("Outer.java"), OUTER);
        Files.writeString(libSrc.resolve("Control.java"), CONTROL);
        Path libClasses = Files.createDirectories(root.resolve("lib-classes"));
        compile(List.of(libSrc.resolve("Outer.java"), libSrc.resolve("Control.java")), libClasses);
        // the whole point: Outer/Control reach the parse as CLASS FILES only, never as source
        assertTrue(Files.exists(libClasses.resolve("a/b/Outer$Grant$GrantOn$GrantOnTo.class")),
                "the private nested type must exist as its own class file, or the fixture proves nothing");

        Path userSrc = Files.createDirectories(root.resolve("user-src/c/d"));
        Files.writeString(userSrc.resolve("User.java"), USER);

        // exactly the shape the generated configuration has for jenkins' test/test-classes -> the harness jar:
        // a library classpath part, named as a dependency of the source set that reads it
        SourceSet lib = new SourceSetImpl.Builder().setName("lib")
                .setSourceDirectories(List.of())
                .setUri(libClasses.toUri())
                .setLibrary(true).setExternalLibrary(true)
                .build();
        user = new SourceSetImpl.Builder().setName("user")
                .setSourceDirectories(List.of(root.resolve("user-src")))
                .setUri(root.resolve("user-classes").toUri())
                .setDependencies(List.of(lib))
                .build();

        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        inspector = javaInspector;
        javaInspector.initialize(new InputConfigurationImpl.Builder()
                .addSourceSets(user)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPathParts(lib)
                .build());
        return javaInspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder().setFailFast(false).build());
    }

    @DisplayName("a private nested type named by a field of the enclosing type does not refuse the parse")
    @Test
    public void privateNestedTypeDoesNotRefuseTheParse() throws IOException {
        Summary summary = parseAgainstLibrary();

        // ⚠ VACUITY FIRST. The first version of this fixture put the library on addClassPath(<dir>), which does
        // not take a class directory: User.java was DROPPED with an UnresolvedSymbolException, summary.types()
        // was empty, and "the parse has no errors" passed on nothing at all.
        assertEquals(List.of("c.d.User"), summary.types().stream()
                .map(org.e2immu.language.cst.api.info.TypeInfo::fullyQualifiedName).sorted().toList(),
                "the source set must actually have parsed");
        assertTrue(summary.parseWarnings().isEmpty(),
                "and resolved its references: " + summary.parseWarnings().stream()
                        .map(e -> String.valueOf(e.getMessage())).toList());
        List<String> messages = summary.parseExceptions().stream()
                .map(e -> String.valueOf(e.getMessage())).toList();
        assertTrue(messages.isEmpty(), "the parse must not produce errors: " + messages);
        assertFalse(summary.haveErrors(), "the parse must be clean");
    }

    /**
     * ⚠ The private nested type must be RESOLVABLE, not merely non-fatal: an inspection that quietly loses it
     * would satisfy the test above and still lose the field's type.
     */
    @DisplayName("and the private nested type resolves, so the field's type argument is not lost")
    @Test
    public void thePrivateNestedTypeResolves() throws IOException {
        parseAgainstLibrary();

        TypeInfo grantOn = findSubType("a.b.Outer", "Grant", "GrantOn");
        assertNotNull(grantOn.findSubType("GrantOnTo", false),
                "GrantOnTo must be a sub type of GrantOn, exactly as in the public control: "
                + grantOn.subTypes().stream().map(TypeInfo::simpleName).toList());
    }

    @DisplayName("CONTROL: the same chain with a PUBLIC leaf — green before the fix as well as after")
    @Test
    public void thePublicChainWasNeverBroken() throws IOException {
        parseAgainstLibrary();

        TypeInfo grantOn = findSubType("a.b.Control", "Grant", "GrantOn");
        assertNotNull(grantOn.findSubType("GrantOnTo", false), "the public leaf has always resolved");
    }

    private TypeInfo findSubType(String topLevel, String... path) {
        // ⚠ type(), not typeIfLoaded(): the latter is the source-set-scoped PEEK, and a LIBRARY type does not
        // belong to the source set that names it. Forcing the load is also what the corpus does at commit time.
        TypeInfo type = inspector.compiledTypesManager().type(topLevel, user);
        assertNotNull(type, topLevel + " was never loaded");
        for (String step : path) {
            TypeInfo next = type.findSubType(step, false);
            assertNotNull(next, step + " not found in " + type.fullyQualifiedName() + ": "
                                + type.subTypes().stream().map(TypeInfo::simpleName).toList());
            type = next;
        }
        return type;
    }

    private static void compile(List<Path> files, Path outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(null, fm, null, List.of(), null, units).call(),
                    "could not compile the library sources");
        }
    }
}
