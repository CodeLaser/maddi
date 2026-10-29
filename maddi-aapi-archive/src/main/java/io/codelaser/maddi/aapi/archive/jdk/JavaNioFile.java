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
import io.codelaser.maddi.annotation.Modified;
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
        static InputStream newInputStream(@NotNull Path path, @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static OutputStream newOutputStream(@NotNull Path path, @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static SeekableByteChannel newByteChannel(
            @NotNull Path path,
            @NotNull @NotModified Set<? extends OpenOption> options,
            @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static SeekableByteChannel newByteChannel(@NotNull Path path, @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @NotModified
        @AllowsInterrupt
        static DirectoryStream<Path> newDirectoryStream(@NotNull Path dir) { return null; }

        //@Independent[T]
        @NotModified
        @AllowsInterrupt
        static DirectoryStream<Path> newDirectoryStream(@NotNull Path dir, @NotNull String glob) { return null; }

        //@Independent[T]
        @NotModified
        @AllowsInterrupt
        static DirectoryStream<Path> newDirectoryStream(@NotNull Path dir, @NotNull DirectoryStream.Filter<? super Path> filter) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt
        static Path createFile(@NotNull Path path, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //frequency 1
        //@Independent[T]
        @AllowsInterrupt
        static Path createDirectory(@NotNull Path dir, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createDirectories(@NotNull Path dir, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempFile(@NotNull Path dir, @Nullable String prefix, @Nullable String suffix, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempFile(@Nullable String prefix, @Nullable String suffix, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempDirectory(@NotNull Path dir, @Nullable String prefix, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createTempDirectory(@Nullable String prefix, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createSymbolicLink(@NotNull Path link, @NotNull Path target, @NotNull @NotModified FileAttribute<?> ... attrs) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path createLink(@NotNull Path link, @NotNull Path existing) { return null; }
        @AllowsInterrupt
        static void delete(@NotNull Path path) { }
        @AllowsInterrupt
        static boolean deleteIfExists(@NotNull Path path) { return false; }
        //@Independent[T]
        @AllowsInterrupt
        static Path copy(@NotNull Path source, @NotNull Path target, @NotNull @NotModified CopyOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path move(@NotNull Path source, @NotNull Path target, @NotNull @NotModified CopyOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Path readSymbolicLink(@NotNull Path link) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static FileStore getFileStore(@NotNull Path path) { return null; }
        @AllowsInterrupt @NotModified
        static boolean isSameFile(@NotNull Path path, @NotNull Path path2) { return false; }
        @AllowsInterrupt @NotModified
        static long mismatch(@NotNull Path path, @NotNull Path path2) { return 0L; }
        @AllowsInterrupt @NotModified
        static boolean isHidden(@NotNull Path path) { return false; }
        @Nullable @AllowsInterrupt @NotModified
        static String probeContentType(@NotNull Path path) { return null; }
        //@Independent[T]
        @Nullable @AllowsInterrupt @NotModified
        static <V extends FileAttributeView> V getFileAttributeView(@NotNull Path path, @NotNull Class<V> type, @NotNull @NotModified LinkOption ... options) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static <A extends BasicFileAttributes> A readAttributes(@NotNull Path path, @NotNull Class<A> type, @NotNull @NotModified LinkOption ... options) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt
        static Path setAttribute(
            @NotNull Path path,
            @NotNull String attribute,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object value,
            @NotNull @NotModified LinkOption ... options) { return null; }

        //@Immutable(hc=true)[T] @Independent[T]
        @AllowsInterrupt @NotModified
        static Object getAttribute(@NotNull Path path, @NotNull String attribute, @NotNull @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Map<String, Object> readAttributes(@NotNull Path path, @NotNull String attributes, @NotNull @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Set<PosixFilePermission> getPosixFilePermissions(@NotNull Path path, @NotNull @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path setPosixFilePermissions(@NotNull Path path, @NotNull @NotModified Set<PosixFilePermission> perms) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static UserPrincipal getOwner(@NotNull Path path, @NotNull @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path setOwner(@NotNull Path path, @NotNull UserPrincipal owner) { return null; }
        @AllowsInterrupt @NotModified
        static boolean isSymbolicLink(@NotNull Path path) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isDirectory(@NotNull Path path, @NotNull @NotModified LinkOption ... options) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isRegularFile(@NotNull Path path, @NotNull @NotModified LinkOption ... options) { return false; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static FileTime getLastModifiedTime(@NotNull Path path, @NotNull @NotModified LinkOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path setLastModifiedTime(@NotNull Path path, @NotNull FileTime time) { return null; }
        @AllowsInterrupt @NotModified
        static long size(@NotNull Path path) { return 0L; }
        //frequency 1
        @AllowsInterrupt @NotModified
        static boolean exists(@NotNull Path path, @NotNull @NotModified LinkOption ... options) { return false; }
        @AllowsInterrupt @NotModified
        static boolean notExists(@NotNull Path path, @NotNull @NotModified LinkOption ... options) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isReadable(@NotNull Path path) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isWritable(@NotNull Path path) { return false; }
        @AllowsInterrupt @NotModified
        static boolean isExecutable(@NotNull Path path) { return false; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Path walkFileTree(
            @NotNull Path start,
            @NotNull @NotModified Set<FileVisitOption> options,
            int maxDepth,
            @NotNull FileVisitor<? super Path> visitor) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Path walkFileTree(@NotNull Path start, @NotNull FileVisitor<? super Path> visitor) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static BufferedReader newBufferedReader(@NotNull Path path, @NotNull Charset cs) { return null; }

        //frequency 1
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static BufferedReader newBufferedReader(@NotNull Path path) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static BufferedWriter newBufferedWriter(@NotNull Path path, @NotNull Charset cs, @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static BufferedWriter newBufferedWriter(@NotNull Path path, @NotNull @NotModified OpenOption ... options) { return null; }

        //frequency 1
        @AllowsInterrupt
        static long copy(@NotNull InputStream in, @NotNull Path target, @NotNull @NotModified CopyOption ... options) { return 0L; }
        @AllowsInterrupt
        static long copy(@NotNull Path source, @NotNull OutputStream out) { return 0L; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static byte [] readAllBytes(@NotNull Path path) { return null; }
        @AllowsInterrupt @NotModified
        static String readString(@NotNull Path path) { return null; }
        @AllowsInterrupt @NotModified
        static String readString(@NotNull Path path, @NotNull Charset cs) { return null; }
        //@Independent[T]
        @AllowsInterrupt @NotModified
        static List<String> readAllLines(@NotNull Path path, @NotNull Charset cs) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static List<String> readAllLines(@NotNull Path path) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path write(@NotNull Path path, @NotNull @NotModified byte [] bytes, @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path write(@NotNull Path path, @NotNull @NotModified Iterable<? extends CharSequence> lines, @NotNull Charset cs, @NotNull @NotModified OpenOption ... options) {
            return null;
        }

        //@Independent[T]
        @AllowsInterrupt
        static Path write(@NotNull Path path, @NotNull @NotModified Iterable<? extends CharSequence> lines, @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path writeString(
            @NotNull Path path,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence csq,
            @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt
        static Path writeString(
            @NotNull Path path,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull CharSequence csq,
            @NotNull Charset cs,
            @NotNull @NotModified OpenOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> list(@NotNull Path dir) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> walk(@NotNull Path start, int maxDepth, @NotNull @NotModified FileVisitOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> walk(@NotNull Path start, @NotNull @NotModified FileVisitOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<Path> find(
            @NotNull Path start,
            int maxDepth,
            /*@IgnoreModifications[T]*/ @NotNull BiPredicate<Path, BasicFileAttributes> matcher,
            @NotNull @NotModified FileVisitOption ... options) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<String> lines(@NotNull Path path, @NotNull Charset cs) { return null; }

        //@Independent[T]
        @AllowsInterrupt @NotModified
        static Stream<String> lines(@NotNull Path path) { return null; }
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
        static Path of(@NotNull String first, @NotNull @NotModified String ... more) { return null; }
        static Path of(@NotNull URI uri) { return null; }
        FileSystem getFileSystem() { return null; }
        boolean isAbsolute() { return false; }
        @Nullable Path getRoot() { return null; }
        @Nullable Path getFileName() { return null; }
        @Nullable Path getParent() { return null; }
        int getNameCount() { return 0; }
        Path getName(int i) { return null; }
        Path subpath(int i, int i1) { return null; }
        boolean startsWith(@NotNull Path path) { return false; }
        boolean startsWith(@NotNull String other) { return false; }
        boolean endsWith(@NotNull Path path) { return false; }
        boolean endsWith(@NotNull String other) { return false; }
        Path normalize() { return null; }
        Path resolve(@NotNull Path path) { return null; }
        //frequency 1
        Path resolve(@NotNull String other) { return null; }
        Path resolve(@NotNull Path first, @NotNull Path ... more) { return null; }
        Path resolve(@NotNull String first, @NotNull String ... more) { return null; }
        Path resolveSibling(@NotNull Path other) { return null; }
        Path resolveSibling(@NotNull String other) { return null; }
        Path relativize(@NotNull Path path) { return null; }
        //frequency 1
        URI toUri() { return null; }
        Path toAbsolutePath() { return null; }
        Path toRealPath(@NotNull @NotModified LinkOption ... linkOption) { return null; }
        //frequency 3
        File toFile() { return null; }

        //override from java.nio.file.Watchable
        WatchKey register(@NotNull WatchService watchService, @NotNull WatchEvent.Kind<?> [] kind, @NotNull WatchEvent.Modifier ... modifier) {
            return null;
        }

        //override from java.nio.file.Watchable
        WatchKey register(@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?> ... events) { return null; }

        //override from java.lang.Iterable
        //@NotModified[H] @NotNull[H]
        Iterator<Path> iterator() { return null; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull Path path) { return 0; }

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
        @Modified WatchKey register(WatchService watchService, WatchEvent.Kind<?> [] kind, WatchEvent.Modifier ... modifier) {
            return null;
        }
        @Modified WatchKey register(WatchService watchService, WatchEvent.Kind<?> ... kind) { return null; }
    }

    //public java.nio.file.DirectoryIteratorException (shadow created for its nullability, generated)
    class DirectoryIteratorException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DirectoryIteratorException$(@NotNull java.io.IOException p0) { }
    }

    //public java.nio.file.FileSystemException (shadow created for its nullability, generated)
    class FileSystemException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FileSystemException$(@Nullable java.lang.String p0) { }
        FileSystemException$(@Nullable java.lang.String p0, @Nullable java.lang.String p1, @Nullable java.lang.String p2) { }
        @Nullable java.lang.String getFile() { return null; }
        @Nullable java.lang.String getMessage() { return null; }
        @Nullable java.lang.String getOtherFile() { return null; }
        @Nullable java.lang.String getReason() { return null; }
    }

    //public java.nio.file.FileSystems (shadow created for its nullability, generated)
    class FileSystems$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.nio.file.FileSystem getFileSystem(@NotNull java.net.URI p0) { return null; }
        static java.nio.file.FileSystem newFileSystem(@NotNull java.nio.file.Path p0) { return null; }
        static java.nio.file.FileSystem newFileSystem(@NotNull java.nio.file.Path p0, @Nullable java.lang.ClassLoader p1) { return null; }
        static java.nio.file.FileSystem newFileSystem(@NotNull java.nio.file.Path p0, @NotNull java.util.Map<java.lang.String, ?> p1) { return null; }
        static java.nio.file.FileSystem newFileSystem(@NotNull java.nio.file.Path p0, @NotNull java.util.Map<java.lang.String, ?> p1, @Nullable java.lang.ClassLoader p2) { return null; }
        static java.nio.file.FileSystem newFileSystem(@NotNull java.net.URI p0, @NotNull java.util.Map<java.lang.String, ?> p1) { return null; }
        static java.nio.file.FileSystem newFileSystem(@NotNull java.net.URI p0, @NotNull java.util.Map<java.lang.String, ?> p1, @Nullable java.lang.ClassLoader p2) { return null; }
    }
}
