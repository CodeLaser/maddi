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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Right-click a Java project → "Analyze with maddi": hands the selected project to {@link MaddiAnalysis},
 * which runs the whole-project analysis on the shared daemon off the UI thread and surfaces the result.
 */
public class AnalyzeMaddiHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        IProject project = projectOf(HandlerUtil.getCurrentSelection(event));
        if (project == null) return null;
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null || !javaProject.exists()) return null;
        MaddiAnalysis.schedule(javaProject, true);
        return null;
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
