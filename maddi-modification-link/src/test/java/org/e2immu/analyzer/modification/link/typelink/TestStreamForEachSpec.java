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
 * Specification-by-example for consumers ({@code Collection.forEach}, {@code Stream.peek}) — the safety net for
 * hardening {@link org.e2immu.analyzer.modification.link.impl.LinkFunctionalInterface} on the consumer side.
 * <p>
 * The governing principle (dual to {@code map}): a consumer {@code c : X -> void} carries the source's elements
 * <em>into the wider scope it captures</em> (a field, another argument, ...). So {@code list.forEach(target::add)}
 * yields {@code list.§xs ~ target.§...} (they share elements) and marks the captured target modified. If the
 * consumer captures nothing relevant, there is no link.
 * <p>
 * Parsed and linked <em>once</em> ({@link TestInstance.Lifecycle#PER_CLASS} + a lazy cache).
 * <p>
 * Several cases are currently WRONG and pinned as gaps (see the {@code gap*} tests); they are the targets for the
 * consumer-side work.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestStreamForEachSpec extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.*;
            import java.util.function.Consumer;
            class C<X> {
                static class M<T> { private T t; void clear() { this.t = null; } }
                private final List<X> field = new ArrayList<>();

                // ---- source elements flow into the captured target ----
                void feedField(List<X> list) { list.forEach(field::add); }
                void feedParam(List<X> list, List<X> target) { list.forEach(target::add); }
                void feedLambda(List<X> list, List<X> target) { list.forEach(x -> target.add(x)); }
                void feedAnon(List<X> list, List<X> target) {
                    list.forEach(new Consumer<X>() { public void accept(X x) { target.add(x); } });
                }
                void applyConsumer(List<X> list, Consumer<? super X> c) { list.forEach(c); }
                void empty(List<X> list) { list.forEach(x -> { }); }
                void modifyElem(List<M<X>> list) { list.forEach(M::clear); }
                void modifyElemLambda(List<M<X>> list) { list.forEach(m -> m.clear()); }
                void peek(List<X> list, List<X> target) { list.stream().peek(target::add).toList(); }
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

    // ---- working: source flows into the captured target -------------------------------------------------

    @DisplayName("forEach(field::add): elements flow into the field's collection; the field is modified")
    @Test
    public void feedField() {
        assertEquals("[0:list.§xs~this.field*.§es] --> -", link("feedField"));
        assertEquals("this.field", modified("feedField"));
    }

    @DisplayName("forEach(target::add) on a parameter: elements flow into the target; the target is modified")
    @Test
    public void feedParam() {
        assertEquals("[0:list.§xs~1:target*.§es, 1:target*.§es~0:list.§xs] --> -", link("feedParam"));
        assertEquals("a.b.C.feedParam(java.util.List,java.util.List):1:target", modified("feedParam"));
    }

    @DisplayName("forEach(anonymous Consumer): same as the method reference, target modified")
    @Test
    public void feedAnon() {
        assertEquals("[0:list.§xs~1:target*.§xs, 1:target*.§xs~0:list.§xs] --> -", link("feedAnon"));
        assertEquals("a.b.C.feedAnon(java.util.List,java.util.List):1:target", modified("feedAnon"));
    }

    @DisplayName("forEach(x -> {}): an empty consumer captures nothing, so there is no link and no modification")
    @Test
    public void empty() {
        assertEquals("[-] --> -", link("empty"));
        assertEquals("", modified("empty"));
    }

    // ---- gaps (to fix) ----------------------------------------------------------------------------------

    @DisplayName("forEach(lambda): elements flow into the target; only the target is modified (like the anon class)")
    @Test
    public void feedLambda() {
        // the lambda's own (void) SAM return variable no longer leaks into the modified set
        assertEquals("[0:list.§xs~1:target*.§xs, 1:target*.§xs~0:list.§xs] --> -", link("feedLambda"));
        assertEquals("a.b.C.feedLambda(java.util.List,java.util.List):1:target", modified("feedLambda"));
    }

    @DisplayName("forEach(M::clear): the unbound receiver's self-modification is the element's, not the enclosing "
                 + "'this' — no longer leaked (element mutation of the source is not yet expressed: conservative)")
    @Test
    public void elementModifyingConsumer() {
        // clear() modifies each element (the SAM's receiver). We no longer misattribute that to the enclosing 'this';
        // expressing it as modification of the source's elements would need LinkFunctionalInterface to carry
        // modifications, so for now nothing is reported (conservative but no false positive).
        assertEquals("[-] --> -", link("modifyElem"));
        assertEquals("", modified("modifyElem"));
    }

    @DisplayName("forEach(m -> m.clear()): the lambda form behaves identically to the M::clear reference — the "
                 + "lambda's own parameter never leaks, so the same conservative (empty) result")
    @Test
    public void elementModifyingConsumerLambda() {
        assertEquals("[-] --> -", link("modifyElemLambda"));
        assertEquals("", modified("modifyElemLambda"));
    }

    @DisplayName("Stream.peek(target::add).toList(): the consumer's modification of target propagates through the "
                 + "terminal .toList() (the object sub-expression's side effects are carried forward)")
    @Test
    public void peek() {
        assertEquals("[0:list.§xs~1:target*.§es, 1:target*.§es~0:list.§xs] --> -", link("peek"));
        assertEquals("a.b.C.peek(java.util.List,java.util.List):1:target", modified("peek"));
    }

    @DisplayName("forEach(opaque Consumer parameter): the opaque consumer may mutate the elements, so the source "
                 + "and the consumer are conservatively marked modified (like the manual 'for (x:list) c.accept(x)')")
    @Test
    public void opaqueConsumerParameter() {
        // we cannot see into c, so no element-level link is produced, but the modification is conservative:
        // both the source list and the consumer c are marked modified.
        assertEquals("[-, -] --> -", link("applyConsumer"));
        assertEquals("a.b.C.applyConsumer(java.util.List,java.util.function.Consumer):0:list, "
                     + "a.b.C.applyConsumer(java.util.List,java.util.function.Consumer):1:c", modified("applyConsumer"));
    }
}
