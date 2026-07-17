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

import com.intellij.codeInsight.hints.declarative.HintFormat;
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition;
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.e2immu.analyzer.ide.plugin.analysis.MaddiAnalysisService;
import org.e2immu.analyzer.ide.client.AnalysisModel;
import org.e2immu.analyzer.ide.plugin.settings.InlineHintsMode;
import org.e2immu.analyzer.ide.plugin.settings.MaddiSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Renders the computed analysis hints ({@code @Immutable}, {@code @Container}, {@code @NotModified},
 * {@code @NotNull}, …) as inline inlays at each declaration (type/method/field/parameter). Matches a
 * maddi element to a declaration's name identifier by range containment + kind.
 */
public class MaddiInlayProvider implements InlayHintsProvider {

    @Override
    public @Nullable InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
        InlineHintsMode mode = MaddiSettings.getInstance().getState().inlineHintsMode;
        if (mode == InlineHintsMode.NONE) return null;
        VirtualFile vf = file.getVirtualFile();
        if (vf == null) return null;
        List<AnalysisModel.ElementAnnotation> annotations =
                MaddiAnalysisService.getInstance(file.getProject()).annotationsForPath(vf.getPath());
        if (annotations.isEmpty()) return null;
        Document doc = file.getViewProvider().getDocument();
        if (doc == null) return null;
        return new Collector(annotations, doc, mode);
    }

    private static final class Collector implements SharedBypassCollector {
        private final List<AnalysisModel.ElementAnnotation> annotations;
        private final Document doc;
        private final InlineHintsMode mode;

        Collector(List<AnalysisModel.ElementAnnotation> annotations, Document doc, InlineHintsMode mode) {
            this.annotations = annotations;
            this.doc = doc;
            this.mode = mode;
        }

        @Override
        public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
            if (!(element instanceof PsiIdentifier)) return;
            String kind = kindOf(element.getParent());
            if (kind == null) return;
            int idOffset = element.getTextRange().getStartOffset();
            // Pick the SMALLEST (most specific) containing range of the right kind, so a nested type/member gets
            // its own annotations rather than an enclosing type's (whose range also contains this identifier).
            AnalysisModel.ElementAnnotation match = null;
            int bestLength = Integer.MAX_VALUE;
            for (AnalysisModel.ElementAnnotation a : annotations) {
                if (!kind.equals(a.kind()) || a.annotations().isEmpty()) continue;
                TextRange r = MaddiPositions.range(doc, a.beginLine(), a.beginCol(), a.endLine(), a.endCol());
                if (r != null && r.contains(idOffset) && r.getLength() < bestLength) {
                    match = a;
                    bestLength = r.getLength();
                }
            }
            if (match == null) return;
            String text = match.annotations().stream()
                    .filter(mode::shows)
                    .map(AnalysisModel.Annotation::text)
                    .collect(java.util.stream.Collectors.joining(" "));
            if (text.isEmpty()) return; // everything filtered out under the current mode
            int anchor = element.getTextRange().getEndOffset();
            sink.addPresentation(
                    new InlineInlayPosition(anchor, true, 0),
                    null,
                    null,
                    HintFormat.Companion.getDefault(),
                    builder -> {
                        builder.text(text, null);
                        return kotlin.Unit.INSTANCE;
                    });
        }
    }

    private static @Nullable String kindOf(PsiElement parent) {
        if (parent instanceof PsiClass) return "TYPE";
        if (parent instanceof PsiMethod) return "METHOD";
        if (parent instanceof PsiField) return "FIELD";
        if (parent instanceof PsiParameter) return "PARAMETER";
        return null;
    }
}
