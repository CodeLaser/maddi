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

/**
 * Seam between the socket transport ({@link DaemonMain}) and the actual analysis
 * ({@link WarmAnalysisService}). Implementations run one whole-project analysis per call.
 */
public interface AnalyzeHandler {

    /** Sink for coarse progress messages emitted during a long analysis. */
    interface StatusSink {
        void status(DaemonProtocol.Status status);
    }

    /**
     * Run a whole-project analysis and return a plain-JSON result. May emit {@code status}
     * updates via the sink. Throwing is allowed: the transport turns it into an {@code error}
     * message and keeps the daemon alive.
     */
    DaemonProtocol.Result analyze(DaemonProtocol.AnalyzeProject request, StatusSink status) throws Exception;
}
