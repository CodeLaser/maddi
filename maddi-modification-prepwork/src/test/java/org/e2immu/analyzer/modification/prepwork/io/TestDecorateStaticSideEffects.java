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

package org.e2immu.analyzer.modification.prepwork.io;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
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
 * {@code DecoratorImpl} must surface a computed/contracted {@code STATIC_SIDE_EFFECTS_METHOD} verdict as a
 * {@code @StaticSideEffects} decoration, so the global-escape signal reaches the IDE surfaces (all three
 * front-ends render {@code DecoratorImpl.annotations(...)}). See road-to-immutability §050 "Static side effects"
 * and docs/eventual-info-hierarchy.md.
 */
public class TestDecorateStaticSideEffects extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class X {
                public void reconfigure() { }
                public void plain() { }
            }
            """;

    private static AnnotationExpression named(List<AnnotationExpression> list, String simpleName) {
        return list.stream().filter(a -> simpleName.equals(a.typeInfo().simpleName())).findFirst().orElse(null);
    }

    @DisplayName("a static-side-effect verdict renders as @StaticSideEffects; a plain method carries none")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        MethodInfo reconfigure = X.findUniqueMethod("reconfigure", 0);
        MethodInfo plain = X.findUniqueMethod("plain", 0);

        // what StaticSideEffectAnalyzerImpl / the safe-surface contract produces
        reconfigure.analysis().set(PropertyImpl.STATIC_SIDE_EFFECTS_METHOD, ValueImpl.BoolImpl.TRUE);

        DecoratorImpl d = new DecoratorImpl(runtime, javaInspector.mainSources());

        assertNotNull(named(d.annotations(reconfigure), "StaticSideEffects"),
                "expected @StaticSideEffects on the reconfiguring method");
        assertNull(named(d.annotations(plain), "StaticSideEffects"),
                "a method with no static side effect must not carry @StaticSideEffects");
    }
}
