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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.e2immu.analyzer.ide.daemon.DaemonAnalysisFixture.analyze;
import static org.e2immu.analyzer.ide.daemon.DaemonAnalysisFixture.annotationsFor;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 4, step 2: the daemon tags an after-mark verdict with the dedicated {@code EVENTUAL} polarity, distinct
 * from the plain {@code POSITIVE} of an unconditional (proven-now) verdict. Verified end-to-end: a {@code SetOnce}
 * holder is computed eventually immutable, and its {@code @Immutable(after=)} / {@code @Mark} / {@code @Only}
 * come back {@code EVENTUAL}, while the unconditional {@code @Container} / {@code @NotModified} stay
 * {@code POSITIVE}. An eventual verdict is never a context default, so the default hint filter always shows it.
 */
public class TestEventualPolarity {

    // a hand-free eventually-immutable type: SetOnce transitions once, and the analyzer computes the mark "value"
    private static final String HOLDER = """
            package x;
            import org.e2immu.support.SetOnce;
            public class Holder {
                private final SetOnce<String> value = new SetOnce<>();
                public void set(String v) { value.set(v); }
                public String get() { return value.get(); }
            }
            """;

    private static DaemonProtocol.Annotation one(List<DaemonProtocol.Annotation> anns, String prefix) {
        return anns.stream().filter(a -> a.text().startsWith(prefix)).findFirst().orElseThrow(
                () -> new AssertionError("no annotation starting with '" + prefix + "' in " + anns));
    }

    @DisplayName("after-mark verdicts are tagged EVENTUAL; unconditional ones stay POSITIVE")
    @Test
    public void test(@TempDir Path dir) throws Exception {
        DaemonProtocol.Result r = analyze(dir, "x/Holder.java", HOLDER);
        assertEquals(0, r.parseErrorCount(), "unexpected parse errors");

        List<DaemonProtocol.Annotation> type = annotationsFor(r, "TYPE", "Holder");
        // the eventual verdict: @Immutable(hc=true, after="value") -> EVENTUAL, never a context default
        DaemonProtocol.Annotation eventualImm = one(type, "@Immutable");
        assertTrue(eventualImm.text().contains("after=\"value\""), eventualImm.text());
        assertEquals(AnnotationTagger.EVENTUAL, eventualImm.polarity());
        assertFalse(eventualImm.contextDefault(), "an eventual verdict is never a context default");
        // the unconditional verdicts on the same type stay POSITIVE
        assertEquals(AnnotationTagger.POSITIVE, one(type, "@Container").polarity());
        assertEquals(AnnotationTagger.POSITIVE, one(type, "@Independent").polarity());

        // @Mark("value") on the transition method
        assertEquals(AnnotationTagger.EVENTUAL, one(annotationsFor(r, "METHOD", "Holder.set"), "@Mark").polarity());

        // @Only(after="value") on the after-mark reader; its plain @NotModified stays POSITIVE
        List<DaemonProtocol.Annotation> get = annotationsFor(r, "METHOD", "Holder.get");
        assertEquals(AnnotationTagger.EVENTUAL, one(get, "@Only").polarity());
        assertEquals(AnnotationTagger.POSITIVE, one(get, "@NotModified").polarity());
    }
}
