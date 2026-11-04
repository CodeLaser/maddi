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
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.rare.AllowsInterrupt;
public class JavaIo {
    public static final String PACKAGE_NAME = "java.io";
    //public class BufferedReader extends Reader
    // wraps an external Reader (advancing/consuming it), so it is linked to and modifies its
    // source -> @Dependent, like BufferedWriter/FilterOutputStream. (Was incorrectly @Independent,
    // which contradicted its own dependent 'in' constructor parameter.)
    class BufferedReader$ {
        BufferedReader$(@NotNull Reader in, int sz) { }
        BufferedReader$(@NotNull Reader in) { }
        //override from java.io.Reader
        //@AllowsInterrupt[H]
        @Modified int read() { return 0; }

        //override from java.io.Reader
        //@AllowsInterrupt[H]
        @Modified int read(/*@Independent[H] @NotNull[H]*/ @NotNull char [] cbuf, int off, int len) { return 0; }
        @Modified @Nullable @AllowsInterrupt String readLine() { return null; }
        //override from java.io.Reader
        //@AllowsInterrupt[H]
        @Modified long skip(long n) { return 0L; }

        //override from java.io.Reader
        //@NotModified[H]
        boolean ready() { return false; }

        //override from java.io.Reader
        //@NotModified[H]
        boolean markSupported() { return false; }

        //override from java.io.Reader
        //@AllowsInterrupt[H]
        @Modified void mark(int readAheadLimit) { }

        //override from java.io.Reader
        //@AllowsInterrupt[H]
        @Modified void reset() { }

        //override from java.io.Closeable, java.io.Reader, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }

        //@Independent[O]
        @AllowsInterrupt
        Stream<String> lines() { return null; }
    }

    //public class BufferedWriter extends Writer
    class BufferedWriter$ {
        //frequency 3
        BufferedWriter$(@NotNull Writer out) { }
        BufferedWriter$(@NotNull Writer out, int sz) { }
        //override from java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void write(int c) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void write(/*@Independent[H] @NotNull[H]*/ @NotNull char [] cbuf, int off, int len) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void write(@NotNull String s, int off, int len) { }
        @AllowsInterrupt void newLine() { }
        //override from java.io.Flushable, java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void flush() { }

        //override from java.io.Closeable, java.io.Writer, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
    }

    //public class ByteArrayInputStream extends InputStream
    @Independent
    class ByteArrayInputStream$ {
        //frequency 1
        ByteArrayInputStream$(/*@Independent[T]*/ @NotModified @NotNull byte [] buf) { }
        ByteArrayInputStream$(/*@Independent[T]*/ @NotModified @NotNull byte [] buf, int offset, int length) { }
        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified int read() { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified int read(/*@Independent[H]*/ @NotNull byte [] b, int off, int len) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        @Modified @NotNull
        byte [] readAllBytes() { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified int readNBytes(/*@Independent[H]*/ @NotNull byte [] b, int off, int len) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified long transferTo(/*@Independent[H]*/ @NotNull OutputStream out) { return 0L; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified long skip(long n) { return 0L; }

        //override from java.io.InputStream
        @NotModified
        int available() { return 0; }

        //override from java.io.InputStream
        @NotModified
        boolean markSupported() { return false; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified void mark(int readAheadLimit) { }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified void reset() { }

        //override from java.io.Closeable, java.io.InputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
    }

    //public class ByteArrayOutputStream extends OutputStream
    @Independent
    class ByteArrayOutputStream$ {
        //frequency 1
        ByteArrayOutputStream$() { }
        ByteArrayOutputStream$(int size) { }
        //override from java.io.OutputStream
        @Modified void write(int b) { }

        //override from java.io.OutputStream
        @Modified void write(/*@Independent[H]*/ @NotNull @NotModified byte [] b, int off, int len) { }
        @Modified void writeBytes(/*@Independent[T]*/ @NotNull byte [] b) { }
        @Modified void writeTo(/*@Independent[T]*/ @NotNull OutputStream out) { }
        @Modified void reset() { }
        //frequency 1
        //@Independent[O]
        @NotModified @NotNull
        byte [] toByteArray() { return null; }
        @NotModified int size() { return 0; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        @NotModified String toString(String charsetName) { return null; }
        @NotModified String toString(/*@Independent[T]*/ Charset charset) { return null; }
        @NotModified String toString(int hibyte) { return null; }
        //override from java.io.Closeable, java.io.OutputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
    }

    //public interface Closeable implements AutoCloseable
    //@Container[M]
    @Independent
    class Closeable$ {
        //override from java.lang.AutoCloseable
        @Modified @AllowsInterrupt
        void close() { }
    }

    //public class File implements Serializable, Comparable<File>
    // immutable like java.nio.file.Path: every field is final, and what delete(), mkdir() or setReadable() change is
    // the file system, not the object. Before 2026-10-12 it was only @Independent, so every File argument --
    // new File(parent, child)'s parent, FileInputStream's file, renameTo's dest -- read as modified.
    @Immutable @Independent
    class File$ {
        static final char separatorChar = '\0';
        static final String separator = null;
        static final char pathSeparatorChar = '\0';
        static final String pathSeparator = null;
        //frequency 8
        File$(@NotNull String pathname) { }
        File$(@Nullable String parent, @NotNull String child) { }
        //frequency 3
        File$(/*@Independent[T]*/ @Nullable File parent, @NotNull String child) { }
        File$(/*@Independent[T]*/ @NotNull URI uri) { }
        //frequency 1
        @NotModified
        String getName() { return null; }
        @Nullable @NotModified String getParent() { return null; }
        //@Independent[O]
        @Nullable @NotModified
        File getParentFile() { return null; }
        @NotModified String getPath() { return null; }
        @NotModified boolean isAbsolute() { return false; }
        //frequency 1
        @NotModified
        String getAbsolutePath() { return null; }

        //frequency 3
        //@Independent[O]
        @NotModified
        File getAbsoluteFile() { return null; }
        @NotModified String getCanonicalPath() { return null; }
        //@Independent[O]
        @NotModified
        File getCanonicalFile() { return null; }

        //@Independent[O]
        URL toURL() { return null; }

        //@Independent[O]
        @NotModified
        URI toURI() { return null; }
        @NotModified boolean canRead() { return false; }
        @NotModified boolean canWrite() { return false; }
        //frequency 6
        @NotModified
        boolean exists() { return false; }

        //frequency 2
        @NotModified
        boolean isDirectory() { return false; }

        //frequency 1
        @NotModified
        boolean isFile() { return false; }
        @NotModified boolean isHidden() { return false; }
        @NotModified long lastModified() { return 0L; }
        @NotModified long length() { return 0L; }
        boolean createNewFile() { return false; }
        //frequency 3
        boolean delete() { return false; }
        void deleteOnExit() { }
        //@Independent[O]
        @Nullable @NotModified
        String [] list() { return null; }

        //@Independent[O]
        @Nullable @NotModified
        String [] list(/*@Independent[M]*/ @Nullable FilenameFilter filter) { return null; }

        //@Independent[O]
        @Nullable @NotModified
        File [] listFiles() { return null; }

        //@Independent[O]
        @Nullable @NotModified
        File [] listFiles(/*@Independent[M]*/ @Nullable FilenameFilter filter) { return null; }

        //@Independent[O]
        @Nullable @NotModified
        File [] listFiles(/*@Independent[M]*/ @Nullable FileFilter filter) { return null; }
        boolean mkdir() { return false; }
        boolean mkdirs() { return false; }
        //frequency 1
        boolean renameTo(/*@Independent[T]*/ @NotNull File dest) { return false; }
        boolean setLastModified(long time) { return false; }
        boolean setReadOnly() { return false; }
        boolean setWritable(boolean writable, boolean ownerOnly) { return false; }
        boolean setWritable(boolean writable) { return false; }
        boolean setReadable(boolean readable, boolean ownerOnly) { return false; }
        boolean setReadable(boolean readable) { return false; }
        boolean setExecutable(boolean executable, boolean ownerOnly) { return false; }
        boolean setExecutable(boolean executable) { return false; }
        @NotModified boolean canExecute() { return false; }
        //@Independent[O]
        @Nullable @NotModified
        static File [] listRoots() { return null; }
        @NotModified long getTotalSpace() { return 0L; }
        @NotModified long getFreeSpace() { return 0L; }
        @NotModified long getUsableSpace() { return 0L; }
        //@Independent[O]
        static File createTempFile(@NotNull String prefix, @Nullable String suffix, /*@Independent[T]*/ @Nullable File directory) { return null; }

        //@Independent[O]
        static File createTempFile(@NotNull String prefix, @Nullable String suffix) { return null; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull File pathname) { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //frequency 1
        //@Immutable(hc=true)[T] @Independent[O]
        @NotModified
        Path toPath() { return null; }
    }

    //public class FileInputStream extends InputStream
    @Independent
    class FileInputStream$ {
        //frequency 1
        FileInputStream$(@NotNull String name) { }
        FileInputStream$(/*@Independent[T]*/ @NotNull File file) { }
        FileInputStream$(/*@Independent[T]*/ @NotNull FileDescriptor fdObj) { }
        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified int read() { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified int read(/*@Independent[H]*/ @NotNull byte [] b) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified int read(/*@Independent[H]*/ @NotNull byte [] b, int off, int len) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        @Modified byte [] readAllBytes() { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        @Modified byte [] readNBytes(int len) { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified long transferTo(/*@Independent[H]*/ @NotNull OutputStream out) { return 0L; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        @Modified long skip(long n) { return 0L; }

        //override from java.io.InputStream
        //@NotModified[H]
        int available() { return 0; }

        //override from java.io.Closeable, java.io.InputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }

        //@Independent[O]
        @NotModified FileDescriptor getFD() { return null; }

        //@Independent[O]
        @NotModified FileChannel getChannel() { return null; }
    }

    //public class FileWriter extends OutputStreamWriter
    @Independent
    class FileWriter$ {
        FileWriter$(@NotNull String fileName) { }
        FileWriter$(@NotNull String fileName, boolean append) { }
        //frequency 3
        FileWriter$(/*@Independent[T]*/ @NotNull File file) { }
        FileWriter$(/*@Independent[T]*/ @NotNull File file, boolean append) { }
        FileWriter$(/*@Independent[T]*/ @NotNull FileDescriptor fd) { }
        FileWriter$(@NotNull String fileName, /*@Independent[T]*/ @NotNull Charset charset) { }
        FileWriter$(@NotNull String fileName, /*@Independent[T]*/ @NotNull Charset charset, boolean append) { }
        FileWriter$(/*@Independent[T]*/ @NotNull File file, /*@Independent[T]*/ @NotNull Charset charset) { }
        FileWriter$(/*@Independent[T]*/ @NotNull File file, /*@Independent[T]*/ @NotNull Charset charset, boolean append) { }
    }

    //public class FilterOutputStream extends OutputStream
    class FilterOutputStream$ {
        FilterOutputStream$(@Nullable OutputStream out) { }
        //override from java.io.OutputStream
        @Modified @AllowsInterrupt
        void write(int b) { }

        //override from java.io.OutputStream
        @Modified @AllowsInterrupt
        void write(/*@Independent[H]*/ @NotNull @NotModified byte [] b) { }

        //override from java.io.OutputStream
        @Modified @AllowsInterrupt
        void write(/*@Independent[H]*/ @NotNull @NotModified byte [] b, int off, int len) { }

        //override from java.io.Flushable, java.io.OutputStream
        //@AllowsInterrupt[H]
        @Modified void flush() { }

        //override from java.io.Closeable, java.io.OutputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
    }

    //public interface Flushable
    //@Container[M]
    @Modified @Independent class Flushable$ {@AllowsInterrupt void flush() { } }

    //public class IOException extends Exception
    class IOException$ {
        IOException$() { }
        //frequency 5
        IOException$(@Nullable String message) { }
        IOException$(@Nullable String message, @Nullable @NotModified Throwable cause) { }
        IOException$(@Nullable @NotModified Throwable cause) { }
    }

    //public abstract class InputStream implements Closeable
    @Independent
    class InputStream$ {
        InputStream$() { }
        //@Independent[O]
        static InputStream nullInputStream() { return null; }
        @Modified @AllowsInterrupt int read() { return 0; }
        @Modified @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull byte [] b) { return 0; }
        @Modified @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull byte [] b, int off, int len) { return 0; }
        //@Independent[O]
        @Modified @AllowsInterrupt
        byte [] readAllBytes() { return null; }

        //@Independent[O]
        @Modified @AllowsInterrupt
        byte [] readNBytes(int len) { return null; }
        @Modified @AllowsInterrupt int readNBytes(/*@Independent[T]*/ @NotNull byte [] b, int off, int len) { return 0; }
        @Modified @AllowsInterrupt long skip(long n) { return 0L; }
        @Modified @AllowsInterrupt void skipNBytes(long n) { }
        @NotModified int available() { return 0; }
        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
        @Modified @AllowsInterrupt void mark(int readlimit) { }
        @Modified @AllowsInterrupt void reset() { }
        @NotModified boolean markSupported() { return false; }
        @Modified @AllowsInterrupt long transferTo(/*@Independent[T]*/ @NotNull OutputStream out) { return 0L; }
    }

    //public abstract class OutputStream implements Closeable, Flushable
    @Independent
    class OutputStream$ {
        //override has frequency 1
        OutputStream$() { }

        //@Independent[O]
        static OutputStream nullOutputStream() { return null; }
        @Modified void write(int i) { }
        @Modified void write(/*@Independent[T]*/ @NotNull @NotModified byte [] b) { }
        @Modified void write(/*@Independent[T]*/ @NotNull @NotModified byte [] b, int off, int len) { }
        //override from java.io.Flushable
        //@AllowsInterrupt[H]
        @Modified void flush() { }

        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
    }

    //public class OutputStreamWriter extends Writer
    class OutputStreamWriter$ {
        OutputStreamWriter$(@NotNull OutputStream out, @NotNull String charsetName) { }
        OutputStreamWriter$(@NotNull OutputStream out) { }
        OutputStreamWriter$(@NotNull OutputStream out, @NotNull Charset cs) { }
        OutputStreamWriter$(@NotNull OutputStream out, @NotNull CharsetEncoder enc) { }
        @NotModified @Nullable String getEncoding() { return null; }
        //override from java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void write(int c) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void write(/*@Independent[H] @NotNull[H]*/ @NotNull char [] cbuf, int off, int len) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void write(@NotNull String str, int off, int len) { }

        //override from java.io.Writer, java.lang.Appendable
        //@AllowsInterrupt[H] @Independent[H]

        @Modified Writer append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[T] @NotNull[H]*/ @Nullable CharSequence csq,
            int start,
            int end) { return null; }

        //override from java.io.Writer, java.lang.Appendable
        //@AllowsInterrupt[H] @Independent[H]

        @Modified Writer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[T] @NotNull[H]*/ @Nullable CharSequence csq) {
            return null;
        }

        //override from java.io.Flushable, java.io.Writer
        //@AllowsInterrupt[H]
        @Modified void flush() { }

        //override from java.io.Closeable, java.io.Writer, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
    }

    //public class PrintStream extends FilterOutputStream implements Appendable, Closeable
    class PrintStream$ {
        PrintStream$(/*@NotNull[H]*/ @NotNull OutputStream out) { }
        PrintStream$(@NotNull OutputStream out, boolean autoFlush) { }
        PrintStream$(@NotNull OutputStream out, boolean autoFlush, @NotNull String encoding) { }
        PrintStream$(@NotNull OutputStream out, boolean autoFlush, @NotNull Charset charset) { }
        PrintStream$(@NotNull String fileName) { }
        PrintStream$(@NotNull String fileName, @NotNull String csn) { }
        PrintStream$(@NotNull String fileName, @NotNull Charset charset) { }
        PrintStream$(@NotNull File file) { }
        PrintStream$(@NotNull File file, @NotNull String csn) { }
        PrintStream$(@NotNull File file, @NotNull Charset charset) { }
        //override from java.io.FilterOutputStream, java.io.Flushable, java.io.OutputStream
        //@AllowsInterrupt[H]
        @Modified void flush() { }

        //override from java.io.Closeable, java.io.FilterOutputStream, java.io.OutputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
        @NotModified boolean checkError() { return false; }
        //override from java.io.FilterOutputStream, java.io.OutputStream
        //@AllowsInterrupt[H]
        @Modified void write(int b) { }

        //override from java.io.FilterOutputStream, java.io.OutputStream
        //@AllowsInterrupt[H]
        @Modified void write(@NotNull @Independent @NotModified byte [] buf, int off, int len) { }

        //override from java.io.FilterOutputStream, java.io.OutputStream
        //@AllowsInterrupt[H]
        @Modified void write(@NotNull @Independent @NotModified byte [] buf) { }
        @Modified @AllowsInterrupt void writeBytes(@NotNull @Independent byte [] buf) { }
        @AllowsInterrupt void print(boolean b) { }
        @AllowsInterrupt void print(char c) { }
        @AllowsInterrupt void print(int i) { }
        @AllowsInterrupt void print(long l) { }
        @AllowsInterrupt void print(float f) { }
        @AllowsInterrupt void print(double d) { }
        @AllowsInterrupt void print(@Independent @NotNull char [] s) { }
        @AllowsInterrupt void print(@Nullable String s) { }
        @AllowsInterrupt void print(@Nullable @Independent Object obj) { }
        @AllowsInterrupt void println() { }
        @AllowsInterrupt void println(boolean x) { }
        @AllowsInterrupt void println(char x) { }
        @AllowsInterrupt void println(int x) { }
        @AllowsInterrupt void println(long x) { }
        @AllowsInterrupt void println(float x) { }
        @AllowsInterrupt void println(double x) { }
        @AllowsInterrupt void println(@Independent @NotNull char [] x) { }
        //frequency 8
        @AllowsInterrupt
        void println(@Nullable String x) { }
        @AllowsInterrupt void println(@Nullable @Independent Object x) { }
        @AllowsInterrupt PrintStream printf(@NotNull String format, @NotNull(nullableArguments = 0) @NotModified @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream printf(@Nullable Locale l, @NotNull String format,
            @NotNull(nullableArguments = 0) @NotModified @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream format(@NotNull String format, @NotNull(nullableArguments = 0) @NotModified @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream format(@Nullable Locale l, @NotNull String format,
            @NotNull(nullableArguments = 0) @NotModified @Independent Object ... args) { return null; }
        //override from java.lang.Appendable
        //@Independent[H]
        @Modified @AllowsInterrupt
        PrintStream append(/*@Immutable(hc=true)[T] @NotModified[T]*/ @Nullable @Independent CharSequence csq) { return null; }

        //override from java.lang.Appendable
        //@Independent[H]
        @Modified @AllowsInterrupt

        PrintStream append(/*@Immutable(hc=true)[T] @NotModified[T]*/ @Nullable @Independent CharSequence csq, int start, int end) {
            return null;
        }

        //override from java.lang.Appendable
        //@Independent[H]
        @Modified @AllowsInterrupt
        PrintStream append(char c) { return null; }
        @NotModified Charset charset() { return null; }
    }

    //public class RandomAccessFile implements DataOutput, DataInput, Closeable
    @Independent
    class RandomAccessFile$ {
        RandomAccessFile$(@NotNull String pathname, @NotNull String mode) { }
        RandomAccessFile$(@NotNull File file, @NotNull String mode) { }
        @NotModified
        FileDescriptor getFD() { return null; }
        @NotModified
        FileChannel getChannel() { return null; }
        @Modified @AllowsInterrupt
        int read() { return 0; }
        @Modified @AllowsInterrupt
        int read(@NotNull byte [] b, int off, int len) { return 0; }
        @Modified @AllowsInterrupt
        int read(@NotNull byte [] b) { return 0; }
        //override from java.io.DataInput
        @Modified void readFully(@NotNull byte [] b) { }

        //override from java.io.DataInput
        @Modified void readFully(@NotNull byte [] b, int off, int len) { }

        //override from java.io.DataInput
        @Modified int skipBytes(int n) { return 0; }

        //override from java.io.DataOutput
        @Modified void write(int b) { }

        //override from java.io.DataOutput
        @Modified void write(@NotNull @NotModified byte [] b) { }

        //override from java.io.DataOutput
        @Modified void write(@NotNull @NotModified byte [] b, int off, int len) { }
        @NotModified long getFilePointer() { return 0L; }
        @AllowsInterrupt
        void seek(long pos) { }
        @NotModified @AllowsInterrupt
        long length() { return 0L; }
        @Modified @AllowsInterrupt
        void setLength(long newLength) { }
        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }

        //override from java.io.DataInput
        @Modified boolean readBoolean() { return false; }

        //override from java.io.DataInput
        @Modified byte readByte() { return 0; }

        //override from java.io.DataInput
        @Modified int readUnsignedByte() { return 0; }

        //override from java.io.DataInput
        @Modified short readShort() { return 0; }

        //override from java.io.DataInput
        @Modified int readUnsignedShort() { return 0; }

        //override from java.io.DataInput
        @Modified char readChar() { return '\0'; }

        //override from java.io.DataInput
        @Modified int readInt() { return 0; }

        //override from java.io.DataInput
        @Modified long readLong() { return 0L; }

        //override from java.io.DataInput
        @Modified float readFloat() { return 0.0F; }

        //override from java.io.DataInput
        @Modified double readDouble() { return 0.0; }

        //override from java.io.DataInput
        @Modified @Nullable String readLine() { return null; }

        //override from java.io.DataInput
        @Modified String readUTF() { return null; }

        //override from java.io.DataOutput
        @Modified void writeBoolean(boolean v) { }

        //override from java.io.DataOutput
        @Modified void writeByte(int v) { }

        //override from java.io.DataOutput
        @Modified void writeShort(int v) { }

        //override from java.io.DataOutput
        @Modified void writeChar(int v) { }

        //override from java.io.DataOutput
        @Modified void writeInt(int v) { }

        //override from java.io.DataOutput
        @Modified void writeLong(long v) { }

        //override from java.io.DataOutput
        @Modified void writeFloat(float v) { }

        //override from java.io.DataOutput
        @Modified void writeDouble(double v) { }

        //override from java.io.DataOutput
        @Modified void writeBytes(@NotNull String s) { }

        //override from java.io.DataOutput
        @Modified void writeChars(@NotNull String s) { }

        //override from java.io.DataOutput
        @Modified void writeUTF(@NotNull String str) { }
    }

    //public abstract class Reader implements Readable, Closeable
    @Independent
    class Reader$ {
        //@Independent[O]
        @NotNull
        static Reader nullReader() { return null; }

        //override from java.lang.Readable
        @Modified @AllowsInterrupt
        int read(/*@Independent[H] @NotNull[H]*/ @NotNull CharBuffer target) { return 0; }
        @Modified @AllowsInterrupt int read() { return 0; }
        @Modified @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull char [] cbuf) { return 0; }
        @Modified @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull char [] c, int i, int i1) { return 0; }
        @Modified @AllowsInterrupt long skip(long n) { return 0L; }
        @NotModified boolean ready() { return false; }
        @NotModified boolean markSupported() { return false; }
        @Modified @AllowsInterrupt void mark(int readAheadLimit) { }
        @Modified @AllowsInterrupt void reset() { }
        //override from java.io.Closeable, java.lang.AutoCloseable
        @Modified @AllowsInterrupt
        void close() { }
        @Modified @AllowsInterrupt long transferTo(/*@Independent[T]*/ @NotNull Writer out) { return 0L; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Reader$(@NotNull Object p0) { }
        static Reader of(@NotNull CharSequence p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified String readAllAsString() { return null; }
        @Modified List<String> readAllLines() { return null; }
    }
    //public interface Serializable
    @ImmutableContainer(hc = true) @Independent class Serializable$ { }
    //public class UncheckedIOException extends RuntimeException
    class UncheckedIOException$ {
        UncheckedIOException$(@Nullable String message, @NotNull @NotModified IOException cause) { }
        UncheckedIOException$(@NotNull @NotModified IOException cause) { }
    }

    //public abstract class Writer implements Appendable, Closeable, Flushable
    @Independent
    class Writer$ {
        //@Independent[O]
        @NotModified @NotNull
        static Writer nullWriter() { return null; }
        @Modified @AllowsInterrupt void write(int c) { }
        @Modified @AllowsInterrupt void write(/*@Independent[T]*/ @NotNull char [] cbuf) { }
        @Modified @AllowsInterrupt void write(/*@Independent[T]*/ @NotNull char [] c, int i, int i1) { }
        //frequency 4
        @Modified @AllowsInterrupt
        void write(@NotNull String str) { }
        @Modified @AllowsInterrupt void write(@NotNull String str, int off, int len) { }
        //override from java.lang.Appendable
        //@Independent[O]
        @Modified @AllowsInterrupt

        Writer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[T]*/ @Nullable CharSequence csq) {
            return null;
        }

        //override from java.lang.Appendable
        //@Independent[O]
        @Modified @AllowsInterrupt

        Writer append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[T]*/ @Nullable CharSequence csq,
            int start,
            int end) { return null; }

        //override from java.lang.Appendable
        //@Independent[O]
        @Modified @AllowsInterrupt
        Writer append(char c) { return null; }

        //override from java.io.Flushable
        //@AllowsInterrupt[H]
        @Modified void flush() { }

        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        @Modified void close() { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Writer$(@NotNull Object p0) { }
    }

    // null contracts (docs/design/nullability.md M2), formerly the analyzer's LibraryNullness
    class DataInput$ {
        @Modified @Nullable String readLine() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void readFully(@NotNull byte[] p0) { }
        @Modified void readFully(@NotNull byte[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean readBoolean() { return false; }
        @Modified byte readByte() { return 0; }
        @Modified char readChar() { return 0; }
        @Modified double readDouble() { return 0; }
        @Modified float readFloat() { return 0; }
        @Modified int readInt() { return 0; }
        @Modified long readLong() { return 0; }
        @Modified short readShort() { return 0; }
        @Modified String readUTF() { return null; }
        @Modified int readUnsignedByte() { return 0; }
        @Modified int readUnsignedShort() { return 0; }
        @Modified int skipBytes(int p0) { return 0; }
    }

    //public java.io.BufferedInputStream (shadow created for its nullability, generated)
    class BufferedInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        BufferedInputStream$(@NotNull InputStream p0) { }
        BufferedInputStream$(@NotNull InputStream p0, int p1) { }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified long transferTo(@NotNull OutputStream p0) { return 0; }
    }

    //public java.io.BufferedOutputStream (shadow created for its nullability, generated)
    class BufferedOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        BufferedOutputStream$(@NotNull OutputStream p0) { }
        BufferedOutputStream$(@NotNull OutputStream p0, int p1) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.io.CharArrayReader (shadow created for its nullability, generated)
    class CharArrayReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CharArrayReader$(@NotNull char[] p0) { }
        CharArrayReader$(@NotNull char[] p0, int p1, int p2) { }
        @Modified int read(@NotNull CharBuffer p0) { return 0; }
        @Modified int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.CharArrayWriter (shadow created for its nullability, generated)
    class CharArrayWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified CharArrayWriter append(@Nullable CharSequence p0) { return null; }
        @Modified CharArrayWriter append(@Nullable CharSequence p0, int p1, int p2) { return null; }
        @Modified void write(@NotNull String p0, int p1, int p2) { }
        @Modified void write(@NotNull char[] p0, int p1, int p2) { }
        @Modified void writeTo(@NotNull Writer p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void reset() { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int size() { return 0; }
        @NotModified char[] toCharArray() { return null; }
    }

    //public java.io.Console (shadow created for its nullability, generated)
    class Console$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Console format(@Nullable Locale p0, @NotNull String p1, @NotNull(nullableArguments = 0) Object... p2) { return null; }
        Console format(@NotNull String p0, @NotNull(nullableArguments = 0) Object... p1) { return null; }
        Console printf(@Nullable Locale p0, @NotNull String p1, @NotNull(nullableArguments = 0) Object... p2) { return null; }
        Console printf(@NotNull String p0, @NotNull(nullableArguments = 0) Object... p1) { return null; }
        @Modified @Nullable String readLine() { return null; }
        @Modified @Nullable String readLine(@Nullable Locale p0, @NotNull String p1, @NotNull(nullableArguments = 0) Object... p2) { return null; }
        @Modified @Nullable String readLine(@NotNull String p0, @NotNull(nullableArguments = 0) Object... p1) { return null; }
        @Modified @Nullable char[] readPassword() { return null; }
        @Modified @Nullable char[] readPassword(@Nullable Locale p0, @NotNull String p1, @NotNull(nullableArguments = 0) Object... p2) { return null; }
        @Modified @Nullable char[] readPassword(@NotNull String p0, @NotNull(nullableArguments = 0) Object... p1) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void flush() { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Charset charset() { return null; }
        @NotModified boolean isTerminal() { return false; }
    }

    //public java.io.DataInputStream (shadow created for its nullability, generated)
    class DataInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DataInputStream$(@Nullable InputStream p0) { }
        @Modified int read(@NotNull byte[] p0) { return 0; }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified void readFully(@NotNull byte[] p0) { }
        @Modified void readFully(@NotNull byte[] p0, int p1, int p2) { }
        @Modified @Nullable String readLine() { return null; }
        static String readUTF(@NotNull DataInput p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean readBoolean() { return false; }
        @Modified byte readByte() { return 0; }
        @Modified char readChar() { return 0; }
        @Modified double readDouble() { return 0; }
        @Modified float readFloat() { return 0; }
        @Modified int readInt() { return 0; }
        @Modified long readLong() { return 0; }
        @Modified short readShort() { return 0; }
        @Modified String readUTF() { return null; }
        @Modified int readUnsignedByte() { return 0; }
        @Modified int readUnsignedShort() { return 0; }
        @Modified int skipBytes(int p0) { return 0; }
    }

    //public java.io.DataOutput (shadow created for its nullability, generated)
    class DataOutput$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void write(@NotNull byte[] p0) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
        @Modified void writeBytes(@NotNull String p0) { }
        @Modified void writeChars(@NotNull String p0) { }
        @Modified void writeUTF(@NotNull String p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void write(int p0) { }
        @Modified void writeBoolean(boolean p0) { }
        @Modified void writeByte(int p0) { }
        @Modified void writeChar(int p0) { }
        @Modified void writeDouble(double p0) { }
        @Modified void writeFloat(float p0) { }
        @Modified void writeInt(int p0) { }
        @Modified void writeLong(long p0) { }
        @Modified void writeShort(int p0) { }
    }

    //public java.io.DataOutputStream (shadow created for its nullability, generated)
    class DataOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DataOutputStream$(@NotNull OutputStream p0) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
        @Modified void writeBytes(@NotNull String p0) { }
        @Modified void writeChars(@NotNull String p0) { }
        @Modified void writeUTF(@NotNull String p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void writeBoolean(boolean p0) { }
        @Modified void writeByte(int p0) { }
        @Modified void writeChar(int p0) { }
        @Modified void writeDouble(double p0) { }
        @Modified void writeFloat(float p0) { }
        @Modified void writeInt(int p0) { }
        @Modified void writeLong(long p0) { }
        @Modified void writeShort(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int size() { return 0; }
    }

    //public java.io.Externalizable (shadow created for its nullability, generated)
    class Externalizable$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void readExternal(@NotNull ObjectInput p0) { }
        @Modified void writeExternal(@NotNull ObjectOutput p0) { }
    }

    //public java.io.FileFilter (shadow created for its nullability, generated)
    class FileFilter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean accept(@NotNull File p0) { return false; }
    }

    //public java.io.FileOutputStream (shadow created for its nullability, generated)
    class FileOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FileOutputStream$(@NotNull File p0) { }
        FileOutputStream$(@NotNull File p0, boolean p1) { }
        FileOutputStream$(@NotNull FileDescriptor p0) { }
        FileOutputStream$(@NotNull String p0) { }
        FileOutputStream$(@NotNull String p0, boolean p1) { }
        @Modified void write(@NotNull byte[] p0) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified FileChannel getChannel() { return null; }
        @NotModified FileDescriptor getFD() { return null; }
    }

    //public java.io.FilePermission (shadow created for its nullability, generated)
    class FilePermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilePermission$(@NotNull String p0, @NotNull String p1) { }
        boolean implies(@Nullable Permission p0) { return false; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getActions() { return null; }
    }

    //public java.io.FileReader (shadow created for its nullability, generated)
    class FileReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FileReader$(@NotNull File p0) { }
        FileReader$(@NotNull File p0, @NotNull Charset p1) { }
        FileReader$(@NotNull FileDescriptor p0) { }
        FileReader$(@NotNull String p0) { }
        FileReader$(@NotNull String p0, @NotNull Charset p1) { }
    }

    //public java.io.FilenameFilter (shadow created for its nullability, generated)
    class FilenameFilter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean accept(@NotNull File p0, @NotNull String p1) { return false; }
    }

    //public java.io.FilterInputStream (shadow created for its nullability, generated)
    class FilterInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilterInputStream$(@Nullable InputStream p0) { }
        @Modified int read(@NotNull byte[] p0) { return 0; }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.FilterReader (shadow created for its nullability, generated)
    class FilterReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilterReader$(@NotNull Reader p0) { }
        @Modified int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.FilterWriter (shadow created for its nullability, generated)
    class FilterWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilterWriter$(@NotNull Writer p0) { }
        @Modified void write(@NotNull String p0, int p1, int p2) { }
        @Modified void write(@NotNull char[] p0, int p1, int p2) { }
    }

    //public java.io.InputStreamReader (shadow created for its nullability, generated)
    class InputStreamReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InputStreamReader$(@NotNull InputStream p0) { }
        InputStreamReader$(@NotNull InputStream p0, @NotNull Charset p1) { }
        InputStreamReader$(@NotNull InputStream p0, @NotNull CharsetDecoder p1) { }
        InputStreamReader$(@NotNull InputStream p0, @NotNull String p1) { }
        @NotModified @Nullable String getEncoding() { return null; }
        @Modified int read(@NotNull CharBuffer p0) { return 0; }
        @Modified int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.InterruptedIOException (shadow created for its nullability, generated)
    class InterruptedIOException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InterruptedIOException$(@Nullable String p0) { }
    }

    //public java.io.InvalidObjectException (shadow created for its nullability, generated)
    class InvalidObjectException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InvalidObjectException$(@Nullable String p0) { }
        InvalidObjectException$(@Nullable String p0, @Nullable Throwable p1) { }
    }

    //public java.io.LineNumberReader (shadow created for its nullability, generated)
    class LineNumberReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LineNumberReader$(@NotNull Reader p0) { }
        LineNumberReader$(@NotNull Reader p0, int p1) { }
        @Modified int read(@NotNull char[] p0, int p1, int p2) { return 0; }
        @Modified @Nullable String readLine() { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setLineNumber(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int getLineNumber() { return 0; }
    }

    //public java.io.ObjectInput (shadow created for its nullability, generated)
    class ObjectInput$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified int read(@NotNull byte[] p0) { return 0; }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void close() { }
        @Modified int read() { return 0; }
        @Modified Object readObject() { return null; }
        @Modified long skip(long p0) { return 0; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int available() { return 0; }
    }

    //public java.io.ObjectInputFilter (shadow created for its nullability, generated)
    class ObjectInputFilter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static ObjectInputFilter merge(ObjectInputFilter p0, @Nullable ObjectInputFilter p1) { return null; }

        //public java.io.ObjectInputFilter.FilterInfo (shadow created for its nullability, generated)
        class FilterInfo {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable Class<?> serialClass() { return null; }
        }
    }

    //public java.io.ObjectInputStream (shadow created for its nullability, generated)
    class ObjectInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectInputStream$(@NotNull InputStream p0) { }
        @NotModified @Nullable ObjectInputFilter getObjectInputFilter() { return null; }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified void readFully(@NotNull byte[] p0) { }
        @Modified void readFully(@NotNull byte[] p0, int p1, int p2) { }
        @Modified @Nullable String readLine() { return null; }
        @Nullable Object readObjectOverride() { return null; }
        @Modified void registerValidation(@NotNull ObjectInputValidation p0, int p1) { }
        Class<?> resolveClass(@NotNull ObjectStreamClass p0) { return null; }
        Object resolveObject(@NotNull Object p0) { return null; }
        Class<?> resolveProxyClass(@NotNull String[] p0) { return null; }
        @Modified void setObjectInputFilter(@Nullable ObjectInputFilter p0) { }

        //public java.io.ObjectInputStream.GetField (shadow created for its nullability, generated)
        class GetField {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            boolean defaulted(@NotNull String p0) { return false; }
            @NotModified Object get(@NotNull String p0, @Nullable Object p1) { return null; }
            @NotModified boolean get(@NotNull String p0, boolean p1) { return false; }
            @NotModified byte get(@NotNull String p0, byte p1) { return 0; }
            @NotModified char get(@NotNull String p0, char p1) { return 0; }
            @NotModified double get(@NotNull String p0, double p1) { return 0; }
            @NotModified float get(@NotNull String p0, float p1) { return 0; }
            @NotModified int get(@NotNull String p0, int p1) { return 0; }
            @NotModified long get(@NotNull String p0, long p1) { return 0; }
            @NotModified short get(@NotNull String p0, short p1) { return 0; }
        }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean readBoolean() { return false; }
        @Modified byte readByte() { return 0; }
        @Modified char readChar() { return 0; }
        @Modified double readDouble() { return 0; }
        @Modified ObjectInputStream.GetField readFields() { return null; }
        @Modified float readFloat() { return 0; }
        @Modified int readInt() { return 0; }
        @Modified long readLong() { return 0; }
        @Modified Object readObject() { return null; }
        @Modified short readShort() { return 0; }
        @Modified String readUTF() { return null; }
        @Modified Object readUnshared() { return null; }
        @Modified int readUnsignedByte() { return 0; }
        @Modified int readUnsignedShort() { return 0; }
        @Modified int skipBytes(int p0) { return 0; }
    }

    //public java.io.ObjectOutput (shadow created for its nullability, generated)
    class ObjectOutput$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void write(@NotNull byte[] p0) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
        @Modified void writeObject(@Nullable Object p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void close() { }
        @Modified void flush() { }
        @Modified void write(int p0) { }
    }

    //public java.io.ObjectOutputStream (shadow created for its nullability, generated)
    class ObjectOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectOutputStream$(@NotNull OutputStream p0) { }
        void annotateClass(@NotNull Class<?> p0) { }
        void annotateProxyClass(@NotNull Class<?> p0) { }
        Object replaceObject(@NotNull Object p0) { return null; }
        @Modified void write(@NotNull byte[] p0) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
        @Modified void writeBytes(@NotNull String p0) { }
        @Modified void writeChars(@NotNull String p0) { }
        void writeClassDescriptor(@NotNull ObjectStreamClass p0) { }
        @Modified void writeObject(@Nullable Object p0) { }
        void writeObjectOverride(@Nullable Object p0) { }
        @Modified void writeUTF(@NotNull String p0) { }
        @Modified void writeUnshared(@Nullable Object p0) { }

        //public java.io.ObjectOutputStream.PutField (shadow created for its nullability, generated)
        class PutField {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Modified void put(@NotNull String p0, @Nullable Object p1) { }
            @Modified void put(@NotNull String p0, boolean p1) { }
            @Modified void put(@NotNull String p0, byte p1) { }
            @Modified void put(@NotNull String p0, char p1) { }
            @Modified void put(@NotNull String p0, double p1) { }
            @Modified void put(@NotNull String p0, float p1) { }
            @Modified void put(@NotNull String p0, int p1) { }
            @Modified void put(@NotNull String p0, long p1) { }
            @Modified void put(@NotNull String p0, short p1) { }
            @Modified void write(@NotNull ObjectOutput p0) { }
        }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified ObjectOutputStream.PutField putFields() { return null; }
        @Modified void reset() { }
        @Modified void writeBoolean(boolean p0) { }
        @Modified void writeByte(int p0) { }
        @Modified void writeChar(int p0) { }
        @Modified void writeDouble(double p0) { }
        @Modified void writeFields() { }
        @Modified void writeFloat(float p0) { }
        @Modified void writeInt(int p0) { }
        @Modified void writeLong(long p0) { }
        @Modified void writeShort(int p0) { }
    }

    //public java.io.ObjectStreamClass (shadow created for its nullability, generated)
    class ObjectStreamClass$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable Class<?> forClass() { return null; }
        @NotModified @Nullable ObjectStreamField getField(@NotNull String p0) { return null; }
        @Nullable static ObjectStreamClass lookup(@NotNull Class<?> p0) { return null; }
        static ObjectStreamClass lookupAny(@NotNull Class<?> p0) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified ObjectStreamField[] getFields() { return null; }
        @NotModified String getName() { return null; }
        @NotModified long getSerialVersionUID() { return 0; }
    }

    //public java.io.ObjectStreamException (shadow created for its nullability, generated)
    class ObjectStreamException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectStreamException$(@Nullable String p0) { }
        ObjectStreamException$(@Nullable String p0, @Nullable Throwable p1) { }
        ObjectStreamException$(@Nullable Throwable p0) { }
    }

    //public java.io.ObjectStreamField (shadow created for its nullability, generated)
    class ObjectStreamField$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectStreamField$(@NotNull String p0, @NotNull Class<?> p1) { }
        ObjectStreamField$(@NotNull String p0, @NotNull Class<?> p1, boolean p2) { }
        int compareTo(@NotNull Object p0) { return 0; }
        @NotModified @Nullable String getTypeString() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getName() { return null; }
        @NotModified int getOffset() { return 0; }
        @NotModified Class<?> getType() { return null; }
        @NotModified char getTypeCode() { return 0; }
        @NotModified boolean isPrimitive() { return false; }
        @NotModified boolean isUnshared() { return false; }
    }

    //public java.io.PipedOutputStream (shadow created for its nullability, generated)
    class PipedOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PipedOutputStream$(@NotNull PipedInputStream p0) { }
        void connect(@NotNull PipedInputStream p0) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.io.PipedReader (shadow created for its nullability, generated)
    class PipedReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PipedReader$(@NotNull PipedWriter p0) { }
        PipedReader$(@NotNull PipedWriter p0, int p1) { }
        void connect(@NotNull PipedWriter p0) { }
        @Modified int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.PipedWriter (shadow created for its nullability, generated)
    class PipedWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PipedWriter$(@NotNull PipedReader p0) { }
        void connect(@NotNull PipedReader p0) { }
        @Modified void write(@NotNull char[] p0, int p1, int p2) { }
    }

    //public java.io.PrintWriter (shadow created for its nullability, generated)
    class PrintWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PrintWriter$(@NotNull File p0) { }
        PrintWriter$(@NotNull File p0, @NotNull Charset p1) { }
        PrintWriter$(@NotNull File p0, @NotNull String p1) { }
        PrintWriter$(@NotNull OutputStream p0) { }
        PrintWriter$(@NotNull OutputStream p0, boolean p1) { }
        PrintWriter$(@NotNull OutputStream p0, boolean p1, @NotNull Charset p2) { }
        PrintWriter$(@NotNull String p0) { }
        PrintWriter$(@NotNull String p0, @NotNull Charset p1) { }
        PrintWriter$(@NotNull String p0, @NotNull String p1) { }
        PrintWriter$(@NotNull Writer p0) { }
        PrintWriter$(@NotNull Writer p0, boolean p1) { }
        @Modified PrintWriter append(@Nullable CharSequence p0) { return null; }
        @Modified PrintWriter append(@Nullable CharSequence p0, int p1, int p2) { return null; }
        PrintWriter format(@Nullable Locale p0, @NotNull String p1, @NotNull(nullableArguments = 0) Object... p2) { return null; }
        PrintWriter format(@NotNull String p0, @NotNull(nullableArguments = 0) Object... p1) { return null; }
        void print(@Nullable Object p0) { }
        void print(@Nullable String p0) { }
        void print(@NotNull char[] p0) { }
        PrintWriter printf(@Nullable Locale p0, @NotNull String p1, @NotNull(nullableArguments = 0) Object... p2) { return null; }
        PrintWriter printf(@NotNull String p0, @NotNull(nullableArguments = 0) Object... p1) { return null; }
        void println(@Nullable Object p0) { }
        void println(@Nullable String p0) { }
        void println(@NotNull char[] p0) { }
        @Modified void write(@NotNull String p0) { }
        @Modified void write(@NotNull String p0, int p1, int p2) { }
        @Modified void write(@NotNull char[] p0) { }
        @Modified void write(@NotNull char[] p0, int p1, int p2) { }
    }

    //public java.io.PushbackInputStream (shadow created for its nullability, generated)
    class PushbackInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PushbackInputStream$(@NotNull InputStream p0) { }
        PushbackInputStream$(@NotNull InputStream p0, int p1) { }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified long transferTo(@NotNull OutputStream p0) { return 0; }
        @Modified void unread(@NotNull byte[] p0) { }
        @Modified void unread(@NotNull byte[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void unread(int p0) { }
    }

    //public java.io.PushbackReader (shadow created for its nullability, generated)
    class PushbackReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PushbackReader$(@NotNull Reader p0) { }
        PushbackReader$(@NotNull Reader p0, int p1) { }
        @Modified int read(@NotNull char[] p0, int p1, int p2) { return 0; }
        @Modified void unread(@NotNull char[] p0) { }
        @Modified void unread(@NotNull char[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void unread(int p0) { }
    }

    //public java.io.SequenceInputStream (shadow created for its nullability, generated)
    class SequenceInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SequenceInputStream$(@NotNull Enumeration<? extends InputStream> p0) { }
        SequenceInputStream$(@NotNull InputStream p0, @NotNull InputStream p1) { }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified long transferTo(@NotNull OutputStream p0) { return 0; }
    }

    //public java.io.SerializablePermission (shadow created for its nullability, generated)
    class SerializablePermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SerializablePermission$(@NotNull String p0) { }
        SerializablePermission$(@NotNull String p0, @Nullable String p1) { }
    }

    //public java.io.StreamTokenizer (shadow created for its nullability, generated)
    class StreamTokenizer$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StreamTokenizer$(@NotNull InputStream p0) { }
        StreamTokenizer$(@NotNull Reader p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified int nextToken() { return 0; }
        @Modified void pushBack() { }
        @Modified void resetSyntax() { }
    }

    //public java.io.StringBufferInputStream (shadow created for its nullability, generated)
    class StringBufferInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StringBufferInputStream$(@NotNull String p0) { }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.StringReader (shadow created for its nullability, generated)
    class StringReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StringReader$(@NotNull String p0) { }
        @Modified int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.StringWriter (shadow created for its nullability, generated)
    class StringWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified StringWriter append(@Nullable CharSequence p0) { return null; }
        @Modified StringWriter append(@Nullable CharSequence p0, int p1, int p2) { return null; }
        @Modified void write(@NotNull String p0) { }
        @Modified void write(@NotNull String p0, int p1, int p2) { }
        @Modified void write(@NotNull char[] p0, int p1, int p2) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified StringBuffer getBuffer() { return null; }
    }

    //public java.io.UnsupportedEncodingException (shadow created for its nullability, generated)
    class UnsupportedEncodingException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnsupportedEncodingException$(@Nullable String p0) { }
    }
}
