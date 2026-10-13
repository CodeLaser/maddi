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
import io.codelaser.maddi.annotation.NotModified;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaUtilZip {
    public static final String PACKAGE_NAME = "java.util.zip";
    //public class ZipOutputStream extends DeflaterOutputStream implements ZipConstants
    class ZipOutputStream$ {
        static final int STORED = 0;
        static final int DEFLATED = 0;
        ZipOutputStream$(/*@NotNull[H]*/ OutputStream out) { }
        ZipOutputStream$(OutputStream out, Charset charset) { }
        void setComment(String comment) { }
        void setMethod(int method) { }
        void setLevel(int level) { }
        void putNextEntry(ZipEntry e) { }
        void closeEntry() { }
        //override from java.io.FilterOutputStream, java.io.OutputStream, java.util.zip.DeflaterOutputStream
        //@AllowsInterrupt[H]
        void write(/*@Independent[H]*/ @NotModified byte [] b, int off, int len) { }

        //override from java.util.zip.DeflaterOutputStream
        void finish() { }

        //override from java.io.Closeable, java.io.FilterOutputStream, java.io.OutputStream, java.lang.AutoCloseable, java.util.zip.DeflaterOutputStream
        //@AllowsInterrupt[H]
        void close() { }
    }

    //public java.util.zip.Adler32 (shadow created for its nullability, generated)
    class Adler32$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void update(@NotNull byte[] p0, int p1, int p2) { }
        void update(@NotNull java.nio.ByteBuffer p0) { }
    }

    //public java.util.zip.CRC32 (shadow created for its nullability, generated)
    class CRC32$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void update(@NotNull byte[] p0, int p1, int p2) { }
        void update(@NotNull java.nio.ByteBuffer p0) { }
    }

    //public java.util.zip.Checksum (shadow created for its nullability, generated)
    class Checksum$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void update(@NotNull byte[] p0) { }
        void update(@NotNull byte[] p0, int p1, int p2) { }
        void update(@NotNull java.nio.ByteBuffer p0) { }
    }

    //public java.util.zip.DataFormatException (shadow created for its nullability, generated)
    class DataFormatException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DataFormatException$(@Nullable java.lang.String p0) { }
    }

    //public java.util.zip.Deflater (shadow created for its nullability, generated)
    class Deflater$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int deflate(@NotNull byte[] p0) { return 0; }
        int deflate(@NotNull byte[] p0, int p1, int p2) { return 0; }
        int deflate(@NotNull byte[] p0, int p1, int p2, int p3) { return 0; }
        int deflate(@NotNull java.nio.ByteBuffer p0) { return 0; }
        int deflate(@NotNull java.nio.ByteBuffer p0, int p1) { return 0; }
        void setDictionary(@NotNull byte[] p0) { }
        void setDictionary(@NotNull byte[] p0, int p1, int p2) { }
        void setDictionary(@NotNull java.nio.ByteBuffer p0) { }
        void setInput(@NotNull byte[] p0) { }
        void setInput(@NotNull byte[] p0, int p1, int p2) { }
        void setInput(@NotNull java.nio.ByteBuffer p0) { }
    }

    //public java.util.zip.DeflaterOutputStream (shadow created for its nullability, generated)
    class DeflaterOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DeflaterOutputStream$(@NotNull java.io.OutputStream p0) { }
        DeflaterOutputStream$(@NotNull java.io.OutputStream p0, boolean p1) { }
        DeflaterOutputStream$(@NotNull java.io.OutputStream p0, @NotNull java.util.zip.Deflater p1) { }
        DeflaterOutputStream$(@NotNull java.io.OutputStream p0, @NotNull java.util.zip.Deflater p1, boolean p2) { }
        DeflaterOutputStream$(@NotNull java.io.OutputStream p0, @NotNull java.util.zip.Deflater p1, int p2) { }
        DeflaterOutputStream$(@NotNull java.io.OutputStream p0, @NotNull java.util.zip.Deflater p1, int p2, boolean p3) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.util.zip.GZIPInputStream (shadow created for its nullability, generated)
    class GZIPInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        GZIPInputStream$(@NotNull java.io.InputStream p0) { }
        GZIPInputStream$(@NotNull java.io.InputStream p0, int p1) { }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.util.zip.GZIPOutputStream (shadow created for its nullability, generated)
    class GZIPOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        GZIPOutputStream$(@NotNull java.io.OutputStream p0) { }
        GZIPOutputStream$(@NotNull java.io.OutputStream p0, boolean p1) { }
        GZIPOutputStream$(@NotNull java.io.OutputStream p0, int p1) { }
        GZIPOutputStream$(@NotNull java.io.OutputStream p0, int p1, boolean p2) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.util.zip.Inflater (shadow created for its nullability, generated)
    class Inflater$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int inflate(@NotNull byte[] p0) { return 0; }
        int inflate(@NotNull byte[] p0, int p1, int p2) { return 0; }
        int inflate(@NotNull java.nio.ByteBuffer p0) { return 0; }
        void setDictionary(@NotNull byte[] p0) { }
        void setDictionary(@NotNull byte[] p0, int p1, int p2) { }
        void setDictionary(@NotNull java.nio.ByteBuffer p0) { }
        void setInput(@NotNull byte[] p0) { }
        void setInput(@NotNull byte[] p0, int p1, int p2) { }
        void setInput(@NotNull java.nio.ByteBuffer p0) { }
    }

    //public java.util.zip.InflaterInputStream (shadow created for its nullability, generated)
    class InflaterInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InflaterInputStream$(@NotNull java.io.InputStream p0) { }
        InflaterInputStream$(@NotNull java.io.InputStream p0, @NotNull java.util.zip.Inflater p1) { }
        InflaterInputStream$(@NotNull java.io.InputStream p0, @NotNull java.util.zip.Inflater p1, int p2) { }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.util.zip.InflaterOutputStream (shadow created for its nullability, generated)
    class InflaterOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InflaterOutputStream$(@NotNull java.io.OutputStream p0) { }
        InflaterOutputStream$(@NotNull java.io.OutputStream p0, @NotNull java.util.zip.Inflater p1) { }
        InflaterOutputStream$(@NotNull java.io.OutputStream p0, @NotNull java.util.zip.Inflater p1, int p2) { }
        void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.util.zip.ZipEntry (shadow created for its nullability, generated)
    class ZipEntry$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipEntry$(@NotNull java.lang.String p0) { }
        ZipEntry$(@NotNull java.util.zip.ZipEntry p0) { }
        @Nullable java.lang.String getComment() { return null; }
        @Nullable java.nio.file.attribute.FileTime getCreationTime() { return null; }
        @Nullable byte[] getExtra() { return null; }
        @Nullable java.nio.file.attribute.FileTime getLastAccessTime() { return null; }
        @Nullable java.nio.file.attribute.FileTime getLastModifiedTime() { return null; }
        void setComment(@Nullable java.lang.String p0) { }
        java.util.zip.ZipEntry setCreationTime(@NotNull java.nio.file.attribute.FileTime p0) { return null; }
        void setExtra(@Nullable byte[] p0) { }
        java.util.zip.ZipEntry setLastAccessTime(@NotNull java.nio.file.attribute.FileTime p0) { return null; }
        java.util.zip.ZipEntry setLastModifiedTime(@NotNull java.nio.file.attribute.FileTime p0) { return null; }
        void setTimeLocal(@NotNull java.time.LocalDateTime p0) { }
    }

    //public java.util.zip.ZipException (shadow created for its nullability, generated)
    class ZipException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipException$(@Nullable java.lang.String p0) { }
    }

    //public java.util.zip.ZipFile (shadow created for its nullability, generated)
    class ZipFile$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipFile$(@NotNull java.io.File p0) { }
        ZipFile$(@NotNull java.io.File p0, @NotNull java.nio.charset.Charset p1) { }
        ZipFile$(@NotNull java.io.File p0, int p1) { }
        ZipFile$(@NotNull java.io.File p0, int p1, @NotNull java.nio.charset.Charset p2) { }
        ZipFile$(@NotNull java.lang.String p0) { }
        ZipFile$(@NotNull java.lang.String p0, @NotNull java.nio.charset.Charset p1) { }
        @Nullable java.lang.String getComment() { return null; }
        @Nullable java.util.zip.ZipEntry getEntry(@NotNull java.lang.String p0) { return null; }
        @Nullable java.io.InputStream getInputStream(@NotNull java.util.zip.ZipEntry p0) { return null; }
    }

    //public java.util.zip.ZipInputStream (shadow created for its nullability, generated)
    class ZipInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipInputStream$(@NotNull java.io.InputStream p0) { }
        ZipInputStream$(@NotNull java.io.InputStream p0, @NotNull java.nio.charset.Charset p1) { }
        java.util.zip.ZipEntry createZipEntry(@NotNull java.lang.String p0) { return null; }
        @Nullable java.util.zip.ZipEntry getNextEntry() { return null; }
        int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        int readNBytes(@NotNull byte[] p0, int p1, int p2) { return 0; }
        long transferTo(@NotNull java.io.OutputStream p0) { return 0; }
    }
}
