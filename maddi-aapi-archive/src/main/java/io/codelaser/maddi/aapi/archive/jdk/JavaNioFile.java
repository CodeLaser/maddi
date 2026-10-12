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
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.rare.AllowsInterrupt;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;

import java.io.*;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class JavaNioFile {
    public static final String PACKAGE_NAME = "java.nio.file";
    //public final class Files
    class Files$ {
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static InputStream newInputStream(Path path, @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static OutputStream newOutputStream(Path path, @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static SeekableByteChannel newByteChannel(
            Path path,
            @NotModified Set<? extends OpenOption> options,
            @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static SeekableByteChannel newByteChannel(Path path, @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @NotModified
        @AllowsInterrupt
        static DirectoryStream<Path> newDirectoryStream(Path dir) { return null; }

        //@Independent[T]
        @NotModified
        @AllowsInterrupt
        static DirectoryStream<Path> newDirectoryStream(Path dir, String glob) { return null; }

        //@Independent[T]
        @NotModified
        @AllowsInterrupt
        static DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt
        static Path createFile(Path path, @NotModified FileAttribute<?> ... attrs) { return null; }

        //frequency 1
        //@Independent[T]
        @AllowsInterrupt
        static Path createDirectory(Path dir, @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createDirectories(Path dir, @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempFile(Path dir, String prefix, String suffix, @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempFile(String prefix, String suffix, @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempDirectory(Path dir, String prefix, @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempDirectory(String prefix, @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createSymbolicLink(Path link, Path target, @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createLink(Path link, Path existing) { return null; }
        @AllowsInterrupt
        static void delete(Path path) { }
        @AllowsInterrupt
        static boolean deleteIfExists(Path path) { return false; }
        //@Independent[T]
        @AllowsInterrupt
        static Path copy(Path source, Path target, @NotModified CopyOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path move(Path source, Path target, @NotModified CopyOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Path readSymbolicLink(Path link) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static FileStore getFileStore(Path path) { return null; }
        @AllowsInterrupt @NotModified
        static boolean isSameFile(Path path, Path path2) { return false; }
        @AllowsInterrupt @NotModified
        static long mismatch(Path path, Path path2) { return 0L; }
        @AllowsInterrupt @NotModified
        static boolean isHidden(Path path) { return false; }
        @AllowsInterrupt @NotModified
        static String probeContentType(Path path) { return null; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, @NotModified LinkOption ... options) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, @NotModified LinkOption ... options) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt
        static Path setAttribute(
            Path path,
            String attribute,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object value,
            @NotModified LinkOption ... options) { return null; }

        //@Immutable(hc=true)[T] @Independent[T]
        @AllowsInterrupt @NotModified
        static Object getAttribute(Path path, String attribute, @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Map<String, Object> readAttributes(Path path, String attributes, @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Set<PosixFilePermission> getPosixFilePermissions(Path path, @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path setPosixFilePermissions(Path path, @NotModified Set<PosixFilePermission> perms) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static UserPrincipal getOwner(Path path, @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path setOwner(Path path, UserPrincipal owner) { return null; }
        @AllowsInterrupt @NotModified
        static boolean isSymbolicLink(Path path) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isDirectory(Path path, @NotModified LinkOption ... options) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isRegularFile(Path path, @NotModified LinkOption ... options) { return false; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static FileTime getLastModifiedTime(Path path, @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path setLastModifiedTime(Path path, FileTime time) { return null; }
        @AllowsInterrupt @NotModified
        static long size(Path path) { return 0L; }
        //frequency 1
        @AllowsInterrupt @NotModified
        static boolean exists(Path path, @NotModified LinkOption ... options) { return false; }
        @AllowsInterrupt @NotModified
        static boolean notExists(Path path, @NotModified LinkOption ... options) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isReadable(Path path) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isWritable(Path path) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isExecutable(Path path) { return false; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Path walkFileTree(
            Path start,
            @NotModified Set<FileVisitOption> options,
            int maxDepth,
            FileVisitor<? super Path> visitor) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Path walkFileTree(Path start, FileVisitor<? super Path> visitor) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static BufferedReader newBufferedReader(Path path, Charset cs) { return null; }

        //frequency 1
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static BufferedReader newBufferedReader(Path path) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static BufferedWriter newBufferedWriter(Path path, Charset cs, @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static BufferedWriter newBufferedWriter(Path path, @NotModified OpenOption ... options) { return null; }

        //frequency 1
        @AllowsInterrupt
        static long copy(InputStream in, Path target, @NotModified CopyOption ... options) { return 0L; }
        @AllowsInterrupt
        static long copy(Path source, OutputStream out) { return 0L; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static byte [] readAllBytes(Path path) { return null; }
        @AllowsInterrupt @NotModified
        static String readString(Path path) { return null; }
        @AllowsInterrupt @NotModified
        static String readString(Path path, Charset cs) { return null; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static List<String> readAllLines(Path path, Charset cs) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static List<String> readAllLines(Path path) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path write(Path path, @NotModified byte [] bytes, @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path write(Path path, @NotModified Iterable<? extends CharSequence> lines, Charset cs, @NotModified OpenOption ... options) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt
        static Path write(Path path, @NotModified Iterable<? extends CharSequence> lines, @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path writeString(
            Path path,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence csq,
            @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path writeString(
            Path path,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ CharSequence csq,
            Charset cs,
            @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> list(Path dir) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> walk(Path start, int maxDepth, @NotModified FileVisitOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> walk(Path start, @NotModified FileVisitOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> find(
            Path start,
            int maxDepth,
            /*@IgnoreModifications[T]*/ BiPredicate<Path, BasicFileAttributes> matcher,
            @NotModified FileVisitOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<String> lines(Path path, Charset cs) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<String> lines(Path path) { return null; }
    }

    //public interface FileVisitor<T>
    class FileVisitor$<T> {
        // exc is null when the iteration of the directory completed without an error; an override must accept it
        // (fernflower's DecompilerTestFixture.deleteRecursively)
        FileVisitResult postVisitDirectory(T dir, @Nullable IOException exc) { return null; }
        FileVisitResult preVisitDirectory(T dir, @NotNull BasicFileAttributes attrs) { return null; }
        FileVisitResult visitFile(T file, @NotNull BasicFileAttributes attrs) { return null; }
        FileVisitResult visitFileFailed(T file, @NotNull IOException exc) { return null; }
    }

    //public class SimpleFileVisitor<T> implements FileVisitor<T>
    class SimpleFileVisitor$<T> {
        FileVisitResult postVisitDirectory(T dir, @Nullable IOException exc) { return null; }
        FileVisitResult preVisitDirectory(T dir, @NotNull BasicFileAttributes attrs) { return null; }
        FileVisitResult visitFile(T file, @NotNull BasicFileAttributes attrs) { return null; }
        FileVisitResult visitFileFailed(T file, @NotNull IOException exc) { return null; }
    }

    //public final class Paths
    class Paths$ {
        static Path get(String first, @NotModified String ... more) { return null; }
        static Path get(URI uri) { return null; }
    }

    //public interface Path implements Comparable<Path>, Iterable<Path>, Watchable
    @Immutable
    class Path$ {
        //frequency 2
        static Path of(String first, @NotModified String ... more) { return null; }
        static Path of(URI uri) { return null; }
        FileSystem getFileSystem() { return null; }
        boolean isAbsolute() { return false; }
        Path getRoot() { return null; }
        Path getFileName() { return null; }
        Path getParent() { return null; }
        int getNameCount() { return 0; }
        Path getName(int i) { return null; }
        Path subpath(int i, int i1) { return null; }
        boolean startsWith(Path path) { return false; }
        boolean startsWith(String other) { return false; }
        boolean endsWith(Path path) { return false; }
        boolean endsWith(String other) { return false; }
        Path normalize() { return null; }
        Path resolve(Path path) { return null; }
        //frequency 1
        Path resolve(String other) { return null; }
        Path resolve(Path first, Path ... more) { return null; }
        Path resolve(String first, String ... more) { return null; }
        Path resolveSibling(Path other) { return null; }
        Path resolveSibling(String other) { return null; }
        Path relativize(Path path) { return null; }
        //frequency 1
        URI toUri() { return null; }
        Path toAbsolutePath() { return null; }
        Path toRealPath(@NotModified LinkOption ... linkOption) { return null; }
        //frequency 3
        File toFile() { return null; }

        //override from java.nio.file.Watchable
        WatchKey register(WatchService watchService, WatchEvent.Kind<?> [] kind, WatchEvent.Modifier ... modifier) {
            return null;
        }

        //override from java.nio.file.Watchable
        WatchKey register(WatchService watcher, WatchEvent.Kind<?> ... events) { return null; }

        //override from java.lang.Iterable
        //@NotModified[H] @NotNull[H]
        Iterator<Path> iterator() { return null; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ Path path) { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //frequency 1
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public interface Watchable
    class Watchable$ {
        WatchKey register(WatchService watchService, WatchEvent.Kind<?> [] kind, WatchEvent.Modifier ... modifier) {
            return null;
        }
        WatchKey register(WatchService watchService, WatchEvent.Kind<?> ... kind) { return null; }
    }
}
