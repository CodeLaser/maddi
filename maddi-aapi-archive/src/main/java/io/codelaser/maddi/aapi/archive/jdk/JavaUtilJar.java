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

import java.io.File;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;

public class JavaUtilJar {
    public static final String PACKAGE_NAME = "java.util.jar";

    //public class JarFile extends ZipFile
    class JarFile$ {
        // null when the jar has no manifest (fernflower StructContext.addSpace passed it on to ContextUnit.setManifest)
        @NotModified @Nullable Manifest getManifest() { return null; }

        @NotModified @Nullable JarEntry getJarEntry(@NotNull String name) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarFile$(@NotNull File p0) { }
        JarFile$(@NotNull File p0, boolean p1) { }
        JarFile$(@NotNull File p0, boolean p1, int p2) { }
        JarFile$(@NotNull File p0, boolean p1, int p2, @NotNull Runtime.Version p3) { }
        JarFile$(@NotNull String p0) { }
        JarFile$(@NotNull String p0, boolean p1) { }
        @NotModified @Nullable ZipEntry getEntry(@NotNull String p0) { return null; }
        @NotModified @Nullable InputStream getInputStream(@NotNull ZipEntry p0) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Runtime.Version getVersion() { return null; }
        @NotModified boolean isMultiRelease() { return false; }
    }

    //public class JarInputStream extends ZipInputStream
    class JarInputStream$ {
        @NotModified @Nullable Manifest getManifest() { return null; }

        @Nullable JarEntry getNextJarEntry() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarInputStream$(@NotNull InputStream p0) { }
        JarInputStream$(@NotNull InputStream p0, boolean p1) { }
        ZipEntry createZipEntry(@NotNull String p0) { return null; }
        @Nullable ZipEntry getNextEntry() { return null; }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.util.jar.Attributes (shadow created for its nullability, generated)
    class Attributes$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Attributes$(@NotNull Attributes p0) { }
        boolean containsKey(@Nullable Object p0) { return false; }
        boolean containsValue(@Nullable Object p0) { return false; }
        @Nullable Object get(@Nullable Object p0) { return null; }
        @NotModified @Nullable String getValue(@Nullable Attributes.Name p0) { return null; }
        @NotModified @Nullable String getValue(@NotNull String p0) { return null; }
        @Modified @Nullable Object put(@NotNull Object p0, @NotNull Object p1) { return null; }
        @Modified void putAll(@NotNull Map<?, ?> p0) { }
        @Modified @Nullable String putValue(@NotNull String p0, @NotNull String p1) { return null; }
        @Modified @Nullable Object remove(@Nullable Object p0) { return null; }

        //public java.util.jar.Attributes.Name (shadow created for its nullability, generated)
        class Name {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            Name(@NotNull String p0) { }
        }
    }

    //public java.util.jar.JarEntry (shadow created for its nullability, generated)
    class JarEntry$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        JarEntry$(@NotNull JarEntry p0) { }
        JarEntry$(@NotNull String p0) { }
        JarEntry$(@NotNull ZipEntry p0) { }
        @NotModified @Nullable Attributes getAttributes() { return null; }
        @NotModified @Nullable Certificate[] getCertificates() { return null; }
        @NotModified @Nullable CodeSigner[] getCodeSigners() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getRealName() { return null; }
    }
}
