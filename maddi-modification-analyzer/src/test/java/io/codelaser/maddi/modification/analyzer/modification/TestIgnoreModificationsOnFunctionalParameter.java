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

package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code @IgnoreModifications} on a parameter must disclaim what that object DOES, not merely protect the object
 * itself — including what it does to the arguments it is handed.
 * <p>
 * {@code MethodModification.go} records modification at four sites for one call {@code receiver.callee(args…)}.
 * Three of them filter on {@code isIgnoreModifications}; the ARGUMENT site does not consult the receiver at all.
 * It asks only whether the CALLEE's declared parameter is {@code @Modified} — for a functional interface that is
 * {@code java.util.function.Function.apply(@Modified T t)} in the JDK hints, which nothing at the call site can
 * influence. So {@code f.apply(this.t)} taints {@code this.t} however the author annotates {@code f}.
 * <p>
 * Measured on vavr 1.0.1 (2026-09-22): {@code @IgnoreModifications} on all five of {@code io.vavr.Tuple2}'s
 * function parameters set the six {@code ignoreModsParameter} properties and changed NOTHING else — fields still
 * modified, methods still modifying, 0 of 133 type verdicts moved. The codebase's own motivating example,
 * {@code Element.visit(@IgnoreModifications Predicate p)} calling {@code p.test(this)}, has the same shape; it
 * works only because {@code Predicate.test}'s ARGUMENT was changed to {@code @NotModified} on 2026-07-23, i.e.
 * fixed on the callee side because the receiver's disclaimer does not reach.
 */
public class TestIgnoreModificationsOnFunctionalParameter extends CommonTest {

    @Language("java")
    private static final String SOURCE = """
            package a.b;
            import io.codelaser.maddi.annotation.rare.IgnoreModifications;
            import java.util.function.Function;

            public class X {

                // CONTROL: no disclaimer, so handing the field to the function taints it
                static class Plain<T> {
                    private final T t;

                    Plain(T t) {
                        this.t = t;
                    }

                    <U> U apply(Function<T, U> f) {
                        return f.apply(t);
                    }
                }

                // the author declares that whatever f does is not this carrier's modification
                static class Disclaimed<T> {
                    private final T t;

                    Disclaimed(T t) {
                        this.t = t;
                    }

                    <U> U apply(@IgnoreModifications Function<T, U> f) {
                        return f.apply(t);
                    }
                }
            }
            """;

    @DisplayName("a disclaimed functional parameter must not taint the fields handed to it")
    @Test
    public void disclaimedReceiverDoesNotTaintArguments() throws IOException {
        run(false);
    }

    /**
     * ⭐ The FAST twin of a defect that only {@code TestShadowCloneBench} ({@code @Tag("slow")}) caught.
     * <p>
     * maddi computes modification TWICE: in {@code MethodModification} (the fixpoint) and in
     * {@code ShadowModificationPass}, written as its mirror and made authoritative by the MODREACH cutover.
     * Applying the receiver-disclaimer rule to only the first left the two disagreeing, which surfaced as
     * TestShadowCloneBench's pinned divergence moving {@code unmodifiedParameter 812 -> 814} — while 420 link +
     * 304 analyzer + 47 run-openjdk fast tests were all green. This variant runs the same source with MODREACH
     * ON, where the shadow pass is the writer, so the invariant "both implementations honour the disclaimer" is
     * checked in three seconds instead of only by the corpus test.
     */
    @DisplayName("... and the MODREACH cutover, the mirror implementation, must honour it identically")
    @Test
    public void disclaimedReceiverUnderModificationReachability() throws IOException {
        run(true);
    }

    private void run(boolean modificationViaReachability) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo typeInfo = bundle.javaInspector().parse("a.b.X", SOURCE);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(typeInfo);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                        .setModificationViaReachability(modificationViaReachability).build());
        analyzer.analyze(analysisOrder);

        String what = " (modReach=" + modificationViaReachability + ")";
        // the control first: without it, the assertion below could pass because nothing taints anything
        assertEquals(ValueImpl.BoolImpl.FALSE, unmodified(typeInfo, "Plain"),
                "the CONTROL must be MODIFIED — otherwise this test proves nothing" + what);
        assertEquals(ValueImpl.BoolImpl.TRUE, unmodified(typeInfo, "Disclaimed"),
                "@IgnoreModifications on the function parameter must disclaim what the function does to the"
                + " field it is handed" + what);
    }

    private ValueImpl.BoolImpl unmodified(TypeInfo typeInfo, String subType) {
        FieldInfo field = typeInfo.findSubType(subType).fields().getFirst();
        return field.analysis().getOrNull(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
    }
}
