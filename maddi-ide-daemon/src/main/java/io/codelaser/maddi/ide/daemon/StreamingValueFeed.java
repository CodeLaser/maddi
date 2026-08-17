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

import org.e2immu.analyzer.ide.daemon.AnalyzeHandler.StatusSink;
import org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed;
import org.e2immu.language.cst.api.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Turns the analyzer's {@link AnalysisValueFeed} into {@code partialResult} frames, so the IDE can show
 * established values while the run is still going. The engine half of this is in
 * {@code maddi-modification-analyzer}; this is the daemon adapter.
 * <p>
 * Why it is worth doing: most of the output is decided in the first pass, and the tail is long in duration
 * but short in decisions. Waiting for the terminal {@code result} means waiting mostly on elements the user
 * is not looking at.
 * <p>
 * Two constraints the feed imposes, both honoured here:
 * <ul>
 *   <li>the {@code analyzed} collection is valid only for the duration of the call, so the frame is built
 *       and handed to the sink inside it, never retained;</li>
 *   <li>the callback runs on the coordinator thread with the workers quiescent, which is what makes reading
 *       {@code info.analysis()} safe — so this must not hand the collection to another thread to process
 *       later. Writing to the socket from here is fine: {@code DaemonMain.send} is synchronized, and the
 *       heartbeat thread already writes concurrently.</li>
 * </ul>
 * Feed exceptions never disturb the analysis (the engine swallows and logs them), but a failure here would
 * cost the user their streamed values silently, so it is logged on this side too.
 */
class StreamingValueFeed implements AnalysisValueFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingValueFeed.class);

    private final StatusSink status;
    private final String requestId;
    private final ResultCollector collector;

    /**
     * How the run ended, or null while it is still going. Written on the coordinator thread that also drives
     * {@link #passCompleted}; volatile because {@link #outcome()} is read afterwards from the analysis thread.
     */
    private volatile Phase terminalPhase;

    StreamingValueFeed(StatusSink status, String requestId, ResultCollector collector) {
        this.status = status;
        this.requestId = requestId;
        this.collector = collector;
    }

    @Override
    public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
        if (analyzed == null || analyzed.isEmpty()) return;
        List<DaemonProtocol.ElementAnnotation> elements = collector.collectForElements(analyzed);
        if (elements.isEmpty()) return; // the pass touched nothing worth displaying
        LOGGER.debug("pass {} ({}): streaming {} element(s)", iteration,
                fullPass ? "full" : "worklist", elements.size());
        // always false in practice: the terminal phases arrive AFTER the last passCompleted, so certainty
        // reaches the front-end on the terminal result (see outcome()) rather than on a partial frame. The
        // field stays because a future engine that certified mid-run would have somewhere to say so.
        status.partialResult(new DaemonProtocol.PartialResult(
                requestId, iteration, fullPass, terminalPhase == Phase.TERMINAL_CERTIFIED, elements));
    }

    @Override
    public void phase(Phase phase, int iteration) {
        if (phase != Phase.CYCLE_BREAKING_ACTIVATED) terminalPhase = phase;
        // Surfaced as status too, so a progress line can show the run ending as it happens.
        status.status(new DaemonProtocol.Status(requestId, "analyze",
                "pass " + iteration + ": " + phase, null, null));
    }

    /**
     * How the run ended, for the terminal result: {@code CERTIFIED} means the values are final, the other
     * outcomes mean best-available. {@code UNKNOWN} when no terminal phase arrived at all — which happens on
     * a run that never got as far as the fixpoint, and is not the same as "not certified".
     */
    String outcome() {
        Phase phase = terminalPhase;
        if (phase == null) return DaemonProtocol.OUTCOME_UNKNOWN;
        // the engine's "TERMINAL_" prefix is an artefact of the enum being shared with CYCLE_BREAKING_ACTIVATED
        return phase.name().startsWith("TERMINAL_") ? phase.name().substring("TERMINAL_".length()) : phase.name();
    }
}
