package org.e2immu.analyzer.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.common.util.IsolateClass;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Class isolates that <b>parse</b> and do not <b>compile</b>.
 * <p>
 * Every case here was found by isolating a real fernflower type and compiling the result by hand, after
 * {@code debug.isolateClass} was added to the DSL. What made them invisible until then is not that maddi's front
 * end is weaker than javac — it <i>is</i> javac, driven through {@code task.parse()} then {@code task.analyze()},
 * so attribution runs and these are javac's own messages. It is that the existing isolate tests re-parse with
 * <b>default</b> {@link JavaInspector.ParseOptions}, where {@code failFast} is false; javac's errors are then
 * logged at INFO and the parse is allowed to succeed. And they do not show up as
 * {@code parseExceptions()} either, so a test asserting on those sees nothing.
 * <p>
 * Hence {@code setFailFast(true)} below, and nothing more elaborate: it is the switch that turns any javac error
 * into a {@code CompilationProblems}, which names each error with its file, line and column. Running javac
 * separately would only recompute a verdict maddi already has.
 * <p>
 * The first three fixtures are reductions of fernflower types, small enough to read:
 * <ul>
 *   <li>{@code SwitchPatternHelper} statically imports {@code ClassNameConstants.JAVA_UTIL_OBJECTS} — an
 *       <b>interface</b> field, hence implicitly static;</li>
 *   <li>{@code ExprProcessor} calls {@code pop()} on an {@code ExpressionStack extends ListStack<Exprent>};</li>
 *   <li>{@code ClassWriter} switches over its own private {@code enum MType}.</li>
 * </ul>
 * The rest are reductions of the <b>hundred-class closed-core corpus</b>, one per root cause of the 34 trees that
 * did not compile once those three were fixed: all 295 javac errors of those trees came down to twelve causes, and
 * each of the drivers below is one of them, named after the corpus symptom it reproduces. A driver here is worth
 * more than the corpus run it came from — the corpus needs a commercial project and 16G, this needs neither — but
 * it is the corpus that says which twelve to write.
 */
public class TestIsolateClass4Compiles {
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

    /**
     * Isolates {@code fqn} out of {@code sources}, taking each kept member's verbatim text straight out of the
     * fixture: the isolate's whole point is that the bodies are copied, so the test has to supply them the way a
     * real driver does, from the source it parsed.
     */
    private Map<String, String> isolate(Map<String, String> sources, String fqn) {
        var parsed = javaInspector.parse(sources,
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        TypeInfo type = parsed.parseResult().findType(fqn);
        IsolateClass.Result r = isolateClass.isolate(type);
        Map<MethodInfo, String> memberSources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            memberSources.put(original, verbatim(sources.get(original.primaryType().fullyQualifiedName()), original));
        }
        Map<String, String> tree = isolateClass.print(r, memberSources);
        tree.forEach((path, source) -> System.out.println("===== " + path + "\n" + source));
        return tree;
    }

    /**
     * Re-parses the emitted tree with fail-fast on, so that any javac error in it is a thrown
     * {@code CompilationProblems} rather than a log line. The type-set check stays: a unit whose symbols cannot be
     * resolved is DROPPED with a warning, and dropping is not an error, so it would otherwise pass silently.
     */
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

    @Language("java")
    private static final String CONSTANTS = """
            package p.q;
            public interface Constants {
                String OBJECTS = "java/util/Objects";
            }
            """;

    @Language("java")
    private static final String USES_STATIC_IMPORT = """
            package a.b;
            import static p.q.Constants.OBJECTS;
            public class UsesStaticImport {
                public static boolean isObjects(String name) {
                    return OBJECTS.equals(name);
                }
            }
            """;

    @DisplayName("an implicitly-static interface field, statically imported, stays static")
    @Test
    public void staticallyImportedInterfaceField() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Constants", CONSTANTS,
                "a.b.UsesStaticImport", USES_STATIC_IMPORT), "a.b.UsesStaticImport");
        // An interface field is implicitly static and maddi does not always report it so. Taken at its word, the
        // field is neither recorded as a static import nor stubbed static: it landed as a plain instance field ON
        // THE ISOLATED TYPE ("public String JAVA_UTIL_OBJECTS;"), and the verbatim body then read it from a static
        // method -- "non-static variable JAVA_UTIL_OBJECTS cannot be referenced from a static context".
        //
        // FIXED, and the fix was not in IsolateClass. This case used to be JVM-STATE DEPENDENT -- reproducing on
        // its own, passing in a warm runtime -- because the two inspection paths disagreed about the field:
        // ClassSymbolScanner reads javac's symbol flags, where the implicit static is present, while
        // ScanCompilationUnit read the written JCModifiers, where it is not. Whichever had run for that interface
        // in that JVM decided the answer. ScanCompilationUnit.field now applies JLS 9.3 itself.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String LIST_STACK = """
            package p.q;
            import java.util.ArrayList;
            public class ListStack<T> extends ArrayList<T> {
                public T pop() {
                    return remove(size() - 1);
                }
            }
            """;

    @Language("java")
    private static final String ITEM = """
            package p.q;
            public class Item {
                public int weight;
            }
            """;

    @Language("java")
    private static final String ITEM_STACK = """
            package p.q;
            public class ItemStack extends ListStack<Item> {
            }
            """;

    @Language("java")
    private static final String USES_GENERIC_SUPERTYPE = """
            package a.b;
            import p.q.Item;
            import p.q.ItemStack;
            public class UsesGenericSupertype {
                public int total(ItemStack stack) {
                    Item first = stack.pop();
                    Item second = stack.pop();
                    return first.weight + second.weight;
                }
            }
            """;

    @DisplayName("a method inherited from a generic supertype keeps its type argument")
    @Test
    public void inheritedGenericMethod() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.ListStack", LIST_STACK, "p.q.Item", ITEM,
                "p.q.ItemStack", ITEM_STACK, "a.b.UsesGenericSupertype", USES_GENERIC_SUPERTYPE),
                "a.b.UsesGenericSupertype");
        // 'pop()' is declared on ListStack<T> and returns T; through 'ItemStack extends ListStack<Item>' that is
        // an Item. Stubbing it on the SCOPE type with an erased return gave "public Object pop()" on ItemStack,
        // and the verbatim 'Item first = stack.pop();' then failed with
        // "incompatible types: Object cannot be converted to Item".
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String USES_ENUM = """
            package a.b;
            public class UsesEnum {
                private enum Kind {CLASS, FIELD, METHOD}

                private static String describe(Kind kind) {
                    switch (kind) {
                        case CLASS:
                            return "class";
                        case FIELD:
                            return "field";
                        default:
                            return "method";
                    }
                }

                public String describeClass() {
                    return describe(Kind.CLASS);
                }
            }
            """;

    @DisplayName("an enum is stubbed as an enum, so a switch over it compiles")
    @Test
    public void switchOverEnum() throws IOException {
        Map<String, String> tree = isolate(Map.of("a.b.UsesEnum", USES_ENUM), "a.b.UsesEnum");
        // The stub came out as "public static class Kind { public static Kind CLASS; ... }" -- a class with static
        // fields, which is enough for 'Kind.CLASS' and not enough for 'case CLASS:':
        // "pattern or enum constant required".
        assertCompiles(tree);
    }

    // ------------------------------------ from the closed-core corpus ------------------------------------

    @Language("java")
    private static final String OWN_CONSTANTS = """
            package a.b;
            public class UsesOwnConstants {
                private static final int FIRST = 1;
                private static final int SECOND = 2;
                private static final String NAME = "n";
                private static final java.util.List<String> NOT_A_CONSTANT = new java.util.ArrayList<>();

                public String pick(int i) {
                    switch (i) {
                        case FIRST:
                            return "first";
                        case SECOND:
                            return "second";
                        default:
                            return "?";
                    }
                }

                public boolean named(String s) {
                    switch (s) {
                        case NAME:
                            return true;
                        default:
                            return false;
                    }
                }

                public int size() {
                    return NOT_A_CONSTANT.size();
                }
            }
            """;

    @DisplayName("the isolated type's own constants keep 'final' and their value")
    @Test
    public void ownConstantsAsCaseLabels() throws IOException {
        Map<String, String> tree = isolate(Map.of("a.b.UsesOwnConstants", OWN_CONSTANTS), "a.b.UsesOwnConstants");
        // The isolated type's own fields were reproduced with 'static' but never 'final', and their initializers
        // were dropped wholesale -- so 'private static final int FIRST = 1' came out as 'static int FIRST;' and its
        // own 'case FIRST:' was "constant expression required". 5 corpus trees, 45 errors, the biggest single cause
        // by error count after the annotation defaults. NOT_A_CONSTANT is the other half of the rule: its
        // initializer reaches into the project, so it must still be dropped -- and then 'final' must be dropped
        // with it, or the field is "not initialized".
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String BASE_WITH_PROTECTED_STATIC = """
            package p.q;
            public class Base {
                protected static String helper(String in) {
                    return in;
                }

                protected static final int LIMIT = 7;
            }
            """;

    @Language("java")
    private static final String INHERITS_STATIC = """
            package a.b;
            import p.q.Base;
            public class InheritsStatic extends Base {
                public String use(String s) {
                    return helper(s) + LIMIT;
                }
            }
            """;

    @DisplayName("an unqualified call to an INHERITED static member needs no static import")
    @Test
    public void inheritedStaticMember() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Base", BASE_WITH_PROTECTED_STATIC,
                "a.b.InheritsStatic", INHERITS_STATIC), "a.b.InheritsStatic");
        // 'helper(s)' has no scope and is declared by another type, so it was recorded as a static import --
        // 'import static p.q.Base.helper;'. But the isolated type EXTENDS Base, so the member is already in scope,
        // and the import does not even compile: a single-static-import must name an ACCESSIBLE member, and
        // 'protected' does not cross the package boundary. "cannot find symbol: static helper".
        // 10 corpus trees, the largest cause of the 34, and the only one that is a wrong decision rather than a
        // missing one (axis2's Stub.getFactory, closed-core's BaseSessionBean.checkAddRestrictedMessage).
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String OUTER_WITH_INNER = """
            package p.q;
            public class Outer {
                public class Inner {
                    public int value;
                }
            }
            """;

    @Language("java")
    private static final String USES_INNER = """
            package a.b;
            import p.q.Outer;
            public class UsesInner {
                public int get(Outer outer) {
                    Outer.Inner inner = outer.new Inner();
                    return inner.value;
                }
            }
            """;

    @DisplayName("an inner (non-static) nested type is not stubbed static")
    @Test
    public void qualifiedNewOfInnerClass() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Outer", OUTER_WITH_INNER, "a.b.UsesInner", USES_INNER),
                "a.b.UsesInner");
        // Every nested stub is made 'static', so that it can be named without an enclosing instance. For a type
        // that really is static that is right; for an INNER class the verbatim text writes 'outer.new Inner()',
        // and javac says "qualified new of static class". 5 corpus trees.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String MARKER = """
            package p.q;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Marker {
                int level() default 0;

                String note() default "";

                Class<? extends Throwable> expected() default Throwable.class;

                String[] tags() default {};
            }
            """;

    @Language("java")
    private static final String USES_ANNOTATION = """
            package a.b;
            import java.io.IOException;
            import p.q.Marker;
            public class UsesAnnotation {
                @Marker(level = 3, expected = IOException.class, tags = {"slow"})
                public void explicit() {
                }

                @Marker
                public void byDefault() {
                }
            }
            """;

    @DisplayName("a stubbed annotation attribute keeps a default, so a bare use of it compiles")
    @Test
    public void annotationAttributeDefault() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Marker", MARKER, "a.b.UsesAnnotation", USES_ANNOTATION),
                "a.b.UsesAnnotation");
        // The attribute is stubbed because ONE use names it; the stub then reads 'int level();' with no default,
        // and every other use -- a bare '@Marker' -- is "annotation @Marker is missing a default value for the
        // element 'level'". Two corpus trees and 200 errors, both unit tests using JUnit 4's @Test(expected=...).
        //
        // 'expected' is that corpus attribute, and it is here because the value has to be of the right TYPE, not
        // merely present: a synthesized 'Class' default must satisfy the '? extends Throwable' bound, and the
        // first attempt emitted 'Class.class' -- "incompatible types: Class<Class> cannot be converted to
        // Class<? extends Throwable>", which turned 200 errors in those two trees into 2. An int and a String
        // alone did not exercise that.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String ABSTRACT_SUPERCLASS_STUB = """
            package p.q;
            import java.io.IOException;
            import java.io.OutputStream;
            public class CountingStream extends OutputStream {
                public int count;

                @Override
                public void write(int b) throws IOException {
                    count++;
                }
            }
            """;

    @Language("java")
    private static final String USES_ABSTRACT_SUPERCLASS = """
            package a.b;
            import java.io.IOException;
            import p.q.CountingStream;
            public class UsesAbstractSuperclass {
                public int go(CountingStream stream) throws IOException {
                    stream.flush();
                    return stream.count;
                }
            }
            """;

    @DisplayName("a stub inherits an abstract method from an abstract CLASS, and must implement it")
    @Test
    public void abstractMethodOfAbstractSuperclass() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.CountingStream", ABSTRACT_SUPERCLASS_STUB,
                "a.b.UsesAbstractSuperclass", USES_ABSTRACT_SUPERCLASS), "a.b.UsesAbstractSuperclass");
        // addDummyInterfaceMethods walks interfacesImplemented() and never the parent class, so the stub is
        // literally 'public class CountingStream extends OutputStream { }' and javac says "is not abstract and does
        // not override abstract method write(int) in OutputStream". 8 corpus trees: OutputStream.write(int),
        // InputStream.read(), Transformer.getErrorListener().
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String APPENDER = """
            package p.q;
            import java.io.IOException;
            public class Appender implements Appendable {
                @Override
                public Appendable append(CharSequence csq) throws IOException {
                    return this;
                }

                @Override
                public Appendable append(CharSequence csq, int start, int end) throws IOException {
                    return this;
                }

                @Override
                public Appendable append(char c) throws IOException {
                    return this;
                }
            }
            """;

    @Language("java")
    private static final String USES_OVERLOADS = """
            package a.b;
            import java.io.IOException;
            import p.q.Appender;
            public class UsesOverloads {
                public void go(Appender appender) throws IOException {
                    appender.append("x");
                }
            }
            """;

    @DisplayName("two abstract methods of the same arity both get a dummy implementation")
    @Test
    public void sameArityOverloads() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Appender", APPENDER, "a.b.UsesOverloads", USES_OVERLOADS),
                "a.b.UsesOverloads");
        // The dummy implementations are collected into a map keyed by name + '/' + parameter COUNT, so of two
        // same-arity overloads only the first is ever generated. 6 corpus trees, always the same pair:
        // org.xml.sax.XMLReader declares parse(InputSource) and parse(String), and the stub got only the first.
        //
        // The supertype has to be a JDK interface for this to bite, and that is not incidental: a STUBBED
        // interface's methods are made 'default' (they keep a body, so implementors need not override), and only
        // the ones actually reached are stubbed at all -- so there is never anything to implement. Every A-family
        // case in the corpus is against a JDK supertype for exactly this reason. java.lang.Appendable is the
        // smallest one with two same-arity abstract methods: append(CharSequence) and append(char).
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String RAW_SPLITERATOR = """
            package p.q;
            import java.util.Spliterator;
            import java.util.function.Consumer;
            public class RawSpliterator implements Spliterator {
                @Override
                public boolean tryAdvance(Consumer action) {
                    return false;
                }

                @Override
                public Spliterator trySplit() {
                    return null;
                }

                @Override
                public long estimateSize() {
                    return 0L;
                }

                @Override
                public int characteristics() {
                    return 0;
                }
            }
            """;

    @Language("java")
    private static final String USES_RAW_SPLITERATOR = """
            package a.b;
            import p.q.RawSpliterator;
            public class UsesRawSpliterator {
                public long go(RawSpliterator spliterator) {
                    return spliterator.estimateSize();
                }
            }
            """;

    @DisplayName("a dummy implementation for a RAW supertype is fully erased, wildcards included")
    @Test
    public void rawSupertypeWildcard() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.RawSpliterator", RAW_SPLITERATOR,
                "a.b.UsesRawSpliterator", USES_RAW_SPLITERATOR), "a.b.UsesRawSpliterator");
        // 'implements Spliterator' is RAW, so what the class inherits is the erasure 'tryAdvance(Consumer)'.
        // eraseOutOfScope substitutes the out-of-scope T by its bound but KEEPS the wildcard, giving
        // 'tryAdvance(Consumer<? super Object>)' -- which neither overrides nor implements it, so javac reports
        // both "is not abstract and does not override" and "name clash ... same erasure". One corpus tree
        // (commons-collections AbstractMapDecorator and MultiValueMap implementing raw java.util.Map), three of
        // its five errors. Spliterator is java.util's smallest interface with a wildcard in an abstract method;
        // only estimateSize() is called here, so the other three have to come from the dummy pass.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String BOXED_CONSTANT = """
            package p.q;
            public class Ids {
                public static final Long PANEL = 49L;
            }
            """;

    @Language("java")
    private static final String USES_BOXED_CONSTANT = """
            package a.b;
            import p.q.Ids;
            public class UsesBoxedConstant {
                public Long get() {
                    return Ids.PANEL;
                }
            }
            """;

    @DisplayName("a stubbed BOXED numeric constant is not handed a bare int")
    @Test
    public void boxedNumericConstant() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Ids", BOXED_CONSTANT, "a.b.UsesBoxedConstant",
                USES_BOXED_CONSTANT), "a.b.UsesBoxedConstant");
        // Every numeric constant is given a distinct int value, so that it can serve as a switch 'case' label.
        // TypeInfo.isNumeric() is true of the BOXED types as well, so the stub came out as
        // 'public static final Long PANEL = 49;' -- "incompatible types: int cannot be converted to Long".
        // A boxed constant cannot be a case label anyway. One corpus tree, one error.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String POLICY = """
            package p.q;
            public interface Policy {
                int weight();
            }
            """;

    @Language("java")
    private static final String USES_INTERFACE_ARRAY = """
            package a.b;
            import java.util.List;
            import p.q.Policy;
            public class UsesInterfaceArray {
                public Policy[] make(List<Object> candidates) {
                    return candidates.stream().filter(c -> Policy.class.isInstance(c)).toArray(Policy[]::new);
                }
            }
            """;

    @DisplayName("'I[]::new' does not give the interface stub a constructor")
    @Test
    public void interfaceArrayConstructorReference() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Policy", POLICY, "a.b.UsesInterfaceArray",
                USES_INTERFACE_ARRAY), "a.b.UsesInterfaceArray");
        // An array creation carries a SYNTHETIC constructor, one int parameter per dimension, and stubbing it
        // writes 'Policy(int dim0) { }' into the type -- which for an interface is not even syntactically Java:
        // "<identifier> expected". 'new Policy[3]' is guarded against exactly that; the ARRAY CONSTRUCTOR
        // REFERENCE 'Policy[]::new' reaches ensureMethodInfo down the MethodReference branch, which has no such
        // guard. One corpus tree (RuleChecker.getRuleCheckerPolicies, ...toArray(ICheckRMTaskPolicy[]::new)).
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String NODE = """
            package p.q;
            import java.util.Iterator;
            public interface Node extends Iterator<String> {
            }
            """;

    @Language("java")
    private static final String EXPR = """
            package p.q;
            public class Expr implements Node {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public String next() {
                    return null;
                }
            }
            """;

    @Language("java")
    private static final String USES_EXPR = """
            package a.b;
            import p.q.Expr;
            public class UsesExpr {
                public boolean go(Expr expr) {
                    return expr.hasNext();
                }
            }
            """;

    @DisplayName("a JDK interface reached through a STUBBED one still has to be implemented")
    @Test
    public void jdkInterfaceThroughStubbedInterface() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Node", NODE, "p.q.Expr", EXPR, "a.b.UsesExpr", USES_EXPR),
                "a.b.UsesExpr");
        // 'class Expr implements Node', where the stub of Node still 'extends Iterator<String>' -- a JDK
        // interface, kept as itself, so its abstract methods really are inherited and really are unimplemented,
        // two levels down. Only hasNext() is called, so next() has to come from the dummy pass, with T bound to
        // String along the way.
        //
        // The corpus case is xalan's 'Expression implements ExpressionNode', where ExpressionNode extends
        // javax.xml.transform.SourceLocator. It is written with java.util.Iterator instead because
        // javax.xml.transform is not part of java.base: this harness would stub it into the emitted tree, and a
        // compilation unit declaring 'package javax.xml.transform' is rejected outright -- "package exists in
        // another module: java.xml". Worth knowing, because it is a real hazard for any isolate that reaches a
        // JDK package outside the modules the run configures: the type is then not partOfJdk, so it is stubbed,
        // and the tree cannot compile no matter what the stub contains.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String FACTORY = """
            package p.q;
            public class Factory<A extends Base<?>> {
                public A create() {
                    return null;
                }
            }
            """;

    @Language("java")
    private static final String BASE_WITH_GENERIC_METHOD = """
            package p.q;
            public interface Base<SELF extends Base<SELF>> {
                <A extends Base<?>> A as(Factory<A> factory);
            }
            """;

    @Language("java")
    private static final String BASE_IMPL = """
            package p.q;
            public class BaseImpl implements Base<BaseImpl> {
                @Override
                public <A extends Base<?>> A as(Factory<A> factory) {
                    return factory.create();
                }
            }
            """;

    @Language("java")
    private static final String USES_GENERIC_METHOD = """
            package a.b;
            import p.q.BaseImpl;
            public class UsesGenericMethod {
                public String describe(BaseImpl impl) {
                    return impl.toString();
                }
            }
            """;

    @DisplayName("a dummy implementation keeps the abstract method's OWN type parameters")
    @Test
    public void dummyKeepsMethodTypeParameters() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Factory", FACTORY, "p.q.Base", BASE_WITH_GENERIC_METHOD,
                "p.q.BaseImpl", BASE_IMPL, "a.b.UsesGenericMethod", USES_GENERIC_METHOD), "a.b.UsesGenericMethod");
        // A dummy implementation reproduced the signature but not the method's own <A>, so A was out of scope
        // where the dummy is declared and eraseOutOfScope replaced it by its bound -- dropping that bound's type
        // arguments in turn, which yields a RAW 'Base' as the type argument for 'A extends Base<?>':
        // "type argument Base is not within bounds of type-variable A". assertj's
        // '<ASSERT extends AbstractAssert<?,?>> ASSERT asInstanceOf(InstanceOfAssertFactory<?, ASSERT>)',
        // 3 corpus trees -- and note the isolated type never calls it: the dummy pass is the only thing that
        // brings this signature into existence.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String CUSTOM_CLONEABLE = """
            package p.q;
            public interface CustomCloneable extends Cloneable {
                Object clone() throws CloneNotSupportedException;
            }
            """;

    @Language("java")
    private static final String ROLE_INFO = """
            package p.q;
            public class RoleInfo implements CustomCloneable {
                public String name;

                @Override
                public Object clone() throws CloneNotSupportedException {
                    return null;
                }
            }
            """;

    @Language("java")
    private static final String USES_CLONEABLE = """
            package a.b;
            import p.q.RoleInfo;
            public class UsesCloneable {
                public Object go(RoleInfo info) {
                    try {
                        return info.clone();
                    } catch (CloneNotSupportedException e) {
                        return info.name;
                    }
                }
            }
            """;

    @DisplayName("the SAM of an interface stub keeps its throws clause")
    @Test
    public void samKeepsThrowsClause() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.CustomCloneable", CUSTOM_CLONEABLE, "p.q.RoleInfo", ROLE_INFO,
                "a.b.UsesCloneable", USES_CLONEABLE), "a.b.UsesCloneable");
        // The verbatim text CATCHES the exception, and catching one that cannot be thrown is itself an error:
        // "exception CloneNotSupportedException is never thrown in body of corresponding try statement". So an
        // interface stub has to keep the throws clause of what it declares.
        //
        // Which is the OPPOSITE of the rule for a dummy implementation -- see the next test -- and that asymmetry
        // is the whole point. Three paths build a method stub (ensureMethodInfo, ensureAbstractMethod for an
        // interface stub's SAM, addDummyImplementation) and each reproduced a different subset of the
        // declaration. Giving them all a throws clause "for consistency" took the corpus from 97 of 100 trees
        // compiling to 9, then to 74, while every unit driver stayed green. What is consistent is the LANGUAGE
        // rule, not the subset: the declaration side must declare at least what the implementation side does, so
        // the interface keeps its exceptions and the implementation narrows.
        assertCompiles(tree);
    }

    // The counterpart rule -- a dummy IMPLEMENTATION declares no throws, because an implementation may narrow and
    // the verbatim call sites must not be made to handle exceptions the original never declared -- has no driver
    // here, deliberately. Two attempts at one did not discriminate: as soon as the isolated code calls the method
    // through the class, ensureMethodInfo stubs it and the dummy pass never fires, so the fixture passes whatever
    // addDummyImplementation does. The evidence for that rule is the corpus (24 trees against 1) and it is
    // recorded where the decision is taken; a green driver that cannot fail would be worse than none.

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String DATA = """
            package p.q;
            public class Data {
                public int weight;
                public String label;
            }
            """;

    @Language("java")
    private static final String USES_LOCAL_CLASS = """
            package a.b;
            import java.util.Comparator;
            import p.q.Data;
            public class UsesLocalClass {
                public int compare(Data a, Data b) {
                    class ByWeight implements Comparator<Data> {
                        @Override
                        public int compare(Data x, Data y) {
                            return x.weight - y.weight;
                        }
                    }
                    return new ByWeight().compare(a, b);
                }
            }
            """;

    @DisplayName("a local class in a kept body has its references stubbed too")
    @Test
    public void localClassInKeptBody() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Data", DATA, "a.b.UsesLocalClass", USES_LOCAL_CLASS),
                "a.b.UsesLocalClass");
        // The visitor descends into an ANONYMOUS class's member bodies deliberately, but there is no case for a
        // LOCAL class declaration, so nothing inside 'class ByWeight { ... }' is walked -- and 'x.weight', reached
        // nowhere else, is never stubbed. The local class itself is verbatim text and needs no stub; what it
        // REFERENCES does. One corpus tree (a Comparator declared inside ContractCtrlBean).
        assertCompiles(tree);
    }

    @Language("java")
    private static final String BASE_WITH_CONSTRUCTOR = """
            package p.q;
            public class BaseUtil {
                private final boolean logging;
                public BaseUtil(boolean logging) {
                    this.logging = logging;
                }
                public static long stateOf(String name) {
                    return name.length();
                }
            }
            """;

    @Language("java")
    private static final String CALLS_SUPER_CONSTRUCTOR = """
            package a.b;
            import p.q.BaseUtil;
            public class CallsSuperConstructor extends BaseUtil {
                public CallsSuperConstructor(boolean enabledLogging) {
                    super(enabledLogging);
                }
                public long state(String name) {
                    return stateOf(name);
                }
            }
            """;

    @Language("java")
    private static final String GENERIC_BASE = """
            package p.q;
            public abstract class Serializer<E> {
                public abstract void write(E element);
            }
            """;

    @Language("java")
    private static final String GENERIC_ABSTRACT = """
            package a.b;
            import p.q.Serializer;
            import java.util.List;
            public abstract class GenericAbstract<T, V, E extends Exception> extends Serializer<T> {
                protected abstract V getValue(T item) throws E;
                protected abstract List<T> derive(List<Long> ids);
                public V first(List<T> items) throws E {
                    return getValue(items.get(0));
                }
            }
            """;

    @DisplayName("an isolated type keeps its own type parameters and its 'abstract'")
    @Test
    public void genericAbstractIsolatedType() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Serializer", GENERIC_BASE,
                "a.b.GenericAbstract", GENERIC_ABSTRACT), "a.b.GenericAbstract");
        // The isolated type's declaration is SYNTHESISED (setTypeNature(class), addTypeModifier(public)), and it
        // used to reproduce neither the type parameters nor 'abstract'. Two distinct failures follow, and they
        // look unrelated until you see the emitted declaration:
        //
        //   'class GenericAbstract extends Serializer<T>'   -- the USE of T survives, coming from the supertype's
        //       own text, while the DECLARATION is gone, so every T/V/E is "cannot find symbol"
        //   'class GenericAbstract { protected abstract V getValue(T); }'  -- "GenericAbstract is not abstract
        //       and does not override abstract method getValue in GenericAbstract", javac naming the type twice
        //
        // Order matters: the parameters are declared before the parent class, the interfaces and the member walk,
        // all three of which can name them. And the bounds go on in a second pass, because a bound may name a
        // sibling ('<T, B extends List<T>>').
        //
        // The largest remaining cluster on the closed-core class-isolate corpus, measured 2026-08-09: five trees
        // of one generic abstract type at ~60 errors each, all of them this.
        assertCompiles(tree);
    }

    @DisplayName("a 'super(...)' in a kept constructor stubs that constructor on the supertype")
    @Test
    public void superConstructorInvocation() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.BaseUtil", BASE_WITH_CONSTRUCTOR,
                "a.b.CallsSuperConstructor", CALLS_SUPER_CONSTRUCTOR), "a.b.CallsSuperConstructor");
        // An ExplicitConstructorInvocation is neither a MethodCall nor a ConstructorCall, and the visitor had no
        // case for it, so nothing reached the supertype's constructor: the stub kept only the members that were
        // CALLED (here 'stateOf'), and with no declared constructor it got the implicit no-arg one. javac then
        // says "constructor BaseUtil in class p.q.BaseUtil cannot be applied to given types; required: no
        // arguments, found: boolean".
        //
        // ⚠ addDefaultConstructorsWhereExtended is the MIRROR IMAGE and does not cover this: it supplies the
        // no-arg constructor a stub needs when it declares others, i.e. it fixes an implicit 'super()' against a
        // stub that has parameters. Neither pass sees the other's case.
        //
        // The dominant cause on the closed-core class-isolate corpus, measured 2026-08-09: 24 of the 54 trees
        // that did not compile, 9 of them subclasses of a single base class, which is the shape this reduces.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String SINK = """
            package p.q;
            import java.io.IOException;
            public interface Sink {
                void write() throws IOException;
            }
            """;

    @Language("java")
    private static final String SINK_BASE = """
            package p.q;
            import java.io.IOException;
            public class SinkBase implements Sink {
                public void write() throws IOException { }
                public void flush() throws IOException { }
            }
            """;

    @Language("java")
    private static final String CALLS_INHERITED_THROWER = """
            package a.b;
            import p.q.SinkBase;
            import java.io.IOException;
            public class CallsInheritedThrower extends SinkBase {
                public void unqualified() {
                    try {
                        write();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                public void qualified() {
                    try {
                        this.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            """;

    @DisplayName("a dummy is not invented for a method the original type declares itself")
    @Test
    public void dummyNeverReplacesADeclarationTheOriginalHas() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Sink", SINK, "p.q.SinkBase", SINK_BASE,
                "a.b.CallsInheritedThrower", CALLS_INHERITED_THROWER), "a.b.CallsInheritedThrower");
        // 'write()' and 'this.flush()' are calls to methods the isolated type INHERITS, so each is stubbed on the
        // isolated type with the original's throws clause. Nothing referenced SinkBase's own members, so its stub
        // is empty, and the dummy pass — reading 'implements Sink' off the ORIGINAL and the method list off the
        // STUB — concludes the obligation is unmet and invents 'public void write() { }'. Deliberately without a
        // throws clause, and rightly so (§6 of docs/isolate-class.md: giving the dummy path the interface's
        // exceptions costs 24 trees). But SinkBase declares that method itself, WITH its exceptions, so the
        // invention contradicts the real thing in the one direction the language forbids:
        //
        //   "write() in a.b.CallsInheritedThrower cannot override write() in p.q.SinkBase;
        //    overridden method does not throw java.io.IOException"
        //
        // The fix is not to reconcile the two declarations but to stop creating the second one: where the original
        // declares the implementation, stub THAT — exceptions, access and all — and invent a dummy only where
        // there is nothing to copy. A dummy is a guess at a declaration; this is the declaration.
        //
        // Four trees of the closed-core class-isolate corpus, measured 2026-08-09: three XML handlers on
        // startDocument()/endDocument(), and the getProperty() tree that §7 had filed as needing a pass over the
        // finished stub graph. It does not need one.
        //
        // ⚠ Note where the error is REPORTED. javac names the nearest subtype that overrides faithfully, which is
        // rarely the type at fault: in three of the four corpus trees the invented dummy sat two classes above the
        // type javac named, and an earlier attempt at this — sending the inherited call to its declaring stub —
        // only moved the message one level up while dropping 15 trees elsewhere.
        assertCompiles(tree);
    }
}
