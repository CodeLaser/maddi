package org.e2immu.analyzer.aapi.archive.v2.jdk;
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

import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.NotNull;
import org.e2immu.annotation.rare.AllowsInterrupt;
public class JavaIo {
    public static final String PACKAGE_NAME = "java.io";
    //public class BufferedReader extends Reader
    @Independent
    class BufferedReader$ {
        BufferedReader$(/*@Independent[T]*/ Reader in, int sz) { }
        BufferedReader$(/*@Independent[T]*/ Reader in) { }
        //override from java.io.Reader
        //@AllowsInterrupt[H]
        int read() { return 0; }

        //override from java.io.Reader
        //@AllowsInterrupt[H]
        int read(/*@Independent[H] @NotNull[H]*/ char [] cbuf, int off, int len) { return 0; }
        @AllowsInterrupt String readLine() { return null; }
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
        void write(/*@Independent[H] @NotNull[H]*/ char [] cbuf, int off, int len) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(String s, int off, int len) { }
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
        int read(/*@Independent[H]*/ byte [] b, int off, int len) { return 0; }

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
        void write(/*@Independent[H]*/ @NotNull byte [] b, int off, int len) { }
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
    @Independent
    class File$ {
        static final char separatorChar = '\0';
        static final String separator = null;
        static final char pathSeparatorChar = '\0';
        static final String pathSeparator = null;
        //frequency 8
        File$(String pathname) { }
        File$(String parent, String child) { }
        //frequency 3
        File$(/*@Independent[T]*/ File parent, String child) { }
        File$(/*@Independent[T]*/ URI uri) { }
        //frequency 1
        @NotModified
        String getName() { return null; }
        @NotModified String getParent() { return null; }
        //@Independent[O]
        @NotModified
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
        @NotModified
        String [] list() { return null; }

        //@Independent[O]
        @NotModified
        String [] list(/*@Independent[M]*/ FilenameFilter filter) { return null; }

        //@Independent[O]
        @NotModified
        File [] listFiles() { return null; }

        //@Independent[O]
        @NotModified
        File [] listFiles(/*@Independent[M]*/ FilenameFilter filter) { return null; }

        //@Independent[O]
        @NotModified
        File [] listFiles(/*@Independent[M]*/ FileFilter filter) { return null; }
        boolean mkdir() { return false; }
        boolean mkdirs() { return false; }
        //frequency 1
        boolean renameTo(/*@Independent[T]*/ File dest) { return false; }
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
        @NotModified
        static File [] listRoots() { return null; }
        @NotModified long getTotalSpace() { return 0L; }
        @NotModified long getFreeSpace() { return 0L; }
        @NotModified long getUsableSpace() { return 0L; }
        //@Independent[O]
        static File createTempFile(String prefix, String suffix, /*@Independent[T]*/ File directory) { return null; }

        //@Independent[O]
        static File createTempFile(String prefix, String suffix) { return null; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ File pathname) { return 0; }

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
        FileInputStream$(String name) { }
        FileInputStream$(/*@Independent[T]*/ File file) { }
        FileInputStream$(/*@Independent[T]*/ FileDescriptor fdObj) { }
        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int read() { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int read(/*@Independent[H]*/ byte [] b) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        int read(/*@Independent[H]*/ byte [] b, int off, int len) { return 0; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        byte [] readAllBytes() { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H] @Independent[O]
        byte [] readNBytes(int len) { return null; }

        //override from java.io.InputStream
        //@AllowsInterrupt[H]
        long transferTo(/*@Independent[H]*/ OutputStream out) { return 0L; }

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
        FileWriter$(String fileName) { }
        FileWriter$(String fileName, boolean append) { }
        //frequency 3
        FileWriter$(/*@Independent[T]*/ File file) { }
        FileWriter$(/*@Independent[T]*/ File file, boolean append) { }
        FileWriter$(/*@Independent[T]*/ FileDescriptor fd) { }
        FileWriter$(String fileName, /*@Independent[T]*/ Charset charset) { }
        FileWriter$(String fileName, /*@Independent[T]*/ Charset charset, boolean append) { }
        FileWriter$(/*@Independent[T]*/ File file, /*@Independent[T]*/ Charset charset) { }
        FileWriter$(/*@Independent[T]*/ File file, /*@Independent[T]*/ Charset charset, boolean append) { }
    }

    //public class FilterOutputStream extends OutputStream
    class FilterOutputStream$ {
        FilterOutputStream$(@NotNull OutputStream out) { }
        //override from java.io.OutputStream
        @AllowsInterrupt
        void write(int b) { }

        //override from java.io.OutputStream
        @AllowsInterrupt
        void write(/*@Independent[H]*/ byte [] b) { }

        //override from java.io.OutputStream
        @AllowsInterrupt
        void write(/*@Independent[H]*/ byte [] b, int off, int len) { }

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
        IOException$(String message) { }
        IOException$(String message, Throwable cause) { }
        IOException$(Throwable cause) { }
    }

    //public abstract class InputStream implements Closeable
    @Independent
    class InputStream$ {
        InputStream$() { }
        //@Independent[O]
        static InputStream nullInputStream() { return null; }
        @AllowsInterrupt int read() { return 0; }
        @AllowsInterrupt int read(/*@Independent[T]*/ byte [] b) { return 0; }
        @AllowsInterrupt int read(/*@Independent[T]*/ byte [] b, int off, int len) { return 0; }
        //@Independent[O]
        @AllowsInterrupt
        byte [] readAllBytes() { return null; }

        //@Independent[O]
        @AllowsInterrupt
        byte [] readNBytes(int len) { return null; }
        @AllowsInterrupt int readNBytes(/*@Independent[T]*/ byte [] b, int off, int len) { return 0; }
        @AllowsInterrupt long skip(long n) { return 0L; }
        @AllowsInterrupt void skipNBytes(long n) { }
        @NotModified int available() { return 0; }
        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
        @AllowsInterrupt void mark(int readlimit) { }
        @AllowsInterrupt void reset() { }
        @NotModified boolean markSupported() { return false; }
        @AllowsInterrupt long transferTo(/*@Independent[T]*/ OutputStream out) { return 0L; }
    }

    //public abstract class OutputStream implements Closeable, Flushable
    @Independent
    class OutputStream$ {
        //override has frequency 1
        OutputStream$() { }

        //@Independent[O]
        static OutputStream nullOutputStream() { return null; }
        void write(int i) { }
        void write(/*@Independent[T]*/ byte [] b) { }
        void write(/*@Independent[T]*/ byte [] b, int off, int len) { }
        //override from java.io.Flushable
        //@AllowsInterrupt[H]
        void flush() { }

        //override from java.io.Closeable, java.lang.AutoCloseable
        //@AllowsInterrupt[H]
        void close() { }
    }

    //public class OutputStreamWriter extends Writer
    class OutputStreamWriter$ {
        OutputStreamWriter$(OutputStream out, String charsetName) { }
        OutputStreamWriter$(OutputStream out) { }
        OutputStreamWriter$(OutputStream out, Charset cs) { }
        OutputStreamWriter$(OutputStream out, CharsetEncoder enc) { }
        String getEncoding() { return null; }
        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(int c) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(/*@Independent[H] @NotNull[H]*/ char [] cbuf, int off, int len) { }

        //override from java.io.Writer
        //@AllowsInterrupt[H]
        void write(String str, int off, int len) { }

        //override from java.io.Writer, java.lang.Appendable
        //@AllowsInterrupt[H] @Independent[H]

        Writer append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[T] @NotNull[H]*/ CharSequence csq,
            int start,
            int end) { return null; }

        //override from java.io.Writer, java.lang.Appendable
        //@AllowsInterrupt[H] @Independent[H]

        Writer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[T] @NotNull[H]*/ CharSequence csq) {
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
        PrintStream$(/*@NotNull[H]*/ OutputStream out) { }
        PrintStream$(OutputStream out, boolean autoFlush) { }
        PrintStream$(OutputStream out, boolean autoFlush, String encoding) { }
        PrintStream$(OutputStream out, boolean autoFlush, Charset charset) { }
        PrintStream$(String fileName) { }
        PrintStream$(String fileName, String csn) { }
        PrintStream$(String fileName, Charset charset) { }
        PrintStream$(File file) { }
        PrintStream$(File file, String csn) { }
        PrintStream$(File file, Charset charset) { }
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
        void write(@Independent byte [] buf, int off, int len) { }

        //override from java.io.FilterOutputStream, java.io.OutputStream
        //@AllowsInterrupt[H]
        void write(@Independent byte [] buf) { }
        @AllowsInterrupt void writeBytes(@Independent byte [] buf) { }
        @AllowsInterrupt void print(boolean b) { }
        @AllowsInterrupt void print(char c) { }
        @AllowsInterrupt void print(int i) { }
        @AllowsInterrupt void print(long l) { }
        @AllowsInterrupt void print(float f) { }
        @AllowsInterrupt void print(double d) { }
        @AllowsInterrupt void print(@Independent @NotNull char [] s) { }
        @AllowsInterrupt void print(String s) { }
        @AllowsInterrupt void print(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object obj) { }
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
        void println(String x) { }
        @AllowsInterrupt void println(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object x) { }
        @AllowsInterrupt PrintStream printf(String format, @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream printf(Locale l, String format, @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream format(String format, @Independent Object ... args) { return null; }
        @AllowsInterrupt PrintStream format(Locale l, String format, @Independent Object ... args) { return null; }
        //override from java.lang.Appendable
        //@Independent[H]
        @AllowsInterrupt
        PrintStream append(/*@Immutable(hc=true)[T] @NotModified[T]*/ @Independent CharSequence csq) { return null; }

        //override from java.lang.Appendable
        //@Independent[H]
        @AllowsInterrupt

        PrintStream append(/*@Immutable(hc=true)[T] @NotModified[T]*/ @Independent CharSequence csq, int start, int end) {
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
        RandomAccessFile$(String pathname, String mode) { }
        RandomAccessFile$(File file, String mode) { }
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
        void readFully(byte [] b) { }

        //override from java.io.DataInput
        void readFully(byte [] b, int off, int len) { }

        //override from java.io.DataInput
        int skipBytes(int n) { return 0; }

        //override from java.io.DataOutput
        void write(int b) { }

        //override from java.io.DataOutput
        void write(byte [] b) { }

        //override from java.io.DataOutput
        void write(byte [] b, int off, int len) { }
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
        String readLine() { return null; }

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
        void writeBytes(String s) { }

        //override from java.io.DataOutput
        void writeChars(String s) { }

        //override from java.io.DataOutput
        void writeUTF(String str) { }
    }

    //public abstract class Reader implements Readable, Closeable
    @Independent
    class Reader$ {
        //@Independent[O]
        @NotNull
        static Reader nullReader() { return null; }

        //override from java.lang.Readable
        @AllowsInterrupt
        int read(/*@Independent[H] @NotNull[H]*/ CharBuffer target) { return 0; }
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
        @AllowsInterrupt long transferTo(/*@Independent[T]*/ Writer out) { return 0L; }
    }
    //public interface Serializable
    @ImmutableContainer(hc = true) @Independent class Serializable$ { }
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
        void write(String str) { }
        @AllowsInterrupt void write(String str, int off, int len) { }
        //override from java.lang.Appendable
        //@Independent[O]
        @AllowsInterrupt

        Writer append(/*@Immutable(hc=true)[T] @Independent[H] @NotModified[T]*/ @NotNull CharSequence csq) {
            return null;
        }

        //override from java.lang.Appendable
        //@Independent[O]
        @AllowsInterrupt

        Writer append(
            /*@Immutable(hc=true)[T] @Independent[H] @NotModified[T]*/ @NotNull CharSequence csq,
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
    }
}
