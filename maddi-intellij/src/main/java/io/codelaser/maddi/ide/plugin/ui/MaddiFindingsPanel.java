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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import io.codelaser.maddi.ide.client.AnalysisModel;
import io.codelaser.maddi.ide.plugin.analysis.MaddiAnalysisService;
import io.codelaser.maddi.ide.plugin.analysis.MaddiResultListener;
import io.codelaser.maddi.ide.plugin.analysis.MaddiRunListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tool-window content: what the run is doing, what it produced, and which daemon produced it.
 * <p>
 * ⛔ <b>THE PANEL USED TO SHOW ONE LINE, AND IT WAS MOSTLY ZEROES.</b> Its only input was the terminal
 * {@code result}, so mid-run it rendered a MERGED partial — which carries element annotations and nothing
 * else — as {@code "0 finding(s), 18771 annotated element(s), 0 hint type(s), 0 ms"}: the findings, the hint
 * count and the elapsed time do not exist until the run ends, and printing them as zeroes reads as "nothing
 * found, instantly" rather than "not known yet". Worse, a run that FAILED published nothing at all, so the
 * previous run's findings stayed on screen unmarked and the error lived for a few seconds in a balloon (#29).
 * <p>
 * So: three areas, and every one of them updates while the run is in flight.
 * <ul>
 *   <li><b>Header</b> — the state (with a client-side elapsed clock, because the daemon's own
 *       {@code elapsedMillis} only exists at the end), the phase and last message, and the DAEMON: where it
 *       was launched from and its build stamp. That last line is not decoration: three defect families were
 *       twice diagnosed as analyzer regressions when the truth was that a stale bundled daemon was answering.</li>
 *   <li><b>Findings tree</b> — as before, but explicitly marked stale while a new run is under way, since
 *       what it shows until the terminal frame is the PREVIOUS run's findings.</li>
 *   <li><b>Run log</b> — every status frame, heartbeat, pass and error, timestamped. The daemon already sends
 *       all of this (a frame per phase, a 2 s heartbeat, a {@code partialResult} per pass); it was being
 *       dropped on the floor.</li>
 * </ul>
 */
public class MaddiFindingsPanel extends SimpleToolWindowPanel implements Disposable {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** Bounded so a long run cannot grow the log without limit; oldest lines go first. */
    private static final int MAX_LOG_LINES = 5_000;

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

    private final JBLabel stateLabel = new JBLabel("not run yet");
    private final JBLabel phaseLabel = new JBLabel(" ");
    private final JBLabel daemonLabel = new JBLabel(" ");
    private final JBTextArea log = new JBTextArea();

    /** Ticks the elapsed clock while a run is in flight; the daemon reports its own time only at the end. */
    private final Timer clock = new Timer(1000, e -> renderState());
    private long runStartedNanos;
    private boolean running;
    private boolean findingsAreStale;
    private String lastPhase = "";
    private String lastMessage = "";
    private String terminal = "";

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

        log.setEditable(false);
        log.setLineWrap(false);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(JBUI.Borders.empty(4, 8, 4, 8));
        header.add(stateLabel);
        header.add(phaseLabel);
        header.add(daemonLabel);

        JBSplitter splitter = new JBSplitter(true, 0.6f);
        splitter.setFirstComponent(new JBScrollPane(tree));
        splitter.setSecondComponent(new JBScrollPane(log));

        JPanel content = new JPanel(new BorderLayout());
        content.add(header, BorderLayout.NORTH);
        content.add(splitter, BorderLayout.CENTER);
        setContent(content);
        setToolbar(buildToolbar());

        clock.setRepeats(true);

        project.getMessageBus().connect(this)
                .subscribe(MaddiResultListener.TOPIC, (MaddiResultListener) this::render);
        project.getMessageBus().connect(this).subscribe(MaddiRunListener.TOPIC, new MaddiRunListener() {
            @Override
            public void runStarted(@NotNull String requestId, @NotNull String daemonInstall,
                                   @NotNull String daemonBuild) {
                MaddiFindingsPanel.this.onRunStarted(requestId, daemonInstall, daemonBuild);
            }

            @Override
            public void statusUpdated(@NotNull String phase, @NotNull String message) {
                lastPhase = phase;
                if (!message.isEmpty()) lastMessage = message;
                append(message.isEmpty() ? phase : phase + ": " + message);
                renderState();
            }

            @Override
            public void passCompleted(int iteration, boolean fullPass, int elementsSoFar) {
                lastMessage = "pass " + iteration + (fullPass ? " (full)" : "") + ", "
                              + elementsSoFar + " element(s) decided";
                append(lastMessage);
                renderState();
            }

            @Override
            public void runFailed(@NotNull String kind, @NotNull String message) {
                MaddiFindingsPanel.this.onRunFailed(kind, message);
            }

            @Override
            public void runFinished(String outcome, int findings, int elements, int parseErrors,
                                    long elapsedMillis) {
                MaddiFindingsPanel.this.onRunFinished(outcome, findings, elements, parseErrors, elapsedMillis);
            }
        });

        AnalysisModel.Result current = MaddiAnalysisService.getInstance(project).latestResult();
        if (current != null) render(current);
        renderDaemon();
        renderState();
    }

    // ---- run events ------------------------------------------------------------------------------------

    private void onRunStarted(String requestId, String daemonInstall, String daemonBuild) {
        running = true;
        runStartedNanos = System.nanoTime();
        terminal = "";
        lastPhase = "starting";
        lastMessage = "";
        // The tree still holds the previous run's findings (a merged partial carries none of its own), and
        // silently showing them as if they were this run's is the stale-result defect. Say so instead.
        findingsAreStale = root.getChildCount() > 0;
        append("--- " + requestId + " started, daemon " + daemonBuild + " at " + daemonInstall);
        renderDaemon();
        renderState();
        clock.start();
    }

    private void onRunFailed(String kind, String message) {
        running = false;
        clock.stop();
        terminal = "FAILED (" + kind + "): " + message;
        append("ERROR [" + kind + "] " + message);
        renderState();
    }

    private void onRunFinished(String outcome, int findings, int elements, int parseErrors,
                               long elapsedMillis) {
        running = false;
        findingsAreStale = false;
        clock.stop();
        AnalysisModel.Result result = MaddiAnalysisService.getInstance(project).latestResult();
        String certainty = String.valueOf(AnalysisModel.certaintyOf(result));
        terminal = findings + " finding(s), " + elements + " annotated element(s)"
                   + (parseErrors > 0 ? ", " + parseErrors + " file(s) did NOT parse" : "")
                   + " — outcome " + (outcome == null ? "UNKNOWN" : outcome) + " (" + certainty + ")"
                   + ", " + elapsedMillis + " ms";
        append("--- finished: " + terminal);
        renderState();
    }

    // ---- rendering -------------------------------------------------------------------------------------

    private void renderState() {
        if (running) {
            long seconds = (System.nanoTime() - runStartedNanos) / 1_000_000_000L;
            stateLabel.setText("running — " + seconds + "s"
                               + (findingsAreStale ? " (the findings below are the PREVIOUS run's)" : ""));
            stateLabel.setForeground(JBColor.foreground());
        } else if (terminal.startsWith("FAILED")) {
            stateLabel.setText(terminal);
            stateLabel.setForeground(JBColor.RED);
        } else if (!terminal.isEmpty()) {
            stateLabel.setText(terminal);
            stateLabel.setForeground(JBColor.foreground());
        } else {
            stateLabel.setText("not run yet");
            stateLabel.setForeground(JBColor.foreground());
        }
        phaseLabel.setText(lastPhase.isEmpty() ? " "
                : lastPhase + (lastMessage.isEmpty() ? "" : ": " + lastMessage));
    }

    private void renderDaemon() {
        MaddiAnalysisService service = MaddiAnalysisService.getInstance(project);
        String install = service.daemonInstall();
        daemonLabel.setText(install.isEmpty() ? "daemon: not started yet"
                : "daemon: build " + service.daemonBuild() + " at " + install);
    }

    private void append(String line) {
        log.append(LocalTime.now().format(CLOCK) + "  " + line + "\n");
        int lines = log.getLineCount();
        if (lines > MAX_LOG_LINES) {
            try {
                log.replaceRange("", 0, log.getLineEndOffset(lines - MAX_LOG_LINES - 1));
            } catch (javax.swing.text.BadLocationException e) {
                log.setText(""); // never let trimming the log break the log
            }
        }
        log.setCaretPosition(log.getDocument().getLength());
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
        for (String problem : result.initializationProblems()) {
            root.add(new DefaultMutableTreeNode(new Row("[initialization] " + problem, null, null, null)));
        }
        model.reload();
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        // The summary moved to the header: as a tree row it was the only thing on screen for the whole run,
        // and three of its four numbers are not known until the run ends.
        renderState();
    }

    private DefaultMutableTreeNode findingNode(AnalysisModel.Finding f, String path) {
        String label = "[" + f.severity() + "/" + f.category() + "] " + f.message();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Row(label, path, f.beginLine(), f.beginCol()));
        if (f.causes() != null) {
            for (AnalysisModel.Finding cause : f.causes()) node.add(findingNode(cause, pathOf(cause.uri())));
        }
        return node;
    }

    // ---- toolbar ---------------------------------------------------------------------------------------

    private JPanel buildToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Analyze with maddi", "Run a whole-project analysis", AllIcons.Actions.Execute) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MaddiAnalysisService.getInstance(project).analyzeInBackground();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!running);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });
        // The fast loop: `installDist`, restart, analyze — no plugin rebuild and no IDE restart. Without this
        // the daemon is kept warm across the very rebuild being tested.
        group.add(new AnAction("Restart daemon", "Stop the daemon; the next analysis starts a fresh one",
                AllIcons.Actions.Restart) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                MaddiAnalysisService.getInstance(project).restartDaemon();
                append("daemon stopped on request; the next analysis will start a fresh one");
                renderDaemon();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!running);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });
        group.add(new AnAction("Clear log", "Clear the run log", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                log.setText("");
            }
        });
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("MaddiToolWindow", group, true);
        toolbar.setTargetComponent(this);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(toolbar.getComponent(), BorderLayout.WEST);
        return wrapper;
    }

    // ---- navigation ------------------------------------------------------------------------------------

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

    /** Visible for tests: the run log, as displayed. */
    public String logText() {
        return log.getText();
    }

    /** Visible for tests: the three header lines, as displayed. */
    public String headerText() {
        return stateLabel.getText() + "\n" + phaseLabel.getText() + "\n" + daemonLabel.getText();
    }

    @Override
    public void dispose() {
        // the clock is the only thing here that outlives the panel if left alone
        clock.stop();
    }
}
