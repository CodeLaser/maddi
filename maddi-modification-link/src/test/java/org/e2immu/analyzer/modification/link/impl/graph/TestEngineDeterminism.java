package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Determinism regression. The engine guarantees two properties, at different strengths:

 1. CLOSURE FACTS AND LABELS are independent of edge-insertion order (assertFactsEqual below). This is the
    property linking correctness rests on.
 2. WITNESS CHOICE is canonical among candidates that are actually OFFERED to putIfBetter (canonical tie-break on
    equal quality; CompositeWitness.print sorts its support set). It is NOT invariant under arbitrary reordering:
    a fact derived before its direct edge arrives keeps the composite witness (closure.add returns false, so the
    direct witness is never offered), and different derivation sequences can offer different single candidates.
    Within one run the insertion order is deterministic (statement order), so outputs are run-to-run stable —
    which is what the flaky-test fixes established. The diamond case below is one where all competing witnesses
    ARE offered, so full-text equality holds and pins the tie-break.

 If assertFactsEqual starts failing after an engine change, some new logic made the CLOSURE order-sensitive:
 fix the logic, do not re-baseline this test.
 */
public class TestEngineDeterminism {
    private final LinkNature IS_IDENTICAL_TO = LinkNatureImpl.makeIdenticalTo(null);

    private record E(String from, String to, LinkNature ln) {
    }

    private static IncrementalFixpointEngine<String, LinkNature> newEngine() {
        return new IncrementalFixpointEngine<>(LinkNature::combine,
                LinkNature::best, LinkNature::valid, LinkNature::score, LinkNature::reverse,
                Object::toString, String::compareTo, _ -> true);
    }

    // all edges share one statement index so that direct witnesses are identical whichever insertion order is used
    private static IncrementalFixpointEngine<String, LinkNature> build(List<E> edges) {
        IncrementalFixpointEngine<String, LinkNature> engine = newEngine();
        for (E e : edges) {
            engine.addSymmetricEdge(e.from(), e.to(), e.ln(), "0");
        }
        return engine;
    }

    // the 'fact  witness' dump reduced to sorted 'fact' lines (strip the double-space-separated witness text)
    private static String factsOf(String printClosure) {
        Set<String> facts = new TreeSet<>();
        for (String line : printClosure.split("\n")) {
            if (!line.isBlank()) facts.add(line.split("\\s\\s+")[0].strip());
        }
        return String.join("\n", facts);
    }

    private static void assertFactsEqual(IncrementalFixpointEngine<String, LinkNature> a,
                                         IncrementalFixpointEngine<String, LinkNature> b) {
        assertEquals(factsOf(a.printClosure()), factsOf(b.printClosure()));
    }

    // strip the ' support: ...' annotation: the support set snapshots derivation HISTORY at construction time and
    // is not refreshed when sub-witnesses later upgrade, so it is inherently order-dependent. A superset support is
    // conservative (it can only trigger more orphan-materialization on removal, never wrongly keep a fact).
    private static String withoutSupport(String printClosure) {
        return printClosure.replaceAll("\\s+support: .*", "");
    }

    @DisplayName("equal-cost witness paths: facts + chosen witness identical under reversed insertion (pins the tie-break)")
    @Test
    public void diamond() {
        // a ≡ b ≡ c and a ≡ d ≡ c: the fact (a,c) has two equal-cost derivations (via b, via d), and both are
        // offered during propagation — the canonical tie-break must pick the same witness whichever arrives first;
        // and a direct edge arriving after its fact was derived must still register its direct witness.
        List<E> edges = List.of(
                new E("a", "b", IS_IDENTICAL_TO),
                new E("b", "c", IS_IDENTICAL_TO),
                new E("a", "d", IS_IDENTICAL_TO),
                new E("d", "c", IS_IDENTICAL_TO));
        List<E> reversed = new ArrayList<>(edges);
        Collections.reverse(reversed);

        assertEquals(withoutSupport(build(edges).printClosure()), withoutSupport(build(reversed).printClosure()));
    }

    @DisplayName("two-level content composition (m∩copy): facts identical under ALL 24 insertion orders")
    @Test
    public void twoLevelComposition() {
        // the TestModificationBasics m∩copy flake: 'm ∈ list.§$s' ∘ 'list.§$s ∩ copy' → 'm ∩ copy', where the
        // middle fact is ITSELF derived ('list.§$s ⊇ copy.§$s' ∘ 'copy.§$s ≺ copy'). Two-level derivations are
        // sensitive to wave ordering (history-gated exploration); the fact set must not be.
        List<E> edges = List.of(
                new E("list", "list.§$s", CONTAINS_AS_FIELD),
                new E("copy", "copy.§$s", CONTAINS_AS_FIELD),
                new E("list.§$s", "copy.§$s", IS_SUPERSET_OF),
                new E("list.§$s", "m", CONTAINS_AS_MEMBER));
        String reference = null;
        int[] idx = {0, 1, 2, 3};
        // all 24 permutations
        List<List<E>> perms = new ArrayList<>();
        permute(edges, new ArrayList<>(), new boolean[4], perms);
        for (List<E> perm : perms) {
            String facts = factsOf(build(perm).printClosure());
            if (reference == null) {
                reference = facts;
                org.junit.jupiter.api.Assertions.assertTrue(facts.contains("m ∩ copy"),
                        "the two-level composite must derive:\n" + facts);
            } else {
                assertEquals(reference, facts, "insertion order changed the fact set: " + perm);
            }
        }
    }

    private static void permute(List<E> src, List<E> cur, boolean[] used, List<List<E>> out) {
        if (cur.size() == src.size()) {
            out.add(new ArrayList<>(cur));
            return;
        }
        for (int i = 0; i < src.size(); i++) {
            if (used[i]) continue;
            used[i] = true;
            cur.add(src.get(i));
            permute(src, cur, used, out);
            cur.removeLast();
            used[i] = false;
        }
    }

    @DisplayName("mixed natures incl. the owner≻content spine: facts+labels identical under reversed insertion")
    @Test
    public void spine() {
        // the varargs shape: target.§ ~ collection.§ ≺ collection ∈ collections.§ closes to target.§ ∩ collections.§
        List<E> edges = List.of(
                new E("target.§is", "collection.§is", SHARES_ELEMENTS),
                new E("collection", "collection.§is", CONTAINS_AS_FIELD),
                new E("collection", "collections.§iss", IS_ELEMENT_OF),
                new E("target", "target.§is", CONTAINS_AS_FIELD));
        List<E> reversed = new ArrayList<>(edges);
        Collections.reverse(reversed);

        assertFactsEqual(build(edges), build(reversed));
    }

    @DisplayName("vertex removal + witness-orphan materialization: facts+labels identical under reversed insertion")
    @Test
    public void removal() {
        // remove the intermediary 'collection': the derived facts between the survivors must be materialized with
        // the same facts and labels whichever insertion order built the graph.
        List<E> edges = List.of(
                new E("target.§is", "collection.§is", SHARES_ELEMENTS),
                new E("collection", "collection.§is", CONTAINS_AS_FIELD),
                new E("collection", "collections.§iss", IS_ELEMENT_OF),
                new E("target", "target.§is", CONTAINS_AS_FIELD));
        List<E> reversed = new ArrayList<>(edges);
        Collections.reverse(reversed);

        IncrementalFixpointEngine<String, LinkNature> forward = build(edges);
        IncrementalFixpointEngine<String, LinkNature> backward = build(reversed);
        for (var engine : List.of(forward, backward)) {
            engine.removeVertices(Set.of("collection", "collection.§is"));
            engine.recompute(Set.of("collection", "collection.§is"), "1", _ -> true);
        }
        assertFactsEqual(forward, backward);
        assertEquals(factsOf(forward.printEdges().replace(" / ", "\n")),
                factsOf(backward.printEdges().replace(" / ", "\n")));
    }
}
