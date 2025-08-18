package org.e2immu.analyzer.aapi.archive.jdk;
import org.e2immu.annotation.Modified;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
public class JavaNioChannels {
    public static final String PACKAGE_NAME = "java.nio.channels";

    class Channels$ {
        static InputStream newInputStream(ReadableByteChannel ch) { return null; }
        static OutputStream newOutputStream(WritableByteChannel ch) { return null; }
        static InputStream newInputStream(AsynchronousByteChannel ch) { return null; }
        static OutputStream newOutputStream(AsynchronousByteChannel ch) { return null; }
        static ReadableByteChannel newChannel(InputStream in) { return null; }
        static WritableByteChannel newChannel(OutputStream out) { return null; }
        static Reader newReader(ReadableByteChannel ch, CharsetDecoder dec, int minBufferCap) { return null; }
        static Reader newReader(ReadableByteChannel ch, String csName) { return null; }
        static Reader newReader(ReadableByteChannel ch, Charset charset) { return null; }
        static Writer newWriter(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) { return null; }
        static Writer newWriter(WritableByteChannel ch, String csName) { return null; }
        static Writer newWriter(WritableByteChannel ch, Charset charset) { return null; }
    }

    class FileChannel$ {
        class MapMode {
            static final FileChannel.MapMode READ_ONLY = null;
            static final FileChannel.MapMode READ_WRITE = null;
            static final FileChannel.MapMode PRIVATE = null;
            public String toString() { return null; }
        }

        static FileChannel open(Path path, Set<? extends OpenOption> options, FileAttribute<?> ... attrs) {
            return null;
        }

        static FileChannel open(Path path, OpenOption... options) { return null; }
        @Modified
        int read(@Modified ByteBuffer byteBuffer) { return 0; }
        @Modified
        long read(@Modified ByteBuffer[] byteBuffer, int i, int i1) { return 0L; }
        @Modified
        long read(@Modified ByteBuffer[] dsts) { return 0L; }
        @Modified
        int write(@Modified ByteBuffer byteBuffer) { return 0; }
        @Modified
        long write(@Modified ByteBuffer[] byteBuffer, int i, int i1) { return 0L; }
        @Modified
        long write(@Modified ByteBuffer[] srcs) { return 0L; }

        long position() { return 0L; }
        FileChannel position(long l) { return null; }

        long size() { return 0L; }
        @Modified
        FileChannel truncate(long l) { return null; }
        @Modified
        void force(boolean b) { }

        @Modified
        long transferTo(long l, long l1, @Modified WritableByteChannel writableByteChannel) { return 0L; }
        @Modified
        long transferFrom(@Modified ReadableByteChannel readableByteChannel, long l, long l1) { return 0L; }

        @Modified
        int read(@Modified ByteBuffer byteBuffer, long l) { return 0; }
        @Modified
        int write(@Modified ByteBuffer byteBuffer, long l) { return 0; }

        // dependent!
        MappedByteBuffer map(FileChannel.MapMode mapMode, long l, long l1) { return null; }

        @Modified
        FileLock lock(long l, long l1, boolean b) { return null; }
        @Modified
        FileLock lock() { return null; }
        @Modified
        FileLock tryLock(long l, long l1, boolean b) { return null; }
        @Modified
        FileLock tryLock() { return null; }
    }
}
