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

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import org.e2immu.analyzer.ide.plugin.settings.MaddiSettings;
import org.jetbrains.annotations.NotNull;

/**
 * When enabled, re-runs a maddi analysis after each successful project build — at which point the
 * compiler output dirs (the hot class files maddi reads) are fresh. Registered as a project listener
 * on the compilation-status topic.
 */
public class MaddiCompilationListener implements CompilationStatusListener {

    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext context) {
        if (aborted) return;
        if (!MaddiSettings.getInstance().getState().autoAnalyzeOnBuild) return;
        // even with compile errors we analyze: the daemon returns findings-only for partial projects
        MaddiAnalysisService.getInstance(context.getProject()).analyzeInBackground();
    }
}
