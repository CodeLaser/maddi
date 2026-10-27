/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.java.openjdk.constructor;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⭐ A CHARACTERIZATION TEST, written to REFUTE a diagnosis rather than to fix a defect.
 *
 * <p>A timefold parse failed with <i>"a second definition of it is now arriving FROM A COMPILED ARTIFACT with a
 * member the committed one does not have: SolverTerminationConfig(Duration,Duration,Integer,Duration,Double)"</i>,
 * and the committed source type held constructors of 1, 2 and 3 parameters for a FIVE-component record whose
 * source declares a compact constructor and two convenience ones. The obvious reading — "a compact constructor is
 * modelled without its canonical parameter list" — is WRONG, and this test is what says so: both record shapes
 * below come out correct, with the canonical parameters present.
 *
 * <p>⛔ Acting on that reading costs real time. Synthesising the canonical constructor the way the javaparser
 * front end does ({@code ParseTypeDeclaration#createSyntheticConstructor}) collides with the constructor this
 * path already builds from javac's generated {@code JCMethodDecl}: {@code MethodInfoImpl.builder()} on a
 * committed method, 62 failures in this module alone.
 *
 * <p>⭐ The real cause was a MISSING CLASSPATH ENTRY. {@code jackson-annotations} was absent from that source
 * set's parse classpath, javac reported {@code package com.fasterxml.jackson.annotation does not exist} 50 times
 * in that one file, and its error recovery truncated the annotated record header — so the compact constructor
 * was modelled with 1 of its 5 parameters. Same family as the test-jar defect fixed in
 * {@code TestTestJarOnClasspath}: a classpath gap, surfacing as a scanner error far from its cause.
 * ⚠ <b>When maddi reports something structurally impossible about ordinary Java, grep the log for javac's own
 * errors first.</b>
 */
public class TestCompactConstructorProbe extends CommonTest {

    @Language("java")
    private static final String COMPACT_AND_CONVENIENCE = """
            package a.b;

            public record R(int a, String b) {
                public R {
                    if (a < 0) throw new IllegalArgumentException();
                }
                public R(int a) {
                    this(a, "x");
                }
                public static R make() {
                    return new R(1, "y");
                }
            }
            """;

    /** No compact constructor at all: the canonical one must be synthesised. */
    @Language("java")
    private static final String CONVENIENCE_ONLY = """
            package a.b;

            public record S(int a, String b) {
                public S(int a) {
                    this(a, "x");
                }
            }
            """;

    @DisplayName("a compact constructor carries the record's components as its parameters")
    @Test
    public void compactConstructorHasTheCanonicalParameters() {
        TypeInfo R = scan("a.b.R", COMPACT_AND_CONVENIENCE);
        MethodInfo canonical = R.findConstructor(2);
        assertNotNull(canonical, "the 2-component record must have a 2-parameter constructor");
        assertTrue(canonical.isCompactConstructor(), "and it is the COMPACT one, not a synthesised copy");
        assertEquals("int", canonical.parameters().getFirst().parameterizedType().typeInfo().simpleName());
        assertEquals("String", canonical.parameters().get(1).parameterizedType().typeInfo().simpleName());
        assertEquals(2, R.constructors().size(), "exactly two: the compact one and the convenience one");
    }

    @DisplayName("a record with no compact constructor gets a synthetic canonical one")
    @Test
    public void withoutACompactConstructorTheCanonicalOneIsSynthesised() {
        TypeInfo S = scan("a.b.S", CONVENIENCE_ONLY);
        MethodInfo canonical = S.findConstructor(2);
        assertNotNull(canonical);
        assertTrue(canonical.isSynthetic(), "synthesised, because the source declares no canonical constructor");
        assertEquals(2, S.constructors().size());
    }
}
