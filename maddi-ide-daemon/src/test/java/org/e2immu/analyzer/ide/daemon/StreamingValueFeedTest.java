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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The daemon half of progressive display: values established by each analysis pass are streamed as
 * {@code partialResult} frames instead of the front-end waiting for the terminal {@code result}.
 * <p>
 * What matters here is that the frames genuinely arrive DURING the run and carry usable annotations, and
 * that what they say is consistent with the final answer — since the whole design rests on values only ever
 * strengthening, a partial frame that contradicted the result would be a real defect, not a cosmetic one.
 */
public class StreamingValueFeedTest {

    private static final String SOURCE = """
            package x;
            public class Box {
                private final java.util.List<String> items = new java.util.ArrayList<>();
                public int count() { return items.size(); }
                public void add(String s) { items.add(s); }
                public int lengthOf(StringBuilder sb) { return sb.length(); }
            }
            """;

    /** Captures the streamed frames, in order, as a front-end's status consumer would see them. */
    private static final class Capture implements AnalyzeHandler.StatusSink {
        final List<DaemonProtocol.PartialResult> partials = new CopyOnWriteArrayList<>();
        final List<String> phaseMessages = new CopyOnWriteArrayList<>();

        @Override
        public void status(DaemonProtocol.Status status) {
            if (status.message() != null && status.message().contains("TERMINAL")) {
                phaseMessages.add(status.message());
            }
        }

        @Override
        public void partialResult(DaemonProtocol.PartialResult partial) {
            partials.add(partial);
        }
    }

    @DisplayName("values are streamed while the analysis runs, not only at the end")
    @Test
    public void partialResultsAreStreamed(@TempDir Path projectDir) throws Exception {
        Capture capture = new Capture();
        DaemonProtocol.Result result =
                DaemonAnalysisFixture.analyze(projectDir, "x/Box.java", SOURCE, false, capture);

        assertEquals(0, result.parseErrorCount(), "unexpected parse errors");
        assertFalse(capture.partials.isEmpty(), "expected at least one partialResult before the result");

        DaemonProtocol.PartialResult first = capture.partials.getFirst();
        assertEquals(1, first.iteration(), "the first frame is the first pass");
        assertTrue(first.fullPass(), "the first pass analyses the whole order");
        assertFalse(first.elements().isEmpty(), "a streamed frame should carry annotations to display");
        assertEquals("test", first.requestId(), "frames must be attributable to their request");
    }

    @DisplayName("a streamed value is never contradicted by the final result")
    @Test
    public void streamedValuesAgreeWithTheResult(@TempDir Path projectDir) throws Exception {
        Capture capture = new Capture();
        DaemonProtocol.Result result =
                DaemonAnalysisFixture.analyze(projectDir, "x/Box.java", SOURCE, false, capture);

        // every annotation the stream showed for an element must still be there at the end: values are
        // write-once and refine monotonically, so displaying early can add to but never misstate the answer
        List<String> contradictions = new ArrayList<>();
        int compared = 0;
        for (DaemonProtocol.PartialResult partial : capture.partials) {
            for (DaemonProtocol.ElementAnnotation streamed : partial.elements()) {
                DaemonProtocol.ElementAnnotation fin = result.elementAnnotations().stream()
                        .filter(e -> e.fqn().equals(streamed.fqn()) && e.kind().equals(streamed.kind()))
                        .findFirst().orElse(null);
                if (fin == null) continue; // element carried nothing worth showing at the end; not a conflict
                for (String shown : streamed.displayAnnotations()) {
                    compared++;
                    if (!fin.displayAnnotations().contains(shown)) {
                        contradictions.add(streamed.fqn() + ": streamed " + shown
                                           + " (pass " + partial.iteration() + "), final "
                                           + fin.displayAnnotations());
                    }
                }
            }
        }
        assertTrue(contradictions.isEmpty(), () -> "streamed values were retracted: " + contradictions);
        // otherwise the loop above could pass by comparing nothing at all
        assertTrue(compared > 0, "expected the stream to have shown annotations that could be checked");
    }

    @DisplayName("the run reports how it ended, so a UI can tell established from final")
    @Test
    public void terminalPhaseIsReported(@TempDir Path projectDir) throws Exception {
        Capture capture = new Capture();
        DaemonAnalysisFixture.analyze(projectDir, "x/Box.java", SOURCE, false, capture);

        assertEquals(1, capture.phaseMessages.size(),
                () -> "expected exactly one terminal phase, got: " + capture.phaseMessages);
        assertTrue(capture.phaseMessages.getFirst().contains("TERMINAL_CERTIFIED"),
                () -> "this small source should reach a certified fixpoint: " + capture.phaseMessages);
    }
}
