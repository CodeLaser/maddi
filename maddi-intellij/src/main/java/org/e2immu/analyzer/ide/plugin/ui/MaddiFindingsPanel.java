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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.e2immu.analyzer.ide.plugin.analysis.MaddiAnalysisService;
import org.e2immu.analyzer.ide.plugin.analysis.MaddiResultListener;
import org.e2immu.analyzer.ide.plugin.model.AnalysisModel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The tool-window content: a tree of findings grouped by file, each row navigating to its source. */
public class MaddiFindingsPanel extends SimpleToolWindowPanel implements Disposable {

    /** A navigable row; path/line/col null for grouping nodes. */
    private record Row(String label, String path, Integer line, Integer col) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final Project project;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("maddi");
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final Tree tree = new Tree(model);

    public MaddiFindingsPanel(Project project) {
        super(true, true);
        this.project = project;
        tree.setRootVisible(false);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) navigateToSelection();
            }
        });
        setContent(new JBScrollPane(tree));

        project.getMessageBus().connect(this)
                .subscribe(MaddiResultListener.TOPIC, (MaddiResultListener) this::render);

        AnalysisModel.Result current = MaddiAnalysisService.getInstance(project).latestResult();
        if (current != null) render(current);
    }

    private void render(AnalysisModel.Result result) {
        root.removeAllChildren();
        // group findings by file
        Map<String, DefaultMutableTreeNode> byFile = new LinkedHashMap<>();
        for (AnalysisModel.Finding f : result.findings()) {
            String path = pathOf(f.uri());
            String fileLabel = path == null ? "(no file)" : path.substring(path.lastIndexOf('/') + 1);
            DefaultMutableTreeNode fileNode = byFile.computeIfAbsent(fileLabel,
                    k -> new DefaultMutableTreeNode(new Row(k, null, null, null)));
            fileNode.add(findingNode(f, path));
        }
        byFile.values().forEach(root::add);
        DefaultMutableTreeNode summary = new DefaultMutableTreeNode(new Row(
                result.findings().size() + " finding(s), " + result.elementAnnotations().size()
                        + " annotated element(s), " + result.elapsedMillis() + " ms", null, null, null));
        root.add(summary);
        model.reload();
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    private DefaultMutableTreeNode findingNode(AnalysisModel.Finding f, String path) {
        String label = "[" + f.severity() + "/" + f.category() + "] " + f.message();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Row(label, path, f.beginLine(), f.beginCol()));
        if (f.causes() != null) {
            for (AnalysisModel.Finding cause : f.causes()) node.add(findingNode(cause, pathOf(cause.uri())));
        }
        return node;
    }

    private void navigateToSelection() {
        TreePath selection = tree.getSelectionPath();
        if (selection == null) return;
        Object last = selection.getLastPathComponent();
        if (!(last instanceof DefaultMutableTreeNode node)) return;
        if (!(node.getUserObject() instanceof Row row) || row.path() == null || row.line() == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(row.path());
        if (vf == null) return;
        int col = row.col() == null ? 0 : Math.max(0, row.col() - 1);
        new OpenFileDescriptor(project, vf, Math.max(0, row.line() - 1), col).navigate(true);
    }

    private static String pathOf(String uri) {
        if (uri == null) return null;
        try {
            URI parsed = URI.create(uri);
            return parsed.getScheme() == null ? uri : parsed.getPath();
        } catch (IllegalArgumentException e) {
            return uri;
        }
    }

    @Override
    public void dispose() {
        // message bus connection is tied to this disposable
    }
}
