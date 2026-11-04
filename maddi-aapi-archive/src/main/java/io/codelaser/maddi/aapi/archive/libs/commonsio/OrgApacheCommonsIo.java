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

package io.codelaser.maddi.aapi.archive.libs.commonsio;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

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
        static void closeQuietly(@NotModified Iterable<Closeable> p0) { }
        static int length(@NotModified byte[] p0) { return 0; }
        static int length(@NotModified char[] p0) { return 0; }
        static int length(@NotModified CharSequence p0) { return 0; }
        static int length(@NotModified Object[] p0) { return 0; }
        static InputStream toInputStream(@NotModified CharSequence p0) { return null; }
        static InputStream toInputStream(@NotModified CharSequence p0, Charset p1) { return null; }
        static InputStream toInputStream(@NotModified CharSequence p0, String p1) { return null; }
        static String toString(@NotModified byte[] p0) { return null; }
        static String toString(@NotModified byte[] p0, String p1) { return null; }
        static void write(@NotModified byte[] p0, OutputStream p1) { }
        static void write(@NotModified byte[] p0, Writer p1) { }
        static void write(@NotModified byte[] p0, Writer p1, Charset p2) { }
        static void write(@NotModified byte[] p0, Writer p1, String p2) { }
        static void write(@NotModified char[] p0, OutputStream p1) { }
        static void write(@NotModified char[] p0, OutputStream p1, Charset p2) { }
        static void write(@NotModified char[] p0, OutputStream p1, String p2) { }
        static void write(@NotModified char[] p0, Writer p1) { }
        static void write(@NotModified CharSequence p0, OutputStream p1) { }
        static void write(@NotModified CharSequence p0, OutputStream p1, Charset p2) { }
        static void write(@NotModified CharSequence p0, OutputStream p1, String p2) { }
        static void write(@NotModified CharSequence p0, Writer p1) { }
        static void write(@NotModified StringBuffer p0, OutputStream p1) { }
        static void write(@NotModified StringBuffer p0, OutputStream p1, String p2) { }
        static void write(@NotModified StringBuffer p0, Writer p1) { }
        static void writeChunked(@NotModified byte[] p0, OutputStream p1) { }
        static void writeChunked(@NotModified char[] p0, Writer p1) { }
        static void writeLines(@NotModified Collection<?> p0, String p1, OutputStream p2) { }
        static void writeLines(@NotModified Collection<?> p0, String p1, OutputStream p2, Charset p3) { }
        static void writeLines(@NotModified Collection<?> p0, String p1, OutputStream p2, String p3) { }
        static void writeLines(@NotModified Collection<?> p0, String p1, Writer p2) { }
    }

    //public class org.apache.commons.io.FileUtils
    // files are immutable (JavaIo); the data written, extensions and file collections are read
    class FileUtils$ {
        static File[] convertFileCollectionToFileArray(@NotModified Collection<File> p0) { return null; }
        static void copyDirectory(File p0, File p1, FileFilter p2, boolean p3, @NotModified CopyOption... p4) { }
        static void copyFile(File p0, File p1, boolean p2, @NotModified CopyOption... p3) { }
        static void copyFile(File p0, File p1, @NotModified CopyOption... p2) { }
        static void copyToDirectory(@NotModified Iterable<File> p0, File p1) { }
        static File getFile(File p0, @NotModified String... p1) { return null; }
        static File getFile(@NotModified String... p0) { return null; }
        static boolean isDirectory(File p0, @NotModified LinkOption... p1) { return false; }
        static boolean isRegularFile(File p0, @NotModified LinkOption... p1) { return false; }
        static Iterator<File> iterateFiles(File p0, @NotModified String[] p1, boolean p2) { return null; }
        static Collection<File> listFiles(File p0, @NotModified String[] p1, boolean p2) { return null; }
        static void moveFile(File p0, File p1, @NotModified CopyOption... p2) { }
        static Stream<File> streamFiles(File p0, boolean p1, @NotModified String... p2) { return null; }
        static File[] toFiles(@NotModified URL... p0) { return null; }
        static void write(File p0, @NotModified CharSequence p1) { }
        static void write(File p0, @NotModified CharSequence p1, boolean p2) { }
        static void write(File p0, @NotModified CharSequence p1, Charset p2) { }
        static void write(File p0, @NotModified CharSequence p1, Charset p2, boolean p3) { }
        static void write(File p0, @NotModified CharSequence p1, String p2) { }
        static void write(File p0, @NotModified CharSequence p1, String p2, boolean p3) { }
        static void writeByteArrayToFile(File p0, @NotModified byte[] p1) { }
        static void writeByteArrayToFile(File p0, @NotModified byte[] p1, boolean p2) { }
        static void writeByteArrayToFile(File p0, @NotModified byte[] p1, int p2, int p3) { }
        static void writeByteArrayToFile(File p0, @NotModified byte[] p1, int p2, int p3, boolean p4) { }
        static void writeLines(File p0, @NotModified Collection<?> p1) { }
        static void writeLines(File p0, @NotModified Collection<?> p1, boolean p2) { }
        static void writeLines(File p0, @NotModified Collection<?> p1, String p2) { }
        static void writeLines(File p0, @NotModified Collection<?> p1, String p2, boolean p3) { }
        static void writeLines(File p0, String p1, @NotModified Collection<?> p2) { }
        static void writeLines(File p0, String p1, @NotModified Collection<?> p2, boolean p3) { }
        static void writeLines(File p0, String p1, @NotModified Collection<?> p2, String p3) { }
        static void writeLines(File p0, String p1, @NotModified Collection<?> p2, String p3, boolean p4) { }
    }

}
