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

package io.codelaser.maddi.ide.plugin.analysis;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What a run is DOING, as opposed to what it produced ({@link MaddiResultListener} carries the values).
 * <p>
 * ⛔ <b>A RUN THAT ENDS BADLY MUST REACH THE SAME SURFACE AS ONE THAT ENDS WELL.</b> Every terminal path here
 * — {@link #runFailed} as much as {@link #runFinished} — is published, because the tool window previously had
 * exactly one way to learn anything ({@code resultUpdated}, on success only): a run that errored left the
 * PREVIOUS run's findings on screen with nothing marking them stale, and its error appeared for a few seconds
 * in a balloon and nowhere else. GitHub #29.
 * <p>
 * The daemon already emits far more than was being shown — a status frame per phase, a 2 s heartbeat through
 * the long analysis phase, and a {@code partialResult} per pass — so this is mostly a matter of not throwing
 * it away. Everything is delivered on the EDT.
 */
public interface MaddiRunListener {
    Topic<MaddiRunListener> TOPIC = Topic.create("maddi analysis run", MaddiRunListener.class);

    /**
     * A run is about to start.
     *
     * @param daemonInstall where the daemon being used comes from
     * @param daemonBuild   its build stamp — the ONE thing that distinguishes two daemons of the same version,
     *                      and worth a permanent place on screen: three defect families were diagnosed twice
     *                      over as analyzer regressions before it turned out a stale bundled daemon was
     *                      answering (2026-08-24). "unknown" for a daemon predating the stamp.
     */
    default void runStarted(@NotNull String requestId, @NotNull String daemonInstall,
                            @NotNull String daemonBuild) {
    }

    /** A phase transition or a heartbeat: the daemon is alive and this is what it is doing. */
    default void statusUpdated(@NotNull String phase, @NotNull String message) {
    }

    /** One analysis pass completed, with the number of elements decided so far (monotonic). */
    default void passCompleted(int iteration, boolean fullPass, int elementsSoFar) {
    }

    /** The run ended without a result: a daemon error, or an exception on the IDE side. */
    default void runFailed(@NotNull String kind, @NotNull String message) {
    }

    /** The run ended with a result; {@code resultUpdated} carries the values themselves. */
    default void runFinished(@Nullable String outcome, int findings, int elements, int parseErrors,
                             long elapsedMillis) {
    }
}
