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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Re-analyzes a Java project after Eclipse builds it, when the auto-analyze preference is on. Gated so it
 * costs nothing when off; the analysis itself is coalesced ({@link MaddiAnalysis}). Registered
 * programmatically by {@link MaddiEclipsePlugin} for {@code POST_BUILD} events.
 */
public class MaddiBuildListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() != IResourceChangeEvent.POST_BUILD) return;
        if (!MaddiPreferences.autoAnalyzeOnBuild()) return;
        IResourceDelta delta = event.getDelta();
        if (delta == null) return;

        Set<IProject> built = new LinkedHashSet<>();
        for (IResourceDelta child : delta.getAffectedChildren()) {
            IResource resource = child.getResource();
            if (resource instanceof IProject project && project.isOpen() && isJava(project)) {
                built.add(project);
            }
        }
        for (IProject project : built) {
            IJavaProject javaProject = JavaCore.create(project);
            if (javaProject != null && javaProject.exists()) {
                MaddiAnalysis.schedule(javaProject, false);
            }
        }
    }

    private static boolean isJava(IProject project) {
        try {
            return project.hasNature(JavaCore.NATURE_ID);
        } catch (CoreException e) {
            return false;
        }
    }
}
