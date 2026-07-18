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
     * Set once the fixpoint is certified: from then on the values are final rather than merely established.
     * Only ever moves false → true, on the coordinator thread that also drives {@link #passCompleted}.
     */
    private boolean certified;

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
        status.partialResult(new DaemonProtocol.PartialResult(
                requestId, iteration, fullPass, certified, elements));
    }

    @Override
    public void phase(Phase phase, int iteration) {
        if (phase == Phase.TERMINAL_CERTIFIED) certified = true;
        // The terminal phases arrive after the last passCompleted, so the certainty they establish reaches the
        // front-end on the terminal result rather than on a partial frame. Surfaced as status so a UI can say
        // which way the run ended: certified, or stopped at the cap / on a plateau with best-available values.
        status.status(new DaemonProtocol.Status(requestId, "analyze",
                "pass " + iteration + ": " + phase, null, null));
    }
}
