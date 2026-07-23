/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.analyzer.eventual;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.common.defaults.ContractReader;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why a defensively-copying accessor does or does not lift its owner out of {@code @Dependent}.
 * <p>
 * This matters because dependence is what caps maddi's own CST: an accessor that hands out its field makes the
 * type {@code @Dependent}, which caps it at FINAL_FIELDS at the independence gate, and FINAL_FIELDS counts as
 * mutable ({@code ImmutableImpl.isMutable()} is {@code value <= 1}), so the hierarchy rule then forces every
 * implementation to MUTABLE. It is the single root behind every missing verdict in that family.
 * <p>
 * The old e2immu computed "dynamic type annotations": {@code return List.of(...)} yielded an
 * {@code @Immutable(hc=true)} marker on the method. This generation keeps the property slot
 * ({@link PropertyImpl#IMMUTABLE_METHOD}, commented "dynamic return type") but still computes nothing into it
 * from a method body: inferring dynamic immutability is inter-procedural and is not done
 * ({@code docs/dynamic-immutability-feasibility.md}). Only a hand-written contract supplies it.
 * <p>
 * {@code SourceContractMaterializer} (part 1) brings a hand-written contract into {@code analysis()}, and
 * {@code DynamicImmutability} (part 3) consumes it where dependence is decided. Only the FIELD-side contract is
 * consumed, though, which is why G below now lifts while B and E -- which annotate the accessor -- still do not.
 * <p>
 * Several assertions below pin a LIMITATION rather than desired behaviour; each says so.
 */
public class TestDynamicImmutableReturn extends CommonTest {

    /** The values that decide whether the owning type escapes {@code @Dependent}. */
    private record Measured(Value.Independent typeIndependent, Value.Immutable typeImmutable,
                            Value.Independent methodIndependent, Value.Immutable methodImmutable,
                            Value.Independent fieldIndependent, Value.Immutable fieldImmutable,
                            Value contractedMethodImmutable) {
    }

    private Measured measure(String source) {
        TypeInfo B = javaInspector.parse("B", source);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo items = B.findUniqueMethod("items", 0);
        return new Measured(
                B.analysis().getOrNull(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class),
                B.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class),
                items.analysis().getOrNull(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class),
                items.analysis().getOrNull(PropertyImpl.IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.class),
                B.fields().getFirst().analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                        ValueImpl.IndependentImpl.class),
                B.fields().getFirst().analysis().getOrNull(PropertyImpl.IMMUTABLE_FIELD,
                        ValueImpl.ImmutableImpl.class),
                new ContractReader(runtime).contracts(items).get(PropertyImpl.IMMUTABLE_METHOD));
    }

    // one test per variant: a JavaInspector cannot parse the same FQN twice (the type's inspection is
    // committed after the first parse -- the eventual pattern itself), and CommonTest builds a fresh
    // analyzer bundle per test.

    @Language("java")
    private static final String A = """
            import java.util.List;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = items; }
              public List<String> items() { return items; }
            }
            """;

    @DisplayName("A: store and hand out the caller's list -- dependent on both sides")
    @Test
    public void testA() {
        Measured m = measure(A);
        assertTrue(m.typeIndependent().isDependent());
        assertTrue(m.typeImmutable().isFinalFields());
        assertTrue(m.methodIndependent().isDependent());
        assertTrue(m.fieldIndependent().isDependent());
    }

    @Language("java")
    private static final String B_ANNOTATED = """
            import java.util.List;
            import org.e2immu.annotation.Immutable;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = items; }
              @Immutable(hc = true)
              public List<String> items() { return items; }
            }
            """;

    @DisplayName("B: a hand-written @Immutable(hc=true) on the accessor changes nothing at all")
    @Test
    public void testB() {
        Measured m = measure(B_ANNOTATED);
        // The contract IS readable from the source...
        assertNotNull(m.contractedMethodImmutable(), "the annotation is on the method");
        assertEquals("@Immutable(hc=true)", m.contractedMethodImmutable().toString());
        // ...and since SourceContractMaterializer it is materialized into analysis() too, so the codec, the
        // guard and the IDE daemon can all see it.
        assertNotNull(m.methodImmutable(), "IMMUTABLE_METHOD is materialized for a source method");
        assertTrue(m.methodImmutable().isImmutableHC());
        // LIMITATION, not desired behaviour: materializing it changes no verdict, because the link engine --
        // where a source accessor's dependence is decided -- never reads IMMUTABLE_METHOD. (ShallowMethodAnalyzer
        // does consult it, but only for methods whose source we do not have.) That is part 3, not part 1.
        assertTrue(m.methodIndependent().isDependent());
        assertTrue(m.typeIndependent().isDependent());
        assertTrue(m.typeImmutable().isFinalFields());
    }

    @Language("java")
    private static final String C_COPY_OUT = """
            import java.util.List;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = items; }
              public List<String> items() { return List.copyOf(items); }
            }
            """;

    @DisplayName("C: copying on the way OUT lifts the accessor, but the constructor still leaks")
    @Test
    public void testC() {
        Measured m = measure(C_COPY_OUT);
        // The link engine DOES understand List.copyOf: no annotation and no dynamic-immutability machinery, yet
        // the accessor is independent. Whatever is missing, it is not an inability to see a defensive copy.
        assertTrue(m.methodIndependent().isIndependent(), "List.copyOf in the accessor is understood");
        // but the constructor still stores the caller's list, so the field -- and the type -- stay dependent
        assertTrue(m.fieldIndependent().isDependent());
        assertTrue(m.typeIndependent().isDependent());
        assertTrue(m.typeImmutable().isFinalFields());
    }

    @Language("java")
    private static final String D_COPY_IN = """
            import java.util.List;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = List.copyOf(items); }
              public List<String> items() { return items; }
            }
            """;

    @DisplayName("D: copying on the way IN now lifts the type -- this is TypeInspectionImpl's actual shape")
    @Test
    public void testD() {
        Measured m = measure(D_COPY_IN);
        // Was the headline limitation: the field provably holds an immutable list after the constructor, but
        // that was not tracked, so handing it out counted as leaking mutable content. DynamicImmutabilityInference
        // (part 2) now proves it from the assignment itself, and DynamicImmutability (part 3) consumes it.
        assertNotNull(m.fieldImmutable(), "the constructor's List.copyOf is the proof");
        assertTrue(m.fieldImmutable().isAtLeastImmutableHC());
        assertFalse(m.fieldIndependent().isDependent());
        assertFalse(m.methodIndependent().isDependent(), "the accessor no longer hands out mutable content");
        assertFalse(m.typeIndependent().isDependent());
        assertTrue(m.typeImmutable().isAtLeastImmutableHC(), "immutable: " + m.typeImmutable());
    }

    @Language("java")
    private static final String E_ANNOTATED_COPY_IN = """
            import java.util.List;
            import org.e2immu.annotation.Immutable;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = List.copyOf(items); }
              @Immutable(hc = true)
              public List<String> items() { return items; }
            }
            """;

    @DisplayName("E: D plus the annotation -- still nothing; the contract cannot rescue it")
    @Test
    public void testE() {
        Measured m = measure(E_ANNOTATED_COPY_IN);
        assertNotNull(m.contractedMethodImmutable());
        assertNotNull(m.methodImmutable(), "the contract is materialized");
        // The annotation on the ACCESSOR is still not what does the work -- only the field-side value is consumed
        // -- but the type now lifts anyway, because the inference proves the field from the constructor's copy.
        // So E has stopped being a limitation by ceasing to matter, not by being fixed.
        assertFalse(m.typeIndependent().isDependent());
        assertTrue(m.typeImmutable().isAtLeastImmutableHC(), "immutable: " + m.typeImmutable());
    }

    @Language("java")
    private static final String G_ANNOTATED_FIELD = """
            import java.util.List;
            import org.e2immu.annotation.Immutable;
            public class B {
              @Immutable(hc = true)
              private final List<String> items;
              public B(List<String> items) { this.items = List.copyOf(items); }
              public List<String> items() { return items; }
            }
            """;

    @DisplayName("G: the FIELD-side contract is materialized AND consumed -- the shape finally lifts")
    @Test
    public void testG() {
        Measured m = measure(G_ANNOTATED_FIELD);
        // part 1 for fields: @Immutable on a source field reaches IMMUTABLE_FIELD.
        assertNotNull(m.fieldImmutable(), "IMMUTABLE_FIELD is materialized for a source field");
        assertTrue(m.fieldImmutable().isImmutableHC());
        // part 3: and it is now consumed. Desired behaviour, no longer a limitation -- DynamicImmutability
        // grades the field by what it HOLDS rather than by its declared type, so handing it out shares nothing.
        // Note this is D's shape (copy in the constructor, plain `return field`), i.e. TypeInspectionImpl's:
        // the one that got nothing before, and the reason the feature exists.
        assertTrue(m.fieldIndependent().isAtLeastIndependentHc(), "field: " + m.fieldIndependent());
        assertTrue(m.methodIndependent().isAtLeastIndependentHc(), "accessor: " + m.methodIndependent());
        assertTrue(m.typeIndependent().isAtLeastIndependentHc(), "type: " + m.typeIndependent());
        assertTrue(m.typeImmutable().isImmutableHC(), "reaches @Immutable(hc=true): " + m.typeImmutable());
    }

    @Language("java")
    private static final String F_COPY_BOTH = """
            import java.util.List;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = List.copyOf(items); }
              public List<String> items() { return List.copyOf(items); }
            }
            """;

    @DisplayName("F: copying in BOTH directions lifts the whole type, with no analyzer change")
    @Test
    public void testF() {
        Measured m = measure(F_COPY_BOTH);
        // Desired behaviour, and it already works. Both leaks are closed by an explicit copy the engine can
        // see, so the field, the accessor and the type are all independent, and the type reaches immutable-HC.
        assertTrue(m.fieldIndependent().isIndependent());
        assertTrue(m.methodIndependent().isIndependent());
        assertTrue(m.typeIndependent().isIndependent());
        assertTrue(m.typeImmutable().isImmutableHC(), "reaches @Immutable(hc=true): " + m.typeImmutable());
        // note: still no IMMUTABLE_METHOD -- independence is carried by the links, not by a dynamic marker
        assertNull(m.methodImmutable());
    }
}
