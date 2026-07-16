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
 * Renders guard findings as problem markers (the {@code io.codelaser.maddi.eclipse.contractViolation} type),
 * so they appear in the Problems view and on the editor ruler — Eclipse's native "mark + explain" surface.
 * The why-chain is folded into the marker message; a richer nested rendering is a later step.
 */
public final class MaddiMarkers {

    public static final String MARKER_TYPE = MaddiEclipsePlugin.PLUGIN_ID + ".contractViolation";

    private MaddiMarkers() {
    }

    /** Clear all maddi markers under {@code root}, then create one per located finding. */
    public static void apply(IWorkspaceRoot root, List<AnalysisModel.Finding> findings) throws CoreException {
        root.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        for (AnalysisModel.Finding finding : findings) {
            IFile file = locate(root, finding.uri());
            if (file == null) continue; // outside the workspace (e.g. a library) — no place to mark
            create(file, finding);
        }
    }

    private static void create(IFile file, AnalysisModel.Finding finding) throws CoreException {
        IMarker marker = file.createMarker(MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, severity(finding.severity()));
        marker.setAttribute(IMarker.MESSAGE, message(finding));
        if (finding.beginLine() != null) marker.setAttribute(IMarker.LINE_NUMBER, finding.beginLine());
    }

    private static int severity(String severity) {
        return "ERROR".equals(severity) ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING;
    }

    /** Message with a compact why-chain appended (" ⇐ cause ⇐ cause …"). */
    private static String message(AnalysisModel.Finding finding) {
        StringBuilder sb = new StringBuilder(finding.message() == null ? "" : finding.message());
        appendCauses(sb, finding.causes(), 0);
        return sb.toString();
    }

    private static void appendCauses(StringBuilder sb, List<AnalysisModel.Finding> causes, int depth) {
        if (causes == null || causes.isEmpty() || depth > 8) return;
        for (AnalysisModel.Finding cause : causes) {
            sb.append(" ⇐ ").append(cause.message());
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
