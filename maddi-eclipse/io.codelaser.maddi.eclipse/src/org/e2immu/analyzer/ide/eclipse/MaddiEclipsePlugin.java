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

import org.e2immu.analyzer.ide.client.MaddiDaemonProcess;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator. Owns the one long-lived maddi daemon process for the workspace (the same
 * {@link MaddiDaemonProcess} the IntelliJ front-end uses, from the shared {@code maddi-ide-client} jar),
 * and closes it on shutdown.
 */
public class MaddiEclipsePlugin extends Plugin {

    public static final String PLUGIN_ID = "io.codelaser.maddi.eclipse";

    private static MaddiEclipsePlugin instance;
    private final MaddiDaemonProcess daemon = new MaddiDaemonProcess();

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        daemon.close();
        instance = null;
        super.stop(context);
    }

    public static MaddiEclipsePlugin get() {
        return instance;
    }

    /** The shared warm-daemon handle; launched lazily by {@link MaddiDaemonProcess#ensureStarted}. */
    public MaddiDaemonProcess daemon() {
        return daemon;
    }

    public static void log(int severity, String message, Throwable t) {
        MaddiEclipsePlugin p = instance;
        if (p != null) p.getLog().log(new Status(severity, PLUGIN_ID, message, t));
    }

    public static void error(String message, Throwable t) {
        log(IStatus.ERROR, message, t);
    }

    public static void info(String message) {
        log(IStatus.INFO, message, null);
    }
}
