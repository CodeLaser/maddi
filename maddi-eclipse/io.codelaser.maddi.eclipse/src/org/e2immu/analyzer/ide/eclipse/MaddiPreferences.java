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

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Preference keys and resolved accessors for the maddi daemon settings. Each accessor prefers the workspace
 * preference (Window → Preferences → maddi), then a system property, then an environment variable — so the
 * plugin works out of the box under a dev launch (sysprops) and configured for real users (preferences).
 */
public final class MaddiPreferences {

    public static final String JDK_HOME = "maddi.jdkHome";
    public static final String DAEMON_INSTALL = "maddi.daemonInstall";
    public static final String DAEMON_XMX_MB = "maddi.daemonXmxMb";

    public static final int DEFAULT_XMX_MB = 4096;

    private MaddiPreferences() {
    }

    /** Home of the maddi JDK (25+); the analysis SDK AND the daemon's run JDK. */
    public static String jdkHome() {
        return resolve(JDK_HOME, "maddi.jdk.home", "MADDI_JDK_HOME");
    }

    /** The daemon {@code installDist} directory (contains {@code bin/} and {@code lib/}). */
    public static String daemonInstall() {
        return resolve(DAEMON_INSTALL, "maddi.daemon.install", "MADDI_DAEMON_INSTALL");
    }

    public static int daemonXmxMb() {
        int v = store().getInt(DAEMON_XMX_MB);
        return v > 0 ? v : DEFAULT_XMX_MB;
    }

    private static String resolve(String prefKey, String systemProperty, String envVar) {
        String v = store().getString(prefKey);
        if (isBlank(v)) v = System.getProperty(systemProperty);
        if (isBlank(v)) v = System.getenv(envVar);
        return isBlank(v) ? null : v.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static IPreferenceStore store() {
        return MaddiEclipsePlugin.get().getPreferenceStore();
    }
}
