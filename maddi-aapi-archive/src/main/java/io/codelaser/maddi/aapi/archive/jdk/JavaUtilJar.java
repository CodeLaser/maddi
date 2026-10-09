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

package io.codelaser.maddi.aapi.archive.jdk;

import io.codelaser.maddi.annotation.Nullable;

import java.util.jar.JarEntry;
import java.util.jar.Manifest;

public class JavaUtilJar {
    public static final String PACKAGE_NAME = "java.util.jar";

    //public class JarFile extends ZipFile
    class JarFile$ {
        // null when the jar has no manifest (fernflower StructContext.addSpace passed it on to ContextUnit.setManifest)
        @Nullable Manifest getManifest() { return null; }

        @Nullable JarEntry getJarEntry(String name) { return null; }
    }

    //public class JarInputStream extends ZipInputStream
    class JarInputStream$ {
        @Nullable Manifest getManifest() { return null; }

        @Nullable JarEntry getNextJarEntry() { return null; }
    }
}
