package org.e2immu.analyzer.aapi.archive.jdk;
import org.e2immu.annotation.Immutable;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.rare.AllowsInterrupt;
import org.e2immu.annotation.rare.IgnoreModifications;

import java.io.*;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
public class JavaNioFile {
    public static final String PACKAGE_NAME = "java.nio.file";

    class FileStore$ {
        String name() { return null; }
        String type() { return null; }
        boolean isReadOnly() { return false; }
        long getTotalSpace() { return 0L; }
        long getUsableSpace() { return 0L; }
        long getUnallocatedSpace() { return 0L; }
        long getBlockSize() { return 0L; }
        boolean supportsFileAttributeView(Class<? extends FileAttributeView> clazz) { return false; }
        boolean supportsFileAttributeView(String string) { return false; }
        <V> V getFileStoreAttributeView(Class<V> clazz) { return null; }
        Object getAttribute(String string) { return null; }
    }

    class FileSystem$ {
        FileSystemProvider provider() { return null; }
        void close() { }
        boolean isOpen() { return false; }
        boolean isReadOnly() { return false; }
        String getSeparator() { return null; }
        Iterable<Path> getRootDirectories() { return null; }
        Iterable<FileStore> getFileStores() { return null; }
        Set<String> supportedFileAttributeViews() { return null; }
        Path getPath(String string, String... string1) { return null; }
        PathMatcher getPathMatcher(String string) { return null; }
        UserPrincipalLookupService getUserPrincipalLookupService() { return null; }
        WatchService newWatchService() { return null; }
    }

    class FileSystems$ {
        static FileSystem getDefault() { return null; }
        static FileSystem getFileSystem(URI uri) { return null; }
        static FileSystem newFileSystem(URI uri, Map<String, ?> env) { return null; }
        static FileSystem newFileSystem(URI uri, Map<String, ?> env, ClassLoader loader) { return null; }
        static FileSystem newFileSystem(Path path, ClassLoader loader) { return null; }
        static FileSystem newFileSystem(Path path, Map<String, ?> env) { return null; }
        static FileSystem newFileSystem(Path path) { return null; }
        static FileSystem newFileSystem(Path path, Map<String, ?> env, ClassLoader loader) { return null; }
    }

    class Files$ {
        @AllowsInterrupt @NotModified
        static InputStream newInputStream(Path path, OpenOption... options) { return null; }
        @AllowsInterrupt
        static OutputStream newOutputStream(Path path, OpenOption... options) { return null; }

        @AllowsInterrupt
        static SeekableByteChannel newByteChannel(
            Path path,
            Set<? extends OpenOption> options,
            FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static SeekableByteChannel newByteChannel(Path path, OpenOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static DirectoryStream<Path> newDirectoryStream(Path dir) { return null; }

        @AllowsInterrupt @NotModified
        static DirectoryStream<Path> newDirectoryStream(Path dir, String glob) { return null; }

        @AllowsInterrupt @NotModified
        static DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            return null;
        }

        @AllowsInterrupt
        static Path createFile(Path path, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createDirectory(Path dir, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createDirectories(Path dir, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createTempFile(Path dir, String prefix, String suffix, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createTempFile(String prefix, String suffix, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createTempDirectory(Path dir, String prefix, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createTempDirectory(String prefix, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createSymbolicLink(Path link, Path target, FileAttribute<?> ... attrs) { return null; }

        @AllowsInterrupt
        static Path createLink(Path link, Path existing) { return null; }

        @AllowsInterrupt
        static void delete(Path path) { }

        @AllowsInterrupt
        static boolean deleteIfExists(Path path) { return false; }

        @AllowsInterrupt
        static Path copy(Path source, Path target, CopyOption... options) { return null; }

        @AllowsInterrupt
        static Path move(Path source, Path target, CopyOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Path readSymbolicLink(Path link) { return null; }

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

        @AllowsInterrupt @NotModified
        static <V> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static <A> A readAttributes(Path path, Class<A> type, LinkOption... options) { return null; }

        @AllowsInterrupt
        static Path setAttribute(Path path, String attribute, Object value, LinkOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Object getAttribute(Path path, String attribute, LinkOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Set<PosixFilePermission> getPosixFilePermissions(Path path, LinkOption... options) { return null; }

        @AllowsInterrupt
        static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> perms) { return null; }

        @AllowsInterrupt @NotModified
        static UserPrincipal getOwner(Path path, LinkOption... options) { return null; }

        @AllowsInterrupt
        static Path setOwner(Path path, UserPrincipal owner) { return null; }

        @AllowsInterrupt @NotModified
        static boolean isSymbolicLink(Path path) { return false; }

        @AllowsInterrupt @NotModified
        static boolean isDirectory(Path path, LinkOption... options) { return false; }

        @AllowsInterrupt @NotModified
        static boolean isRegularFile(Path path, LinkOption... options) { return false; }

        @AllowsInterrupt @NotModified
        static FileTime getLastModifiedTime(Path path, LinkOption... options) { return null; }

        @AllowsInterrupt
        static Path setLastModifiedTime(Path path, FileTime time) { return null; }

        @AllowsInterrupt @NotModified
        static long size(Path path) { return 0L; }

        @AllowsInterrupt @NotModified
        static boolean exists(Path path, LinkOption... options) { return false; }

        @AllowsInterrupt @NotModified
        static boolean notExists(Path path, LinkOption... options) { return false; }

        @AllowsInterrupt @NotModified
        static boolean isReadable(Path path) { return false; }

        @AllowsInterrupt @NotModified
        static boolean isWritable(Path path) { return false; }

        @AllowsInterrupt @NotModified
        static boolean isExecutable(Path path) { return false; }

        @AllowsInterrupt @NotModified
        static Path walkFileTree(
            Path start,
            Set<FileVisitOption> options,
            int maxDepth,
            @IgnoreModifications
            FileVisitor<? super Path> visitor) { return null; }

        @AllowsInterrupt @NotModified
        static Path walkFileTree(Path start, @IgnoreModifications FileVisitor<? super Path> visitor) { return null; }

        @AllowsInterrupt @NotModified
        static BufferedReader newBufferedReader(Path path, Charset cs) { return null; }

        @AllowsInterrupt @NotModified
        static BufferedReader newBufferedReader(Path path) { return null; }

        @AllowsInterrupt
        static BufferedWriter newBufferedWriter(Path path, Charset cs, OpenOption... options) { return null; }

        @AllowsInterrupt
        static BufferedWriter newBufferedWriter(Path path, OpenOption... options) { return null; }

        @AllowsInterrupt
        static long copy(InputStream in, Path target, CopyOption... options) { return 0L; }

        @AllowsInterrupt
        static long copy(Path source, OutputStream out) { return 0L; }

        @AllowsInterrupt @NotModified
        static byte[] readAllBytes(Path path) { return null; }

        @AllowsInterrupt @NotModified
        static String readString(Path path) { return null; }

        @AllowsInterrupt @NotModified
        static String readString(Path path, Charset cs) { return null; }

        @AllowsInterrupt @NotModified
        static List<String> readAllLines(Path path, Charset cs) { return null; }

        @AllowsInterrupt @NotModified
        static List<String> readAllLines(Path path) { return null; }

        @AllowsInterrupt
        static Path write(Path path, byte[] bytes, OpenOption... options) { return null; }

        @AllowsInterrupt
        static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs, OpenOption... options) {
            return null;
        }

        @AllowsInterrupt
        static Path write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) { return null; }

        @AllowsInterrupt
        static Path writeString(Path path, CharSequence csq, OpenOption... options) { return null; }

        @AllowsInterrupt
        static Path writeString(Path path, CharSequence csq, Charset cs, OpenOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Stream<Path> list(Path dir) { return null; }

        @AllowsInterrupt @NotModified
        static Stream<Path> walk(Path start, int maxDepth, FileVisitOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Stream<Path> walk(Path start, FileVisitOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Stream<Path> find(
            Path start,
            int maxDepth,
            BiPredicate<Path, BasicFileAttributes> matcher,
            FileVisitOption... options) { return null; }

        @AllowsInterrupt @NotModified
        static Stream<String> lines(Path path, Charset cs) { return null; }

        @AllowsInterrupt @NotModified
        static Stream<String> lines(Path path) { return null; }
    }

    @Immutable
    class LinkOption$ {
    }

    @Immutable
    class Path$ {
        static Path of(String first, @NotModified String... more) { return null; }
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
        Path resolve(String other) { return null; }
        Path resolveSibling(Path other) { return null; }
        Path resolveSibling(String other) { return null; }
        Path relativize(Path path) { return null; }
        URI toUri() { return null; }
        Path toAbsolutePath() { return null; }
        Path toRealPath(@NotModified LinkOption... linkOption) { return null; }
        File toFile() { return null; }

        WatchKey register(WatchService watchService, @NotModified WatchEvent.Kind<?> [] kind, @NotModified WatchEvent.Modifier... modifier) {
            return null;
        }

        WatchKey register(WatchService watcher, @NotModified WatchEvent.Kind<?> ... events) { return null; }
        Iterator<Path> iterator() { return null; }
        int compareTo(Path path) { return 0; }
        public boolean equals(Object object) { return false; }
        public int hashCode() { return 0; }
        public String toString() { return null; }
    }

    class Paths$ {
        static Path get(String first, String... more) { return null; }
        static Path get(URI uri) { return null; }
    }
}
