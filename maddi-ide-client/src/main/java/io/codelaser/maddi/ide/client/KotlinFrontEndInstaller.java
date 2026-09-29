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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads the Kotlin front end for the daemon, on demand, so the IDE plugins need not carry it.
 * <p>
 * The K2 compiler cannot be published to Maven Central (PUBLISHING.md, "Why Kotlin is separate"); it ships only
 * inside the {@code maddi-kotlin-<version>.zip} CLI distribution, as its {@code lib-k2/} directory — exactly the
 * directory of jars {@code K2Realm} loads. So installing means: fetch that zip for the daemon's own version, keep
 * {@code lib-k2/*.jar} and nothing else, and hand the directory to the daemon ({@link #ENV_K2_HOME}).
 * <p>
 * ⚠ The version must be the daemon's. The realm shares the CST and the {@code maddi-kotlin-api} contract with the
 * host, so K2 jars from another release load and then disagree with the daemon's classes at the boundary.
 */
public final class KotlinFrontEndInstaller {

    /** Read by the daemon at startup (DaemonMain), which turns it into {@code -Dmaddi.k2.home}. */
    public static final String ENV_K2_HOME = "MADDI_K2_HOME";

    /** Where the CLI distributions are published (release-cli.sh): one release per tag {@code v<version>}. */
    public static final String RELEASE_BASE = "https://github.com/CodeLaser/maddi/releases/download";

    private static final String LIB_K2 = "lib-k2/";
    /** The one jar every valid K2 directory must hold: the front end itself, which the realm finds by service. */
    private static final String FRONT_END_JAR_PREFIX = "maddi-kotlin-k2";
    private static final String COMPLETE_MARKER = ".complete";

    private final Path cacheRoot;

    /** @param cacheRoot a directory the installer owns; each version gets {@code k2-<version>/} inside it */
    public KotlinFrontEndInstaller(Path cacheRoot) {
        this.cacheRoot = cacheRoot;
    }

    /** The published CLI zip for a release. */
    public static URI releaseZip(String version) {
        return URI.create(RELEASE_BASE + "/v" + version + "/maddi-kotlin-" + version + ".zip");
    }

    /** The directory for a version, whether or not it is installed. */
    public Path homeFor(String version) {
        return cacheRoot.resolve("k2-" + version);
    }

    /** The installed directory of K2 jars for {@code version}, or {@code null} when it is not (completely) there. */
    public Path installed(String version) {
        Path home = homeFor(version);
        return Files.isRegularFile(home.resolve(COMPLETE_MARKER)) && holdsFrontEnd(home) ? home : null;
    }

    /** Download the release zip for {@code version} and install its K2 jars; see {@link #install(String, URI, LongConsumer)}. */
    public Path install(String version, LongConsumer bytesRead) throws IOException {
        return install(version, releaseZip(version), bytesRead);
    }

    /**
     * Install the K2 jars of the distribution at {@code zip}. The zip is streamed, never unpacked whole: only
     * entries under a {@code lib-k2/} directory ending in {@code .jar} are kept, each by its file name alone (so no
     * entry can write outside the target). The target is filled under a temporary name and moved into place only
     * when it holds the front end, so a failed or interrupted download never leaves a directory that looks
     * installed.
     *
     * @param bytesRead progress, in bytes of the zip read so far; may be {@code null}
     * @return the installed directory
     * @throws IOException with a message fit for the user: the release does not exist, or its zip carries no
     *                     {@code lib-k2/} (a release published before the Kotlin front end could be downloaded)
     */
    public Path install(String version, URI zip, LongConsumer bytesRead) throws IOException {
        Path home = homeFor(version);
        Files.createDirectories(cacheRoot);
        Path staging = Files.createTempDirectory(cacheRoot, "k2-" + version + "-");
        try {
            URLConnection connection = zip.toURL().openConnection();
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(60_000);
            int jars = 0;
            try (InputStream raw = connection.getInputStream();
                 ZipInputStream in = new ZipInputStream(new CountingInputStream(raw, bytesRead))) {
                ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    String name = entry.getName();
                    int at = name.lastIndexOf(LIB_K2);
                    if (entry.isDirectory() || at < 0 || !name.endsWith(".jar")) continue;
                    String fileName = name.substring(at + LIB_K2.length());
                    if (fileName.isEmpty() || fileName.contains("/") || fileName.contains("\\")) continue;
                    Files.copy(in, staging.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    jars++;
                }
            }
            if (!holdsFrontEnd(staging)) {
                throw new IOException("the maddi " + version + " distribution at " + zip + " holds no Kotlin front end ("
                                      + jars + " jar(s) under lib-k2/, none of them " + FRONT_END_JAR_PREFIX
                                      + "); it may predate downloadable Kotlin support");
            }
            Files.writeString(staging.resolve(COMPLETE_MARKER), version + "\n");
            deleteRecursively(home);
            Files.move(staging, home, StandardCopyOption.ATOMIC_MOVE);
            return home;
        } catch (java.io.FileNotFoundException e) {
            throw new IOException("no maddi " + version + " Kotlin distribution at " + zip, e);
        } finally {
            deleteRecursively(staging);
        }
    }

    private static boolean holdsFrontEnd(Path dir) {
        if (!Files.isDirectory(dir)) return false;
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                    .anyMatch(n -> n.startsWith(FRONT_END_JAR_PREFIX) && n.endsWith(".jar"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Whether any of the directories holds a Kotlin source file. Stops at the first one: this decides whether to
     * offer the download at all, and a Java project must not pay for a full walk of its tree to learn that.
     */
    public static boolean anyKotlinSource(List<String> directories) {
        for (String directory : directories) {
            Path dir = Path.of(directory);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> paths = Files.walk(dir)) {
                if (paths.anyMatch(p -> p.getFileName().toString().endsWith(".kt"))) return true;
            } catch (IOException | java.io.UncheckedIOException e) {
                // an unreadable directory says nothing either way
            }
        }
        return false;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class CountingInputStream extends java.io.FilterInputStream {
        private final LongConsumer bytesRead;
        private long count;

        CountingInputStream(InputStream in, LongConsumer bytesRead) {
            super(in);
            this.bytesRead = bytesRead;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) advance(1);
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = super.read(buffer, offset, length);
            if (n > 0) advance(n);
            return n;
        }

        private void advance(long n) {
            count += n;
            if (bytesRead != null) bytesRead.accept(count);
        }
    }
}
