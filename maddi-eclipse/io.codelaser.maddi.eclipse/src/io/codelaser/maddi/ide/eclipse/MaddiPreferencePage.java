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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Window → Preferences → maddi: the maddi JDK (25+), the daemon distribution directory, and its heap. */
public class MaddiPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public MaddiPreferencePage() {
        super(GRID);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(MaddiEclipsePlugin.get().getPreferenceStore());
        setDescription("maddi runs its analysis in an out-of-process daemon on a JDK 25+ (NOT the analyzed "
                + "project's JRE).");
    }

    @Override
    protected void createFieldEditors() {
        addField(new DirectoryFieldEditor(MaddiPreferences.JDK_HOME,
                "maddi JDK (25+) home:", getFieldEditorParent()));
        addField(new DirectoryFieldEditor(MaddiPreferences.DAEMON_INSTALL,
                "Daemon install directory:", getFieldEditorParent()));
        IntegerFieldEditor xmx = new IntegerFieldEditor(MaddiPreferences.DAEMON_XMX_MB,
                "Daemon max heap (MB):", getFieldEditorParent());
        xmx.setValidRange(512, 131072);
        addField(xmx);

        addField(new ComboFieldEditor(MaddiPreferences.HINT_FILTER,
                "Hints show:", hintFilterChoices(), getFieldEditorParent()));

        addField(new BooleanFieldEditor(MaddiPreferences.INLINE_HINTS,
                "Show hints in the editor (needs Java > Editor > Code Minings enabled)",
                getFieldEditorParent()));

        addField(new ComboFieldEditor(MaddiPreferences.HINT_PLACEMENT,
                "Declaration hints go:", hintPlacementChoices(), getFieldEditorParent()));

        addField(new BooleanFieldEditor(MaddiPreferences.AUTO_ANALYZE_ON_BUILD,
                "Re-analyze automatically after a build", getFieldEditorParent()));

        addField(new BooleanFieldEditor(MaddiPreferences.WARN_NEAR_MISSES,
                "Warn about near misses (types/methods that narrowly miss a property)", getFieldEditorParent()));
    }

    /** {label, stored-value} pairs for the hint-filter combo, straight from the enum. */
    private static String[][] hintFilterChoices() {
        HintFilter[] values = HintFilter.values();
        String[][] choices = new String[values.length][2];
        for (int i = 0; i < values.length; i++) {
            choices[i][0] = values[i].label();
            choices[i][1] = values[i].name();
        }
        return choices;
    }

    /** {label, stored-value} pairs for the placement combo. */
    private static String[][] hintPlacementChoices() {
        HintPlacement[] values = HintPlacement.values();
        String[][] choices = new String[values.length][2];
        for (int i = 0; i < values.length; i++) {
            choices[i][0] = values[i].label();
            choices[i][1] = values[i].name();
        }
        return choices;
    }
}
