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

package org.e2immu.analyzer.ide.plugin.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayHintsPassFactory;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.e2immu.analyzer.ide.client.MaddiDaemonProcess;
import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.e2immu.analyzer.ide.plugin.settings.MaddiSettings;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

/**
 * Owns the daemon for a project, runs whole-project analyses off the EDT, caches the latest result
 * indexed by source-file path, and publishes a refresh so the display surfaces repaint. All four
 * surfaces read from the single cached result here.
 */
@Service(Service.Level.PROJECT)
public final class MaddiAnalysisService implements Disposable {
    private static final Logger LOG = Logger.getInstance(MaddiAnalysisService.class);
    private static final String PLUGIN_ID = "io.codelaser.maddi.intellij";
    private static final String NOTIFICATION_GROUP = "maddi";

    private final Project project;
    private final MaddiDaemonProcess daemon = new MaddiDaemonProcess();
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile AnalysisModel.Result latest;
    // keyed by absolute source-file path (scheme stripped), so VirtualFile.getPath() lookups match
    private volatile Map<String, List<AnalysisModel.Finding>> findingsByPath = Map.of();
    private volatile Map<String, List<AnalysisModel.ElementAnnotation>> annotationsByPath = Map.of();

    public MaddiAnalysisService(Project project) {
        this.project = project;
    }

    public static MaddiAnalysisService getInstance(Project project) {
        return project.getService(MaddiAnalysisService.class);
    }

    /** Kick off a whole-project analysis with a progress indicator. Coalesces: skips if one is running. */
    public void analyzeInBackground() {
        if (!running.compareAndSet(false, true)) {
            LOG.info("maddi analysis already running; skipping this trigger");
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "maddi: analyzing", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    analyze(indicator);
                } catch (Exception e) {
                    LOG.warn("maddi analysis failed", e);
                    notifyUser("Analysis failed: " + e.getMessage(), NotificationType.ERROR);
                } finally {
                    running.set(false);
                }
            }
        });
    }

    private void analyze(ProgressIndicator indicator) throws Exception {
        MaddiSettings.State settings = MaddiSettings.getInstance().getState();
        String jdkHome = settings.jdkHome == null ? "" : settings.jdkHome.trim();
        if (jdkHome.isEmpty()) {
            jdkHome = System.getProperty("maddi.jdk.home", "").trim(); // dev fallback (runIde)
        }
        if (jdkHome.isEmpty()) {
            notifyUser("Set a JDK 25+ home in Settings → maddi before analyzing.", NotificationType.WARNING);
            return;
        }
        indicator.setText("maddi: starting daemon");
        Path logFile = Path.of(PathManager.getLogPath(), "maddi-daemon.log");
        daemon.ensureStarted(resolveInstallDir(settings), Path.of(jdkHome), settings.daemonXmxMb, logFile);

        indicator.setText("maddi: building configuration");
        String resolvedJdkHome = jdkHome;
        AnalysisModel.AnalyzeConfig config =
                ReadAction.compute(() -> new MaddiConfigBuilder().build(project, resolvedJdkHome));
        String requestId = "req-" + requestCounter.incrementAndGet();

        JsonNode node = daemon.analyze(requestId, config,
                status -> indicator.setText("maddi: " + status.path("phase").asText("analyzing")));
        if ("error".equals(node.path("type").asText())) {
            notifyUser("Daemon error: " + node.path("message").asText(), NotificationType.ERROR);
            return;
        }
        AnalysisModel.Result result =
                daemon.client().objectMapper().treeToValue(node, AnalysisModel.Result.class);
        applyResult(result);
    }

    /**
     * Index a result and refresh the display surfaces. Public so tests (and any future non-daemon result
     * source) can drive the UI with a canned result: {@link #index} runs synchronously, so the surface
     * lookups ({@link #findingsForPath}/{@link #annotationsForPath}) are populated on return.
     */
    public void applyResult(AnalysisModel.Result result) {
        index(result);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            // Declarative inlay hints cache on a modification stamp that restart() alone does not invalidate,
            // so the first result would only show up on a later pass. Reset it so inlays recompute now.
            DeclarativeInlayHintsPassFactory.Companion.resetModificationStamp();
            DaemonCodeAnalyzer.getInstance(project).restart(); // repaint annotators/inlays/gutter
            project.getMessageBus().syncPublisher(MaddiResultListener.TOPIC).resultUpdated(result);
        });
    }

    private void index(AnalysisModel.Result result) {
        Map<String, List<AnalysisModel.Finding>> fb = new HashMap<>();
        for (AnalysisModel.Finding f : result.findings()) {
            String path = pathOf(f.uri());
            if (path != null) fb.computeIfAbsent(path, k -> new ArrayList<>()).add(f);
        }
        Map<String, List<AnalysisModel.ElementAnnotation>> ab = new HashMap<>();
        for (AnalysisModel.ElementAnnotation e : result.elementAnnotations()) {
            String path = pathOf(e.uri());
            if (path != null) ab.computeIfAbsent(path, k -> new ArrayList<>()).add(e);
        }
        this.latest = result;
        this.findingsByPath = fb;
        this.annotationsByPath = ab;
    }

    public List<AnalysisModel.Finding> findingsForPath(String path) {
        return findingsByPath.getOrDefault(path, List.of());
    }

    public List<AnalysisModel.ElementAnnotation> annotationsForPath(String path) {
        return annotationsByPath.getOrDefault(path, List.of());
    }

    public AnalysisModel.Result latestResult() {
        return latest;
    }

    private Path resolveInstallDir(MaddiSettings.State settings) {
        if (settings.daemonInstallDir != null && !settings.daemonInstallDir.isBlank()) {
            return Path.of(settings.daemonInstallDir);
        }
        String dev = System.getProperty("maddi.daemon.install", ""); // dev fallback (runIde)
        if (!dev.isBlank()) return Path.of(dev);
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        if (descriptor == null) throw new IllegalStateException("maddi plugin descriptor not found");
        return descriptor.getPluginPath().resolve("daemon"); // bundled (M4 packaging)
    }

    /** maddi returns {@code file:///abs/File.java}; index/query by the bare path so VirtualFile paths match. */
    private static String pathOf(String uri) {
        if (uri == null) return null;
        try {
            URI parsed = URI.create(uri);
            return parsed.getScheme() == null ? uri : parsed.getPath();
        } catch (IllegalArgumentException e) {
            return uri;
        }
    }

    private void notifyUser(String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification("maddi", content, type)
                .notify(project);
    }

    @Override
    public void dispose() {
        daemon.close();
    }
}
