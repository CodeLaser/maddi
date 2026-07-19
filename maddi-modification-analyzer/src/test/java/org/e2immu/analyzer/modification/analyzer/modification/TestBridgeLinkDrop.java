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

package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Task #43 (immutability-transform-divergence.md): in-repo reproduction of the loop-transform
 * bridge's element-aliasing link drop. A mini Loop/LoopData (Object[] slot array + builder + FI
 * body + run driver) mimics jfocus-transform's support types; the PointM_t-shaped transformed
 * code stores the caller's StringBuilder[] in slot 0, reloads it with a downcast inside the body
 * method, and copies elements into the field. The field's elements ALIAS the caller's — the field
 * must be @Dependent and the type must NOT promote past @FinalFields. Probes print the link
 * summaries at every hop so the dropping point can be named.
 */
public class TestBridgeLinkDrop extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            import java.util.stream.IntStream;
            public class PointMT {
                public interface LoopData {
                    Object get(int i);
                    Object loopValue();
                }
                public static class LoopDataImpl implements LoopData {
                    private final Object[] data;
                    private final Iterator<?> iterator;
                    private final Function<LoopData, LoopData> body;
                    private Object loopValue;
                    private LoopDataImpl(Object[] data, Iterator<?> iterator, Function<LoopData, LoopData> body) {
                        this.data = data;
                        this.iterator = iterator;
                        this.body = body;
                    }
                    public Object get(int i) { return data[i]; }
                    public Object loopValue() { return loopValue; }
                    public static class Builder {
                        private final Object[] data = new Object[4];
                        private Iterator<?> iterator;
                        private Function<LoopData, LoopData> body;
                        public Builder set(int i, Object o) { data[i] = o; return this; }
                        public Builder iterator(Iterator<?> it) { this.iterator = it; return this; }
                        public Builder body(Function<LoopData, LoopData> b) { this.body = b; return this; }
                        public LoopDataImpl build() { return new LoopDataImpl(data, iterator, body); }
                    }
                }
                public static LoopData run(LoopData ldIn) {
                    LoopDataImpl ld = (LoopDataImpl) ldIn;
                    LoopData current = ldIn;
                    while (ld.iterator.hasNext()) {
                        ld.loopValue = ld.iterator.next();
                        current = ld.body.apply(current);
                    }
                    return current;
                }

                private final StringBuilder[] parts;
                public PointMT(StringBuilder[] c) {
                    this.parts = new StringBuilder[c.length];
                    LoopData ldIn = new LoopDataImpl.Builder()
                        .set(0, c)
                        .iterator(IntStream.iterate(0, i -> i < c.length, i -> i + 1).iterator())
                        .body(this::ctorBody)
                        .build();
                    run(ldIn);
                }
                private LoopData ctorBody(LoopData ld) {
                    StringBuilder[] c = (StringBuilder[]) ld.get(0);
                    int i = (int) ld.loopValue();
                    this.parts[i] = c[i];
                    return ld;
                }
                public int total() {
                    int s = 0;
                    for (StringBuilder x : parts) s += x.length();
                    return s;
                }
            }
            """;

    @DisplayName("bridge reproduction: where does the parts~c element link die?")
    @Test
    public void test() {
        TypeInfo pointMT = javaInspector.parse("a.b.PointMT", INPUT);
        List<Info> ao = prepWork(pointMT);
        analyzer.go(ao, 10);

        FieldInfo parts = pointMT.getFieldByName("parts", true);
        System.out.println("BRIDGE immutable=" + pointMT.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.class));
        System.out.println("BRIDGE independentField=" + parts.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                ValueImpl.IndependentImpl.class));
        System.out.println("BRIDGE fieldLinks=" + parts.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class));

        // hop 1: the body method's own summary — does ctorBody link this.parts (content) to ld?
        MethodInfo ctorBody = pointMT.findUniqueMethod("ctorBody", 1);
        MethodLinkedVariables mlvBody = ctorBody.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        System.out.println("BRIDGE ctorBody METHOD_LINKS=" + mlvBody);

        // hop 1b: statement-level links inside ctorBody — do the links exist locally and die at
        // summary time, or never form?
        ctorBody.methodBody().statements().forEach(stmt -> {
            var vd = org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl.of(stmt);
            if (vd == null) return;
            vd.variableInfoStream().forEach(vi -> {
                var links = vi.linkedVariables();
                if (links != null && !links.isEmpty()) {
                    System.out.println("BRIDGE-STMT " + stmt.source().index() + " " + vi.variable().simpleName()
                                       + " :: " + links);
                }
            });
        });

        // hop 2: Builder.set — does the parameter o link into this.data?
        TypeInfo builder = pointMT.findSubType("LoopDataImpl").findSubType("Builder");
        MethodInfo set = builder.findUniqueMethod("set", 2);
        System.out.println("BRIDGE Builder.set METHOD_LINKS=" + set.analysis().getOrNull(
                MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class));

        // hop 3: LoopDataImpl.get — does the return value link to this.data content?
        MethodInfo get = pointMT.findSubType("LoopDataImpl").findUniqueMethod("get", 1);
        System.out.println("BRIDGE LoopDataImpl.get METHOD_LINKS=" + get.analysis().getOrNull(
                MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class));

        // hop 4: the constructor — do ldIn/parts carry links to the parameter c?
        MethodInfo ctor = pointMT.constructors().getFirst();
        System.out.println("BRIDGE ctor METHOD_LINKS=" + ctor.analysis().getOrNull(
                MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class));
        System.out.println("BRIDGE ctorParam c UNMODIFIED=" + ctor.parameters().getFirst().analysis()
                .getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class)
                + " INDEPENDENT=" + ctor.parameters().getFirst().analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.class));

        // CHARACTERIZATION of the CURRENT, UNSOUND state (task #43) — these assertions pin the bug
        // and must FLIP when the fix lands. Ground truth: parts[i] aliases the caller's c[i]
        // (StringBuilder, mutable) — the field must be @Dependent and the type must NOT pass
        // @FinalFields. Diagnosis (2026-07-19, hop probes above):
        // 1. statement level: the links EXIST locally — `this.parts~c` at the write, and
        //    `c.§$ ← 0:ld.§$` at the accessor call (the abstract interface callee weakens the
        //    implementation's `ret ∈ this.data` to the some-value/hidden-content §$ face);
        // 2. summary level: FieldAnalyzer.computeLinkedVariables drops links to LOCALS without
        //    composing one hop (parts~c ∘ c.§$←ld.§$ never becomes parts~ld.§$), and
        //    LinkComputer.filteredPi drops ld's local-target links — so METHOD_LINKS and the
        //    field's LINKS both come out empty;
        // 3. even composed, ld is a PRIVATE method's parameter: the exposure chain to the PUBLIC
        //    ctor param c runs through the FI application (ld ≡ ldIn inside run) — private-param
        //    dependence must propagate through the Λ application for independence.
        // Fix design in immutability-transform-divergence.md / task #43.
        assertEquals("@Immutable(hc=true)", String.valueOf(pointMT.analysis().getOrNull(
                        PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class)),
                "CURRENT (unsound) verdict — flips to @FinalFields when #43 lands");
        assertEquals("@Independent", String.valueOf(parts.analysis().getOrNull(
                        PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.class)),
                "CURRENT (unsound) verdict — flips to @Dependent when #43 lands");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
}
