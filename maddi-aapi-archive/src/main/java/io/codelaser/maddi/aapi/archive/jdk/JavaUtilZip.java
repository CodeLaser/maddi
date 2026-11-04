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
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;

import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;

public class JavaUtilZip {
    public static final String PACKAGE_NAME = "java.util.zip";
    //public class ZipOutputStream extends DeflaterOutputStream implements ZipConstants
    class ZipOutputStream$ {
        static final int STORED = 0;
        static final int DEFLATED = 0;
        ZipOutputStream$(/*@NotNull[H]*/ OutputStream out) { }
        ZipOutputStream$(OutputStream out, Charset charset) { }
        @Modified void setComment(String comment) { }
        @Modified void setMethod(int method) { }
        @Modified void setLevel(int level) { }
        @Modified void putNextEntry(ZipEntry e) { }
        @Modified void closeEntry() { }
        //override from java.io.FilterOutputStream, java.io.OutputStream, java.util.zip.DeflaterOutputStream
        //@AllowsInterrupt[H]
        @Modified void write(/*@Independent[H]*/ @NotModified byte [] b, int off, int len) { }

        //override from java.util.zip.DeflaterOutputStream
        void finish() { }

        //override from java.io.Closeable, java.io.FilterOutputStream, java.io.OutputStream, java.lang.AutoCloseable, java.util.zip.DeflaterOutputStream
        //@AllowsInterrupt[H]
        @Modified void close() { }
    }

    //public java.util.zip.Adler32 (shadow created for its nullability, generated)
    class Adler32$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void update(@NotNull ByteBuffer p0) { }
        @Modified void update(@NotNull byte[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void reset() { }
        @Modified void update(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified long getValue() { return 0; }
    }

    //public java.util.zip.CRC32 (shadow created for its nullability, generated)
    class CRC32$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void update(@NotNull ByteBuffer p0) { }
        @Modified void update(@NotNull byte[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void reset() { }
        @Modified void update(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified long getValue() { return 0; }
    }

    //public java.util.zip.Checksum (shadow created for its nullability, generated)
    class Checksum$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void update(@NotNull ByteBuffer p0) { }
        @Modified void update(@NotNull byte[] p0) { }
        @Modified void update(@NotNull byte[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void reset() { }
        @Modified void update(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified long getValue() { return 0; }
    }

    //public java.util.zip.DataFormatException (shadow created for its nullability, generated)
    class DataFormatException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DataFormatException$(@Nullable String p0) { }
    }

    //public java.util.zip.Deflater (shadow created for its nullability, generated)
    class Deflater$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int deflate(@NotNull ByteBuffer p0) { return 0; }
        int deflate(@NotNull ByteBuffer p0, int p1) { return 0; }
        int deflate(@NotNull byte[] p0) { return 0; }
        int deflate(@NotNull byte[] p0, int p1, int p2) { return 0; }
        int deflate(@NotNull byte[] p0, int p1, int p2, int p3) { return 0; }
        @Modified void setDictionary(@NotNull ByteBuffer p0) { }
        @Modified void setDictionary(@NotNull byte[] p0) { }
        @Modified void setDictionary(@NotNull byte[] p0, int p1, int p2) { }
        @Modified void setInput(@NotNull ByteBuffer p0) { }
        @Modified void setInput(@NotNull byte[] p0) { }
        @Modified void setInput(@NotNull byte[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void close() { }
        @Modified void reset() { }
        @Modified void setLevel(int p0) { }
        @Modified void setStrategy(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int getAdler() { return 0; }
        @NotModified long getBytesRead() { return 0; }
        @NotModified long getBytesWritten() { return 0; }
        @NotModified int getTotalIn() { return 0; }
        @NotModified int getTotalOut() { return 0; }
    }

    //public java.util.zip.DeflaterOutputStream (shadow created for its nullability, generated)
    class DeflaterOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DeflaterOutputStream$(@NotNull OutputStream p0) { }
        DeflaterOutputStream$(@NotNull OutputStream p0, @NotNull Deflater p1) { }
        DeflaterOutputStream$(@NotNull OutputStream p0, @NotNull Deflater p1, boolean p2) { }
        DeflaterOutputStream$(@NotNull OutputStream p0, @NotNull Deflater p1, int p2) { }
        DeflaterOutputStream$(@NotNull OutputStream p0, @NotNull Deflater p1, int p2, boolean p3) { }
        DeflaterOutputStream$(@NotNull OutputStream p0, boolean p1) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.util.zip.GZIPInputStream (shadow created for its nullability, generated)
    class GZIPInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        GZIPInputStream$(@NotNull InputStream p0) { }
        GZIPInputStream$(@NotNull InputStream p0, int p1) { }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.util.zip.GZIPOutputStream (shadow created for its nullability, generated)
    class GZIPOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        GZIPOutputStream$(@NotNull OutputStream p0) { }
        GZIPOutputStream$(@NotNull OutputStream p0, boolean p1) { }
        GZIPOutputStream$(@NotNull OutputStream p0, int p1) { }
        GZIPOutputStream$(@NotNull OutputStream p0, int p1, boolean p2) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.util.zip.Inflater (shadow created for its nullability, generated)
    class Inflater$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        int inflate(@NotNull ByteBuffer p0) { return 0; }
        int inflate(@NotNull byte[] p0) { return 0; }
        int inflate(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified void setDictionary(@NotNull ByteBuffer p0) { }
        @Modified void setDictionary(@NotNull byte[] p0) { }
        @Modified void setDictionary(@NotNull byte[] p0, int p1, int p2) { }
        @Modified void setInput(@NotNull ByteBuffer p0) { }
        @Modified void setInput(@NotNull byte[] p0) { }
        @Modified void setInput(@NotNull byte[] p0, int p1, int p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void close() { }
        @Modified void reset() { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int getAdler() { return 0; }
        @NotModified long getBytesRead() { return 0; }
        @NotModified long getBytesWritten() { return 0; }
        @NotModified int getRemaining() { return 0; }
        @NotModified int getTotalIn() { return 0; }
        @NotModified int getTotalOut() { return 0; }
    }

    //public java.util.zip.InflaterInputStream (shadow created for its nullability, generated)
    class InflaterInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InflaterInputStream$(@NotNull InputStream p0) { }
        InflaterInputStream$(@NotNull InputStream p0, @NotNull Inflater p1) { }
        InflaterInputStream$(@NotNull InputStream p0, @NotNull Inflater p1, int p2) { }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
    }

    //public java.util.zip.InflaterOutputStream (shadow created for its nullability, generated)
    class InflaterOutputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InflaterOutputStream$(@NotNull OutputStream p0) { }
        InflaterOutputStream$(@NotNull OutputStream p0, @NotNull Inflater p1) { }
        InflaterOutputStream$(@NotNull OutputStream p0, @NotNull Inflater p1, int p2) { }
        @Modified void write(@NotNull byte[] p0, int p1, int p2) { }
    }

    //public java.util.zip.ZipEntry (shadow created for its nullability, generated)
    class ZipEntry$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipEntry$(@NotNull String p0) { }
        ZipEntry$(@NotNull ZipEntry p0) { }
        @NotModified @Nullable String getComment() { return null; }
        @NotModified @Nullable FileTime getCreationTime() { return null; }
        @NotModified @Nullable byte[] getExtra() { return null; }
        @NotModified @Nullable FileTime getLastAccessTime() { return null; }
        @NotModified @Nullable FileTime getLastModifiedTime() { return null; }
        @Modified void setComment(@Nullable String p0) { }
        @Modified ZipEntry setCreationTime(@NotNull FileTime p0) { return null; }
        @Modified void setExtra(@Nullable byte[] p0) { }
        @Modified ZipEntry setLastAccessTime(@NotNull FileTime p0) { return null; }
        @Modified ZipEntry setLastModifiedTime(@NotNull FileTime p0) { return null; }
        @Modified void setTimeLocal(@NotNull LocalDateTime p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setCompressedSize(long p0) { }
        @Modified void setCrc(long p0) { }
        @Modified void setMethod(int p0) { }
        @Modified void setSize(long p0) { }
        @Modified void setTime(long p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified long getCompressedSize() { return 0; }
        @NotModified long getCrc() { return 0; }
        @NotModified int getMethod() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified long getSize() { return 0; }
        @NotModified long getTime() { return 0; }
        @NotModified LocalDateTime getTimeLocal() { return null; }
        @NotModified boolean isDirectory() { return false; }
    }

    //public java.util.zip.ZipException (shadow created for its nullability, generated)
    class ZipException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipException$(@Nullable String p0) { }
    }

    //public java.util.zip.ZipFile (shadow created for its nullability, generated)
    class ZipFile$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipFile$(@NotNull File p0) { }
        ZipFile$(@NotNull File p0, @NotNull Charset p1) { }
        ZipFile$(@NotNull File p0, int p1) { }
        ZipFile$(@NotNull File p0, int p1, @NotNull Charset p2) { }
        ZipFile$(@NotNull String p0) { }
        ZipFile$(@NotNull String p0, @NotNull Charset p1) { }
        @NotModified @Nullable String getComment() { return null; }
        @NotModified @Nullable ZipEntry getEntry(@NotNull String p0) { return null; }
        @NotModified @Nullable InputStream getInputStream(@NotNull ZipEntry p0) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getName() { return null; }
        @NotModified int size() { return 0; }
    }

    //public java.util.zip.ZipInputStream (shadow created for its nullability, generated)
    class ZipInputStream$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ZipInputStream$(@NotNull InputStream p0) { }
        ZipInputStream$(@NotNull InputStream p0, @NotNull Charset p1) { }
        ZipEntry createZipEntry(@NotNull String p0) { return null; }
        @Nullable ZipEntry getNextEntry() { return null; }
        @Modified int read(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified int readNBytes(@NotNull byte[] p0, int p1, int p2) { return 0; }
        @Modified long transferTo(@NotNull OutputStream p0) { return 0; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void closeEntry() { }
    }
}
