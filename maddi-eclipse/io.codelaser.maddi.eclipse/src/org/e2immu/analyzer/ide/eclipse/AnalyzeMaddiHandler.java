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
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import java.nio.file.Path;

/**
 * Right-click a Java project → "Analyze with maddi": build the config from JDT, run the whole-project
 * analysis on the shared daemon off the UI thread, and surface guard findings as markers. The full
 * request/response is exactly the round-trip the IntelliJ front-end performs, over the same client.
 * <p>
 * The maddi JDK (25+) and the daemon distribution directory are read (for now) from system properties or
 * environment; a preference page is a later step.
 */
public class AnalyzeMaddiHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IProject project = projectOf(HandlerUtil.getCurrentSelection(event));
        if (project == null) return null;
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null || !javaProject.exists()) return null;

        Job job = Job.create("maddi: analyzing " + project.getName(), monitor -> {
            try {
                analyze(javaProject);
            } catch (Exception e) {
                MaddiEclipsePlugin.error("maddi analysis failed", e);
            }
            return Status.OK_STATUS;
        });
        job.setUser(true);
        job.schedule();
        return null;
    }

    private void analyze(IJavaProject javaProject) throws Exception {
        String jdkHome = setting("maddi.jdk.home", "MADDI_JDK_HOME");
        String install = setting("maddi.daemon.install", "MADDI_DAEMON_INSTALL");
        if (jdkHome == null || install == null) {
            MaddiEclipsePlugin.info("Set maddi.jdk.home (JDK 25+) and maddi.daemon.install (daemon dir) first.");
            return;
        }

        MaddiDaemonProcess daemon = MaddiEclipsePlugin.get().daemon();
        daemon.ensureStarted(Path.of(install), Path.of(jdkHome), 4096, null);

        AnalysisModel.AnalyzeConfig config = new MaddiEclipseConfigBuilder().build(javaProject, jdkHome);
        JsonNode node = daemon.analyze("req-" + System.nanoTime(), config, status -> { });
        if ("error".equals(node.path("type").asText())) {
            MaddiEclipsePlugin.error("daemon error: " + node.path("message").asText(), null);
            return;
        }
        AnalysisModel.Result result = daemon.client().objectMapper().treeToValue(node, AnalysisModel.Result.class);
        ResourcesPlugin.getWorkspace().run(
                m -> MaddiMarkers.apply(ResourcesPlugin.getWorkspace().getRoot(), result.findings()),
                null);
        MaddiEclipsePlugin.log(IStatus.INFO,
                "maddi: " + result.findings().size() + " finding(s), " + result.hintsLoaded() + " hints", null);
    }

    private static String setting(String systemProperty, String envVar) {
        String v = System.getProperty(systemProperty);
        if (v == null || v.isBlank()) v = System.getenv(envVar);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static IProject projectOf(ISelection selection) {
        if (!(selection instanceof IStructuredSelection structured) || structured.isEmpty()) return null;
        Object element = structured.getFirstElement();
        if (element instanceof IProject project) return project;
        if (element instanceof IJavaProject javaProject) return javaProject.getProject();
        if (element instanceof IAdaptable adaptable) {
            IProject adapted = adaptable.getAdapter(IProject.class);
            if (adapted != null) return adapted;
            IJavaProject javaProject = adaptable.getAdapter(IJavaProject.class);
            if (javaProject != null) return javaProject.getProject();
        }
        return null;
    }
}
