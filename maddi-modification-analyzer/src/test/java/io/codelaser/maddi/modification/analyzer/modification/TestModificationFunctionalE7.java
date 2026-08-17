package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAN-modification-reachability §9 correction 2 / §11 ownership split: E7 characterization.
 * <p>
 * Lambda/method-reference modification travels through the FunctionalInterfaceVariable's captured
 * Result (result().modified(), surfaced as extraModified in LinkAppliedFunctionalInterface), NOT
 * through parameter links — so it is invisible to reachability edges E1 (argument links) and E6
 * (overrides). These tests pin the shapes the dedicated E7 edge class must reproduce: what the
 * captured-Result path carries today (preserve), and where it stops (fix).
 */
public class TestModificationFunctionalE7 extends CommonTest {

    // ------------------------------------------------------------------ shape 1: captured field, lambda

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                interface Op { int apply(String s); }
                int j;
                int go(String in) {
                    return run(in, t -> { j = t.length(); return j; });
                }
                int run(String s, Op op) {
                    return op.apply(s);
                }
            }
            """;

    @DisplayName("E7 shape 1: lambda (not MR) modifies captured field, custom FI, direct argument")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        MethodInfo run = X.findUniqueMethod("run", 2);
        MethodInfo go = X.findUniqueMethod("go", 1);

        // the callee that merely applies the SAM is not itself modifying; applying the SAM marks
        // the FI parameter's object graph modified
        assertTrue(run.isNonModifying());
        assertEquals("a.b.X.run(String,a.b.X.Op):1:op", mlv(run).sortedModifiedString());
        // the captured-field write inside the lambda implicates go's receiver, at the site where
        // the lambda is created and passed (creation-site attribution)
        assertTrue(go.isModifying());
        assertEquals("$_fi0, this", mlv(go).sortedModifiedString());
        assertFalse(X.getFieldByName("j", true).isUnmodified());
    }

    // ------------------------------------------------------------------ shape 2: captured parameter

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                interface Op { int apply(String s); }
                int go(Set<Integer> out, String in) {
                    return run(in, t -> { out.add(t.length()); return t.length(); });
                }
                int run(String s, Op op) {
                    return op.apply(s);
                }
            }
            """;

    @DisplayName("E7 shape 2: lambda modifies captured parameter of the enclosing method")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        MethodInfo run = X.findUniqueMethod("run", 2);
        MethodInfo go = X.findUniqueMethod("go", 2);

        assertTrue(run.isNonModifying());
        assertEquals("$_fi0, a.b.X.go(java.util.Set,String):0:out", mlv(go).sortedModifiedString());
        // the receiver of go is untouched; the captured parameter carries the modification
        assertTrue(go.isNonModifying());
        assertTrue(go.parameters().getFirst().isModified(), "go:0:out must be modified");
        assertTrue(go.parameters().get(1).isUnmodified(), "go:1:in stays unmodified");
    }

    // ------------------------------------------------------------------ shape 3: captured local (negative)

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class X {
                interface Op { int apply(String s); }
                int go(String in) {
                    Set<Integer> local = new HashSet<>();
                    run(in, t -> { local.add(t.length()); return local.size(); });
                    return local.size();
                }
                int run(String s, Op op) {
                    return op.apply(s);
                }
            }
            """;

    @DisplayName("E7 shape 3: lambda modifies a captured local only — nothing escapes")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        MethodInfo run = X.findUniqueMethod("run", 2);
        MethodInfo go = X.findUniqueMethod("go", 1);

        assertTrue(run.isNonModifying());
        // only the fiv itself is marked; the captured local does not escape
        assertEquals("$_fi1", mlv(go).sortedModifiedString());
        // the modified object is method-local: no false positive on go or its parameter
        assertTrue(go.isNonModifying());
        assertTrue(go.parameters().getFirst().isUnmodified());
    }

    // ------------------------------------------------------------------ shape 4: one forwarding hop

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            public class X {
                @FunctionalInterface
                public interface ThrowingFunction {
                    void apply(TryData o) throws Throwable;
                }
                public interface TryData {
                    ThrowingFunction throwingFunction();
                }
                public static class TryDataImpl implements TryData {
                    private final ThrowingFunction throwingFunction;
                    public TryDataImpl(ThrowingFunction throwingFunction) {
                        this.throwingFunction = throwingFunction;
                    }
                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }
                }
                public static void run(TryData td) {
                    try {
                        td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        throw new UnsupportedOperationException();
                    }
                }
                public static void outer(TryData td) {
                    run(td);
                }
                private Set<Integer> someSet = new HashSet<>();
                public void method() {
                    TryData td = new TryDataImpl(this::methodBody);
                    outer(td);
                }
                private void methodBody(TryData tryData) {
                    this.someSet.add(5);
                }
            }
            """;

    @DisplayName("E7 shape 4: captured-Result modification across one forwarding hop")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT4);
        List<Info> ao = prepWork(X);
        analyzer.go(ao, 7);

        MethodInfo run = X.findUniqueMethod("run", 1);
        MethodInfo outer = X.findUniqueMethod("outer", 1);
        MethodInfo method = X.findUniqueMethod("method", 0);
        // without @GetSet the application is opaque: run conservatively marks its whole td
        // parameter modified; outer inherits td-modified via argument links; at method, the
        // modified td holds the fiv, whose captured Result surfaces this.someSet
        assertEquals("a.b.X.run(a.b.X.TryData):0:td, run", mlv(run).sortedModifiedString());
        assertEquals("a.b.X.outer(a.b.X.TryData):0:td", mlv(outer).sortedModifiedString());
        assertTrue(method.isModifying(), "method must be modifying: its lambda writes this.someSet");
        assertEquals("this, this.someSet", mlv(method).sortedModifiedString());
        assertFalse(X.getFieldByName("someSet", true).isUnmodified(), "someSet must be modified");
    }

    // ------------------------------------------------------------------ shape 5: external application site

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            class X {
                Set<Integer> someSet = new HashSet<>();
                void method(List<String> strings) {
                    strings.forEach(s -> someSet.add(s.length()));
                }
            }
            """;

    @DisplayName("E7 shape 5: captured-field modification, applied inside an annotated (external) method")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT5);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        MethodInfo method = X.findUniqueMethod("method", 1);

        // forEach's body is external; the fiv's captured Result is the only carrier
        assertTrue(method.isModifying(), "method must be modifying: the consumer writes this.someSet");
        assertEquals("this, this.someSet", mlv(method).sortedModifiedString());
        assertFalse(X.getFieldByName("someSet", true).isUnmodified(), "someSet must be modified");
        assertTrue(method.parameters().getFirst().isUnmodified(), "iterating does not modify strings");
    }

    // ------------------------------------------------------------------ shape 6: precise path + forwarding hop

    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.method.GetSet;
            import java.util.HashSet;
            import java.util.Set;
            public class X {
                @FunctionalInterface
                public interface ThrowingFunction {
                    void apply(@Independent(hc = true) @Modified TryData o) throws Throwable;
                }
                public interface TryData {
                    @GetSet
                    @NotModified
                    ThrowingFunction throwingFunction();
                }
                public static void run(TryData td) {
                    try {
                        td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        throw new UnsupportedOperationException();
                    }
                }
                public static void outer(TryData td) {
                    run(td);
                }
                public static class TryDataImpl implements TryData {
                    private final ThrowingFunction throwingFunction;
                    public TryDataImpl(ThrowingFunction throwingFunction) {
                        this.throwingFunction = throwingFunction;
                    }
                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }
                }
                private Set<Integer> someSet = new HashSet<>();
                public void method() {
                    TryData td = new TryDataImpl(this::methodBody);
                    outer(td);
                }
                private void methodBody(TryData tryData) {
                    this.someSet.add(5);
                }
            }
            """;

    @DisplayName("E7 shape 6: precise (@GetSet/@NotModified) captured-Result path across a forwarding hop")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT6);
        List<Info> ao = prepWork(X);
        analyzer.go(ao, 7);

        MethodInfo run = X.findUniqueMethod("run", 1);
        MethodInfo outer = X.findUniqueMethod("outer", 1);
        MethodInfo method = X.findUniqueMethod("method", 0);
        // with annotations the application is resolved precisely: run's mlv carries the $_afi
        // marker on td.throwingFunction, and apply's @Modified annotation marks td itself
        assertEquals("[0:td.throwingFunction*\u2197$_afi0] --> -", mlv(run).toString());
        assertEquals("""
                a.b.X.ThrowingFunction.apply(a.b.X.TryData):0:o, a.b.X.run(a.b.X.TryData):0:td, \
                run, td.throwingFunction""", mlv(run).sortedModifiedString());
        // the marker survives the outer() hop and still expands at method()
        assertTrue(method.isModifying(), "method must be modifying: its lambda writes this.someSet");
        assertEquals("this, this.someSet", mlv(method).sortedModifiedString());
        assertFalse(X.getFieldByName("someSet", true).isUnmodified(), "someSet must be modified");
    }

    // ------------------------------------------------------------------ shape 7: callback registry (field-stored FI)

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class X {
                interface Op { int apply(String s); }
                private Op callback;
                private Set<Integer> someSet = new HashSet<>();
                void register() {
                    this.callback = t -> { someSet.add(t.length()); return t.length(); };
                }
                int trigger(String s) {
                    return callback.apply(s);
                }
                int method(String in) {
                    register();
                    return trigger(in);
                }
            }
            """;

    @DisplayName("E7 shape 7: FI stored in a field by one method, applied by another")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT7);
        List<Info> ao = prepWork(X);
        analyzer.go(ao, 7);

        MethodInfo register = X.findUniqueMethod("register", 0);
        MethodInfo trigger = X.findUniqueMethod("trigger", 1);
        MethodInfo method = X.findUniqueMethod("method", 1);
        // ATTRIBUTION SEMANTICS, load-bearing for E7: the captured someSet write is charged at the
        // CREATION site (register), not at the application site (trigger). trigger is modifying for
        // a different reason: applying an opaque field-held FI marks this.callback's object graph
        // modified. E7 must reproduce this creation-site attribution for field-stored callbacks —
        // an application-site-only edge class would leave register's someSet write unattributed.
        assertTrue(register.isModifying());
        assertEquals("this, this.someSet", mlv(register).sortedModifiedString());
        assertTrue(trigger.isModifying());
        assertEquals("this, this.callback", mlv(trigger).sortedModifiedString());
        assertTrue(method.isModifying());
        assertFalse(X.getFieldByName("someSet", true).isUnmodified(), "someSet must be modified");
    }

    // ------------------------------------------------------------------ shape 8: created but never applied

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            class X {
                interface Op { int apply(String s); }
                int j;
                int go(String in) {
                    return run(in, t -> { j = t.length(); return j; });
                }
                int run(String s, Op op) {
                    return s.length();
                }
            }
            """;

    @DisplayName("E7 shape 8: lambda with captured-field write passed but never applied")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT8);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        MethodInfo run = X.findUniqueMethod("run", 2);
        MethodInfo go = X.findUniqueMethod("go", 1);
        assertTrue(run.isNonModifying());
        assertEquals("", mlv(run).sortedModifiedString());
        // ANSWER: attribution is EAGER. run provably ignores op (empty modified set), yet go is
        // still charged with the lambda's captured write — passing a capturing lambda implicates
        // its captured targets unconditionally, whether or not any application site is reachable.
        // Sound over-approximation; E7 may therefore encode unconditional creation-site edges.
        assertTrue(go.isModifying());
        // note: no $_fi0 here, unlike shape 1 — the fiv is only marked when actually applied
        assertEquals("this", mlv(go).sortedModifiedString());
        assertFalse(X.getFieldByName("j", true).isUnmodified());
    }

    // ------------------------------------------------------------------

    private static MethodLinkedVariables mlv(MethodInfo mi) {
        return mi.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
    }
}
