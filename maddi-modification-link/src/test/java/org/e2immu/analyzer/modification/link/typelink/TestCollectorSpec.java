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
 * Specification-by-example for collectors ({@code Stream.collect(Collector<T,A,R>)}). See {@code linking-manual.md}
 * §7.5.
 * <p>
 * {@code Collector} is <em>not</em> a functional interface (5 abstract methods), so it is an opaque library object,
 * not a liftable lambda. The relationship "the result {@code R} shares hidden content with the stream's {@code T}" is
 * declared by {@code Stream.collect} being {@code @Independent(hc=true)}, but the shallow summary drops the return link
 * because generically {@code R} and {@code T} are distinct type parameters. {@link
 * org.e2immu.analyzer.modification.link.impl.LinkMethodCall#collectorReturnValue} realizes it at the concrete call
 * site: when all of the concrete {@code R}'s hidden-content type parameters come from the stream's, it links
 * {@code result.§ ⊆ stream.§}.
 * <p>
 * <b>Tier 1</b> (accumulating collectors) and the identity/concrete-key map collectors are covered here.
 * <b>Tier 2</b> — collectors whose keys/values are a foreign type parameter produced by a function (e.g. a generic
 * {@code groupingBy(classifier)}) — is deferred: those conservatively produce no link (see {@link #tier2Deferred()}).
 * <p>
 * Parsed and linked <em>once</em> ({@link TestInstance.Lifecycle#PER_CLASS} + a lazy cache).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCollectorSpec extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.*;
            import java.util.stream.*;
            class C<X> {
                // ---- Tier 1: accumulating collectors — the result is a collection of the stream's elements ----
                List<X> toList(List<X> in) { return in.stream().collect(Collectors.toList()); }
                Set<X> toSet(List<X> in) { return in.stream().collect(Collectors.toSet()); }
                List<X> toColl(List<X> in) { return in.stream().collect(Collectors.toCollection(ArrayList::new)); }
                List<X> toUnmod(List<X> in) { return in.stream().collect(Collectors.toUnmodifiableList()); }
                // reference: the direct terminal, which already linked correctly
                List<X> toListDirect(List<X> in) { return in.stream().toList(); }

                // ---- identity / concrete-key map collectors — keys and values still derive from the elements ----
                Map<X,X> toMapId(List<X> in) { return in.stream().collect(Collectors.toMap(x->x, x->x)); }
                Map<X,List<X>> groupId(List<X> in) { return in.stream().collect(Collectors.groupingBy(x->x)); }

                // ---- Tier 3: scalar/reducing collectors — the result is independent of the elements ----
                String joining(List<String> in) { return in.stream().collect(Collectors.joining()); }
                long counting(List<X> in) { return in.stream().collect(Collectors.counting()); }

                // ---- Tier 2 (deferred): a key produced by a function is a foreign type parameter ----
                <K> Map<K,List<X>> groupByKey(List<X> in, java.util.function.Function<X,K> f) {
                    return in.stream().collect(Collectors.groupingBy(f));
                }
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

    // ---- Tier 1: accumulating collectors --------------------------------------------------------------------

    @DisplayName("collect(toList()): the result's elements are a subset of the stream's (parity with stream().toList())")
    @Test
    public void toList() {
        assertEquals("[-] --> toList.§xs⊆0:in.§xs", link("toList"));
        assertEquals("[-] --> toListDirect.§xs⊆0:in.§xs", link("toListDirect"));
    }

    @DisplayName("collect(toSet()) / toCollection(ArrayList::new) / toUnmodifiableList(): same accumulating shape")
    @Test
    public void toSetCollUnmod() {
        assertEquals("[-] --> toSet.§xs⊆0:in.§xs", link("toSet"));
        assertEquals("[-] --> toColl.§xs⊆0:in.§xs", link("toColl"));
        assertEquals("[-] --> toUnmod.§xs⊆0:in.§xs", link("toUnmod"));
    }

    // ---- identity / concrete-key map collectors -------------------------------------------------------------

    @DisplayName("collect(toMap(x->x, x->x)): keys and values are the elements, so the map's content ⊆ the stream's")
    @Test
    public void toMapIdentity() {
        assertEquals("[-] --> toMapId.§xxs⊆0:in.§xs", link("toMapId"));
    }

    @DisplayName("collect(groupingBy(x->x)): keys and (list) values derive from the elements")
    @Test
    public void groupingByIdentity() {
        assertEquals("[-] --> groupId.§xxss⊆0:in.§xs", link("groupId"));
    }

    // ---- Tier 3: scalar/reducing collectors (must stay empty) ----------------------------------------------

    @DisplayName("collect(joining()) / counting(): the result is a fresh scalar, unrelated to the elements — no link")
    @Test
    public void scalarCollectors() {
        assertEquals("[-] --> -", link("joining"));
        assertEquals("[-] --> -", link("counting"));
    }

    // ---- Tier 2 (deferred) ---------------------------------------------------------------------------------

    @DisplayName("GAP (Tier 2): groupingBy with a function-produced key of a foreign type parameter is conservative "
                 + "(no link) — the value list ⊆ elements is not yet expressed")
    @Test
    public void tier2Deferred() {
        // K is a foreign type parameter (not from the stream), so collectorReturnValue skips; the values (List<X>)
        // do come from the stream, but expressing that requires threading the classifier function (Tier 2).
        assertEquals("[-, -] --> -", link("groupByKey"));
    }
}
