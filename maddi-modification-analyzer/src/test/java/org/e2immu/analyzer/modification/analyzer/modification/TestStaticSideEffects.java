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
import org.e2immu.analyzer.modification.analyzer.impl.StaticSideEffectAnalyzerImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code @StaticSideEffects}: a method modifies static/global state of another type (road-to-immutability §050).
 * The global-escape signal underpinning the confinement guard's method-granularity arm. Gated on env {@code SSE}
 * (here flipped in-process). First cut: an assignment to, or a modifying call on, another type's static field.
 */
public class TestStaticSideEffects extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class Config {
                static int level;
            }
            public class X {
                private int n;
                void raise() { Config.level = 5; }       // assignment to another type's static field -> SSE
                void pure() { int x = 1 + 1; }           // no static state touched
                void own() { this.n = 3; }               // own instance field -> not a static side effect
            }
            """;

    private static boolean sse(MethodInfo mi) {
        return mi.analysis().getOrDefault(PropertyImpl.STATIC_SIDE_EFFECTS_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @DisplayName("assignment to another type's static field is a static side effect; own/pure methods are not")
    @Test
    public void test() throws IOException {
        boolean saved = StaticSideEffectAnalyzerImpl.ENABLED;
        StaticSideEffectAnalyzerImpl.ENABLED = true;
        try {
            TypeInfo X = javaInspector.parse("a.b.X", INPUT);
            List<Info> ao = prepWork(X);
            analyzer.go(ao);

            assertTrue(sse(X.findUniqueMethod("raise", 0)), "Config.level = 5 is a static side effect");
            assertFalse(sse(X.findUniqueMethod("pure", 0)), "a pure method has no static side effect");
            assertFalse(sse(X.findUniqueMethod("own", 0)), "modifying an own instance field is not a static side effect");
        } finally {
            StaticSideEffectAnalyzerImpl.ENABLED = saved;
        }
    }
}
