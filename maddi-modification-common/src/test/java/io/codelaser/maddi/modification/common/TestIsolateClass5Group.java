package io.codelaser.maddi.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.modification.common.util.IsolateClass;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.modification.common.CommonTest.javaInspectorFactory;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Isolating a <b>set</b> of types together, rather than one.
 * <p>
 * What the set buys is one thing, and every driver here measures the same thing from a different side: a reference
 * from one isolated type to another reaches the <b>real</b> type — its body, its field, its supertype — where a
 * single isolate would have reduced it to a stub. {@link #oneAtATimeStubsTheSibling()} is the control: the same
 * reference, the same fixture, one type asked for instead of three.
 * <p>
 * The gate is {@code TestIsolateClass4Compiles}': re-parse with {@code failFast}, so a javac error is a thrown
 * {@code CompilationProblems} rather than a log line, plus the type-set check, because a unit whose symbols do not
 * resolve is DROPPED with a warning and dropping is not an error. Both matter here: the failure mode of a group
 * isolate is a duplicate or contradictory declaration — the sibling's method stubbed body-less onto a subtype that
 * keeps it verbatim — which is a compile error and nothing else.
 */
public class TestIsolateClass5Group {
    protected JavaInspector javaInspector;
    protected IsolateClass isolateClass;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = javaInspectorFactory().withSources(SourceSetImpl.testProtocolSourceSet());
        isolateClass = new IsolateClass(javaInspector);
    }

    @Language("java")
    private static final String BASE = """
            package p.q;
            public class Base {
                public String name;
                public String prefix() { return "["; }
                public String describe() { return "base " + name; }
                public static String tag() { return "T"; }
            }
            """;

    /**
     * Four ways of naming a sibling isolate's member, and each one arrives at a different branch of the visitor:
     * {@code super.describe()} at {@code superTypeStubOf}, {@code Base.tag()} at {@code declaringOwner} through a
     * written type name, and {@code prefix()} / {@code name} — INHERITED and unqualified — at {@code selfType()},
     * which is Sub's own stand-in and not the type that declares them. That last pair is the interesting one: it
     * is where a group isolate writes a body-less duplicate over a declaration it keeps verbatim one level up.
     */
    @Language("java")
    private static final String SUB = """
            package p.q;
            public class Sub extends Base {
                @Override
                public String describe() {
                    return prefix() + super.describe() + name + Base.tag();
                }
            }
            """;

    @Language("java")
    private static final String HELPER = """
            package r.s;
            public class Helper {
                public static String decorate(String s) { return s; }
            }
            """;

    @Language("java")
    private static final String CLIENT = """
            package a.b;
            import p.q.Sub;
            import r.s.Helper;
            import static p.q.Base.tag;
            public class Client {
                private Sub sub;
                public String render() {
                    return Helper.decorate(sub.describe()) + sub.name + tag();
                }
            }
            """;

    private static final Map<String, String> SOURCES =
            Map.of("p.q.Base", BASE, "p.q.Sub", SUB, "r.s.Helper", HELPER, "a.b.Client", CLIENT);

    /** Isolates {@code fqns} as one set, taking each kept member's verbatim text straight out of the fixture. */
    private Map<String, String> isolate(String... fqns) {
        var parsed = javaInspector.parse(SOURCES,
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        List<TypeInfo> types = java.util.Arrays.stream(fqns).map(parsed.parseResult()::findType).toList();
        IsolateClass.Result r = isolateClass.isolate(types);
        Map<MethodInfo, String> memberSources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            memberSources.put(original, verbatim(SOURCES.get(original.primaryType().fullyQualifiedName()), original));
        }
        Map<String, String> tree = isolateClass.print(r, memberSources);
        tree.forEach((path, source) -> System.out.println("===== " + path + "\n" + source));
        return tree;
    }

    private void assertCompiles(Map<String, String> tree) throws IOException {
        Map<String, String> byFqn = new LinkedHashMap<>();
        tree.forEach((path, source) ->
                byFqn.put(path.substring(0, path.length() - ".java".length()).replace('/', '.'), source));
        javaInspector.invalidateAllSources();
        var summary = javaInspector.parse(byFqn, new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).build());
        assertEquals(byFqn.keySet().size(), summary.types().size(),
                "expected " + byFqn.keySet() + " but got " + summary.types());
    }

    /** The member's own lines, cut out of the compilation unit by its recorded source span. */
    private static String verbatim(String unitSource, MethodInfo methodInfo) {
        String[] lines = unitSource.split("\n", -1);
        var source = methodInfo.source();
        StringBuilder sb = new StringBuilder();
        for (int line = source.beginLine(); line <= source.endLine(); line++) {
            if (line > source.beginLine()) sb.append('\n');
            sb.append(lines[line - 1]);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------------------

    @DisplayName("one unit per isolated type, and the shared dependency stubbed once")
    @Test
    public void projectLayout() throws IOException {
        Map<String, String> tree = isolate("a.b.Client", "p.q.Sub", "p.q.Base");
        assertEquals(java.util.Set.of("a/b/Client.java", "p/q/Base.java", "p/q/Sub.java", "r/s/Helper.java"),
                tree.keySet(), tree.keySet().toString());

        // every isolate keeps its own body; Helper is the only thing reduced to a stub
        assertTrue(tree.get("p/q/Base.java").contains("return \"base \" + name;"), tree.get("p/q/Base.java"));
        assertTrue(tree.get("p/q/Sub.java").contains("return prefix() + super.describe() + name + Base.tag();"),
                tree.get("p/q/Sub.java"));
        assertTrue(tree.get("a/b/Client.java").contains("Helper.decorate(sub.describe())"),
                tree.get("a/b/Client.java"));
        assertTrue(tree.get("r/s/Helper.java").contains("return null;"), tree.get("r/s/Helper.java"));
        assertCompiles(tree);
    }

    @DisplayName("a sibling isolate is the real type: no stub of it, and no second declaration of its members")
    @Test
    public void siblingIsNotStubbed() throws IOException {
        Map<String, String> tree = isolate("a.b.Client", "p.q.Sub", "p.q.Base");

        // 'Sub extends Base' is reproduced against the kept Base, and Sub declares NOTHING of Base's: the
        // inherited 'name' and 'prefix()', both named unqualified, and 'describe()' reached through 'super.',
        // all resolve upwards. A duplicate here is the group isolate's characteristic failure, and note that
        // neither shape is a compile error -- 'public String prefix() { return null; }' is a legal override and
        // a second 'name' a legal shadow, so assertCompiles does NOT see them; these two assertions do.
        // The set is asked for in the order Client, Sub, Base deliberately, so that a sibling is referenced
        // before it is reached in the list. Verified to fail when the kept members are registered per type
        // inside the member walk rather than over the whole set beforehand (this driver and two others)
        String sub = tree.get("p/q/Sub.java");
        assertTrue(sub.contains("class Sub extends Base"), sub);
        assertFalse(sub.contains("return null;"), sub);
        // the DECLARATION, not the call the verbatim body makes
        assertFalse(sub.contains("String prefix()"), sub);
        assertFalse(sub.contains("String name"), sub);
        // Client imports the sibling rather than declaring a stub for it
        String client = tree.get("a/b/Client.java");
        assertTrue(client.contains("import p.q.Sub;"), client);
        // and an import belongs to the unit whose text needs it. 'toImport' is one list for the whole isolate, so
        // offering all of it to every isolated unit put 'import a.b.Client' and 'import r.s.Helper' at the top of
        // p/q/Base.java, which names neither
        String base = tree.get("p/q/Base.java");
        assertFalse(base.contains("import "), base);
        assertCompiles(tree);
    }

    @DisplayName("a static import of a sibling's member lands in the unit that needs it, and only there")
    @Test
    public void staticImportOfASibling() throws IOException {
        Map<String, String> tree = isolate("a.b.Client", "p.q.Sub", "p.q.Base");
        String client = tree.get("a/b/Client.java");
        assertTrue(client.contains("import static p.q.Base.tag;"), client);
        // Sub inherits tag(), so it needs no import for it -- and never the other unit's
        assertFalse(tree.get("p/q/Sub.java").contains("import static"), tree.get("p/q/Sub.java"));
        assertCompiles(tree);
    }

    @DisplayName("the control: isolated on its own, the sibling is a stub with an empty body")
    @Test
    public void oneAtATimeStubsTheSibling() throws IOException {
        Map<String, String> tree = isolate("a.b.Client");
        assertTrue(tree.containsKey("p/q/Sub.java"), tree.keySet().toString());
        String sub = tree.get("p/q/Sub.java");
        // the very declaration the group isolate keeps verbatim, here reduced to what a stub can say
        assertTrue(sub.contains("return null;"), sub);
        assertFalse(sub.contains("super.describe()"), sub);
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String TWO_IN_ONE_FILE = """
            package t.u;
            import r.s.Helper;
            import r.s.Other;
            public class First {
                public String go() { return Helper.decorate("x"); }
            }
            class Second {
                public String go() { return Other.tweak("y"); }
            }
            """;

    @Language("java")
    private static final String OTHER = """
            package r.s;
            public class Other {
                public static String tweak(String s) { return s; }
            }
            """;

    /**
     * ⛔ Two isolated types declared in ONE source file, which is where per-unit bookkeeping keyed by the
     * compilation unit silently breaks: {@code CompilationUnit.equals} is {@code (uri, sourceSet)} and an isolated
     * unit inherits its original's URI, so both entries are written under one key and the last one wins. Each
     * type here names a stub of its own by its simple name, so whichever entry loses, that unit is emitted
     * without the import its verbatim text needs — "cannot find symbol", and the unit is dropped.
     */
    @DisplayName("two isolates out of one source file keep their own imports")
    @Test
    public void twoIsolatesFromOneFile() throws IOException {
        var parsed = javaInspector.parse(Map.of("t.u.First", TWO_IN_ONE_FILE, "r.s.Helper", HELPER,
                        "r.s.Other", OTHER),
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        TypeInfo first = parsed.parseResult().findType("t.u.First");
        TypeInfo second = parsed.parseResult().findType("t.u.Second");
        assertEquals(first.compilationUnit(), second.compilationUnit(), "the fixture's point: one file");

        IsolateClass.Result r = isolateClass.isolate(List.of(first, second));
        Map<MethodInfo, String> memberSources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            memberSources.put(original, verbatim(TWO_IN_ONE_FILE, original));
        }
        Map<String, String> tree = isolateClass.print(r, memberSources);
        tree.forEach((path, source) -> System.out.println("===== " + path + "\n" + source));

        assertTrue(tree.get("t/u/First.java").contains("import r.s.Helper;"), tree.get("t/u/First.java"));
        assertTrue(tree.get("t/u/Second.java").contains("import r.s.Other;"), tree.get("t/u/Second.java"));
        assertCompiles(tree);
    }

    @Language("java")
    private static final String OUTER = """
            package o;
            public class Outer {
                public static class Inner {
                    public int value;
                }
                public int read(Inner i) { return i.value; }
            }
            """;

    @DisplayName("a type and its own enclosing type cannot be isolated together")
    @Test
    public void nestedPairIsRejected() {
        var parsed = javaInspector.parse(Map.of("o.Outer", OUTER),
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        TypeInfo outer = parsed.parseResult().findType("o.Outer");
        TypeInfo inner = parsed.parseResult().findType("o.Outer.Inner");
        assertNotNull(inner);
        // an isolate is lifted to the top level of its package, so 'Outer.Inner' in Outer's verbatim text would
        // name the nested stub while 'o/Inner.java' sits beside it: two declarations, and the text picks one
        assertThrows(UnsupportedOperationException.class, () -> isolateClass.isolate(List.of(outer, inner)));
    }

    @Language("java")
    private static final String TWO_ENTRIES = """
            package o;
            public class One {
                public static class Entry { public int x; }
            }
            """;

    @Language("java")
    private static final String TWO_ENTRIES_2 = """
            package o;
            public class Two {
                public static class Entry { public int y; }
            }
            """;

    @DisplayName("two isolates that would emit the same file are rejected rather than silently merged")
    @Test
    public void samePathIsRejected() {
        var parsed = javaInspector.parse(Map.of("o.One", TWO_ENTRIES, "o.Two", TWO_ENTRIES_2),
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        TypeInfo one = parsed.parseResult().findType("o.One.Entry");
        TypeInfo two = parsed.parseResult().findType("o.Two.Entry");
        // both would be emitted as 'o/Entry.java', and print() keys its result by path: the second overwrote the
        // first, and the caller saw a tree that was one file short with no indication of which
        assertThrows(UnsupportedOperationException.class, () -> isolateClass.isolate(List.of(one, two)));
    }
}
