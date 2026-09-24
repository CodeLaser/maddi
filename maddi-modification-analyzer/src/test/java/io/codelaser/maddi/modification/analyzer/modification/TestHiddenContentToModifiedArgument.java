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

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * A field whose declared type is an unbound type parameter is hidden content of its holder: the holder cannot modify
 * it, so handing it to a {@code @Modified} parameter modifies neither the field nor the holder (road to immutability
 * 030 and 080: "we can ignore it here, because t is part of the hidden content of the type").
 * <ul>
 * <li>{@code Pair.mapFirst}: vavr's {@code Tuple2.map}, {@code f.apply(first)} with {@code first : T1} -- the shape
 *     that, through one {@code Tuple2.map} call, marked {@code List.Cons.tail} modified.</li>
 * <li>{@code Holder.perform}: the control -- a CONCRETE, non-immutable field handed over (vavr's
 *     {@code FutureImpl.perform}); that is accessible content and stays a modification.</li>
 * <li>{@code applyTo}/{@code caller2}/{@code caller3}: a type-parameter PARAMETER handed over stays modified. That is
 *     not the book's answer ({@code t} is hidden content of the method too), but it is the one channel that carries a
 *     concrete function's modification back to the caller's argument; exempting it loses {@code caller2}'s and
 *     {@code caller3}'s real modification of {@code sb}. Same for {@code fold}'s {@code zero}.</li>
 * <li>{@code caller}: the book's call-site half -- a concrete {@code Append} modifies {@code first}, which is the
 *     caller's {@code sb} -- is NOT implemented, before or after this rule: {@code sb} comes out unmodified.</li>
 * </ul>
 * {@code Fn.apply}'s parameter is CONTRACTED {@code @Modified}, the unit-scale stand-in for the JDK hint
 * {@code Function.apply(@Modified T)}; without the contract the abstract fold already leaves an {@code A}-typed
 * parameter unmodified, even with {@code Append} modifying its argument.
 */
public class TestHiddenContentToModifiedArgument extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import io.codelaser.maddi.annotation.Modified;
            import java.util.List;

            public class X {
                interface Fn<A, R> {
                    R apply(@Modified A a);
                }

                static final class Append implements Fn<StringBuilder, StringBuilder> {
                    @Override
                    public StringBuilder apply(StringBuilder sb) {
                        return sb.append("!");
                    }
                }

                static final class Pair<T1, T2> {
                    private final T1 first;
                    private final T2 second;

                    Pair(T1 first, T2 second) {
                        this.first = first;
                        this.second = second;
                    }

                    <U> Pair<U, T2> mapFirst(Fn<? super T1, ? extends U> f) {
                        return new Pair<>(f.apply(first), second);
                    }

                    T1 first() {
                        return first;
                    }
                }

                static <U, T> U fold(List<T> xs, U zero, Fn<U, U> f) {
                    U acc = zero;
                    for (T x : xs) {
                        acc = f.apply(acc);
                    }
                    return acc;
                }

                static final class Holder {
                    private final StringBuilder sb = new StringBuilder();

                    void perform(Fn<StringBuilder, ?> f) {
                        f.apply(sb);
                    }

                    int length() {
                        return sb.length();
                    }
                }

                static void caller(StringBuilder sb) {
                    Pair<StringBuilder, Integer> p = new Pair<>(sb, 1);
                    p.mapFirst(new Append());
                }

                static <T> void applyTo(T t, Fn<T, ?> f) {
                    f.apply(t);
                }

                static void caller2(StringBuilder sb) {
                    applyTo(sb, new Append());
                }

                static void caller3(StringBuilder sb) {
                    applyTo(sb, s -> s.append("?"));
                }
            }
            """;

    @DisplayName("a hidden-content field handed to a @Modified parameter does not modify its holder")
    @Test
    public void test() throws IOException {
        run(false);
    }

    @DisplayName("... and the same with the MODREACH cutover on, as every corpus run has it")
    @Test
    public void testModReach() throws IOException {
        run(true);
    }

    private void run(boolean modReach) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo x = bundle.javaInspector().parse("a.b.X", INPUT);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(x);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                        .setModificationViaReachability(modReach).build());
        analyzer.analyze(analysisOrder);

        TypeInfo pair = x.findSubType("Pair");
        TypeInfo holder = x.findSubType("Holder");
        MethodInfo apply = x.findSubType("Fn").findUniqueMethod("apply", 1);
        MethodInfo mapFirst = pair.findUniqueMethod("mapFirst", 1);
        MethodInfo perform = holder.findUniqueMethod("perform", 1);
        MethodInfo applyTo = x.findUniqueMethod("applyTo", 2);
        FieldInfo first = pair.getFieldByName("first", true);
        FieldInfo sbField = holder.getFieldByName("sb", true);
        ParameterInfo zero = x.findUniqueMethod("fold", 3).parameters().get(1);

        System.out.println("### modReach=" + modReach);
        System.out.println("### Pair.first unmodified=" + first.isUnmodified() + " mapFirst modifying="
                           + mapFirst.isModifying() + " Pair " + immutable(pair));
        System.out.println("### Holder.sb unmodified=" + sbField.isUnmodified() + " perform modifying="
                           + perform.isModifying() + " Holder " + immutable(holder));
        System.out.println("### applyTo:t modified=" + applyTo.parameters().getFirst().isModified()
                           + " fold:zero modified=" + zero.isModified()
                           + " caller:sb " + modified(x, "caller") + " caller2:sb " + modified(x, "caller2")
                           + " caller3:sb " + modified(x, "caller3"));

        assertTrue(apply.parameters().getFirst().isModified(), "precondition: the contract");

        assertTrue(first.isUnmodified(), "first : T1 is hidden content of Pair");
        assertFalse(mapFirst.isModifying(), "handing hidden content to f does not modify the pair");
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE_HC, immutable(pair));

        assertFalse(sbField.isUnmodified(), "control: a concrete StringBuilder handed over IS modified");
        assertTrue(perform.isModifying());
        assertSame(ValueImpl.ImmutableImpl.FINAL_FIELDS, immutable(holder));

        assertTrue(applyTo.parameters().getFirst().isModified(), "a type-parameter parameter stays the channel");
        assertTrue(modified(x, "caller2"), "Append modifies caller2's sb");
        assertTrue(modified(x, "caller3"), "the lambda modifies caller3's sb");
        assertFalse(modified(x, "caller"), "the call-site half is not implemented (see the class comment)");
    }

    private static boolean modified(TypeInfo x, String method) {
        return x.findUniqueMethod(method, 1).parameters().getFirst().isModified();
    }

    private static Value.Immutable immutable(TypeInfo t) {
        return t.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
    }
}
