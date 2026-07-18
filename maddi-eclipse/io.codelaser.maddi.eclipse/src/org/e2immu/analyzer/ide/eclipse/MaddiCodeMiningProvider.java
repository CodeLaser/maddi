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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Renders maddi's computed annotations inline in the Java editor, the Eclipse counterpart of the IntelliJ
 * plugin's declarative inlay hints. Each analyzed declaration gets a {@link MaddiHintCodeMining} carrying
 * its annotations ({@code @Container}, {@code @NotModified}, …) under the shared {@link HintFilter}.
 * <p>
 * This is the public code-mining path: the provider is contributed to
 * {@code org.eclipse.ui.workbench.texteditor.codeMiningProviders} and installed by
 * {@code AbstractTextEditor.installCodeMiningProviders()}, which reads that registry. JDT's own
 * references/implementations minings are registered through the very same extension point, so nothing here
 * depends on JDT internals. Two consequences of living in JDT's editor, though:
 * <ul>
 *   <li>JDT gates the whole mechanism on <em>Preferences &gt; Java &gt; Editor &gt; Code Minings &gt; Enable
 *       code minings</em>. With that off, no third-party minings render either — including these.</li>
 *   <li>Minings are re-queried on document change by JDT's reconciler. An analysis run changes no document,
 *       so {@link MaddiCodeMiningRefresher} pushes the refresh when a new result lands.</li>
 * </ul>
 * Results come from the last whole-project analysis ({@link MaddiResults}); this provider never triggers one,
 * so typing shows stale hints until the next analysis rather than blocking the editor.
 */
public class MaddiCodeMiningProvider extends AbstractCodeMiningProvider {

    @Override
    public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
                                                                             IProgressMonitor monitor) {
        return CompletableFuture.completedFuture(minings(viewer));
    }

    /**
     * Computed synchronously: the result is already in memory, so this is a filter over a list, not analysis.
     * Returning an incomplete future here would only add latency and a cancellation path to maintain.
     */
    private List<ICodeMining> minings(ITextViewer viewer) {
        if (!MaddiPreferences.inlineHints()) return List.of();
        AnalysisModel.Result result = MaddiResults.get().latest();
        if (result == null || result.elementAnnotations() == null) return List.of();
        IDocument document = viewer.getDocument();
        Path editedFile = editedFile();
        if (document == null || editedFile == null) return List.of();

        HintFilter filter = MaddiPreferences.hintFilter();
        List<ICodeMining> minings = new ArrayList<>();
        for (AnalysisModel.ElementAnnotation element : result.elementAnnotations()) {
            if (!editedFile.equals(MaddiDocumentPositions.toPath(element.uri()))) continue;
            String text = filter.textFor(element);
            if (text.isEmpty()) continue; // nothing to show for this declaration under the current filter
            Position position = MaddiDocumentPositions.position(document, element.beginLine(),
                    element.beginCol());
            if (position == null) continue; // the document moved on since the analysis; skip rather than guess
            minings.add(new MaddiHintCodeMining(position, this, text));
        }
        return minings;
    }

    /** The file this editor is showing, as an absolute path, or null when it is not a workspace file. */
    private Path editedFile() {
        ITextEditor editor = getAdapter(ITextEditor.class);
        if (editor == null) return null;
        IEditorInput input = editor.getEditorInput();
        if (input == null) return null;
        IFile file = input.getAdapter(IFile.class);
        if (file == null || file.getLocation() == null) return null;
        return file.getLocation().toFile().toPath();
    }

}
