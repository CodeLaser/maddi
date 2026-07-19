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

package org.e2immu.analyzer.modification.analyzer.integration;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
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
 * The 51% type-null cluster (elasticsearch first contact): nullness roots in verdicts that never
 * arrive (here: an abstract method without hints keeps its interface undecided) and CASCADES
 * through non-private field types. The breaking pass must convert "a verdict that will never
 * arrive" into the pessimistic FINAL_FIELDS instead of leaving the whole closure null.
 */
public class TestTypeNullBreaking extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            class X {
                interface I {
                    void m(List<String> in);
                }
                static class Holder {
                    final I callback;
                    Holder(I callback) { this.callback = callback; }
                    I callback() { return callback; }
                }
            }
            """;

    @DisplayName("undecided-forever types are broken to FINAL_FIELDS, and the cascade with them")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);
        IteratingAnalyzer iterating = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(20).build());
        iterating.analyze(analysisOrder);

        TypeInfo I = X.findSubType("I");
        TypeInfo holder = X.findSubType("Holder");
        Value.Immutable immI = I.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        Value.Immutable immHolder = holder.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.class);
        assertNotNull(immI, "the interface must not stay immutability-undecided after breaking");
        assertNotNull(immHolder, "the cascade must be broken with it");
        // pessimistic, never optimistic: neither may claim immutability from missing information
        assertFalse(immI.isAtLeastImmutableHC(), "broken verdict must be pessimistic: " + immI);
        assertFalse(immHolder.isAtLeastImmutableHC(), "broken verdict must be pessimistic: " + immHolder);
    }
}
