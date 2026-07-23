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
import org.e2immu.analyzer.modification.analyzer.impl.EventualCluster;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@code EVENTUALLY_UNMODIFIED_PARAMETER} (spec: {@code docs/spec-eventually-unmodified-parameter.md}), the
 * parameter twin of {@code @NotModified(after=)}: a static helper that reads the argument through the
 * argument's own eventual field is honestly {@code @Modified} on that parameter, but leaves the argument
 * unmodified once the argument's marks have fired. The property is computed by the commit walk rooted in the
 * parameter, and consumed at call sites that hand a bare {@code this} to such a helper -- the labels join the
 * caller's {@code EVENTUALLY_NON_MODIFYING_METHOD} set, which is what makes the static-helper shape
 * ({@code ParameterizedTypePrinter.print(…, this, …)}) excusable at all.
 */
public class TestEventuallyUnmodifiedParameter extends CommonTest {

    private Set<String> eup(TypeInfo typeInfo, String methodName, int params, int index) {
        return typeInfo.findUniqueMethod(methodName, params).parameters().get(index).analysis()
                .getOrDefault(PropertyImpl.EVENTUALLY_UNMODIFIED_PARAMETER, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                .set();
    }

    private Set<String> nonModAfter(TypeInfo typeInfo, String methodName, int params) {
        return typeInfo.findUniqueMethod(methodName, params).analysis()
                .getOrDefault(PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                .set();
    }

    // the ParameterizedTypePrinter shape, reduced: a static helper reads the argument through the argument's
    // eventual field, so its parameter is honestly modified -- but unmodified once 'inspection' is committed
    // on the argument. The caller hands bare 'this' to the helper.
    @Language("java")
    private static final String INPUT = """
            import org.e2immu.support.EventuallyFinalOnDemand;

            public class A {
              static class T {
                private final EventuallyFinalOnDemand<String> inspection = new EventuallyFinalOnDemand<>();
                public void commit(String s) { inspection.setFinal(s); }
                public String data() { return inspection.get(); }
                public int mySize() { return Helper.size(this); }
              }
              static class Helper {
                static int size(T t) { return t.inspection.get().length(); }
                static int sizeViaAccessor(T t) { return t.data().length(); }
                static void reassign(T t) { t = null; }
              }
            }
            """;

    @DisplayName("gate on, composed with MODREACH: the helper parameter earns its after-labels, and the caller consumes them")
    @Test
    public void testStaticHelper() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            TypeInfo A = javaInspector.parse("A", INPUT);
            // the plain fixpoint freezes the helper parameters optimistically unmodified=TRUE (the known
            // TolerantWrite optimism the shadow pass exists to correct), and eup only fires on an honest
            // FALSE -- so run composed, exactly like the dogfood DoD environment
            var iterating = new org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl(javaInspector,
                    new org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl.ConfigurationBuilder()
                            .setMaxIterations(10)
                            .setModificationViaReachability(true)
                            .build());
            iterating.analyze(prepWork(A));
            TypeInfo T = A.findSubType("T");
            TypeInfo helper = A.findSubType("Helper");

            // the parameter reads through the argument's eventual field: eup = [inspection], the plain
            // verdict stays the honest FALSE
            assertEquals(Set.of("inspection"), eup(helper, "size", 1, 0));
            MethodInfo size = helper.findUniqueMethod("size", 1);
            assertFalse(size.parameters().getFirst().analysis()
                    .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue());
            // the same read through the argument's own accessor: the abstract/concrete accessor look-through
            // resolves t.data() to the field
            assertEquals(Set.of("inspection"), eup(helper, "sizeViaAccessor", 1, 0));
            // rebinding the parameter bails: no promise
            assertEquals(Set.of(), eup(helper, "reassign", 1, 0));

            // consumption: mySize() hands bare this to the helper; the helper's labels, committable on T,
            // join the caller's eventually-non-modifying set
            assertEquals(Set.of("inspection"), nonModAfter(T, "mySize", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }

    @DisplayName("gate off: nothing is computed, nothing is consumed")
    @Test
    public void testGateOff() {
        boolean saved = EventualCluster.ENABLED;
        EventualCluster.ENABLED = false;
        try {
            TypeInfo A = javaInspector.parse("A", INPUT);
            analyzer.go(prepWork(A));
            TypeInfo helper = A.findSubType("Helper");
            assertEquals(Set.of(), eup(helper, "size", 1, 0));
            assertEquals(Set.of(), eup(helper, "sizeViaAccessor", 1, 0));
            assertEquals(Set.of(), nonModAfter(A.findSubType("T"), "mySize", 0));
        } finally {
            EventualCluster.ENABLED = saved;
        }
    }
}
