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
 * decides behaviour, so we widen the variety of inputs). Each test pins the computed links/modifications for one
 * construct.
 */
public class TestLanguageConstructs extends CommonTest {

    private MethodLinkedVariables compute(String fqn, String src, String methodName) {
        TypeInfo type = javaInspector.parse(fqn, src);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(type);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        MethodInfo mi = type.methodStream().filter(m -> m.name().equals(methodName)).findFirst()
                .orElseGet(() -> type.constructors().getFirst()); // "<init>" -> the (single) constructor
        return lc.doMethod(mi);
    }

    private String link(String fqn, String src, String methodName) {
        return compute(fqn, src, methodName).toString();
    }

    private String modified(String fqn, String src, String methodName) {
        return compute(fqn, src, methodName).sortedModifiedString();
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

    @DisplayName("for-each links the loop variable to the elements, for both a collection and an array")
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
        // for-each over an ARRAY: the loop variable is an element of the array (arr[i]); the returned value is
        // therefore linked to the array parameter, mirroring the collection case.
        assertEquals("[0:arr∋$_ce1] --> m←0:arr[0],m←$_ce1,m∈0:arr", link("a.b.Arr", arr, "m"));
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

    @DisplayName("try-with-resources propagates resource modification, like a plain assignment")
    @Test
    public void tryWithResourcesModification() {
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
        // the resource variable 's' of 'try (Res s = r)' is now linked to 'r' (the resource declaration is processed
        // like any local-variable creation), so s.modify() propagates to the parameter r, as for the plain case.
        assertEquals("a.b.Try.m(a.b.Try.Res):0:r", modified("a.b.Try", tryRes, "m"));
    }

    @DisplayName("varargs parameter links like an array (element + hidden content)")
    @Test
    public void varargs() {
        @Language("java") String src = """
                package a.b;
                public class C<X> { X m(X... xs) { return xs[0]; } }
                """;
        assertEquals("[-] --> m←0:xs[0],m∈0:xs", link("a.b.C", src, "m"));
    }

    @DisplayName("anonymous class captures a parameter: the instance links to the captured variable")
    @Test
    public void anonymousClassCapture() {
        @Language("java") String src = """
                package a.b;
                import java.util.function.Supplier;
                public class C<X> {
                    Supplier<X> m(X x) { return new Supplier<X>() { public X get() { return x; } }; }
                }
                """;
        assertEquals("[-] --> m←get,m←0:x", link("a.b.C", src, "m"));
    }

    @DisplayName("nested record deconstruction pattern Q(P(X xx))")
    @Test
    public void nestedRecordPattern() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    record P<Y>(Y y) {}
                    record Q<Y>(P<Y> p) {}
                    X m(Q<X> q) { if (q instanceof Q(P(X xx))) return xx; return null; }
                }
                """;
        assertEquals("[-] --> m←$_ce0,m≺0:q", link("a.b.C", src, "m"));
    }

    @DisplayName("switch with a guard (case String s when ...) links through the arms")
    @Test
    public void switchGuard() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    X m(Object o, X def) {
                        return switch (o) { case String s when s.isEmpty() -> def; default -> def; };
                    }
                }
                """;
        assertEquals("[-, -] --> m←1:def", link("a.b.C", src, "m"));
    }

    @DisplayName("intersection type bound (<T extends A & B>) does not break ternary linking")
    @Test
    public void intersectionTypeBound() {
        @Language("java") String src = """
                package a.b;
                import java.io.Serializable;
                public class C {
                    <T extends Comparable<T> & Serializable> T m(T a, T b, boolean c) { return c ? a : b; }
                }
                """;
        assertEquals("[-, -, -] --> m←0:a,m←1:b", link("a.b.C", src, "m"));
    }

    @DisplayName("enum constructor links the parameter into the field")
    @Test
    public void enumConstructor() {
        @Language("java") String src = """
                package a.b;
                import java.util.List;
                import java.util.ArrayList;
                public enum E {
                    A(new ArrayList<>());
                    final List<String> list;
                    E(List<String> l) { this.list = l; }
                }
                """;
        assertEquals("[0:l→this*.list,0:l.§m≡this*.list.§m] --> -", link("a.b.E", src, "<init>"));
    }

    @DisplayName("non-static inner class: writing the outer field links the argument and modifies 'this'")
    @Test
    public void innerClassOuterField() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    private X field;
                    class Inner { X get() { return field; } }
                    Inner m(X x) { this.field = x; return new Inner(); }
                }
                """;
        MethodLinkedVariables mlv = compute("a.b.C", src, "m");
        assertEquals("[0:x→this*.field] --> -", mlv.toString());
        assertEquals("this", mlv.sortedModifiedString());
    }

    @DisplayName("array store arr[0] = x puts x into the array and modifies it")
    @Test
    public void arrayStore() {
        @Language("java") String src = """
                package a.b;
                public class C<X> { void m(X[] arr, X x) { arr[0] = x; } }
                """;
        MethodLinkedVariables mlv = compute("a.b.C", src, "m");
        assertEquals("[0:arr*[0]←1:x,0:arr*∋1:x, 1:x→0:arr*[0],1:x∈0:arr*] --> -", mlv.toString());
        assertEquals("a.b.C.m(Object[],Object):0:arr", mlv.sortedModifiedString());
    }

    @DisplayName("labeled continue across nested loops still links the returned element to the array")
    @Test
    public void labeledContinue() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    X m(X[][] g) {
                        outer: for (X[] r : g) { for (X x : r) { if (x == null) continue outer; return x; } }
                        return null;
                    }
                }
                """;
        assertEquals("[0:g[0][0]∈0:g[0],0:g[0]∋$_ce3,0:g∋∋$_ce3] -->"
                     + " m←0:g[0][0],m←$_ce3,m∈0:g[0],m∈∈0:g", link("a.b.C", src, "m"));
    }

    @DisplayName("do-while loop links the returned array element to the array")
    @Test
    public void doWhile() {
        @Language("java") String src = """
                package a.b;
                public class C<X> {
                    X m(X[] arr) { int i = 0; X r = null; do { r = arr[i]; i++; } while (i < arr.length); return r; }
                }
                """;
        assertEquals("[0:arr∋$_ce1] --> m←0:arr[0],m←$_ce1,m∈0:arr", link("a.b.C", src, "m"));
    }

    @DisplayName("instanceof pattern binding links the returned variable to the tested expression")
    @Test
    public void instanceofPattern() {
        @Language("java") String src = """
                package a.b;
                public class C { String m(Object o) { if (o instanceof String s) return s; return ""; } }
                """;
        assertEquals("[-] --> m←$_ce0,m←0:o", link("a.b.C", src, "m"));
    }

    @DisplayName("nested ternary links to all three arms")
    @Test
    public void nestedTernary() {
        @Language("java") String src = """
                package a.b;
                public class C<X> { X m(X a, X b, X c, int s) { return s == 0 ? a : s == 1 ? b : c; } }
                """;
        assertEquals("[-, -, -, -] --> m←0:a,m←1:b,m←2:c", link("a.b.C", src, "m"));
    }

    @DisplayName("multi-catch (catch (A | B e)) is handled; a plain exception carries no hidden-content link")
    @Test
    public void multiCatch() {
        @Language("java") String src = """
                package a.b;
                public class C {
                    Throwable m(boolean b) {
                        try { return null; }
                        catch (IllegalStateException | IllegalArgumentException e) { return e; }
                    }
                }
                """;
        assertEquals("[-] --> -", link("a.b.C", src, "m"));
    }
}
