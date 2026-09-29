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

package io.codelaser.maddi.ide.plugin.ui;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import io.codelaser.maddi.ide.plugin.analysis.MaddiAnalysisService;
import io.codelaser.maddi.ide.client.AnalysisModel;
import io.codelaser.maddi.ide.plugin.settings.MaddiSettings;
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
        List<String> kinds = kindsOf(element);
        // parameters are covered by inlay hints, not the gutter
        if (kinds.isEmpty() || kinds.contains("PARAMETER")) return null;

        PsiFile file = element.getContainingFile();
        VirtualFile vf = file == null ? null : file.getVirtualFile();
        if (vf == null) return null;
        List<AnalysisModel.ElementAnnotation> annotations =
                MaddiAnalysisService.getInstance(element.getProject()).annotationsForPath(vf.getPath());
        if (annotations.isEmpty()) return null;
        Document doc = file.getViewProvider().getDocument();
        if (doc == null) return null;

        AnalysisModel.ElementAnnotation match = MaddiElementMatch.find(annotations, doc, kinds,
                element.getTextRange().getStartOffset(), a -> !a.displayAnnotations().isEmpty());
        if (match == null) return null;

        // The gutter keeps the FULL text, roster and all: it is the surface with room for it, and the inline
        // hint deliberately abbreviates (AnalysisModel#abbreviate).
        String text = String.join(" ", match.displayAnnotations());
        // ⛔ AN EVENTUAL VERDICT AND A PLAIN ONE LOOKED IDENTICAL. Polarity drove filtering only, so
        // "@Immutable" and "@Immutable(hc=true,after=…)" -- a proven property and one that holds only after a
        // mark -- reached the gutter under the same icon, and the difference was legible only by reading the
        // tooltip to its end. It is the distinction the whole eventual family exists to make.
        javax.swing.Icon icon = AnalysisModel.isEventual(match)
                ? AllIcons.Nodes.Static : AllIcons.Nodes.Annotationtype;
        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                icon,
                e -> "maddi: " + text,
                null,
                GutterIconRenderer.Alignment.LEFT,
                () -> "maddi analysis: " + text);
    }

    /**
     * The maddi kinds, in order of preference, of the declaration whose NAME is this leaf; empty for none. Java's;
     * a language with another PSI overrides it (the Kotlin provider). A marker must anchor on a leaf, which the
     * name identifier is.
     */
    protected List<String> kindsOf(PsiElement leaf) {
        if (!(leaf instanceof PsiIdentifier)) return List.of();
        PsiElement parent = leaf.getParent();
        if (parent instanceof PsiClass) return List.of("TYPE");
        if (parent instanceof PsiMethod) return List.of("METHOD");
        if (parent instanceof PsiField) return List.of("FIELD");
        return List.of();
    }
}
