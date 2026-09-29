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

package io.codelaser.maddi.ide.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The download is exercised against local zips of the CLI distribution's shape; no network. */
public class KotlinFrontEndInstallerTest {

    @Test
    public void keepsOnlyTheK2Jars(@TempDir Path tmp) throws Exception {
        Path zip = zip(tmp.resolve("d.zip"),
                "maddi-kotlin-1.2.3/bin/maddi-kotlin",
                "maddi-kotlin-1.2.3/lib/maddi-run-kotlin-1.2.3.jar",
                "maddi-kotlin-1.2.3/lib-k2/maddi-kotlin-k2-1.2.3.jar",
                "maddi-kotlin-1.2.3/lib-k2/kotlin-compiler-2.4.0.jar",
                // an entry that tries to leave the target directory
                "maddi-kotlin-1.2.3/lib-k2/../../escape.jar");
        KotlinFrontEndInstaller installer = new KotlinFrontEndInstaller(tmp.resolve("cache"));
        assertNull(installer.installed("1.2.3"));

        AtomicLong read = new AtomicLong();
        Path home = installer.install("1.2.3", zip.toUri(), read::set);

        assertEquals(home, installer.installed("1.2.3"));
        try (Stream<Path> files = Files.list(home)) {
            assertEquals(List.of("kotlin-compiler-2.4.0.jar", "maddi-kotlin-k2-1.2.3.jar"), files
                    .map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".jar")).sorted().toList());
        }
        assertFalse(Files.exists(tmp.resolve("escape.jar")));
        assertFalse(Files.exists(tmp.resolve("cache").resolve("escape.jar")));
        // the stream stops at the last entry, before the central directory: progress is monotone, not total
        assertTrue(read.get() > 0 && read.get() <= Files.size(zip), "progress: " + read.get());
        try (Stream<Path> left = Files.list(tmp.resolve("cache"))) {
            assertEquals(List.of("k2-1.2.3"), left.map(p -> p.getFileName().toString()).toList(),
                    "no staging directory left behind");
        }
    }

    @Test
    public void aDistributionWithoutTheFrontEndIsRefused(@TempDir Path tmp) throws Exception {
        // the shape of a release made before the realm existed: the K2 jars in lib/, no lib-k2/
        Path zip = zip(tmp.resolve("old.zip"),
                "maddi-kotlin-0.9.1/lib/maddi-kotlin-k2-0.9.1.jar",
                "maddi-kotlin-0.9.1/lib/kotlin-compiler-2.4.0.jar");
        KotlinFrontEndInstaller installer = new KotlinFrontEndInstaller(tmp.resolve("cache"));
        IOException e = assertThrows(IOException.class, () -> installer.install("0.9.1", zip.toUri(), null));
        assertTrue(e.getMessage().contains("holds no Kotlin front end"), e.getMessage());
        assertNull(installer.installed("0.9.1"));
        try (Stream<Path> left = Files.list(tmp.resolve("cache"))) {
            assertEquals(0, left.count(), "a refused download leaves nothing behind");
        }
    }

    @Test
    public void aMissingReleaseSaysSo(@TempDir Path tmp) {
        KotlinFrontEndInstaller installer = new KotlinFrontEndInstaller(tmp.resolve("cache"));
        IOException e = assertThrows(IOException.class,
                () -> installer.install("9.9.9", tmp.resolve("absent.zip").toUri(), null));
        assertTrue(e.getMessage().contains("no maddi 9.9.9 Kotlin distribution"), e.getMessage());
    }

    @Test
    public void releaseUrl() {
        assertEquals("https://github.com/CodeLaser/maddi/releases/download/v1.0.0/maddi-kotlin-1.0.0.zip",
                KotlinFrontEndInstaller.releaseZip("1.0.0").toString());
    }

    @Test
    public void detectsKotlinSources(@TempDir Path tmp) throws Exception {
        Path java = Files.createDirectories(tmp.resolve("java/p"));
        Files.writeString(java.resolve("A.java"), "package p; class A {}");
        Path kotlin = Files.createDirectories(tmp.resolve("kotlin/p"));
        Files.writeString(kotlin.resolve("B.kt"), "package p\nclass B");
        assertFalse(KotlinFrontEndInstaller.anyKotlinSource(List.of(tmp.resolve("java").toString())));
        assertTrue(KotlinFrontEndInstaller.anyKotlinSource(List.of(tmp.resolve("java").toString(),
                tmp.resolve("kotlin").toString(), tmp.resolve("absent").toString())));
    }

    private static Path zip(Path target, String... entries) throws IOException {
        try (OutputStream out = Files.newOutputStream(target); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(("content of " + entry).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        assertNotNull(target);
        return target;
    }
}
