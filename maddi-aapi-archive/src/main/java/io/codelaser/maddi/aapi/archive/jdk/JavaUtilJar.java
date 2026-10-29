/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.aapi.archive.jdk;

import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.Nullable;

import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import io.codelaser.maddi.annotation.NotNull;

public class JavaUtilJar {
    public static final String PACKAGE_NAME = "java.util.jar";

    //public class JarFile extends ZipFile
    class JarFile$ {
        // null when the jar has no manifest (fernflower StructContext.addSpace passed it on to ContextUnit.setManifest)
        @NotModified @Nullable Manifest getManifest() { return null; }

        @NotModified @Nullable JarEntry getJarEntry(@NotNull String name) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarFile$(@NotNull java.io.File p0) { }
        JarFile$(@NotNull java.io.File p0, boolean p1) { }
        JarFile$(@NotNull java.io.File p0, boolean p1, int p2) { }
        JarFile$(@NotNull java.io.File p0, boolean p1, int p2, @NotNull java.lang.Runtime.Version p3) { }
        JarFile$(@NotNull java.lang.String p0) { }
        JarFile$(@NotNull java.lang.String p0, boolean p1) { }
        @NotModified @Nullable java.util.zip.ZipEntry getEntry(@NotNull java.lang.String p0) { return null; }
        @NotModified @Nullable java.io.InputStream getInputStream(@NotNull java.util.zip.ZipEntry p0) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified java.lang.Runtime.Version getVersion() { return null; }
        @NotModified boolean isMultiRelease() { return false; }
    }

    //public class JarInputStream extends ZipInputStream
    class JarInputStream$ {
        @NotModified @Nullable Manifest getManifest() { return null; }

        @Nullable JarEntry getNextJarEntry() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarInputStream$(@NotNull java.io.InputStream p0) { }
        JarInputStream$(@NotNull java.io.InputStream p0, boolean p1) { }
        java.util.zip.ZipEntry createZipEntry(@NotNull java.lang.String p0) { return null; }
        @Nullable java.util.zip.ZipEntry getNextEntry() { return null; }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.util.jar.Attributes (shadow created for its nullability, generated)
    class Attributes$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Attributes$(@NotNull java.util.jar.Attributes p0) { }
        boolean containsKey(@Nullable java.lang.Object p0) { return false; }
        boolean containsValue(@Nullable java.lang.Object p0) { return false; }
        @Nullable java.lang.Object get(@Nullable java.lang.Object p0) { return null; }
        @NotModified @Nullable java.lang.String getValue(@Nullable java.util.jar.Attributes.Name p0) { return null; }
        @NotModified @Nullable java.lang.String getValue(@NotNull java.lang.String p0) { return null; }
        @Modified @Nullable java.lang.Object put(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return null; }
        @Modified void putAll(@NotNull java.util.Map<?, ?> p0) { }
        @Modified @Nullable java.lang.String putValue(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { return null; }
        @Modified @Nullable java.lang.Object remove(@Nullable java.lang.Object p0) { return null; }

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
        @NotModified @Nullable java.util.jar.Attributes getAttributes() { return null; }
        @NotModified @Nullable java.security.cert.Certificate[] getCertificates() { return null; }
        @NotModified @Nullable java.security.CodeSigner[] getCodeSigners() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getRealName() { return null; }
    }
}
