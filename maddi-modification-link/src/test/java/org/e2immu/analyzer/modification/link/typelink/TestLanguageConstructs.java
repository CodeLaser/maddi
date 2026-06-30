package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises how the link/modification analysis handles a range of Java language constructs (the input is what
 * decides behaviour, so we widen the variety of inputs). Each test pins the computed links for one construct;
 * {@link #forEachOverArrayVsCollection()} additionally flags a real gap.
 */
public class TestLanguageConstructs extends CommonTest {

    private String link(String fqn, String src, String methodName) {
        TypeInfo type = javaInspector.parse(fqn, src);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(type);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        MethodInfo mi = type.methodStream().filter(m -> m.name().equals(methodName)).findFirst()
                .orElseGet(() -> type.constructors().getFirst()); // "<init>" -> the (single) constructor
        MethodLinkedVariables mlv = lc.doMethod(mi);
        return mlv.toString();
    }

    private String modified(String fqn, String src, String methodName) {
        TypeInfo type = javaInspector.parse(fqn, src);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(type);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        MethodInfo mi = type.methodStream().filter(m -> m.name().equals(methodName)).findFirst()
                .orElseGet(() -> type.constructors().getFirst());
        return lc.doMethod(mi).sortedModifiedString();
    }

    @DisplayName("ternary / conditional expression links to both arms")
    @Test
    public void ternary() {
        @Language("java") String src = """
                package a.b;
                public class C<X> { X m(X a, X b, boolean c) { return c ? a : b; } }
                """;
        assertEquals("[-, -, -] --> m←0:a,m←1:b", link("a.b.C", src, "m"));
    }

    @DisplayName("switch expression (arrow + yield) links to both arms")
    @Test
    public void switchYield() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    X m(X a, X b, int sel) { return switch (sel) { case 0 -> a; default -> { yield b; } }; }
                }
                """;
        assertEquals("[-, -, -] --> m←0:a,m←1:b", link("a.b.C", src, "m"));
    }

    @DisplayName("chained assignment p = q = a links through")
    @Test
    public void chainedAssignment() {
        @Language("java") String src = """
                package a.b;
                public class C<X> { X m(X a) { X p, q; p = q = a; return q; } }
                """;
        assertEquals("[-] --> m←0:a", link("a.b.C", src, "m"));
    }

    @DisplayName("local variable type inference (var) is transparent to linking")
    @Test
    public void varLocal() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    private final X f;
                    C(X in) { var v = in; this.f = v; }
                }
                """;
        assertEquals("[0:in→this*.f] --> -", link("a.b.C", src, "<init>"));
    }

    @DisplayName("record deconstruction pattern binds the component")
    @Test
    public void recordDeconstruction() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    record R<Y>(Y y) {}
                    X m(R<X> r) { if (r instanceof R(X xx)) return xx; return null; }
                }
                """;
        assertEquals("[-] --> m←$_ce0,m≺0:r", link("a.b.C", src, "m"));
    }

    @DisplayName("cast preserves linking")
    @Test
    public void cast() {
        @Language("java") String src = """
                package a.b;
                public class C<X> { X m(Object o) { return (X) o; } }
                """;
        assertEquals("[-] --> m←0:o", link("a.b.C", src, "m"));
    }

    @DisplayName("compound assignment on a primitive links the accumulator to the argument")
    @Test
    public void compoundAssignment() {
        @Language("java") String src = """
                package a.b;
                public class C { int m(int a) { int s = 0; s += a; return s; } }
                """;
        assertEquals("[-] --> m←0:a", link("a.b.C", src, "m"));
    }

    @DisplayName("bound method reference in forEach shares hidden content")
    @Test
    public void boundMethodReferenceForEach() {
        @Language("java") String src = """
                package a.b;
                import java.util.ArrayList;
                import java.util.List;
                public class C<X> {
                    List<X> m(List<X> in) { List<X> out = new ArrayList<>(); in.forEach(out::add); return out; }
                }
                """;
        assertEquals("[-] --> m.§es~0:in.§xs", link("a.b.C", src, "m"));
    }

    @DisplayName("labeled break, nested array iteration and array access linking")
    @Test
    public void labeledBreakNestedArray() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    X m(X[][] g) {
                        outer: for (X[] row : g) { for (X x : row) { if (x != null) break outer; } }
                        return g[0][0];
                    }
                }
                """;
        assertEquals("[0:g[0][0]∈0:g[0]] --> m←0:g[0][0],m∈0:g[0],m∈∈0:g", link("a.b.C", src, "m"));
    }

    @DisplayName("for-each links to hidden content for a collection, but NOT for an array (gap)")
    @Test
    public void forEachOverArrayVsCollection() {
        @Language("java") String coll = """
                package a.b;
                import java.util.List;
                public class Coll<X> { X m(List<X> list) { for (X x : list) { return x; } return null; } }
                """;
        // for-each over a Collection: the loop variable links to the collection's hidden content
        assertEquals("[0:list.§xs∋$_ce2] --> m←$_ce2,m∈0:list.§xs", link("a.b.Coll", coll, "m"));

        @Language("java") String arr = """
                package a.b;
                public class Arr<X> { X m(X[] arr) { for (X x : arr) { return x; } return null; } }
                """;
        // GAP: for-each over an ARRAY does not link the loop variable to the array's elements, so the returned
        // value is not linked to the parameter (contrast the collection case above and array *access* linking,
        // which does work). Pinned so the discrepancy is visible.
        assertEquals("[-] --> -", link("a.b.Arr", arr, "m"));
    }

    @DisplayName("wildcard ? extends X: get() links the result to the list's hidden content")
    @Test
    public void wildcardExtends() {
        @Language("java") String src = """
                package a.b;
                import java.util.List;
                public class C<X> { X m(List<? extends X> list) { return list.get(0); } }
                """;
        assertEquals("[-] --> m∈0:list.§xs", link("a.b.C", src, "m"));
    }

    @DisplayName("wildcard ? super X: add(x) puts x into the list's hidden content and modifies the list")
    @Test
    public void wildcardSuper() {
        @Language("java") String src = """
                package a.b;
                import java.util.List;
                public class C<X> { void m(List<? super X> list, X x) { list.add(x); } }
                """;
        assertEquals("[0:list*.§xs∋1:x, 1:x∈0:list*.§xs] --> -", link("a.b.C", src, "m"));
    }

    @DisplayName("switch type pattern (case String s) links through the arms")
    @Test
    public void typeSwitchPattern() {
        @Language("java") String src = """
                package a.b;
                public class C { Object m(Object o) { return switch (o) { case String s -> s; default -> o; }; } }
                """;
        assertEquals("[-] --> m←0:o", link("a.b.C", src, "m"));
    }

    @DisplayName("constructor reference (C::new) links the result to a functional-interface variable")
    @Test
    public void constructorReference() {
        @Language("java") String src = """
                package a.b;
                import java.util.function.Supplier;
                public class C<X> {
                    Supplier<C<X>> m() { return C::new; }
                    C() { }
                }
                """;
        assertEquals("[] --> m←Λ$_fi0", link("a.b.C", src, "m"));
    }

    @DisplayName("try-with-resources does NOT propagate resource modification, unlike a plain assignment (gap)")
    @Test
    public void tryWithResourcesModificationGap() {
        @Language("java") String plain = """
                package a.b;
                public class Plain {
                    interface Res extends AutoCloseable { void modify(); @Override void close(); }
                    void m(Res r) { Res s = r; s.modify(); }
                }
                """;
        // a plain 'Res s = r' links s to r, so s.modify() marks the parameter r as modified
        assertEquals("a.b.Plain.m(a.b.Plain.Res):0:r", modified("a.b.Plain", plain, "m"));

        @Language("java") String tryRes = """
                package a.b;
                public class Try {
                    interface Res extends AutoCloseable { void modify(); @Override void close(); }
                    void m(Res r) { try (Res s = r) { s.modify(); } catch (Exception e) { } }
                }
                """;
        // GAP: the resource variable 's' of 'try (Res s = r)' is not linked to 'r', so the modification s.modify()
        // is not propagated to the parameter r (contrast the plain assignment above). Pinned to flag the gap.
        assertEquals("", modified("a.b.Try", tryRes, "m"));
    }
}
