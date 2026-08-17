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

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Re-queries the code minings of every open editor when a new analysis result lands.
 * <p>
 * JDT's mining reconciler refreshes on document change; an analysis run changes no document, so without this
 * the inline hints would only appear after the next keystroke. Registered on {@link MaddiResults} by the
 * activator, and also called when a preference changes the hints' content.
 */
public final class MaddiCodeMiningRefresher implements Runnable {

    @Override
    public void run() {
        // MaddiResults notifies from the analysis job; touching the workbench requires the UI thread
        Display display = Display.getDefault();
        if (display.isDisposed()) return;
        display.asyncExec(MaddiCodeMiningRefresher::refreshOpenEditors);
    }

    public static void refreshOpenEditors() {
        if (!PlatformUI.isWorkbenchRunning()) return;
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference reference : page.getEditorReferences()) {
                    // false: refresh what is already open; never restore an editor just to update hints
                    refresh(reference.getEditor(false));
                }
            }
        }
    }

    private static void refresh(IEditorPart editor) {
        if (!(editor instanceof ITextEditor textEditor)) return;
        // AbstractTextEditor hands out its source viewer under ITextOperationTarget; the viewer is what
        // owns the installed mining providers.
        Object target = textEditor.getAdapter(ITextOperationTarget.class);
        if (target instanceof ISourceViewerExtension5 viewer && viewer.hasCodeMiningProviders()) {
            viewer.updateCodeMinings();
        }
    }
}
