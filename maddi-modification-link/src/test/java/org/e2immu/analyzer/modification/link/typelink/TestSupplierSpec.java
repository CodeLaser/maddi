package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
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
 * Specification-by-example for suppliers ({@code Supplier<T>}: {@code T get()}, no parameters) — the safety net for
 * hardening {@link org.e2immu.analyzer.modification.link.impl.LinkFunctionalInterface} on the supplier side.
 * <p>
 * The governing principle (dual to {@code map}/{@code forEach}): a supplier <em>produces</em> a value on demand, and
 * that value flows OUT to the result of the method that uses the supplier. So {@code optional.orElseGet(() -> field)}
 * yields {@code result ← optional's content} <em>and</em> {@code result ← field} (the two alternatives). Whatever the
 * produced value links to (a captured field/parameter, an element of a captured collection, the fields of a freshly
 * created object) is carried onto the result; a freshly created value with no captures adds no link.
 * <p>
 * {@code Optional.orElseGet}, {@code Objects.requireNonNullElseGet} and {@code Stream.generate} are the vehicles.
 * Parsed and linked <em>once</em> ({@link TestInstance.Lifecycle#PER_CLASS} + a lazy cache).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSupplierSpec extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.*;
            import java.util.function.Supplier;
            import java.util.stream.Stream;
            class C<X> {
                static class Box<T> { T t; Box(T t){this.t=t;} Box(){this.t=null;} T get(){return t;} }
                private X field;
                X getField(){ return field; }

                // ---- the produced value flows to the result ----
                X supField(Optional<X> opt) { return opt.orElseGet(() -> field); }
                X supParam(Optional<X> opt, X alt) { return opt.orElseGet(() -> alt); }
                X supElement(Optional<X> opt, List<X> list) { return opt.orElseGet(() -> list.get(0)); }

                // ---- a freshly created value: no capture -> no link; capture -> flows into the new object ----
                Box<X> supFresh(Optional<Box<X>> opt) { return opt.orElseGet(() -> new Box<>()); }
                Box<X> supFreshCapture(Optional<Box<X>> opt, X x) { return opt.orElseGet(() -> new Box<>(x)); }

                // ---- method / constructor references as suppliers ----
                X supMethodRef(Optional<X> opt) { return opt.orElseGet(this::getField); }
                X supMethodRefOther(Optional<X> opt, C<X> other) { return opt.orElseGet(other::getField); }
                Box<X> supCtorRef(Optional<Box<X>> opt) { return opt.orElseGet(Box::new); }

                // ---- other supplier consumers ----
                X requireElse(X in, X alt) { return Objects.requireNonNullElseGet(in, () -> alt); }
                Stream<X> genParam(X alt) { return Stream.generate(() -> alt); }
            }
            """;

    private Map<String, MethodLinkedVariables> cache;

    private MethodLinkedVariables mlvOf(String methodName) {
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
        return mlvOf(methodName).toString();
    }

    private String modified(String methodName) {
        return mlvOf(methodName).sortedModifiedString();
    }

    // ---- the produced value flows to the result -------------------------------------------------------------

    @DisplayName("supplier captures a field: result is the optional's content OR the field")
    @Test
    public void supField() {
        assertEquals("[-] --> supField←this.field,supField←0:opt.§x", link("supField"));
        assertEquals("", modified("supField"));
    }

    @DisplayName("supplier captures a parameter: result is the optional's content OR the parameter")
    @Test
    public void supParam() {
        assertEquals("[-, -] --> supParam←1:alt,supParam←0:opt.§x", link("supParam"));
    }

    @DisplayName("supplier returns an element of a captured collection: result relates to those elements")
    @Test
    public void supElement() {
        assertEquals("[-, -] --> supElement←1:list.§xs,supElement←0:opt.§x", link("supElement"));
    }

    // ---- freshly created values ----------------------------------------------------------------------------

    @DisplayName("supplier returns a fresh object with no capture: adds no link (result is just the optional's content)")
    @Test
    public void supFresh() {
        assertEquals("[-] --> supFresh←0:opt.§x", link("supFresh"));
        assertEquals("", modified("supFresh"));
    }

    @DisplayName("supplier returns a fresh object capturing an argument: the argument flows into the new object's field")
    @Test
    public void supFreshCapture() {
        assertEquals("[-, -] --> supFreshCapture.t←1:x,supFreshCapture.t≺0:opt.§x,supFreshCapture.t≺0:opt,"
                     + "supFreshCapture←0:opt.§x", link("supFreshCapture"));
    }

    // ---- method / constructor references -------------------------------------------------------------------

    @DisplayName("bound method reference (this::getField) behaves like the equivalent lambda")
    @Test
    public void supMethodRef() {
        assertEquals("[-] --> supMethodRef←this.field,supMethodRef←0:opt.§x", link("supMethodRef"));
    }

    @DisplayName("method reference on another instance: result relates to that instance's field")
    @Test
    public void supMethodRefOther() {
        assertEquals("[0:opt≈1:other, -] --> supMethodRefOther←1:other.field,supMethodRefOther←0:opt.§x",
                link("supMethodRefOther"));
    }

    @DisplayName("constructor reference (Box::new) as a supplier: fresh object, no link and NO spurious modification "
                 + "of the enclosing 'this' (the constructor's own 'this' is the fresh object)")
    @Test
    public void supCtorRef() {
        assertEquals("[-] --> supCtorRef←0:opt.§x", link("supCtorRef"));
        assertEquals("", modified("supCtorRef")); // used to wrongly report 'this'
    }

    // ---- other consumers -----------------------------------------------------------------------------------

    @DisplayName("Objects.requireNonNullElseGet: result is the value OR the supplier's produced value")
    @Test
    public void requireElse() {
        assertEquals("[-, -] --> requireElse←0:in,requireElse←1:alt", link("requireElse"));
    }

    @DisplayName("Stream.generate(() -> alt): the stream's elements come from the supplier's produced value")
    @Test
    public void genParam() {
        assertEquals("[-] --> genParam.§xs⊆0:alt", link("genParam"));
    }
}
