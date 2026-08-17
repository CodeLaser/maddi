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
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Adjudication probe for maddi-modification-analyzer/immutability-transform-divergence.md (the
 * jfocus-transform thread's Point/@FinalFields vs Point_t/@ImmutableHC divergence): analyze the
 * UNTRANSFORMED Point and print the evidence trail — coords' UNMODIFIED_FIELD, LINKS,
 * INDEPENDENT_FIELD, and the type verdict — so the capping rule can be named.
 */
public class TestLoopTransformDivergence extends CommonTest {

    @Language("java")
    private static final String POINT = """
            package a.b;
            public class Point {
                private final int[] coords;
                public Point(int[] c) {
                    this.coords = new int[c.length];
                    for (int i = 0; i < c.length; i++) this.coords[i] = c[i];
                }
                public int total() {
                    int s = 0;
                    for (int x : coords) s += x;
                    return s;
                }
            }
            """;

    @DisplayName("defensive int[] copy: transported content is immutable, no dependence, @ImmutableHC")
    @Test
    public void test() {
        TypeInfo point = javaInspector.parse("a.b.Point", POINT);
        List<Info> ao = prepWork(point);
        analyzer.go(ao, 10);

        FieldInfo coords = point.getFieldByName("coords", true);
        System.out.println("DIVERGENCE immutable=" + point.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.class));
        System.out.println("DIVERGENCE unmodifiedField=" + coords.analysis().getOrNull(PropertyImpl.UNMODIFIED_FIELD,
                ValueImpl.BoolImpl.class));
        System.out.println("DIVERGENCE independentField=" + coords.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                ValueImpl.IndependentImpl.class));
        System.out.println("DIVERGENCE fieldLinks=" + coords.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class));
        Value.Independent ctorParamIndep = point.constructors().getFirst().parameters().getFirst()
                .analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.class);
        System.out.println("DIVERGENCE ctorParamIndependent=" + ctorParamIndep);

        // the adjudicated verdict (immutability-transform-divergence.md): a private, final,
        // construction-confined, never-exposed int[] — a defensive VALUE copy links coords ~/∈ to
        // the ctor parameter, but the transported content (int) is immutable: no aliasing, no
        // dependence. Both the transformed and untransformed sides now agree on @ImmutableHC.
        org.junit.jupiter.api.Assertions.assertTrue(coords.isUnmodified());
        org.junit.jupiter.api.Assertions.assertEquals("@Independent",
                String.valueOf(coords.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                        ValueImpl.IndependentImpl.class)));
        org.junit.jupiter.api.Assertions.assertEquals("@Immutable(hc=true)",
                String.valueOf(point.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                        ValueImpl.ImmutableImpl.class)));
    }

    /**
     * The SOUND-DIRECTION control: the same defensive-copy shape over MUTABLE elements
     * (StringBuilder[]). Here the elements themselves are shared — mutation through the caller's
     * array contents reaches the field's contents — so the dependence is REAL and the type must
     * NOT be promoted. Guards the transported-content filter against over-firing.
     */
    @Language("java")
    private static final String POINT_MUTABLE = """
            package a.b;
            public class PointM {
                private final StringBuilder[] parts;
                public PointM(StringBuilder[] c) {
                    this.parts = new StringBuilder[c.length];
                    for (int i = 0; i < c.length; i++) this.parts[i] = c[i];
                }
                public int total() {
                    int s = 0;
                    for (StringBuilder x : parts) s += x.length();
                    return s;
                }
            }
            """;

    @DisplayName("defensive StringBuilder[] copy: elements are shared and mutable, dependence is real")
    @Test
    public void testMutableElements() {
        TypeInfo pointM = javaInspector.parse("a.b.PointM", POINT_MUTABLE);
        List<Info> ao = prepWork(pointM);
        analyzer.go(ao, 10);

        FieldInfo parts = pointM.getFieldByName("parts", true);
        Value.Independent indep = parts.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                ValueImpl.IndependentImpl.class);
        System.out.println("DIVERGENCE-M independentField=" + indep);
        System.out.println("DIVERGENCE-M immutable=" + pointM.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.class));
        org.junit.jupiter.api.Assertions.assertNotNull(indep);
        org.junit.jupiter.api.Assertions.assertFalse(indep.isIndependent(),
                "shared mutable elements: the field depends on the ctor parameter");
        Value.Immutable imm = pointM.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.class);
        org.junit.jupiter.api.Assertions.assertNotNull(imm);
        org.junit.jupiter.api.Assertions.assertFalse(imm.isAtLeastImmutableHC(),
                "must not be promoted: the hidden content is reachable through the caller's array");
    }
}
