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

import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;

import java.net.URI;
import java.util.List;

/**
 * Turns a maddi result into editor markers, two flavours:
 * <ul>
 *   <li><b>Contract violations</b> ({@link #VIOLATION_TYPE}, a problem marker) — the guard findings; these
 *       show in the Problems view and on the editor ruler, with the why-chain in the message.</li>
 *   <li><b>Analysis hints</b> ({@link #HINT_TYPE}, a plain text marker) — the computed annotations
 *       ({@code @Container}, {@code @NotModified}, …) on each declaration; these show as a gutter icon +
 *       hover in the editor (mapped to an annotation type in plugin.xml) and stay OUT of the Problems view,
 *       so they inform without flooding it.</li>
 * </ul>
 * Both are cleared and rebuilt on each analysis.
 */
public final class MaddiMarkers {

    public static final String VIOLATION_TYPE = MaddiEclipsePlugin.PLUGIN_ID + ".contractViolation";
    public static final String HINT_TYPE = MaddiEclipsePlugin.PLUGIN_ID + ".analysisHint";

    private MaddiMarkers() {
    }

    /** Clear all maddi markers under {@code root}, then create fresh ones for the latest result. */
    public static void apply(IWorkspaceRoot root, AnalysisModel.Result result) throws CoreException {
        root.deleteMarkers(VIOLATION_TYPE, true, IResource.DEPTH_INFINITE);
        root.deleteMarkers(HINT_TYPE, true, IResource.DEPTH_INFINITE);

        for (AnalysisModel.Finding finding : result.findings()) {
            IFile file = locate(root, finding.uri());
            if (file != null) createViolation(file, finding);
        }
        for (AnalysisModel.ElementAnnotation element : result.elementAnnotations()) {
            if (element.displayAnnotations() == null || element.displayAnnotations().isEmpty()) continue;
            IFile file = locate(root, element.uri());
            if (file != null) createHint(file, element);
        }
    }

    private static void createViolation(IFile file, AnalysisModel.Finding finding) throws CoreException {
        IMarker marker = file.createMarker(VIOLATION_TYPE);
        marker.setAttribute(IMarker.SEVERITY, severity(finding.severity()));
        marker.setAttribute(IMarker.MESSAGE, message(finding));
        if (finding.beginLine() != null) marker.setAttribute(IMarker.LINE_NUMBER, finding.beginLine());
    }

    private static void createHint(IFile file, AnalysisModel.ElementAnnotation element) throws CoreException {
        IMarker marker = file.createMarker(HINT_TYPE);
        marker.setAttribute(IMarker.MESSAGE, String.join(" ", element.displayAnnotations()));
        if (element.beginLine() != null) marker.setAttribute(IMarker.LINE_NUMBER, element.beginLine());
    }

    private static int severity(String severity) {
        return "ERROR".equals(severity) ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING;
    }

    /** Message with a compact why-chain appended, one cause per line for readable tooltips. */
    private static String message(AnalysisModel.Finding finding) {
        StringBuilder sb = new StringBuilder(finding.message() == null ? "" : finding.message());
        appendCauses(sb, finding.causes(), 1);
        return sb.toString();
    }

    private static void appendCauses(StringBuilder sb, List<AnalysisModel.Finding> causes, int depth) {
        if (causes == null || causes.isEmpty() || depth > 8) return;
        for (AnalysisModel.Finding cause : causes) {
            sb.append('\n').append("  ".repeat(depth)).append("⇐ ").append(cause.message());
            appendCauses(sb, cause.causes(), depth + 1);
        }
    }

    /** maddi returns {@code file:///abs/File.java}; find the workspace file at that filesystem location. */
    private static IFile locate(IWorkspaceRoot root, String uri) {
        if (uri == null) return null;
        try {
            IFile[] files = root.findFilesForLocationURI(URI.create(uri));
            return files.length == 0 ? null : files[0];
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
