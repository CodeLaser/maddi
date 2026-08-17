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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Resolves the daemon {@code installDist} directory (a {@code bin/} launcher + {@code lib/*.jar}). Prefers a
 * configured location (preference / system property / env), else the copy bundled inside this plugin under
 * {@code daemon/}. The bundle declares {@code Eclipse-BundleShape: dir} so it is installed unpacked and the
 * daemon can be launched directly from disk.
 */
public final class MaddiDaemonInstall {

    private MaddiDaemonInstall() {
    }

    public static Path resolve() throws IOException {
        String configured = MaddiPreferences.daemonInstall();
        if (configured != null) return Path.of(configured);

        Bundle bundle = FrameworkUtil.getBundle(MaddiDaemonInstall.class);
        if (bundle == null) return null;
        URL entry = FileLocator.find(bundle, IPath.fromOSString("daemon"), null);
        if (entry == null) return null; // daemon not bundled (dev build without the copy step)
        URL fileUrl = FileLocator.toFileURL(entry);
        return new File(fileUrl.getPath()).toPath();
    }
}
