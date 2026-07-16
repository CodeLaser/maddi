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

package org.e2immu.analyzer.ide.plugin.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

/** Settings → Tools → maddi. Configures the maddi JDK (25+), the daemon, and the display surfaces. */
public class MaddiConfigurable implements Configurable {

    private TextFieldWithBrowseButton jdkHomeField;
    private TextFieldWithBrowseButton daemonInstallField;
    private JBIntSpinner xmxSpinner;
    private JBCheckBox autoAnalyzeOnBuild;
    private JBCheckBox showGuardFindings;
    private JBCheckBox showInlayHints;
    private JBCheckBox showGutterIcons;

    @Override
    public @Nls String getDisplayName() {
        return "maddi";
    }

    @Override
    public @Nullable JComponent createComponent() {
        jdkHomeField = new TextFieldWithBrowseButton();
        FileChooserDescriptor folder = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        jdkHomeField.addBrowseFolderListener(null, folder.withTitle("Select a JDK 25+ home"));

        daemonInstallField = new TextFieldWithBrowseButton();
        daemonInstallField.addBrowseFolderListener(null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Daemon distribution directory (optional override)"));

        xmxSpinner = new JBIntSpinner(4096, 512, 131072, 512);
        autoAnalyzeOnBuild = new JBCheckBox("Re-analyze automatically after each successful build");
        showGuardFindings = new JBCheckBox("Mark guard contract violations");
        showInlayHints = new JBCheckBox("Show computed annotations as inline hints");
        showGutterIcons = new JBCheckBox("Show gutter icons on analyzed declarations");

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("maddi JDK (25+) home:", jdkHomeField, 1, false)
                .addTooltip("maddi runs on this JDK and reads java.base from it. NOT the analyzed project's SDK.")
                .addLabeledComponent("Daemon install override:", daemonInstallField, 1, false)
                .addLabeledComponent("Daemon max heap (MB):", xmxSpinner, 1, false)
                .addComponent(autoAnalyzeOnBuild)
                .addComponent(showGuardFindings)
                .addComponent(showInlayHints)
                .addComponent(showGutterIcons)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        MaddiSettings.State s = state();
        return !jdkHomeField.getText().equals(nullToEmpty(s.jdkHome))
                || !daemonInstallField.getText().equals(nullToEmpty(s.daemonInstallDir))
                || xmxSpinner.getNumber() != s.daemonXmxMb
                || autoAnalyzeOnBuild.isSelected() != s.autoAnalyzeOnBuild
                || showGuardFindings.isSelected() != s.showGuardFindings
                || showInlayHints.isSelected() != s.showInlayHints
                || showGutterIcons.isSelected() != s.showGutterIcons;
    }

    @Override
    public void apply() {
        MaddiSettings.State s = state();
        s.jdkHome = jdkHomeField.getText().trim();
        s.daemonInstallDir = daemonInstallField.getText().trim();
        s.daemonXmxMb = xmxSpinner.getNumber();
        s.autoAnalyzeOnBuild = autoAnalyzeOnBuild.isSelected();
        s.showGuardFindings = showGuardFindings.isSelected();
        s.showInlayHints = showInlayHints.isSelected();
        s.showGutterIcons = showGutterIcons.isSelected();
    }

    @Override
    public void reset() {
        MaddiSettings.State s = state();
        jdkHomeField.setText(nullToEmpty(s.jdkHome));
        daemonInstallField.setText(nullToEmpty(s.daemonInstallDir));
        xmxSpinner.setNumber(s.daemonXmxMb);
        autoAnalyzeOnBuild.setSelected(s.autoAnalyzeOnBuild);
        showGuardFindings.setSelected(s.showGuardFindings);
        showInlayHints.setSelected(s.showInlayHints);
        showGutterIcons.setSelected(s.showGutterIcons);
    }

    private static MaddiSettings.State state() {
        return MaddiSettings.getInstance().getState();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
