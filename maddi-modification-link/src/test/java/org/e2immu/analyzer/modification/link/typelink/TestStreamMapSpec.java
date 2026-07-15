package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Specification-by-example for {@code Stream.map(...)} (and closely related {@code Optional.map}) linking — the
 * safety net for hardening {@link org.e2immu.analyzer.modification.link.impl.LinkFunctionalInterface}.
 * <p>
 * The governing principle: {@code map(f)} with {@code f : X -> Y} lifts the function's own return↔parameter
 * relationship up to the stream/collection level ({@code source.§xs -> result.§ys}); the link nature weakens with
 * the "distance" between Y and X, and if X and Y are <em>unrelated</em> there is no link at all. Each map result is
 * explained by the corresponding function mlv (see {@link #functionRelationships()}) applied through
 * {@code Stream.map}'s own mlv {@code map.§rs⊆Λ0:function}.
 * <p>
 * Parsed and linked <em>once</em> ({@link TestInstance.Lifecycle#PER_CLASS} + a lazy cache).
 * <p>
 * All map cases pass. (The constructor-reference and JDK-static-method-reference cases, formerly gaps, are fixed.)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestStreamMapSpec extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.*;
            class C<X> {
                record R<V>(V v) {}
                record P<T>(T field) {}

                // ---- the functions f: X -> Y (their mlv is the "input relationship") ----
                <Y> Y identity(Y y) { return y; }
                static <Y> Y sIdentity(Y y) { return y; }
                <Y> Y[] identityArr(Y[] y) { return y; }
                <Y> R<Y> wrap(Y y) { return new R<>(y); }
                static <Y> R<Y> sWrap(Y y) { return new R<>(y); }
                <Y> List<R<Y>> wrapList(Y y) { return List.of(new R<>(y)); }
                <Y> Y first(Y[] ys) { return ys[0]; }
                <Y> Y firstOf(List<Y> l) { return l.get(0); }
                <Y> String toStr(Y y) { return y.toString(); }
                <Y> String constString(Y y) { return "c"; }

                // ---- relatedness: how Y relates to X drives the link nature ----
                List<X> mapIdentityMR(List<X> list) { return list.stream().map(this::identity).toList(); }
                List<X> mapIdentityLambda(List<X> list) { return list.stream().map(x -> x).toList(); }
                List<R<X>> mapWrap(List<X> list) { return list.stream().map(this::wrap).toList(); }
                List<List<R<X>>> mapWrapList(List<X> list) { return list.stream().map(this::wrapList).toList(); }
                List<String> mapUnrelatedMR(List<X> list) { return list.stream().map(this::toStr).toList(); }
                List<String> mapUnrelatedLambda(List<X> list) { return list.stream().map(x -> "s").toList(); }
                List<String> mapConst(List<X> list) { return list.stream().map(this::constString).toList(); }

                // ---- extraction: element pulled out of an array-typed / collection-typed element ----
                List<X> mapFirstArrayMR(List<X[]> list) { return list.stream().map(this::first).toList(); }
                List<X> mapFirstArrayLambda(List<X[]> list) { return list.stream().map(a -> a[0]).toList(); }
                List<X> mapFirstCollMR(List<List<X>> list) { return list.stream().map(this::firstOf).toList(); }
                List<X> mapFirstCollLambda(List<List<X>> list) { return list.stream().map(l -> l.get(0)).toList(); }

                // ---- record-component accessor (P<X> -> X) ----
                List<X> mapAccessorMR(List<P<X>> list) { return list.stream().map(P::field).toList(); }
                List<X> mapAccessorLambda(List<P<X>> list) { return list.stream().map(p -> p.field()).toList(); }

                // ---- array-typed source, identity ----
                List<X[]> mapArrayIdentity(List<X[]> list) { return list.stream().map(this::identityArr).toList(); }

                // ---- chained map.map ----
                List<R<X>> mapChained(List<X> list) { return list.stream().map(this::identity).map(this::wrap).toList(); }

                // ---- Optional.map ----
                Optional<R<X>> optionalMap(Optional<X> opt) { return opt.map(this::wrap); }

                // ---- static method reference (own static) ----
                List<X> mapStaticIdentity(List<X> list) { return list.stream().map(C::sIdentity).toList(); }
                List<R<X>> mapStaticWrap(List<X> list) { return list.stream().map(C::sWrap).toList(); }

                // ---- constructor reference vs the equivalent lambda ----
                List<R<X>> mapCtorLambda(List<X> list) { return list.stream().map(x -> new R<>(x)).toList(); }
                List<R<X>> mapCtorRef(List<X> list) { return list.stream().map(R::new).toList(); }

                // ---- unbound instance method reference ----
                List<X> mapUnboundGetFirst(List<List<X>> list) { return list.stream().map(List::getFirst).toList(); }

                // ---- JDK static method reference ----
                List<String> mapJdkStaticMR(List<X> list) { return list.stream().map(String::valueOf).toList(); }
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
            MethodInfo mapM = javaInspector.compiledTypesManager().get(Stream.class).findUniqueMethod("map", 1);
            cache.put("Stream.map", mapM.analysis().getOrCreate(MethodLinkedVariablesImpl.METHOD_LINKS,
                    () -> lc.doMethod(mapM)));
        }
        return cache.get(methodName);
    }

    private String mlv(String methodName) {
        return mlvOf(methodName).toString();
    }

    // ---- the pieces that explain the results ---------------------------------------------------------------

    @DisplayName("the function relationships f: X -> Y (each map result is derived from one of these)")
    @Test
    public void functionRelationships() {
        assertEquals("[-] --> identity←0:y", mlv("identity"));           // Y -> Y            : return is the argument
        assertEquals("[-] --> wrap.v←0:y", mlv("wrap"));                 // Y -> R<Y>         : field holds the argument
        assertEquals("[-] --> wrapList.§$s≥0:y", mlv("wrapList"));       // Y -> List<R<Y>>   : content contains the argument
        assertEquals("[-] --> first∈0:ys,first←0:ys[0]", mlv("first"));  // Y[] -> Y          : an element of the array
        assertEquals("[-] --> firstOf∈0:l.§ys", mlv("firstOf"));         // List<Y> -> Y      : an element of the collection
        assertEquals("[-] --> -", mlv("toStr"));                         // Y -> String       : unrelated (fresh)
        assertEquals("[-] --> -", mlv("constString"));                   // Y -> String       : constant, unrelated
    }

    @DisplayName("Stream.map's own summary: the result's elements come from the applied function")
    @Test
    public void streamMapOwnSummary() {
        assertEquals("[-] --> map.§rs⊆Λ0:function", mlv("Stream.map"));
    }

    // ---- relatedness axis ---------------------------------------------------------------------------------

    @DisplayName("identity (Y->Y): result shares elements with the source (⊆)")
    @Test
    public void identity() {
        assertEquals("[-] --> mapIdentityMR.§m←0:list.§m,mapIdentityMR.§xs⊆0:list.§xs", mlv("mapIdentityMR"));
        assertEquals("[-] --> mapIdentityLambda.§m←0:list.§m,mapIdentityLambda.§xs⊆0:list.§xs", mlv("mapIdentityLambda"));
    }

    @DisplayName("transparent wrap (Y->R<Y>, same dimension): the wrapping is invisible, still ⊆")
    @Test
    public void transparentWrap() {
        assertEquals("[-] --> mapWrap.§m←0:list.§m,mapWrap.§xs⊆0:list.§xs", mlv("mapWrap"));
    }

    @DisplayName("dimension-adding wrap (Y->List<R<Y>>): a level is added, only object graphs overlap (∩)")
    @Test
    public void dimensionAddingWrap() {
        assertEquals("[-] --> mapWrapList.§xss∩0:list.§xs", mlv("mapWrapList"));
    }

    @DisplayName("unrelated (Y->String): X and Y are unrelated, so there is NO link")
    @Test
    public void unrelated() {
        assertEquals("[-] --> -", mlv("mapUnrelatedMR"));
        assertEquals("[-] --> -", mlv("mapUnrelatedLambda"));
        assertEquals("[-] --> -", mlv("mapConst"));
    }

    // ---- extraction axis ---------------------------------------------------------------------------------

    @DisplayName("extract from an array element (Y[]->Y): result is inside the source's object graph (≤)")
    @Test
    public void extractFromArray() {
        assertEquals("[-] --> mapFirstArrayMR.§xs≤0:list.§xss", mlv("mapFirstArrayMR"));
        assertEquals("[-] --> mapFirstArrayLambda.§xs≤0:list.§xss", mlv("mapFirstArrayLambda"));
    }

    @DisplayName("extract from a collection element (List<Y>->Y): result is inside the source's object graph (≤)")
    @Test
    public void extractFromCollection() {
        assertEquals("[-] --> mapFirstCollMR.§xs≤0:list.§xss", mlv("mapFirstCollMR"));
        assertEquals("[-] --> mapFirstCollLambda.§xs≤0:list.§xss", mlv("mapFirstCollLambda"));
    }

    // ---- more receiver / function shapes ----------------------------------------------------------------

    @DisplayName("record-component accessor (P<X> -> X): result shares elements with the source")
    @Test
    public void accessor() {
        assertEquals("[-] --> mapAccessorMR.§m←0:list.§m,mapAccessorMR.§xs⊆0:list.§xs", mlv("mapAccessorMR"));
        assertEquals("[-] --> mapAccessorLambda.§m←0:list.§m,mapAccessorLambda.§xs⊆0:list.§xs", mlv("mapAccessorLambda"));
    }

    @DisplayName("array-typed source, identity (X[]->X[]): elements (arrays) shared, one dimension up")
    @Test
    public void arrayTypedSourceIdentity() {
        assertEquals("[-] --> mapArrayIdentity.§m←0:list.§m,mapArrayIdentity.§xss⊆0:list.§xss", mlv("mapArrayIdentity"));
    }

    @DisplayName("chained map.map (identity then wrap): the whole chain is transparent, still ⊆")
    @Test
    public void chainedMap() {
        assertEquals("[-] --> mapChained.§m←0:list.§m,mapChained.§xs⊆0:list.§xs", mlv("mapChained"));
    }

    @DisplayName("Optional.map (single element): the wrapped content links from the source's content")
    @Test
    public void optionalMap() {
        assertEquals("[-] --> optionalMap.§x←0:opt.§x", mlv("optionalMap"));
    }

    @DisplayName("static method reference (own): behaves like the instance-method / lambda equivalents")
    @Test
    public void staticMethodReference() {
        assertEquals("[-] --> mapStaticIdentity.§m←0:list.§m,mapStaticIdentity.§xs⊆0:list.§xs", mlv("mapStaticIdentity"));
        assertEquals("[-] --> mapStaticWrap.§m←0:list.§m,mapStaticWrap.§xs⊆0:list.§xs", mlv("mapStaticWrap"));
    }

    @DisplayName("constructor via lambda (x -> new R<>(x)): the wrapping is transparent, ⊆")
    @Test
    public void constructorViaLambda() {
        assertEquals("[-] --> mapCtorLambda.§m←0:list.§m,mapCtorLambda.§xs⊆0:list.§xs", mlv("mapCtorLambda"));
    }

    @DisplayName("unbound instance method reference (List::getFirst) is lifted like the equivalent lambda")
    @Test
    public void unboundMethodReference() {
        assertEquals("[-] --> mapUnboundGetFirst.§xs≤0:list.§xss", mlv("mapUnboundGetFirst"));
    }

    // ---- known gaps (to fix) ----------------------------------------------------------------------------

    @DisplayName("constructor reference (R::new) lifts like the equivalent lambda (x -> new R<>(x))")
    @Test
    public void constructorReference() {
        // the SAM's return value is the new object; the constructor's 'param → this.field' relationship is
        // re-homed onto that return value, so R::new behaves like this::wrap / x -> new R<>(x)
        assertEquals("[-] --> mapCtorRef.§m←0:list.§m,mapCtorRef.§xs⊆0:list.§xs", mlv("mapCtorRef"));
    }

    @DisplayName("a JDK static method reference (String::valueOf) returns a fresh, unrelated value: no link")
    @Test
    public void jdkStaticMethodReference() {
        // String.valueOf(X) returns a fresh String, unrelated to X, so the result has NO link (like mapUnrelatedMR).
        // Previously a phantom '∩valueOf' link was produced by lifting a link to the SAM's own return variable.
        assertEquals("[-] --> -", mlv("mapJdkStaticMR"));
    }
}
