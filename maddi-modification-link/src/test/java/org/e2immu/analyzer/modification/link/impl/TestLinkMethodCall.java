package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Specification-by-example for {@link LinkMethodCall}, the class that turns a callee's summary
 * ({@code MethodLinkedVariables}: return/parameters/modifications) into links at a concrete call site.
 * <p>
 * Uses self-contained holders rather than JDK collections, so each test isolates one behaviour and pins the exact
 * links + modified variables:
 * <ul>
 *     <li>{@code Box<T>}: one field {@code t} — the basic single-hidden-content holder</li>
 *     <li>{@code SubBox<T> extends Box<T>}: inherited methods via a supertype receiver</li>
 *     <li>{@code Pair<A,B>}: two type parameters / two fields — selectivity and two-argument setters</li>
 *     <li>{@code Triple<P,Q,R>}: three type parameters / three fields</li>
 * </ul>
 * The code is parsed and linked <em>once</em> ({@link TestInstance.Lifecycle#PER_CLASS} + a lazy cache); every test
 * reads from that shared result. Paths of {@link LinkMethodCall} exercised: {@code constructorCall} (incl.
 * multi-argument, generic factory, and from-accessors); {@code objectToReturnValue} (accessors, {@code
 * this}-returning/fluent, nested/receiver-chain/array-index receivers, multi-field selectivity, generic
 * substitution, inheritance, argument pass-through); {@code parametersToObject} + modification (mutators, mutator
 * returning the object, no-argument modifier, two-argument setter, holder-into-object, field/array/nested
 * receivers); {@code linksBetweenParameters} (static argument-to-argument, incl. varargs fan-out).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestLinkMethodCall extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class C<X, Y, Z> {
                static class Box<T> {
                    private T t;
                    Box(T t) { this.t = t; }
                    T get() { return t; }
                    void set(T v) { this.t = v; }
                    void clear() { this.t = null; }
                    Box<T> self() { return this; }
                    Box<T> setReturn(T v) { this.t = v; return this; }
                }
                static class SubBox<T> extends Box<T> { SubBox(T t) { super(t); } }
                static class Pair<A, B> {
                    private A a; private B b;
                    Pair(A a, B b) { this.a = a; this.b = b; }
                    A first() { return a; }
                    B second() { return b; }
                    void setFirst(A x) { this.a = x; }
                    void set(A x, B y) { this.a = x; this.b = y; }
                }
                static class Triple<P, Q, R> {
                    private P p; private Q q; private R r;
                    Triple(P p, Q q, R r) { this.p = p; this.q = q; this.r = r; }
                    R third() { return r; }
                }
                private Box<X> myBox;

                Box<X> make(X x) { return new Box<>(x); }
                Box<X> makeFromGet(Box<X> box) { return new Box<>(box.get()); }
                X read(Box<X> box) { return box.get(); }
                Box<X> fluent(Box<X> box) { return box.self(); }
                Box<X> chain(Box<X> box) { return box.self().self(); }
                X pairFirst(Pair<X, X> pair) { return pair.first(); }
                void write(Box<X> box, X x) { box.set(x); }
                Box<X> writeReturn(Box<X> box, X x) { return box.setReturn(x); }
                void clear(Box<X> box) { box.clear(); }
                void pairSetFirst(Pair<X, X> pair, X x) { pair.setFirst(x); }
                static <T> void transfer(Box<T> from, Box<T> to) { to.set(from.get()); }
                static <T> T pass(Box<T> b) { return b.get(); }
                X nestedRead(Box<Box<X>> bb) { return bb.get().get(); }
                void nestedWrite(Box<Box<X>> bb, X x) { bb.get().set(x); }

                X readField() { return this.myBox.get(); }
                void writeField(X x) { this.myBox.set(x); }
                void putBoth(Pair<X, Y> pair, X a, Y b) { pair.set(a, b); }
                static <T> Box<T> wrap(T t) { return new Box<>(t); }
                Box<X> useWrap(X x) { return wrap(x); }
                X readSub(SubBox<X> sb) { return sb.get(); }
                void writeSub(SubBox<X> sb, X x) { sb.set(x); }
                X pairFirstXY(Pair<X, Y> pair) { return pair.first(); }
                Y pairSecondXY(Pair<X, Y> pair) { return pair.second(); }
                X arrayGet(Box<X>[] boxes) { return boxes[0].get(); }
                void arraySet(Box<X>[] boxes, X x) { boxes[0].set(x); }
                Box<X> arrayElement(Box<X>[] boxes) { return boxes[0]; }

                void copyFrom(Box<X> dst, Box<X> src) { dst.set(src.get()); }
                void store(Box<Box<X>> outer, Box<X> inner) { outer.set(inner); }
                Pair<X, Y> makePair(X a, Y b) { return new Pair<>(a, b); }
                Pair<X, X> pairFromBoxes(Box<X> b1, Box<X> b2) { return new Pair<>(b1.get(), b2.get()); }
                static <T> T identity(T t) { return t; }
                X id(X x) { return identity(x); }
                Z tripleThird(Triple<X, Y, Z> tr) { return tr.third(); }
                static <T> void fillAll(Box<T> target, T... vs) { for (T v : vs) target.set(v); }
                void useFill(Box<X> box, X a, X b) { fillAll(box, a, b); }
                X grid2d(Box<X>[][] grid) { return grid[0][0].get(); }
                X grid2dElement(X[][] grid) { return grid[0][0]; }
            }
            """;

    // parsed + prepped + linked once, shared by all tests (PER_CLASS instance keeps this cache alive)
    private Map<String, MethodLinkedVariables> cache;

    private MethodLinkedVariables mlv(String methodName) {
        if (cache == null) {
            TypeInfo c = javaInspector.parse("a.b.C", INPUT);
            new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(c);
            LinkComputerImpl lc = new LinkComputerImpl(javaInspector);
            cache = new HashMap<>();
            c.methodStream().forEach(m -> cache.put(m.name(), lc.doMethod(m)));
        }
        return cache.get(methodName);
    }

    private String link(String methodName) {
        return mlv(methodName).toString();
    }

    private String modified(String methodName) {
        return mlv(methodName).sortedModifiedString();
    }

    // ---- constructorCall ------------------------------------------------------------------------------------

    @DisplayName("constructor: the argument flows into the new object's field")
    @Test
    public void make() {
        assertEquals("[-] --> make.t←0:x", link("make"));
        assertEquals("", modified("make"));
    }

    @DisplayName("constructor fed by an accessor: hidden content is shared into the new object")
    @Test
    public void makeFromGet() {
        assertEquals("[-] --> makeFromGet.t←0:box.t", link("makeFromGet"));
    }

    @DisplayName("generic factory method: the type parameter is substituted, argument flows into the field")
    @Test
    public void useWrap() {
        assertEquals("[-] --> useWrap.t←0:x", link("useWrap"));
    }

    @DisplayName("two-argument constructor: each argument flows into its own field")
    @Test
    public void makePair() {
        assertEquals("[-, -] --> makePair.a←0:a,makePair.b←1:b", link("makePair"));
    }

    @DisplayName("constructor fed by two accessors: each field is fed from the corresponding box")
    @Test
    public void pairFromBoxes() {
        assertEquals("[-, -] --> pairFromBoxes.a←0:b1.t,pairFromBoxes.b←1:b2.t", link("pairFromBoxes"));
    }

    // ---- objectToReturnValue (return relates to the object) -------------------------------------------------

    @DisplayName("accessor: the object's field flows to the return value")
    @Test
    public void read() {
        assertEquals("[-] --> read←0:box.t", link("read"));
        assertEquals("", modified("read"));
    }

    @DisplayName("static accessor with the receiver as a parameter: field flows to the return value")
    @Test
    public void pass() {
        assertEquals("[-] --> pass←0:b.t", link("pass"));
    }

    @DisplayName("static identity: the argument is passed straight through to the return value")
    @Test
    public void id() {
        assertEquals("[-] --> id←0:x", link("id"));
    }

    @DisplayName("inherited accessor (SubBox extends Box): supertype method resolves, field flows to the return")
    @Test
    public void readSub() {
        assertEquals("[-] --> readSub←0:sb.t", link("readSub"));
    }

    @DisplayName("receiver chain this.field.method(): the field's content flows to the return value")
    @Test
    public void readField() {
        assertEquals("[] --> readField←this.myBox.t", link("readField"));
        assertEquals("", modified("readField"));
    }

    @DisplayName("multi-field holder: only the requested field flows to the return value")
    @Test
    public void pairFirst() {
        assertEquals("[-] --> pairFirst←0:pair.a", link("pairFirst"));
    }

    @DisplayName("two type parameters: first() selects field a")
    @Test
    public void pairFirstXY() {
        assertEquals("[-] --> pairFirstXY←0:pair.a", link("pairFirstXY"));
    }

    @DisplayName("two type parameters: second() selects field b")
    @Test
    public void pairSecondXY() {
        assertEquals("[-] --> pairSecondXY←0:pair.b", link("pairSecondXY"));
    }

    @DisplayName("three type parameters: third() selects field r")
    @Test
    public void tripleThird() {
        assertEquals("[-] --> tripleThird←0:tr.r", link("tripleThird"));
    }

    @DisplayName("nested holder Box<Box<X>>: the doubly-nested field flows to the return value")
    @Test
    public void nestedRead() {
        assertEquals("[-] --> nestedRead←0:bb.t.t", link("nestedRead"));
    }

    @DisplayName("array-indexed receiver boxes[0].get(): the element's field flows to the return value")
    @Test
    public void arrayGet() {
        assertEquals("[-] --> arrayGet←0:boxes[0].t", link("arrayGet"));
    }

    @DisplayName("two-dimensional array receiver grid[0][0].get(): the element's field flows to the return value")
    @Test
    public void grid2d() {
        assertEquals("[0:grid[0][0]∈0:grid[0]] --> grid2d←0:grid[0][0].t", link("grid2d"));
    }

    @DisplayName("returning an array element: return value links to the element and is an element of the array")
    @Test
    public void arrayElement() {
        assertEquals("[-] --> arrayElement∈0:boxes,arrayElement←0:boxes[0]", link("arrayElement"));
    }

    @DisplayName("returning a two-dimensional array element: nested element-of links to the array")
    @Test
    public void grid2dElement() {
        assertEquals("[0:grid[0][0]∈0:grid[0]] -->"
                     + " grid2dElement←0:grid[0][0],grid2dElement∈0:grid[0],grid2dElement∈∈0:grid",
                link("grid2dElement"));
    }

    @DisplayName("fluent (this-returning) call: the return value links to the object")
    @Test
    public void fluent() {
        assertEquals("[-] --> fluent←0:box", link("fluent"));
    }

    @DisplayName("chained fluent calls still link the return value to the object")
    @Test
    public void chain() {
        assertEquals("[-] --> chain←0:box", link("chain"));
    }

    // ---- parametersToObject + modification -----------------------------------------------------------------

    @DisplayName("mutator: the argument flows into the object's field; object and argument are modified")
    @Test
    public void write() {
        assertEquals("[0:box*.t←1:x*, 1:x*→0:box*.t] --> -", link("write"));
        assertEquals("a.b.C.write(a.b.C.Box,Object):0:box, a.b.C.write(a.b.C.Box,Object):1:x", modified("write"));
    }

    @DisplayName("mutator returning the object: argument into the object AND return value links to the object")
    @Test
    public void writeReturn() {
        assertEquals("[0:box*.t←1:x*, 1:x*→0:box*.t] -->"
                     + " writeReturn.t←0:box*.t,writeReturn.t←1:x*,writeReturn←0:box*", link("writeReturn"));
        assertEquals("a.b.C.writeReturn(a.b.C.Box,Object):0:box, a.b.C.writeReturn(a.b.C.Box,Object):1:x",
                modified("writeReturn"));
    }

    @DisplayName("no-argument modifier: only the object is modified, no links")
    @Test
    public void clear() {
        assertEquals("[-] --> -", link("clear"));
        assertEquals("a.b.C.clear(a.b.C.Box):0:box", modified("clear"));
    }

    @DisplayName("mutating a field of this (this.field.set(x)): x flows in; this and the field are modified")
    @Test
    public void writeField() {
        assertEquals("[0:x*→this.myBox*.t] --> -", link("writeField"));
        assertEquals("a.b.C.writeField(Object):0:x, this, this.myBox", modified("writeField"));
    }

    @DisplayName("mutator whose argument is a holder: the argument's field flows into the object's field")
    @Test
    public void copyFrom() {
        assertEquals("[0:dst*.t←1:src.t*, 1:src.t*→0:dst*.t] --> -", link("copyFrom"));
        assertEquals("a.b.C.copyFrom(a.b.C.Box,a.b.C.Box):0:dst, a.b.C.copyFrom(a.b.C.Box,a.b.C.Box):1:src, src.t",
                modified("copyFrom"));
    }

    @DisplayName("storing a whole holder into a container: the argument itself becomes the object's field content")
    @Test
    public void store() {
        assertEquals("[0:outer*.t←1:inner*, 1:inner*→0:outer*.t] --> -", link("store"));
        assertEquals("a.b.C.store(a.b.C.Box,a.b.C.Box):0:outer, a.b.C.store(a.b.C.Box,a.b.C.Box):1:inner",
                modified("store"));
    }

    @DisplayName("multi-field holder mutator: the argument flows into the targeted field")
    @Test
    public void pairSetFirst() {
        assertEquals("[0:pair*.a←1:x*, 1:x*→0:pair*.a] --> -", link("pairSetFirst"));
        assertEquals("a.b.C.pairSetFirst(a.b.C.Pair,Object):0:pair, a.b.C.pairSetFirst(a.b.C.Pair,Object):1:x",
                modified("pairSetFirst"));
    }

    @DisplayName("two-argument setter: each argument flows into its own field")
    @Test
    public void putBoth() {
        assertEquals("[0:pair*.a←1:a*,0:pair*.b←2:b*, 1:a*→0:pair*.a, 2:b*→0:pair*.b] --> -", link("putBoth"));
        assertEquals("a.b.C.putBoth(a.b.C.Pair,Object,Object):0:pair, "
                     + "a.b.C.putBoth(a.b.C.Pair,Object,Object):1:a, "
                     + "a.b.C.putBoth(a.b.C.Pair,Object,Object):2:b", modified("putBoth"));
    }

    @DisplayName("inherited mutator (SubBox extends Box): argument flows into the inherited field")
    @Test
    public void writeSub() {
        assertEquals("[0:sb*.t←1:x*, 1:x*→0:sb*.t] --> -", link("writeSub"));
        assertEquals("a.b.C.writeSub(a.b.C.SubBox,Object):0:sb, a.b.C.writeSub(a.b.C.SubBox,Object):1:x",
                modified("writeSub"));
    }

    @DisplayName("nested holder mutator: the argument flows into the doubly-nested field")
    @Test
    public void nestedWrite() {
        assertEquals("[0:bb.t*.t←1:x*, 1:x*→0:bb.t*.t] --> -", link("nestedWrite"));
        assertEquals("a.b.C.nestedWrite(a.b.C.Box,Object):0:bb, a.b.C.nestedWrite(a.b.C.Box,Object):1:x, bb.t",
                modified("nestedWrite"));
    }

    @DisplayName("array-indexed mutator boxes[0].set(x): argument flows into the element's field")
    @Test
    public void arraySet() {
        assertEquals("[0:boxes*[0]*.t←1:x*, 1:x*→0:boxes*[0]*.t] --> -", link("arraySet"));
        assertEquals("a.b.C.arraySet(a.b.C.Box[],Object):0:boxes, a.b.C.arraySet(a.b.C.Box[],Object):1:x, boxes[0]",
                modified("arraySet"));
    }

    // ---- linksBetweenParameters (static, argument-to-argument) ----------------------------------------------

    @DisplayName("static call: a field flows from one argument to another (links between parameters)")
    @Test
    public void transfer() {
        assertEquals("[0:from.t*→1:to*.t, 1:to*.t←0:from.t*] --> -", link("transfer"));
        assertEquals("a.b.C.transfer(a.b.C.Box,a.b.C.Box):0:from, a.b.C.transfer(a.b.C.Box,a.b.C.Box):1:to, from.t",
                modified("transfer"));
    }

    @DisplayName("varargs fan-out: each varargs argument flows into the target's field (downgrade of vs[i]/vs)")
    @Test
    public void useFill() {
        assertEquals("[0:box*.t∈1:a*,0:box*.t∈2:b*, 1:a*∋0:box*.t,1:a*~2:b*, 2:b*∋0:box*.t,2:b*~1:a*] --> -",
                link("useFill"));
        assertEquals("a.b.C.useFill(a.b.C.Box,Object,Object):0:box, "
                     + "a.b.C.useFill(a.b.C.Box,Object,Object):1:a, "
                     + "a.b.C.useFill(a.b.C.Box,Object,Object):2:b", modified("useFill"));
    }
}
