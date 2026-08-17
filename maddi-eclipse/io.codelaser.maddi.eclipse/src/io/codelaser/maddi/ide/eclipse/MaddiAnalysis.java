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

package org.e2immu.analyzer.ide.eclipse;

import com.fasterxml.jackson.databind.JsonNode;
import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.e2immu.analyzer.ide.client.MaddiDaemonProcess;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs a whole-project analysis on the shared daemon, off the UI thread, and surfaces the result (markers +
 * findings view). Shared by the manual {@link AnalyzeMaddiHandler} and the {@link MaddiBuildListener}
 * auto-trigger. Coalesces: while one run is in flight, further triggers are dropped (the daemon is
 * single-request, and re-running on every incremental build would only pile up).
 */
public final class MaddiAnalysis {

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private MaddiAnalysis() {
    }

    /**
     * Schedule an analysis of {@code javaProject}. {@code reveal} opens the findings view and reports a
     * summary — appropriate for a manual run, not for a background build trigger.
     */
    public static void schedule(IJavaProject javaProject, boolean reveal) {
        Job job = Job.create("maddi: analyzing " + javaProject.getElementName(), monitor -> {
            if (!RUNNING.compareAndSet(false, true)) return Status.OK_STATUS; // one at a time
            try {
                analyze(javaProject, reveal, monitor);
            } catch (Exception e) {
                MaddiEclipsePlugin.error("maddi analysis failed", e);
            } finally {
                RUNNING.set(false);
            }
            return Status.OK_STATUS;
        });
        job.setUser(reveal); // manual runs show foreground progress; build triggers stay quiet
        job.schedule();
    }

    private static void analyze(IJavaProject javaProject, boolean reveal, IProgressMonitor monitor)
            throws Exception {
        // The total is not knowable up front — the analysis is a fixpoint iteration, not a fixed number of
        // steps — so the task is indeterminate and the phases arrive as subtasks.
        SubMonitor progress = SubMonitor.convert(monitor, "maddi", IProgressMonitor.UNKNOWN);
        progress.subTask("starting the analysis daemon");
        String jdkHome = MaddiPreferences.jdkHome();
        Path install = MaddiDaemonInstall.resolve(); // configured dir, else the copy bundled in the plugin
        if (jdkHome == null || install == null) {
            MaddiEclipsePlugin.info("Set the maddi JDK (25+) in Window → Preferences → maddi (the daemon is "
                    + "bundled; set a daemon install directory only to override it).");
            return;
        }

        MaddiDaemonProcess daemon = MaddiEclipsePlugin.get().daemon();
        daemon.ensureStarted(install, Path.of(jdkHome), MaddiPreferences.daemonXmxMb(), null);

        AnalysisModel.AnalyzeConfig config = new MaddiEclipseConfigBuilder()
                .build(javaProject, jdkHome, MaddiPreferences.warnNearMisses());
        // Streamed frames land here: values established by each analysis pass, so the editor is annotated
        // while the run continues rather than only when it ends, and the progress text says what is
        // happening. Everything that is not the terminal frame arrives through this consumer.
        progress.subTask("building the project model");
        JsonNode node = daemon.analyze("req-" + System.nanoTime(), config,
                frame -> onStreamedFrame(daemon, frame, progress));
        if ("error".equals(node.path("type").asText())) {
            MaddiEclipsePlugin.error("daemon error: " + node.path("message").asText(), null);
            return;
        }
        AnalysisModel.Result result = daemon.client().objectMapper().treeToValue(node, AnalysisModel.Result.class);

        ResourcesPlugin.getWorkspace().run(
                m -> MaddiMarkers.apply(ResourcesPlugin.getWorkspace().getRoot(), result),
                null);
        MaddiResults.get().update(result);
        if (reveal) {
            revealFindingsView();
            // say how the run ended: a run that stopped at the iteration cap produces annotations that look
            // exactly like certified ones, and nothing else on screen would give that away
            String certainty = AnalysisModel.certaintyLabel(AnalysisModel.certaintyOf(result));
            MaddiEclipsePlugin.log(IStatus.INFO,
                    "maddi: " + result.findings().size() + " finding(s), " + result.hintsLoaded() + " hints"
                    + (certainty == null ? "" : " — " + certainty),
                    null);
        }
    }

    /**
     * A non-terminal frame from the daemon: either progress, or values established so far.
     * <p>
     * Values are merged into what is already shown rather than replacing it — one frame is the elements of
     * one analysis pass. Markers are deliberately left alone: {@link MaddiMarkers#apply} rebuilds the whole
     * workspace's markers, far too heavy per pass, and findings are not partial anyway. Hints do update,
     * because they read {@link MaddiResults} through the code-mining provider.
     * <p>
     * Both kinds also move the progress text. Without it an analysis of a large project is a spinner with
     * nothing behind it for minutes, which is indistinguishable from a hang.
     */
    private static void onStreamedFrame(MaddiDaemonProcess daemon, JsonNode frame, SubMonitor progress) {
        if (!AnalysisModel.PARTIAL_RESULT.equals(frame.path("type").asText())) {
            // a status frame: phase transitions, plus a heartbeat every couple of seconds during the long
            // analysis phase, which is what makes "still running" visible at all
            String phase = frame.path("phase").asText("");
            String message = frame.path("message").asText("");
            if (!phase.isEmpty() || !message.isEmpty()) {
                progress.subTask(message.isEmpty() ? phase : phase + ": " + message);
            }
            return;
        }
        try {
            AnalysisModel.PartialResult partial =
                    daemon.client().objectMapper().treeToValue(frame, AnalysisModel.PartialResult.class);
            AnalysisModel.Result merged = MaddiResults.get().mergePartial(partial);
            // report what has been decided, not a percentage: the run is a fixpoint iteration whose length
            // is not known in advance, so a fraction would be invented
            progress.subTask("analyzing — pass " + partial.iteration() + ", "
                             + merged.elementAnnotations().size() + " element(s) so far");
        } catch (Exception e) {
            // a frame we could not read costs the user an early glimpse, never the run
            MaddiEclipsePlugin.log(IStatus.WARNING, "maddi: could not read a streamed result", e);
        }
    }

    private static void revealFindingsView() {
        Display.getDefault().asyncExec(() -> {
            try {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                if (page != null) page.showView(MaddiFindingsView.ID);
            } catch (Exception e) {
                MaddiEclipsePlugin.error("could not open the maddi findings view", e);
            }
        });
    }
}
