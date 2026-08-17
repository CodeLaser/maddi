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

package org.e2immu.analyzer.ide.daemon;

import org.e2immu.analyzer.modification.analyzer.impl.StaticSideEffectAnalyzerImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.e2immu.analyzer.ide.daemon.DaemonAnalysisFixture.analyze;
import static org.e2immu.analyzer.ide.daemon.DaemonAnalysisFixture.annotationsFor;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The computed {@code @StaticSideEffects} verdict reaches the IDE surfaces end-to-end: the daemon decorates a
 * static-side-effect method with {@code @StaticSideEffects} and tags it with the {@code NEGATIVE} attention
 * polarity (an outward effect the designer should always see, not a missing safety guarantee, but rendered like
 * the baseline cautions). See road-to-immutability §050 "Static side effects".
 */
public class TestStaticSideEffectPolarity {

    // raise() assigns another type's static field -> a static side effect
    private static final String SOURCE = """
            package x;
            class Config { static int level; }
            public class X {
                public void raise() { Config.level = 5; }
                public void plain() { int y = 1 + 1; }
            }
            """;

    private static DaemonProtocol.Annotation one(List<DaemonProtocol.Annotation> anns, String prefix) {
        return anns.stream().filter(a -> a.text().startsWith(prefix)).findFirst().orElseThrow(
                () -> new AssertionError("no annotation starting with '" + prefix + "' in " + anns));
    }

    @DisplayName("a computed @StaticSideEffects verdict is decorated and tagged NEGATIVE, end-to-end")
    @Test
    public void test(@TempDir Path dir) throws Exception {
        boolean saved = StaticSideEffectAnalyzerImpl.ENABLED;
        StaticSideEffectAnalyzerImpl.ENABLED = true;
        try {
            DaemonProtocol.Result r = analyze(dir, "x/X.java", SOURCE);
            assertEquals(0, r.parseErrorCount(), "unexpected parse errors");

            List<DaemonProtocol.Annotation> raise = annotationsFor(r, "METHOD", "X.raise");
            DaemonProtocol.Annotation sse = one(raise, "@StaticSideEffects");
            assertEquals(AnnotationTagger.NEGATIVE, sse.polarity(), sse.toString());

            List<DaemonProtocol.Annotation> plain = annotationsFor(r, "METHOD", "X.plain");
            assertTrue(plain.stream().noneMatch(a -> a.text().startsWith("@StaticSideEffects")),
                    "a method with no static side effect must not carry @StaticSideEffects: " + plain);
        } finally {
            StaticSideEffectAnalyzerImpl.ENABLED = saved;
        }
    }
}
