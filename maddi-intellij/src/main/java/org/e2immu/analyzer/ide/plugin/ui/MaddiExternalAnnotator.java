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

package org.e2immu.analyzer.ide.plugin.ui;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.e2immu.analyzer.ide.plugin.analysis.MaddiAnalysisService;
import org.e2immu.analyzer.ide.plugin.model.AnalysisModel;
import org.e2immu.analyzer.ide.plugin.settings.MaddiSettings;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Paints the guard system's findings as error/warning highlights, with the nested {@code causes()}
 * why-chain rendered into the tooltip. Reads from the cached result in {@link MaddiAnalysisService};
 * a fresh analysis calls {@code DaemonCodeAnalyzer.restart()}, which re-runs this annotator.
 */
public class MaddiExternalAnnotator
        extends ExternalAnnotator<MaddiExternalAnnotator.Input, List<AnalysisModel.Finding>> {

    public record Input(PsiFile file, String path) {
    }

    @Override
    public @Nullable Input collectInformation(PsiFile file, Editor editor, boolean hasErrors) {
        VirtualFile vf = file.getVirtualFile();
        return vf == null ? null : new Input(file, vf.getPath());
    }

    @Override
    public @Nullable List<AnalysisModel.Finding> doAnnotate(Input input) {
        if (!MaddiSettings.getInstance().getState().showGuardFindings) return List.of();
        return MaddiAnalysisService.getInstance(input.file().getProject()).findingsForPath(input.path());
    }

    @Override
    public void apply(PsiFile file, List<AnalysisModel.Finding> findings, AnnotationHolder holder) {
        if (findings == null || findings.isEmpty()) return;
        Document doc = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (doc == null) return;
        for (AnalysisModel.Finding f : findings) {
            TextRange range = MaddiPositions.range(doc, f.beginLine(), f.beginCol(), f.endLine(), f.endCol());
            if (range == null) continue;
            HighlightSeverity severity = "ERROR".equals(f.severity())
                    ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;
            holder.newAnnotation(severity, f.message())
                    .range(range)
                    .tooltip(tooltip(f))
                    .create();
        }
    }

    private static String tooltip(AnalysisModel.Finding f) {
        StringBuilder sb = new StringBuilder("<html><body style='width:400px'>");
        sb.append("<b>maddi</b>: ").append(escape(f.message()));
        appendCauses(sb, f.causes());
        sb.append("</body></html>");
        return sb.toString();
    }

    private static void appendCauses(StringBuilder sb, List<AnalysisModel.Finding> causes) {
        if (causes == null || causes.isEmpty()) return;
        sb.append("<ul style='margin-top:4px'>");
        for (AnalysisModel.Finding c : causes) {
            sb.append("<li>").append(escape(c.message()));
            appendCauses(sb, c.causes());
            sb.append("</li>");
        }
        sb.append("</ul>");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
