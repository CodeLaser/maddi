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
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Part 2: inferring what a field HOLDS by following a defensive copy from the call site, through a constructor
 * parameter, to the field. The shape is {@code TypeInspectionImpl}'s — the copy happens in
 * {@code Builder.commit()}, and the product constructor merely assigns — so a local "assigned
 * {@code List.copyOf(...)}" detector sees nothing.
 * <p>
 * Every test below either exercises the inference or pins one of its two soundness gates. The negatives matter
 * more than the positive: an inference that concluded in any of those cases would be producing a false verdict,
 * not an imprecise one.
 */
public class TestDynamicImmutabilityInference extends CommonTest {

    private record Measured(Value.Immutable fieldImmutable, Value.Independent typeIndependent,
                            Value.Immutable typeImmutable) {
    }

    private Measured measure(String source, String fieldOwner, String fieldName) {
        TypeInfo B = javaInspector.parse("B", source);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        TypeInfo owner = "B".equals(fieldOwner) ? B : B.findSubType(fieldOwner);
        return new Measured(
                owner.getFieldByName(fieldName, true).analysis()
                        .getOrNull(PropertyImpl.IMMUTABLE_FIELD, ValueImpl.ImmutableImpl.class),
                owner.analysis().getOrNull(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class),
                owner.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class));
    }

    // ---- the target shape ----

    @Language("java")
    private static final String COPY_AT_CALL_SITE = """
            import java.util.ArrayList;
            import java.util.List;
            public class B {
              private final List<String> items;
              private B(List<String> items) { this.items = items; }
              public List<String> items() { return items; }
              public static class Builder {
                private final List<String> collected = new ArrayList<>();
                public void add(String s) { collected.add(s); }
                public B commit() { return new B(List.copyOf(collected)); }
              }
            }
            """;

    @DisplayName("the copy is followed from Builder.commit() through the private constructor to the field")
    @Test
    public void testCopyAtCallSite() {
        Measured m = measure(COPY_AT_CALL_SITE, "B", "items");
        assertNotNull(m.fieldImmutable(), "the field should have been inferred dynamically immutable");
        assertTrue(m.fieldImmutable().isAtLeastImmutableHC(), "inferred: " + m.fieldImmutable());
        // and the point of the whole exercise: the type escapes @Dependent without any annotation
        assertNotNull(m.typeIndependent());
        assertFalse(m.typeIndependent().isDependent(), "independent: " + m.typeIndependent());
        assertNotNull(m.typeImmutable());
        assertTrue(m.typeImmutable().isAtLeastImmutableHC(), "immutable: " + m.typeImmutable());
    }

    // ---- gate 1: callers must be enumerable, i.e. the constructor must be private ----

    @Language("java")
    private static final String PUBLIC_CONSTRUCTOR = """
            import java.util.ArrayList;
            import java.util.List;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = items; }
              public List<String> items() { return items; }
              public static class Builder {
                private final List<String> collected = new ArrayList<>();
                public B commit() { return new B(List.copyOf(collected)); }
              }
            }
            """;

    @DisplayName("a public constructor blocks it: callers outside the source set cannot be enumerated")
    @Test
    public void testPublicConstructorBlocks() {
        Measured m = measure(PUBLIC_CONSTRUCTOR, "B", "items");
        // identical body to the target shape apart from one keyword. Any consumer may legally call
        // new B(myMutableList), so the visible caller's List.copyOf proves nothing.
        assertNull(m.fieldImmutable(), "must not infer through a publicly reachable constructor");
    }

    // ---- gate 2: every call site must pass an immutable object ----

    @Language("java")
    private static final String ONE_BAD_CALL_SITE = """
            import java.util.ArrayList;
            import java.util.List;
            public class B {
              private final List<String> items;
              private B(List<String> items) { this.items = items; }
              public List<String> items() { return items; }
              public static class Builder {
                private final List<String> collected = new ArrayList<>();
                public B commit() { return new B(List.copyOf(collected)); }
                public B commitRaw() { return new B(collected); }
              }
            }
            """;

    @DisplayName("one call site passing the raw list kills it: the value is a meet, never a sample")
    @Test
    public void testOneBadCallSiteBlocks() {
        Measured m = measure(ONE_BAD_CALL_SITE, "B", "items");
        assertNull(m.fieldImmutable(), "one unproven call site must sink the whole inference");
    }

    // ---- Collections.unmodifiableList is a view, not a copy ----

    @Language("java")
    private static final String UNMODIFIABLE_VIEW = """
            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.List;
            public class B {
              private final List<String> items;
              private B(List<String> items) { this.items = items; }
              public List<String> items() { return items; }
              public static class Builder {
                private final List<String> collected = new ArrayList<>();
                public B commit() { return new B(Collections.unmodifiableList(collected)); }
              }
            }
            """;

    @DisplayName("Collections.unmodifiableList is not proof: the caller keeps a handle on the backing list")
    @Test
    public void testUnmodifiableViewIsNotProof() {
        Measured m = measure(UNMODIFIABLE_VIEW, "B", "items");
        // deliberately not annotated @ImmutableContainer in the AAPI, and this is why: the Builder can still
        // add to `collected` afterwards and the field would see it.
        assertNull(m.fieldImmutable(), "a view must not be accepted as a defensive copy");
    }

    // ---- the purely local shape still works (no call-site walk needed) ----

    @Language("java")
    private static final String COPY_IN_CONSTRUCTOR = """
            import java.util.List;
            public class B {
              private final List<String> items;
              public B(List<String> items) { this.items = List.copyOf(items); }
              public List<String> items() { return items; }
            }
            """;

    @DisplayName("a copy made in the constructor needs no call-site walk, so a public constructor is fine")
    @Test
    public void testCopyInConstructor() {
        Measured m = measure(COPY_IN_CONSTRUCTOR, "B", "items");
        assertNotNull(m.fieldImmutable(), "the assignment itself is the proof here");
        assertTrue(m.fieldImmutable().isAtLeastImmutableHC());
        assertNotNull(m.typeImmutable());
        assertTrue(m.typeImmutable().isAtLeastImmutableHC(), "immutable: " + m.typeImmutable());
    }

    // ---- a non-final field: the assignment set is not knowably complete ----

    @Language("java")
    private static final String REASSIGNED_LATER = """
            import java.util.ArrayList;
            import java.util.List;
            public class B {
              private List<String> items;
              private B(List<String> items) { this.items = items; }
              public List<String> items() { return items; }
              public void reset() { this.items = new ArrayList<>(); }
              public static class Builder {
                private final List<String> collected = new ArrayList<>();
                public B commit() { return new B(List.copyOf(collected)); }
              }
            }
            """;

    @DisplayName("a field assigned outside construction is not inferable: reset() would break the promise")
    @Test
    public void testReassignedFieldBlocks() {
        Measured m = measure(REASSIGNED_LATER, "B", "items");
        assertNull(m.fieldImmutable(), "a non-final field must not carry a dynamic-immutability verdict");
    }
}
