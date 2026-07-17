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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/** Seeds preference defaults (only the daemon heap has one; the JDK/install paths are required input). */
public class MaddiPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = MaddiEclipsePlugin.get().getPreferenceStore();
        store.setDefault(MaddiPreferences.DAEMON_XMX_MB, MaddiPreferences.DEFAULT_XMX_MB);
        store.setDefault(MaddiPreferences.HINT_FILTER, MaddiPreferences.DEFAULT_HINT_FILTER.name());
        store.setDefault(MaddiPreferences.AUTO_ANALYZE_ON_BUILD, false);
    }
}
