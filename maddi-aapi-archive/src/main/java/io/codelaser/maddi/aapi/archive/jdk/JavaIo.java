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
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
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
        int read() { return 0; }

        //override from java.io.Reader
        //@AllowsInterrupt[H]
        int read(/*@Independent[H] @NotNull[H]*/ @NotNull char [] cbuf, int off, int len) { return 0; }
        @Nullable @AllowsInterrupt String readLine() { return null; }
        //override from java.io.Reader
        //@AllowsInterrupt[H]
        long skip(long n) { return 0L; }

        //override from java.io.Reader
        //@NotModified[H]
        boolean ready() { return false; }

        //override from java.io.Reader
        //@NotModified[H]
        boolean markSupported() { return false; }

        //override from java.io.Reader
        //@AllowsInterrupt[H]
        void mark(int readAheadLimit) { }

        //override from java.io.Reader
        //@AllowsInterrupt[H]
        void reset() { }

        //override from java.io.Closeable, java.io.Reader, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }

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
        void write(int c) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(/*@Independent[H] @NotNull[H]*/ @NotNull char [] cbuf, int off, int len) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(@NotNull String s, int off, int len) { }
        @AllowsInterrupt void newLine() { }
        //override from java.io.Flushable, java.io.Writer
        //@AllowsInterrupt[H]
        void flush() { }

        //override from java.io.Closeable, java.io.Writer, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
    }

    //public class ByteArrayInputStream extends InputStream
    @Independent
    class ByteArrayInputStream$ {
        //frequency 1
        ByteArrayInputStream$(/*@Independent[T]*/ @NotModified @NotNull byte [] buf) { }
        ByteArrayInputStream$(/*@Independent[T]*/ @NotModified @NotNull byte [] buf, int offset, int length) { }
        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int read() { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int read(/*@Independent[H]*/ @NotNull byte [] b, int off, int len) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        @NotNull
        byte [] readAllBytes() { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int readNBytes(/*@Independent[H]*/ @NotNull byte [] b, int off, int len) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        long transferTo(/*@Independent[H]*/ @NotNull OutputStream out) { return 0L; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        long skip(long n) { return 0L; }

        //override from java.io.InputStream
        @NotModified
        int available() { return 0; }

        //override from java.io.InputStream
        @NotModified
        boolean markSupported() { return false; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        void mark(int readAheadLimit) { }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        void reset() { }

        //override from java.io.Closeable, java.io.InputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
    }

    //public class ByteArrayOutputStream extends OutputStream
    @Independent
    class ByteArrayOutputStream$ {
        //frequency 1
        ByteArrayOutputStream$() { }
        ByteArrayOutputStream$(int size) { }
        //override from java.io.OutputStream
        void write(int b) { }

        //override from java.io.OutputStream
        void write(/*@Independent[H]*/ @NotNull @NotModified byte [] b, int off, int len) { }
        void writeBytes(/*@Independent[T]*/ @NotNull byte [] b) { }
        void writeTo(/*@Independent[T]*/ @NotNull OutputStream out) { }
        void reset() { }
        //frequency 1
        //@Independent[O]
        @NotModified @NotNull
        byte [] toByteArray() { return null; }
        int size() { return 0; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toString(String charsetName) { return null; }
        String toString(/*@Independent[T]*/ Charset charset) { return null; }
        String toString(int hibyte) { return null; }
        //override from java.io.Closeable, java.io.OutputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
    }

    //public interface Closeable implements AutoCloseable
    //@Container[M]
    @Independent
    class Closeable$ {
        //override from java.lang.AutoCloseable
        @AllowsInterrupt
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
        int read() { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int read(/*@Independent[H]*/ @NotNull byte [] b) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int read(/*@Independent[H]*/ @NotNull byte [] b, int off, int len) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        byte [] readAllBytes() { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        byte [] readNBytes(int len) { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        long transferTo(/*@Independent[H]*/ @NotNull OutputStream out) { return 0L; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        long skip(long n) { return 0L; }

        //override from java.io.InputStream
        //@NotModified[H]
        int available() { return 0; }

        //override from java.io.Closeable, java.io.InputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }

        //@Independent[O]
        FileDescriptor getFD() { return null; }

        //@Independent[O]
        FileChannel getChannel() { return null; }
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
        @AllowsInterrupt
        void write(int b) { }

        //override from java.io.OutputStream
        @AllowsInterrupt
        void write(/*@Independent[H]*/ @NotNull @NotModified byte [] b) { }

        //override from java.io.OutputStream
        @AllowsInterrupt
        void write(/*@Independent[H]*/ @NotNull @NotModified byte [] b, int off, int len) { }

        //override from java.io.Flushable, java.io.OutputStream
        //@AllowsInterrupt[H]
        void flush() { }

        //override from java.io.Closeable, java.io.OutputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
    }

    //public interface Flushable
    //@Container[M]
    @Independent class Flushable$ {@AllowsInterrupt void flush() { } }

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
        @AllowsInterrupt int read() { return 0; }
        @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull byte [] b) { return 0; }
        @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull byte [] b, int off, int len) { return 0; }
        //@Independent[O]
        @AllowsInterrupt
        byte [] readAllBytes() { return null; }

        //@Independent[O]
        @AllowsInterrupt
        byte [] readNBytes(int len) { return null; }
        @AllowsInterrupt int readNBytes(/*@Independent[T]*/ @NotNull byte [] b, int off, int len) { return 0; }
        @AllowsInterrupt long skip(long n) { return 0L; }
        @AllowsInterrupt void skipNBytes(long n) { }
        @NotModified int available() { return 0; }
        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
        @AllowsInterrupt void mark(int readlimit) { }
        @AllowsInterrupt void reset() { }
        @NotModified boolean markSupported() { return false; }
        @AllowsInterrupt long transferTo(/*@Independent[T]*/ @NotNull OutputStream out) { return 0L; }
    }

    //public abstract class OutputStream implements Closeable, Flushable
    @Independent
    class OutputStream$ {
        //override has frequency 1
        OutputStream$() { }

        //@Independent[O]
        static OutputStream nullOutputStream() { return null; }
        void write(int i) { }
        void write(/*@Independent[T]*/ @NotNull @NotModified byte [] b) { }
        void write(/*@Independent[T]*/ @NotNull @NotModified byte [] b, int off, int len) { }
        //override from java.io.Flushable
        //@AllowsInterrupt[H]
        void flush() { }

        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
    }

    //public class OutputStreamWriter extends Writer
    class OutputStreamWriter$ {
        OutputStreamWriter$(@NotNull OutputStream out, @NotNull String charsetName) { }
        OutputStreamWriter$(@NotNull OutputStream out) { }
        OutputStreamWriter$(@NotNull OutputStream out, @NotNull Charset cs) { }
        OutputStreamWriter$(@NotNull OutputStream out, @NotNull CharsetEncoder enc) { }
        @Nullable String getEncoding() { return null; }
        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(int c) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(/*@Independent[H] @NotNull[H]*/ @NotNull char [] cbuf, int off, int len) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(@NotNull String str, int off, int len) { }

        //override from java.io.Writer, java.lang.Appendable
        //@AllowsInterrupt[H] @Independent[H]

        Writer append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[T] @NotNull[H]*/ @Nullable CharSequence csq,
            int start,
            int end) { return null; }

        //override from java.io.Writer, java.lang.Appendable
        //@AllowsInterrupt[H] @Independent[H]

        Writer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[T] @NotNull[H]*/ @Nullable CharSequence csq) {
            return null;
        }

        //override from java.io.Flushable, java.io.Writer
        //@AllowsInterrupt[H]
        void flush() { }

        //override from java.io.Closeable, java.io.Writer, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
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
        void flush() { }

        //override from java.io.Closeable, java.io.FilterOutputStream, java.io.OutputStream, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
        @NotModified boolean checkError() { return false; }
        //override from java.io.FilterOutputStream, java.io.OutputStream
        //@AllowsInterrupt[H]
        void write(int b) { }

        //override from java.io.FilterOutputStream, java.io.OutputStream
        //@AllowsInterrupt[H]
        void write(@NotNull @Independent @NotModified byte [] buf, int off, int len) { }

        //override from java.io.FilterOutputStream, java.io.OutputStream
        //@AllowsInterrupt[H]
        void write(@NotNull @Independent @NotModified byte [] buf) { }
        @AllowsInterrupt void writeBytes(@NotNull @Independent byte [] buf) { }
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
        @AllowsInterrupt PrintStream printf(@NotNull String format, @NotNull @NotModified @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream printf(@Nullable Locale l, @NotNull String format,
            @NotNull @NotModified @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream format(@NotNull String format, @NotNull @NotModified @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream format(@Nullable Locale l, @NotNull String format,
            @NotNull @NotModified @Independent Object ... args) { return null; }
        //override from java.lang.Appendable
        //@Independent[H]
        @AllowsInterrupt
        PrintStream append(/*@Immutable(hc=true)[T] @NotModified[T]*/ @Nullable @Independent CharSequence csq) { return null; }

        //override from java.lang.Appendable
        //@Independent[H]
        @AllowsInterrupt

        PrintStream append(/*@Immutable(hc=true)[T] @NotModified[T]*/ @Nullable @Independent CharSequence csq, int start, int end) {
            return null;
        }

        //override from java.lang.Appendable
        //@Independent[H]
        @AllowsInterrupt
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
        @AllowsInterrupt
        int read() { return 0; }
        @AllowsInterrupt
        int read(@NotNull byte [] b, int off, int len) { return 0; }
        @AllowsInterrupt
        int read(@NotNull byte [] b) { return 0; }
        //override from java.io.DataInput
        void readFully(@NotNull byte [] b) { }

        //override from java.io.DataInput
        void readFully(@NotNull byte [] b, int off, int len) { }

        //override from java.io.DataInput
        int skipBytes(int n) { return 0; }

        //override from java.io.DataOutput
        void write(int b) { }

        //override from java.io.DataOutput
        void write(@NotNull @NotModified byte [] b) { }

        //override from java.io.DataOutput
        void write(@NotNull @NotModified byte [] b, int off, int len) { }
        long getFilePointer() { return 0L; }
        @AllowsInterrupt
        void seek(long pos) { }
        @NotModified @AllowsInterrupt
        long length() { return 0L; }
        @AllowsInterrupt
        void setLength(long newLength) { }
        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }

        //override from java.io.DataInput
        boolean readBoolean() { return false; }

        //override from java.io.DataInput
        byte readByte() { return 0; }

        //override from java.io.DataInput
        int readUnsignedByte() { return 0; }

        //override from java.io.DataInput
        short readShort() { return 0; }

        //override from java.io.DataInput
        int readUnsignedShort() { return 0; }

        //override from java.io.DataInput
        char readChar() { return '\0'; }

        //override from java.io.DataInput
        int readInt() { return 0; }

        //override from java.io.DataInput
        long readLong() { return 0L; }

        //override from java.io.DataInput
        float readFloat() { return 0.0F; }

        //override from java.io.DataInput
        double readDouble() { return 0.0; }

        //override from java.io.DataInput
        @Nullable String readLine() { return null; }

        //override from java.io.DataInput
        String readUTF() { return null; }

        //override from java.io.DataOutput
        void writeBoolean(boolean v) { }

        //override from java.io.DataOutput
        void writeByte(int v) { }

        //override from java.io.DataOutput
        void writeShort(int v) { }

        //override from java.io.DataOutput
        void writeChar(int v) { }

        //override from java.io.DataOutput
        void writeInt(int v) { }

        //override from java.io.DataOutput
        void writeLong(long v) { }

        //override from java.io.DataOutput
        void writeFloat(float v) { }

        //override from java.io.DataOutput
        void writeDouble(double v) { }

        //override from java.io.DataOutput
        void writeBytes(@NotNull String s) { }

        //override from java.io.DataOutput
        void writeChars(@NotNull String s) { }

        //override from java.io.DataOutput
        void writeUTF(@NotNull String str) { }
    }

    //public abstract class Reader implements Readable, Closeable
    @Independent
    class Reader$ {
        //@Independent[O]
        @NotNull
        static Reader nullReader() { return null; }

        //override from java.lang.Readable
        @AllowsInterrupt
        int read(/*@Independent[H] @NotNull[H]*/ @NotNull CharBuffer target) { return 0; }
        @AllowsInterrupt int read() { return 0; }
        @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull char [] cbuf) { return 0; }
        @AllowsInterrupt int read(/*@Independent[T]*/ @NotNull char [] c, int i, int i1) { return 0; }
        @AllowsInterrupt long skip(long n) { return 0L; }
        @NotModified boolean ready() { return false; }
        @NotModified boolean markSupported() { return false; }
        @AllowsInterrupt void mark(int readAheadLimit) { }
        @AllowsInterrupt void reset() { }
        //override from java.io.Closeable, java.lang.AutoCloseable
        @AllowsInterrupt
        void close() { }
        @AllowsInterrupt long transferTo(/*@Independent[T]*/ @NotNull Writer out) { return 0L; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Reader$(@NotNull java.lang.Object p0) { }
        static java.io.Reader of(@NotNull java.lang.CharSequence p0) { return null; }
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
        @AllowsInterrupt void write(int c) { }
        @AllowsInterrupt void write(/*@Independent[T]*/ @NotNull char [] cbuf) { }
        @AllowsInterrupt void write(/*@Independent[T]*/ @NotNull char [] c, int i, int i1) { }
        //frequency 4
        @AllowsInterrupt
        void write(@NotNull String str) { }
        @AllowsInterrupt void write(@NotNull String str, int off, int len) { }
        //override from java.lang.Appendable
        //@Independent[O]
        @AllowsInterrupt

        Writer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[T]*/ @Nullable CharSequence csq) {
            return null;
        }

        //override from java.lang.Appendable
        //@Independent[O]
        @AllowsInterrupt

        Writer append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[T]*/ @Nullable CharSequence csq,
            int start,
            int end) { return null; }

        //override from java.lang.Appendable
        //@Independent[O]
        @AllowsInterrupt
        Writer append(char c) { return null; }

        //override from java.io.Flushable
        //@AllowsInterrupt[H]
        void flush() { }

        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Writer$(@NotNull java.lang.Object p0) { }
    }

    // null contracts (docs/design/nullability.md M2), formerly the analyzer's LibraryNullness
    class DataInput$ {
        @Nullable String readLine() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void readFully(@NotNull byte[] p0) { }
        void readFully(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.io.BufferedInputStream (shadow created for its nullability, generated)
    class BufferedInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        BufferedInputStream$(@NotNull java.io.InputStream p0) { }
        BufferedInputStream$(@NotNull java.io.InputStream p0, int p1) { }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        long transferTo(@NotNull java.io.OutputStream p0) { return 0; }
    }

    //public java.io.BufferedOutputStream (shadow created for its nullability, generated)
    class BufferedOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        BufferedOutputStream$(@NotNull java.io.OutputStream p0) { }
        BufferedOutputStream$(@NotNull java.io.OutputStream p0, int p1) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.io.CharArrayReader (shadow created for its nullability, generated)
    class CharArrayReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CharArrayReader$(@NotNull char[] p0) { }
        CharArrayReader$(@NotNull char[] p0, int p1, int p2) { }
        int read(@NotNull char[] p0, int p1, int p2) { return 0; }
        int read(@NotNull java.nio.CharBuffer p0) { return 0; }
    }

    //public java.io.CharArrayWriter (shadow created for its nullability, generated)
    class CharArrayWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.io.CharArrayWriter append(@Nullable java.lang.CharSequence p0) { return null; }
        java.io.CharArrayWriter append(@Nullable java.lang.CharSequence p0, int p1, int p2) { return null; }
        void write(@NotNull char[] p0, int p1, int p2) { }
        void write(@NotNull java.lang.String p0, int p1, int p2) { }
        void writeTo(@NotNull java.io.Writer p0) { }
    }

    //public java.io.Console (shadow created for its nullability, generated)
    class Console$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.io.Console format(@Nullable java.util.Locale p0, @NotNull java.lang.String p1, @NotNull java.lang.Object... p2) { return null; }
        java.io.Console format(@NotNull java.lang.String p0, @NotNull java.lang.Object... p1) { return null; }
        java.io.Console printf(@Nullable java.util.Locale p0, @NotNull java.lang.String p1, @NotNull java.lang.Object... p2) { return null; }
        java.io.Console printf(@NotNull java.lang.String p0, @NotNull java.lang.Object... p1) { return null; }
        @Nullable java.lang.String readLine() { return null; }
        @Nullable java.lang.String readLine(@Nullable java.util.Locale p0, @NotNull java.lang.String p1, @NotNull java.lang.Object... p2) { return null; }
        @Nullable java.lang.String readLine(@NotNull java.lang.String p0, @NotNull java.lang.Object... p1) { return null; }
        @Nullable char[] readPassword() { return null; }
        @Nullable char[] readPassword(@Nullable java.util.Locale p0, @NotNull java.lang.String p1, @NotNull java.lang.Object... p2) { return null; }
        @Nullable char[] readPassword(@NotNull java.lang.String p0, @NotNull java.lang.Object... p1) { return null; }
    }

    //public java.io.DataInputStream (shadow created for its nullability, generated)
    class DataInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DataInputStream$(@Nullable java.io.InputStream p0) { }
        int read(@NotNull byte[] p0) { return 0; }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        void readFully(@NotNull byte[] p0) { }
        void readFully(@NotNull byte[] p0, int p1, int p2) { }
        @Nullable java.lang.String readLine() { return null; }
        static java.lang.String readUTF(@NotNull java.io.DataInput p0) { return null; }
    }

    //public java.io.DataOutput (shadow created for its nullability, generated)
    class DataOutput$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void write(@NotNull byte[] p0) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
        void writeBytes(@NotNull java.lang.String p0) { }
        void writeChars(@NotNull java.lang.String p0) { }
        void writeUTF(@NotNull java.lang.String p0) { }
    }

    //public java.io.DataOutputStream (shadow created for its nullability, generated)
    class DataOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DataOutputStream$(@NotNull java.io.OutputStream p0) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
        void writeBytes(@NotNull java.lang.String p0) { }
        void writeChars(@NotNull java.lang.String p0) { }
        void writeUTF(@NotNull java.lang.String p0) { }
    }

    //public java.io.Externalizable (shadow created for its nullability, generated)
    class Externalizable$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void readExternal(@NotNull java.io.ObjectInput p0) { }
        void writeExternal(@NotNull java.io.ObjectOutput p0) { }
    }

    //public java.io.FileFilter (shadow created for its nullability, generated)
    class FileFilter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean accept(@NotNull java.io.File p0) { return false; }
    }

    //public java.io.FileOutputStream (shadow created for its nullability, generated)
    class FileOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FileOutputStream$(@NotNull java.io.File p0) { }
        FileOutputStream$(@NotNull java.io.File p0, boolean p1) { }
        FileOutputStream$(@NotNull java.io.FileDescriptor p0) { }
        FileOutputStream$(@NotNull java.lang.String p0) { }
        FileOutputStream$(@NotNull java.lang.String p0, boolean p1) { }
        void write(@NotNull byte[] p0) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.io.FilePermission (shadow created for its nullability, generated)
    class FilePermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilePermission$(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { }
        boolean implies(@Nullable java.security.Permission p0) { return false; }
    }

    //public java.io.FileReader (shadow created for its nullability, generated)
    class FileReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FileReader$(@NotNull java.io.File p0) { }
        FileReader$(@NotNull java.io.File p0, @NotNull java.nio.charset.Charset p1) { }
        FileReader$(@NotNull java.io.FileDescriptor p0) { }
        FileReader$(@NotNull java.lang.String p0) { }
        FileReader$(@NotNull java.lang.String p0, @NotNull java.nio.charset.Charset p1) { }
    }

    //public java.io.FilenameFilter (shadow created for its nullability, generated)
    class FilenameFilter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean accept(@NotNull java.io.File p0, @NotNull java.lang.String p1) { return false; }
    }

    //public java.io.FilterInputStream (shadow created for its nullability, generated)
    class FilterInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilterInputStream$(@Nullable java.io.InputStream p0) { }
        int read(@NotNull byte[] p0) { return 0; }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.FilterReader (shadow created for its nullability, generated)
    class FilterReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilterReader$(@NotNull java.io.Reader p0) { }
        int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.FilterWriter (shadow created for its nullability, generated)
    class FilterWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FilterWriter$(@NotNull java.io.Writer p0) { }
        void write(@NotNull char[] p0, int p1, int p2) { }
        void write(@NotNull java.lang.String p0, int p1, int p2) { }
    }

    //public java.io.InputStreamReader (shadow created for its nullability, generated)
    class InputStreamReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InputStreamReader$(@NotNull java.io.InputStream p0) { }
        InputStreamReader$(@NotNull java.io.InputStream p0, @NotNull java.nio.charset.Charset p1) { }
        InputStreamReader$(@NotNull java.io.InputStream p0, @NotNull java.nio.charset.CharsetDecoder p1) { }
        InputStreamReader$(@NotNull java.io.InputStream p0, @NotNull java.lang.String p1) { }
        @Nullable java.lang.String getEncoding() { return null; }
        int read(@NotNull char[] p0, int p1, int p2) { return 0; }
        int read(@NotNull java.nio.CharBuffer p0) { return 0; }
    }

    //public java.io.InterruptedIOException (shadow created for its nullability, generated)
    class InterruptedIOException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InterruptedIOException$(@Nullable java.lang.String p0) { }
    }

    //public java.io.InvalidObjectException (shadow created for its nullability, generated)
    class InvalidObjectException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InvalidObjectException$(@Nullable java.lang.String p0) { }
        InvalidObjectException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
    }

    //public java.io.LineNumberReader (shadow created for its nullability, generated)
    class LineNumberReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LineNumberReader$(@NotNull java.io.Reader p0) { }
        LineNumberReader$(@NotNull java.io.Reader p0, int p1) { }
        int read(@NotNull char[] p0, int p1, int p2) { return 0; }
        @Nullable java.lang.String readLine() { return null; }
    }

    //public java.io.ObjectInput (shadow created for its nullability, generated)
    class ObjectInput$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int read(@NotNull byte[] p0) { return 0; }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Nullable java.lang.Object readObject() { return null; }
    }

    //public java.io.ObjectInputFilter (shadow created for its nullability, generated)
    class ObjectInputFilter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.io.ObjectInputFilter merge(java.io.ObjectInputFilter p0, @Nullable java.io.ObjectInputFilter p1) { return null; }

        //public java.io.ObjectInputFilter.FilterInfo (shadow created for its nullability, generated)
        class FilterInfo {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable java.lang.Class<?> serialClass() { return null; }
        }
    }

    //public java.io.ObjectInputStream (shadow created for its nullability, generated)
    class ObjectInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectInputStream$(@NotNull java.io.InputStream p0) { }
        @Nullable java.io.ObjectInputFilter getObjectInputFilter() { return null; }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        void readFully(@NotNull byte[] p0) { }
        void readFully(@NotNull byte[] p0, int p1, int p2) { }
        @Nullable java.lang.String readLine() { return null; }
        @Nullable java.lang.Object readObject() { return null; }
        @Nullable java.lang.Object readObjectOverride() { return null; }
        @Nullable java.lang.Object readUnshared() { return null; }
        void registerValidation(@NotNull java.io.ObjectInputValidation p0, int p1) { }
        java.lang.Class<?> resolveClass(@NotNull java.io.ObjectStreamClass p0) { return null; }
        java.lang.Object resolveObject(@NotNull java.lang.Object p0) { return null; }
        java.lang.Class<?> resolveProxyClass(@NotNull java.lang.String[] p0) { return null; }
        void setObjectInputFilter(@Nullable java.io.ObjectInputFilter p0) { }

        //public java.io.ObjectInputStream.GetField (shadow created for its nullability, generated)
        class GetField {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            boolean defaulted(@NotNull java.lang.String p0) { return false; }
            boolean get(@NotNull java.lang.String p0, boolean p1) { return false; }
            byte get(@NotNull java.lang.String p0, byte p1) { return 0; }
            char get(@NotNull java.lang.String p0, char p1) { return 0; }
            double get(@NotNull java.lang.String p0, double p1) { return 0; }
            float get(@NotNull java.lang.String p0, float p1) { return 0; }
            int get(@NotNull java.lang.String p0, int p1) { return 0; }
            long get(@NotNull java.lang.String p0, long p1) { return 0; }
            @Nullable java.lang.Object get(@NotNull java.lang.String p0, @Nullable java.lang.Object p1) { return null; }
            short get(@NotNull java.lang.String p0, short p1) { return 0; }
        }
    }

    //public java.io.ObjectOutput (shadow created for its nullability, generated)
    class ObjectOutput$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void write(@NotNull byte[] p0) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
        void writeObject(@Nullable java.lang.Object p0) { }
    }

    //public java.io.ObjectOutputStream (shadow created for its nullability, generated)
    class ObjectOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectOutputStream$(@NotNull java.io.OutputStream p0) { }
        void annotateClass(@NotNull java.lang.Class<?> p0) { }
        void annotateProxyClass(@NotNull java.lang.Class<?> p0) { }
        java.lang.Object replaceObject(@NotNull java.lang.Object p0) { return null; }
        void write(@NotNull byte[] p0) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
        void writeBytes(@NotNull java.lang.String p0) { }
        void writeChars(@NotNull java.lang.String p0) { }
        void writeClassDescriptor(@NotNull java.io.ObjectStreamClass p0) { }
        void writeObject(@Nullable java.lang.Object p0) { }
        void writeObjectOverride(@Nullable java.lang.Object p0) { }
        void writeUnshared(@Nullable java.lang.Object p0) { }
        void writeUTF(@NotNull java.lang.String p0) { }

        //public java.io.ObjectOutputStream.PutField (shadow created for its nullability, generated)
        class PutField {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void put(@NotNull java.lang.String p0, boolean p1) { }
            void put(@NotNull java.lang.String p0, byte p1) { }
            void put(@NotNull java.lang.String p0, char p1) { }
            void put(@NotNull java.lang.String p0, double p1) { }
            void put(@NotNull java.lang.String p0, float p1) { }
            void put(@NotNull java.lang.String p0, int p1) { }
            void put(@NotNull java.lang.String p0, long p1) { }
            void put(@NotNull java.lang.String p0, @Nullable java.lang.Object p1) { }
            void put(@NotNull java.lang.String p0, short p1) { }
            void write(@NotNull java.io.ObjectOutput p0) { }
        }
    }

    //public java.io.ObjectStreamClass (shadow created for its nullability, generated)
    class ObjectStreamClass$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.Class<?> forClass() { return null; }
        @Nullable java.io.ObjectStreamField getField(@NotNull java.lang.String p0) { return null; }
        @Nullable static java.io.ObjectStreamClass lookup(@NotNull java.lang.Class<?> p0) { return null; }
        static java.io.ObjectStreamClass lookupAny(@NotNull java.lang.Class<?> p0) { return null; }
    }

    //public java.io.ObjectStreamException (shadow created for its nullability, generated)
    class ObjectStreamException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectStreamException$(@Nullable java.lang.String p0) { }
        ObjectStreamException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
        ObjectStreamException$(@Nullable java.lang.Throwable p0) { }
    }

    //public java.io.ObjectStreamField (shadow created for its nullability, generated)
    class ObjectStreamField$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ObjectStreamField$(@NotNull java.lang.String p0, @NotNull java.lang.Class<?> p1) { }
        ObjectStreamField$(@NotNull java.lang.String p0, @NotNull java.lang.Class<?> p1, boolean p2) { }
        int compareTo(@NotNull java.lang.Object p0) { return 0; }
        @Nullable java.lang.String getTypeString() { return null; }
    }

    //public java.io.PipedOutputStream (shadow created for its nullability, generated)
    class PipedOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PipedOutputStream$(@NotNull java.io.PipedInputStream p0) { }
        void connect(@NotNull java.io.PipedInputStream p0) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.io.PipedReader (shadow created for its nullability, generated)
    class PipedReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PipedReader$(@NotNull java.io.PipedWriter p0) { }
        PipedReader$(@NotNull java.io.PipedWriter p0, int p1) { }
        void connect(@NotNull java.io.PipedWriter p0) { }
        int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.PipedWriter (shadow created for its nullability, generated)
    class PipedWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PipedWriter$(@NotNull java.io.PipedReader p0) { }
        void connect(@NotNull java.io.PipedReader p0) { }
        void write(@NotNull char[] p0, int p1, int p2) { }
    }

    //public java.io.PrintWriter (shadow created for its nullability, generated)
    class PrintWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PrintWriter$(@NotNull java.io.File p0) { }
        PrintWriter$(@NotNull java.io.File p0, @NotNull java.nio.charset.Charset p1) { }
        PrintWriter$(@NotNull java.io.File p0, @NotNull java.lang.String p1) { }
        PrintWriter$(@NotNull java.io.OutputStream p0) { }
        PrintWriter$(@NotNull java.io.OutputStream p0, boolean p1) { }
        PrintWriter$(@NotNull java.io.OutputStream p0, boolean p1, @NotNull java.nio.charset.Charset p2) { }
        PrintWriter$(@NotNull java.lang.String p0) { }
        PrintWriter$(@NotNull java.lang.String p0, @NotNull java.nio.charset.Charset p1) { }
        PrintWriter$(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { }
        PrintWriter$(@NotNull java.io.Writer p0) { }
        PrintWriter$(@NotNull java.io.Writer p0, boolean p1) { }
        java.io.PrintWriter append(@Nullable java.lang.CharSequence p0) { return null; }
        java.io.PrintWriter append(@Nullable java.lang.CharSequence p0, int p1, int p2) { return null; }
        java.io.PrintWriter format(@Nullable java.util.Locale p0, @NotNull java.lang.String p1, @NotNull java.lang.Object... p2) { return null; }
        java.io.PrintWriter format(@NotNull java.lang.String p0, @NotNull java.lang.Object... p1) { return null; }
        void print(@NotNull char[] p0) { }
        void print(@Nullable java.lang.Object p0) { }
        void print(@Nullable java.lang.String p0) { }
        java.io.PrintWriter printf(@Nullable java.util.Locale p0, @NotNull java.lang.String p1, @NotNull java.lang.Object... p2) { return null; }
        java.io.PrintWriter printf(@NotNull java.lang.String p0, @NotNull java.lang.Object... p1) { return null; }
        void println(@NotNull char[] p0) { }
        void println(@Nullable java.lang.Object p0) { }
        void println(@Nullable java.lang.String p0) { }
        void write(@NotNull char[] p0) { }
        void write(@NotNull char[] p0, int p1, int p2) { }
        void write(@NotNull java.lang.String p0) { }
        void write(@NotNull java.lang.String p0, int p1, int p2) { }
    }

    //public java.io.PushbackInputStream (shadow created for its nullability, generated)
    class PushbackInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PushbackInputStream$(@NotNull java.io.InputStream p0) { }
        PushbackInputStream$(@NotNull java.io.InputStream p0, int p1) { }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        long transferTo(@NotNull java.io.OutputStream p0) { return 0; }
        void unread(@NotNull byte[] p0) { }
        void unread(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.io.PushbackReader (shadow created for its nullability, generated)
    class PushbackReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PushbackReader$(@NotNull java.io.Reader p0) { }
        PushbackReader$(@NotNull java.io.Reader p0, int p1) { }
        int read(@NotNull char[] p0, int p1, int p2) { return 0; }
        void unread(@NotNull char[] p0) { }
        void unread(@NotNull char[] p0, int p1, int p2) { }
    }

    //public java.io.SequenceInputStream (shadow created for its nullability, generated)
    class SequenceInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SequenceInputStream$(@NotNull java.util.Enumeration<? extends java.io.InputStream> p0) { }
        SequenceInputStream$(@NotNull java.io.InputStream p0, @NotNull java.io.InputStream p1) { }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        long transferTo(@NotNull java.io.OutputStream p0) { return 0; }
    }

    //public java.io.SerializablePermission (shadow created for its nullability, generated)
    class SerializablePermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SerializablePermission$(@NotNull java.lang.String p0) { }
        SerializablePermission$(@NotNull java.lang.String p0, @Nullable java.lang.String p1) { }
    }

    //public java.io.StreamTokenizer (shadow created for its nullability, generated)
    class StreamTokenizer$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StreamTokenizer$(@NotNull java.io.InputStream p0) { }
        StreamTokenizer$(@NotNull java.io.Reader p0) { }
    }

    //public java.io.StringBufferInputStream (shadow created for its nullability, generated)
    class StringBufferInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StringBufferInputStream$(@NotNull java.lang.String p0) { }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.StringReader (shadow created for its nullability, generated)
    class StringReader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StringReader$(@NotNull java.lang.String p0) { }
        int read(@NotNull char[] p0, int p1, int p2) { return 0; }
    }

    //public java.io.StringWriter (shadow created for its nullability, generated)
    class StringWriter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.io.StringWriter append(@Nullable java.lang.CharSequence p0) { return null; }
        java.io.StringWriter append(@Nullable java.lang.CharSequence p0, int p1, int p2) { return null; }
        void write(@NotNull char[] p0, int p1, int p2) { }
        void write(@NotNull java.lang.String p0) { }
        void write(@NotNull java.lang.String p0, int p1, int p2) { }
    }

    //public java.io.UnsupportedEncodingException (shadow created for its nullability, generated)
    class UnsupportedEncodingException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnsupportedEncodingException$(@Nullable java.lang.String p0) { }
    }
}
