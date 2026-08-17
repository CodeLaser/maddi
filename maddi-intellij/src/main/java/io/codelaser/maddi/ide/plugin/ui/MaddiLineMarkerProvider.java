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

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import org.e2immu.analyzer.ide.plugin.analysis.MaddiAnalysisService;
import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.e2immu.analyzer.ide.plugin.settings.MaddiSettings;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A gutter icon next to each analyzed type/method/field carrying computed annotations; the tooltip
 * shows the full set ({@code @Immutable}, {@code @Container}, {@code @NotModified}, …). Anchors on the
 * declaration's name identifier (a leaf), matched to the maddi element by range containment + kind.
 */
public class MaddiLineMarkerProvider implements LineMarkerProvider {

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
        if (!MaddiSettings.getInstance().getState().showGutterIcons) return null;
        if (!(element instanceof PsiIdentifier)) return null;
        String kind = kindOf(element.getParent());
        if (kind == null) return null;

        PsiFile file = element.getContainingFile();
        VirtualFile vf = file == null ? null : file.getVirtualFile();
        if (vf == null) return null;
        List<AnalysisModel.ElementAnnotation> annotations =
                MaddiAnalysisService.getInstance(element.getProject()).annotationsForPath(vf.getPath());
        if (annotations.isEmpty()) return null;
        Document doc = file.getViewProvider().getDocument();
        if (doc == null) return null;

        int idOffset = element.getTextRange().getStartOffset();
        // A nested type/member sits inside its enclosing type's range, so several same-kind annotations can
        // contain this identifier. Pick the SMALLEST (most specific) containing range, so e.g. a nested class
        // gets its own annotations, not the outer class's.
        AnalysisModel.ElementAnnotation match = null;
        int bestLength = Integer.MAX_VALUE;
        for (AnalysisModel.ElementAnnotation a : annotations) {
            if (!kind.equals(a.kind()) || a.displayAnnotations().isEmpty()) continue;
            TextRange r = MaddiPositions.range(doc, a.beginLine(), a.beginCol(), a.endLine(), a.endCol());
            if (r != null && r.contains(idOffset) && r.getLength() < bestLength) {
                match = a;
                bestLength = r.getLength();
            }
        }
        if (match == null) return null;

        String text = String.join(" ", match.displayAnnotations());
        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Nodes.Annotationtype,
                e -> "maddi: " + text,
                null,
                GutterIconRenderer.Alignment.LEFT,
                () -> "maddi analysis: " + text);
    }

    private static @Nullable String kindOf(PsiElement parent) {
        if (parent instanceof PsiClass) return "TYPE";
        if (parent instanceof PsiMethod) return "METHOD";
        if (parent instanceof PsiField) return "FIELD";
        return null; // parameters are covered by inlay hints, not the gutter
    }
}
