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
import io.codelaser.maddi.annotation.NotNull;

public class JavaUtilJar {
    public static final String PACKAGE_NAME = "java.util.jar";

    //public class JarFile extends ZipFile
    class JarFile$ {
        // null when the jar has no manifest (fernflower StructContext.addSpace passed it on to ContextUnit.setManifest)
        @Nullable Manifest getManifest() { return null; }

        @Nullable JarEntry getJarEntry(@NotNull String name) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarFile$(@NotNull java.io.File p0) { }
        JarFile$(@NotNull java.io.File p0, boolean p1) { }
        JarFile$(@NotNull java.io.File p0, boolean p1, int p2) { }
        JarFile$(@NotNull java.io.File p0, boolean p1, int p2, @NotNull java.lang.Runtime.Version p3) { }
        JarFile$(@NotNull java.lang.String p0) { }
        JarFile$(@NotNull java.lang.String p0, boolean p1) { }
        @Nullable java.util.zip.ZipEntry getEntry(@NotNull java.lang.String p0) { return null; }
        @Nullable java.io.InputStream getInputStream(@NotNull java.util.zip.ZipEntry p0) { return null; }
    }

    //public class JarInputStream extends ZipInputStream
    class JarInputStream$ {
        @Nullable Manifest getManifest() { return null; }

        @Nullable JarEntry getNextJarEntry() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarInputStream$(@NotNull java.io.InputStream p0) { }
        JarInputStream$(@NotNull java.io.InputStream p0, boolean p1) { }
        java.util.zip.ZipEntry createZipEntry(@NotNull java.lang.String p0) { return null; }
        @Nullable java.util.zip.ZipEntry getNextEntry() { return null; }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.util.jar.Attributes (shadow created for its nullability, generated)
    class Attributes$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Attributes$(@NotNull java.util.jar.Attributes p0) { }
        boolean containsKey(@Nullable java.lang.Object p0) { return false; }
        boolean containsValue(@Nullable java.lang.Object p0) { return false; }
        @Nullable java.lang.Object get(@Nullable java.lang.Object p0) { return null; }
        @Nullable java.lang.String getValue(@Nullable java.util.jar.Attributes.Name p0) { return null; }
        @Nullable java.lang.String getValue(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.Object put(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return null; }
        void putAll(@NotNull java.util.Map<?, ?> p0) { }
        @Nullable java.lang.String putValue(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { return null; }
        @Nullable java.lang.Object remove(@Nullable java.lang.Object p0) { return null; }

        //public java.util.jar.Attributes.Name (shadow created for its nullability, generated)
        class Name {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            Name(@NotNull java.lang.String p0) { }
        }
    }

    //public java.util.jar.JarEntry (shadow created for its nullability, generated)
    class JarEntry$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarEntry$(@NotNull java.util.jar.JarEntry p0) { }
        JarEntry$(@NotNull java.lang.String p0) { }
        JarEntry$(@NotNull java.util.zip.ZipEntry p0) { }
        @Nullable java.util.jar.Attributes getAttributes() { return null; }
        @Nullable java.security.cert.Certificate[] getCertificates() { return null; }
        @Nullable java.security.CodeSigner[] getCodeSigners() { return null; }
    }
}
