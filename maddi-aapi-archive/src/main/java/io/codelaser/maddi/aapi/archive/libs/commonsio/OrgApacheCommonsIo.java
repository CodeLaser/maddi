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

package io.codelaser.maddi.aapi.archive.libs.commonsio;

import io.codelaser.maddi.annotation.NotModified;

/**
 * commons-io (2.20.0), from the library-call census (TestLibraryCallCensus, 2026-10-13). Most of its 118
 * argument-modifying calls on the census corpora are right as they are: {@code IOUtils.copy}, {@code toString(InputStream)},
 * {@code readLines}, {@code closeQuietly} consume, write or close the stream they are given. What was wrong is the data
 * a call only reads: the bytes, characters and lines written, the extensions listed. Generated from the jar with javap,
 * so only the members that carry an annotation are here; names types in full and imports none, like libs/assertj.
 */
public class OrgApacheCommonsIo {
    public static final String PACKAGE_NAME = "org.apache.commons.io";

    //public class org.apache.commons.io.IOUtils
    // the data written (arrays, character sequences, collections) is read; streams, readers and writers are consumed or written, as their defaults say, and a buffer handed to read*/copy*/skip* is filled
    class IOUtils$ {
        static void closeQuietly(@NotModified java.lang.Iterable<java.io.Closeable> p0) { }
        static int length(@NotModified byte[] p0) { return 0; }
        static int length(@NotModified char[] p0) { return 0; }
        static int length(@NotModified java.lang.CharSequence p0) { return 0; }
        static int length(@NotModified java.lang.Object[] p0) { return 0; }
        static java.io.InputStream toInputStream(@NotModified java.lang.CharSequence p0) { return null; }
        static java.io.InputStream toInputStream(@NotModified java.lang.CharSequence p0, java.nio.charset.Charset p1) { return null; }
        static java.io.InputStream toInputStream(@NotModified java.lang.CharSequence p0, java.lang.String p1) { return null; }
        static java.lang.String toString(@NotModified byte[] p0) { return null; }
        static java.lang.String toString(@NotModified byte[] p0, java.lang.String p1) { return null; }
        static void write(@NotModified byte[] p0, java.io.OutputStream p1) { }
        static void write(@NotModified byte[] p0, java.io.Writer p1) { }
        static void write(@NotModified byte[] p0, java.io.Writer p1, java.nio.charset.Charset p2) { }
        static void write(@NotModified byte[] p0, java.io.Writer p1, java.lang.String p2) { }
        static void write(@NotModified char[] p0, java.io.OutputStream p1) { }
        static void write(@NotModified char[] p0, java.io.OutputStream p1, java.nio.charset.Charset p2) { }
        static void write(@NotModified char[] p0, java.io.OutputStream p1, java.lang.String p2) { }
        static void write(@NotModified char[] p0, java.io.Writer p1) { }
        static void write(@NotModified java.lang.CharSequence p0, java.io.OutputStream p1) { }
        static void write(@NotModified java.lang.CharSequence p0, java.io.OutputStream p1, java.nio.charset.Charset p2) { }
        static void write(@NotModified java.lang.CharSequence p0, java.io.OutputStream p1, java.lang.String p2) { }
        static void write(@NotModified java.lang.CharSequence p0, java.io.Writer p1) { }
        static void write(@NotModified java.lang.StringBuffer p0, java.io.OutputStream p1) { }
        static void write(@NotModified java.lang.StringBuffer p0, java.io.OutputStream p1, java.lang.String p2) { }
        static void write(@NotModified java.lang.StringBuffer p0, java.io.Writer p1) { }
        static void writeChunked(@NotModified byte[] p0, java.io.OutputStream p1) { }
        static void writeChunked(@NotModified char[] p0, java.io.Writer p1) { }
        static void writeLines(@NotModified java.util.Collection<?> p0, java.lang.String p1, java.io.OutputStream p2) { }
        static void writeLines(@NotModified java.util.Collection<?> p0, java.lang.String p1, java.io.OutputStream p2, java.nio.charset.Charset p3) { }
        static void writeLines(@NotModified java.util.Collection<?> p0, java.lang.String p1, java.io.OutputStream p2, java.lang.String p3) { }
        static void writeLines(@NotModified java.util.Collection<?> p0, java.lang.String p1, java.io.Writer p2) { }
    }

    //public class org.apache.commons.io.FileUtils
    // files are immutable (JavaIo); the data written, extensions and file collections are read
    class FileUtils$ {
        static java.io.File[] convertFileCollectionToFileArray(@NotModified java.util.Collection<java.io.File> p0) { return null; }
        static void copyDirectory(java.io.File p0, java.io.File p1, java.io.FileFilter p2, boolean p3, @NotModified java.nio.file.CopyOption... p4) { }
        static void copyFile(java.io.File p0, java.io.File p1, boolean p2, @NotModified java.nio.file.CopyOption... p3) { }
        static void copyFile(java.io.File p0, java.io.File p1, @NotModified java.nio.file.CopyOption... p2) { }
        static void copyToDirectory(@NotModified java.lang.Iterable<java.io.File> p0, java.io.File p1) { }
        static java.io.File getFile(java.io.File p0, @NotModified java.lang.String... p1) { return null; }
        static java.io.File getFile(@NotModified java.lang.String... p0) { return null; }
        static boolean isDirectory(java.io.File p0, @NotModified java.nio.file.LinkOption... p1) { return false; }
        static boolean isRegularFile(java.io.File p0, @NotModified java.nio.file.LinkOption... p1) { return false; }
        static java.util.Iterator<java.io.File> iterateFiles(java.io.File p0, @NotModified java.lang.String[] p1, boolean p2) { return null; }
        static java.util.Collection<java.io.File> listFiles(java.io.File p0, @NotModified java.lang.String[] p1, boolean p2) { return null; }
        static void moveFile(java.io.File p0, java.io.File p1, @NotModified java.nio.file.CopyOption... p2) { }
        static java.util.stream.Stream<java.io.File> streamFiles(java.io.File p0, boolean p1, @NotModified java.lang.String... p2) { return null; }
        static java.io.File[] toFiles(@NotModified java.net.URL... p0) { return null; }
        static void write(java.io.File p0, @NotModified java.lang.CharSequence p1) { }
        static void write(java.io.File p0, @NotModified java.lang.CharSequence p1, boolean p2) { }
        static void write(java.io.File p0, @NotModified java.lang.CharSequence p1, java.nio.charset.Charset p2) { }
        static void write(java.io.File p0, @NotModified java.lang.CharSequence p1, java.nio.charset.Charset p2, boolean p3) { }
        static void write(java.io.File p0, @NotModified java.lang.CharSequence p1, java.lang.String p2) { }
        static void write(java.io.File p0, @NotModified java.lang.CharSequence p1, java.lang.String p2, boolean p3) { }
        static void writeByteArrayToFile(java.io.File p0, @NotModified byte[] p1) { }
        static void writeByteArrayToFile(java.io.File p0, @NotModified byte[] p1, boolean p2) { }
        static void writeByteArrayToFile(java.io.File p0, @NotModified byte[] p1, int p2, int p3) { }
        static void writeByteArrayToFile(java.io.File p0, @NotModified byte[] p1, int p2, int p3, boolean p4) { }
        static void writeLines(java.io.File p0, @NotModified java.util.Collection<?> p1) { }
        static void writeLines(java.io.File p0, @NotModified java.util.Collection<?> p1, boolean p2) { }
        static void writeLines(java.io.File p0, @NotModified java.util.Collection<?> p1, java.lang.String p2) { }
        static void writeLines(java.io.File p0, @NotModified java.util.Collection<?> p1, java.lang.String p2, boolean p3) { }
        static void writeLines(java.io.File p0, java.lang.String p1, @NotModified java.util.Collection<?> p2) { }
        static void writeLines(java.io.File p0, java.lang.String p1, @NotModified java.util.Collection<?> p2, boolean p3) { }
        static void writeLines(java.io.File p0, java.lang.String p1, @NotModified java.util.Collection<?> p2, java.lang.String p3) { }
        static void writeLines(java.io.File p0, java.lang.String p1, @NotModified java.util.Collection<?> p2, java.lang.String p3, boolean p4) { }
    }
}
