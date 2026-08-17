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

package org.e2immu.analyzer.modification.analyzer.rewire;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.io.AnalysisFingerprint;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The derived-tier {@code Value.rewire} implementations (previously NYI): {@code IMPLEMENTATIONS}
 * ({@code SetOfMethodInfoImpl}), field {@code LINKS} ({@code LinksImpl}), and {@code METHOD_LINKS}
 * ({@code MethodLinkedVariablesImpl}). These are what a fingerprint-stable type's <em>full</em> analysis carry needs
 * (docs/analysis-rewiring.md); the parked {@code carryOnRewire} only reached the parse-time/intrinsic tier. Here we take
 * the real analyzed values and rewire them directly (they are not opted into {@code carryOnRewire}, so the rewire
 * phase would filter them out — the point is that the value-level rewire re-points references and does not throw).
 */
public class TestDerivedRewire extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            public class X {
                interface I { void m(); }
                static class Impl implements I { public void m() {} }
                private final List<String> data;
                X(List<String> d) { this.data = d; }
                public List<String> get() { return data; }
            }
            """;

    private AnalyzerBundle bundle;

    private TypeInfo analyze() throws IOException {
        bundle = buildAnalyzerBundle();
        TypeInfo x = bundle.javaInspector().parse("a.b.X", INPUT);
        List<Info> order = bundle.prepAnalyzer().doPrimaryType(x);
        new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build()).analyze(order);
        return x;
    }

    private static TypeInfo subType(TypeInfo x, String simpleName) {
        return x.subTypes().stream().filter(t -> simpleName.equals(t.simpleName())).findFirst().orElseThrow();
    }

    @DisplayName("IMPLEMENTATIONS, LINKS and METHOD_LINKS rewire: re-point references, no NYI")
    @Test
    public void test() throws IOException {
        TypeInfo x = analyze();
        MethodInfo abstractM = subType(x, "I").findUniqueMethod("m", 0);
        MethodInfo implM = subType(x, "Impl").findUniqueMethod("m", 0);
        FieldInfo dataField = x.fields().stream().filter(f -> "data".equals(f.name())).findFirst().orElseThrow();
        MethodInfo getMethod = x.findUniqueMethod("get", 0);

        ValueImpl.SetOfMethodInfoImpl implementations = abstractM.analysis()
                .getOrNull(PropertyImpl.IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.class);
        assertNotNull(implementations, "the abstract method must carry IMPLEMENTATIONS");
        assertTrue(contains(implementations, implM), "sanity: IMPLEMENTATIONS holds Impl.m");

        InfoMap infoMap = bundle.javaInspector().runtime().newInfoMap(Set.of(x));
        Set<TypeInfo> rewiredSet = infoMap.rewireAll();
        assertEquals(1, rewiredSet.size());
        TypeInfo x2 = rewiredSet.iterator().next();
        assertNotSame(x, x2);
        MethodInfo implM2 = infoMap.methodInfo(implM);
        assertNotSame(implM, implM2, "the implementation was rewired to a new object");

        // IMPLEMENTATIONS: the rewired set holds the rewired implementation object, not the replaced one
        Value.SetOfMethodInfo rewiredImpls = (Value.SetOfMethodInfo) implementations.rewire(infoMap);
        Set<MethodInfo> set = new HashSet<>();
        rewiredImpls.methodInfoSet().forEach(set::add);
        assertTrue(set.stream().anyMatch(mi -> mi == implM2), "rewired IMPLEMENTATIONS holds the rewired Impl.m");
        assertFalse(set.stream().anyMatch(mi -> mi == implM), "must not still hold the replaced Impl.m");

        // field LINKS: rewire must not throw (no synthetic-variable NYI) and stays a same-size Links
        LinksImpl links = dataField.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertNotNull(links, "the field must carry LINKS");
        Value rewiredLinks = assertDoesNotThrow(() -> links.rewire(infoMap));
        assertInstanceOf(LinksImpl.class, rewiredLinks);
        assertEquals(links.size(), ((LinksImpl) rewiredLinks).size());

        // METHOD_LINKS: rewire must not throw
        MethodLinkedVariablesImpl methodLinks = getMethod.analysis()
                .getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(methodLinks, "the getter must carry METHOD_LINKS");
        assertDoesNotThrow(() -> methodLinks.rewire(infoMap));
    }

    @DisplayName("the analyzer-output filtered rewire carries verdicts + links, unlike the carryOnRewire rewire")
    @Test
    public void testFullCarry() throws IOException {
        TypeInfo x = analyze();
        MethodInfo getMethod = x.findUniqueMethod("get", 0);
        MethodInfo abstractM = subType(x, "I").findUniqueMethod("m", 0);

        InfoMap infoMap = bundle.javaInspector().runtime().newInfoMap(Set.of(x));
        infoMap.rewireAll();

        // the analyzer-output rewire must not throw (every kept property implements rewire) and carries strictly more than
        // the carryOnRewire-filtered rewire.
        org.e2immu.language.cst.api.analysis.PropertyValueMap full =
                assertDoesNotThrow(() -> getMethod.analysis().rewire(infoMap, AnalysisFingerprint.ANALYZER_OUTPUT_ONLY));
        org.e2immu.language.cst.api.analysis.PropertyValueMap filtered = getMethod.analysis().rewire(infoMap);
        assertTrue(full.propertyValueStream().count() > filtered.propertyValueStream().count(),
                "the analyzer-output rewire carries verdicts + links; the plain rewire keeps only carryOnRewire");

        // METHOD_LINKS is carried by the analyzer-output rewire (re-pointed) but dropped by the plain rewire
        assertNotNull(full.getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class),
                "the analyzer-output rewire carries METHOD_LINKS");
        assertNull(filtered.getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class),
                "rewire drops METHOD_LINKS (not carryOnRewire)");

        // the abstract method's IMPLEMENTATIONS survive a full carry, re-pointed to the rewired implementation
        org.e2immu.language.cst.api.analysis.PropertyValueMap abstractFull =
                assertDoesNotThrow(() -> abstractM.analysis().rewire(infoMap, AnalysisFingerprint.ANALYZER_OUTPUT_ONLY));
        ValueImpl.SetOfMethodInfoImpl impls = abstractFull
                .getOrNull(PropertyImpl.IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.class);
        assertNotNull(impls, "the analyzer-output rewire carries IMPLEMENTATIONS");
        MethodInfo implM2 = infoMap.methodInfo(subType(x, "Impl").findUniqueMethod("m", 0));
        Set<MethodInfo> set = new HashSet<>();
        impls.methodInfoSet().forEach(set::add);
        assertTrue(set.stream().anyMatch(mi -> mi == implM2), "carried IMPLEMENTATIONS points at the rewired Impl.m");
    }

    private static boolean contains(Value.SetOfMethodInfo set, MethodInfo methodInfo) {
        for (MethodInfo mi : set.methodInfoSet()) {
            if (mi.equals(methodInfo)) return true;
        }
        return false;
    }
}
