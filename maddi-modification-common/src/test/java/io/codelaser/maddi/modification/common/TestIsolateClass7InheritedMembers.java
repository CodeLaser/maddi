package io.codelaser.maddi.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.common.util.IsolateClass;
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
import java.util.regex.Pattern;

import static io.codelaser.maddi.modification.common.CommonTest.javaInspectorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A stub does not re-declare a member it inherits from an ANALYSED supertype — the JDK, or a type the isolate
 * carries verbatim. {@code IsolationCore.inheritedFromAnalysedSupertype}.
 * <p>
 * Why: a stub's body is read by the analyses consuming the isolate, and an empty body is an optimistic verdict.
 * A closed-core {@code ArrayList<I> extends java.util.ArrayList<I>} overriding {@code add(I)} was stubbed as
 * {@code add(I o) { return false; }}; a utility registering an object in such a list and returning it then
 * linked the returned object to nothing, and a factory publishing what it returns was judged fresh: 409 wrong
 * rewrites in 50 class isolates, all of them accepted by javac. Inherited, the method is
 * {@code java.util.ArrayList.add}, whose summary links the argument into the list.
 * <p>
 * The decision table below: dropped are an override of a public concrete JDK method with the same erasure
 * ({@code add}, {@code get}, {@code toString}, and {@code clone} on a list, which {@code ArrayList} already
 * makes public) and an interface's re-declaration of an inherited abstract method; kept are an override that
 * widens access ({@code clone} on a stream, {@code Object}'s being protected), one whose supertype is abstract there ({@code read}),
 * one that drops a checked exception the supertype declares ({@code close}) and, of course, a member the
 * supertype does not have. The unit still has to compile in every case: the callers are verbatim text.
 */
public class TestIsolateClass7InheritedMembers {
    private JavaInspector javaInspector;
    private IsolateClass isolateClass;

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
    private static final String ITEM = """
            package p.q;
            public class Item {
                public int weight;
                public Item() { }
            }
            """;

    @Language("java")
    private static final String MY_LIST = """
            package p.q;
            public class MyList<I> extends java.util.ArrayList<I> {
                @Override public boolean add(I o) { return super.add(o); }
                @Override public I get(int i) { return super.get(i); }
                @Override public String toString() { return "MyList"; }
                @Override public Object clone() { return super.clone(); }
                public int sizeTwice() { return 2 * size(); }
            }
            """;

    @Language("java")
    private static final String MY_STREAM = """
            package p.q;
            public class MyStream extends java.io.InputStream implements Cloneable {
                @Override public int read() { return -1; }
                @Override public void close() { }
                @Override public Object clone() { return this; }
            }
            """;

    @Language("java")
    private static final String MY_ITEMS = """
            package p.q;
            public interface MyItems<I> extends java.util.List<I> {
                @Override boolean add(I o);
                void extra();
            }
            """;

    @Language("java")
    private static final String REGISTRY = """
            package p.q;
            public class Registry {
                public MyList<Item> items = new MyList<>();
                public Item register(Item c) { items.add(c); return c; }
            }
            """;

    @Language("java")
    private static final String USE = """
            package a.b;
            import p.q.Item;
            import p.q.MyItems;
            import p.q.MyList;
            import p.q.MyStream;
            import p.q.Registry;
            public class Use {
                public int run(MyList<Item> list, MyStream stream, MyItems<Item> items, Registry registry) {
                    Item item = new Item();
                    list.add(item);
                    Item first = list.get(0);
                    String s = list.toString();
                    Object listCopy = list.clone();
                    int twice = list.sizeTwice();
                    int b = stream.read();
                    stream.close();
                    Object copy = stream.clone();
                    items.add(item);
                    items.extra();
                    Item registered = registry.register(item);
                    return first.weight + s.length() + listCopy.hashCode() + copy.hashCode() + twice + b
                           + registered.weight;
                }
            }
            """;

    private static final Map<String, String> SOURCES = Map.of("p.q.Item", ITEM, "p.q.MyList", MY_LIST,
            "p.q.MyStream", MY_STREAM, "p.q.MyItems", MY_ITEMS, "p.q.Registry", REGISTRY, "a.b.Use", USE);

    @DisplayName("a stub leaves an inherited, analysed member to the supertype that declares it")
    @Test
    public void inheritedMembersAreNotStubbed() throws IOException {
        Map<String, String> tree = isolate(List.of("a.b.Use"));
        assertDecisionTable(tree);
        // the registering utility is a stub here: its factory stays 'return null;' -- it returns a parameter
        assertTrue(tree.get("p/q/Registry.java").contains("return null;"));
        assertCompiles(tree);
    }

    @DisplayName("the same with the utility carried verbatim: its list's add is then the JDK's, with a summary")
    @Test
    public void inheritedMembersWithTheUtilityKept() throws IOException {
        Map<String, String> tree = isolate(List.of("a.b.Use", "p.q.Registry"));
        assertDecisionTable(tree);
        assertTrue(tree.get("p/q/Registry.java").contains("items.add(c); return c;"));
        assertCompiles(tree);
    }

    private static void assertDecisionTable(Map<String, String> tree) {
        String myList = tree.get("p/q/MyList.java");
        assertTrue(myList.contains("extends java.util.ArrayList<I>") || myList.contains("extends ArrayList<I>"), myList);
        assertNotDeclared(myList, "add");
        assertNotDeclared(myList, "get");
        assertNotDeclared(myList, "toString");
        assertNotDeclared(myList, "clone");       // java.util.ArrayList.clone() is public already
        assertDeclared(myList, "sizeTwice");      // not inherited at all
        String myStream = tree.get("p/q/MyStream.java");
        assertDeclared(myStream, "read");         // abstract in InputStream: a class stub owes it
        assertDeclared(myStream, "close");        // InputStream.close() throws IOException; this one does not
        assertDeclared(myStream, "clone");        // widens Object's protected clone() to public
        String myItems = tree.get("p/q/MyItems.java");
        assertNotDeclared(myItems, "add");        // an interface inherits the abstract method
        assertDeclared(myItems, "extra");
    }

    private static void assertDeclared(String unit, String name) {
        assertTrue(declares(unit, name), () -> "expected a declaration of " + name + " in\n" + unit);
    }

    private static void assertNotDeclared(String unit, String name) {
        assertFalse(declares(unit, name), () -> "expected NO declaration of " + name + " in\n" + unit);
    }

    /** A method declaration: a return type, the name, a parameter list, then a body, a semicolon or 'throws'. */
    private static boolean declares(String unit, String name) {
        return Pattern.compile("[\\w<>\\[\\],]+\\s+" + name + "\\([^)]*\\)\\s*(\\{|;|throws)").matcher(unit).find();
    }

    // -- the same harness as TestIsolateClass4Compiles, for a group of isolated types --------------------------

    private Map<String, String> isolate(List<String> fqns) {
        var parsed = javaInspector.parse(SOURCES,
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        List<TypeInfo> types = fqns.stream().map(fqn -> parsed.parseResult().findType(fqn)).toList();
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
}
