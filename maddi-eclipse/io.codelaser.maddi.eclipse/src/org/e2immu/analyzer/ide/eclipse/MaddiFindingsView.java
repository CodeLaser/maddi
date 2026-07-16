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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import java.net.URI;

/**
 * The "maddi Findings" view: a navigable table of the latest analysis's findings (guard contract
 * violations and analyzer messages). Double-click opens the file at the finding's line. This is the
 * Eclipse analog of the IntelliJ findings tool window; it reads the shared {@link MaddiResults}.
 */
public class MaddiFindingsView extends ViewPart {

    public static final String ID = "io.codelaser.maddi.eclipse.findingsView";

    private TableViewer viewer;
    private final Runnable resultListener = this::refreshOnUiThread;

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        column(table, "Severity", 80);
        column(table, "File", 220);
        column(table, "Line", 60);
        column(table, "Message", 640);

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new FindingLabelProvider());
        viewer.setInput(MaddiResults.get().findings());
        viewer.addDoubleClickListener(e -> openSelected());

        MaddiResults.get().addListener(resultListener);
    }

    @Override
    public void setFocus() {
        if (viewer != null) viewer.getControl().setFocus();
    }

    @Override
    public void dispose() {
        MaddiResults.get().removeListener(resultListener);
        super.dispose();
    }

    private void refreshOnUiThread() {
        Display display = Display.getDefault();
        display.asyncExec(() -> {
            if (viewer != null && !viewer.getControl().isDisposed()) {
                viewer.setInput(MaddiResults.get().findings());
            }
        });
    }

    private void openSelected() {
        IStructuredSelection selection = viewer.getStructuredSelection();
        if (!(selection.getFirstElement() instanceof AnalysisModel.Finding finding)) return;
        IFile file = locate(finding.uri());
        if (file == null) return;
        try {
            IWorkbenchPage page = getSite().getPage();
            IMarker marker = markerAt(file, finding);
            if (marker != null) {
                IDE.openEditor(page, marker); // navigates to the marker's line
            } else {
                IDE.openEditor(page, file);
            }
        } catch (Exception e) {
            MaddiEclipsePlugin.error("could not open finding location", e);
        }
    }

    /** A maddi marker already placed on this file at the finding's line, so the editor jumps to it. */
    private static IMarker markerAt(IFile file, AnalysisModel.Finding finding) {
        try {
            IMarker fallback = null;
            for (IMarker marker : file.findMarkers(MaddiMarkers.MARKER_TYPE, false, IResource.DEPTH_ZERO)) {
                fallback = marker;
                if (finding.beginLine() != null
                        && finding.beginLine().equals(marker.getAttribute(IMarker.LINE_NUMBER, -1))) {
                    return marker;
                }
            }
            return fallback;
        } catch (Exception e) {
            return null;
        }
    }

    private static IFile locate(String uri) {
        if (uri == null) return null;
        try {
            IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(URI.create(uri));
            return files.length == 0 ? null : files[0];
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void column(Table table, String title, int width) {
        TableColumn c = new TableColumn(table, SWT.NONE);
        c.setText(title);
        c.setWidth(width);
    }

    /** Renders a {@link AnalysisModel.Finding} across the four columns. */
    private static final class FindingLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof AnalysisModel.Finding f)) return "";
            return switch (columnIndex) {
                case 0 -> f.severity();
                case 1 -> fileName(f.uri());
                case 2 -> f.beginLine() == null ? "" : String.valueOf(f.beginLine());
                case 3 -> f.message();
                default -> "";
            };
        }

        private static String fileName(String uri) {
            if (uri == null) return "";
            int slash = uri.lastIndexOf('/');
            return slash < 0 ? uri : uri.substring(slash + 1);
        }
    }
}
