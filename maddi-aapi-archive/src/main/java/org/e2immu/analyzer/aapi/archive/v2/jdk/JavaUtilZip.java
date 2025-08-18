package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;

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
        void write(/*@Independent[H]*/ byte [] b, int off, int len) { }

        //override from java.util.zip.DeflaterOutputStream
        void finish() { }

        //override from java.io.Closeable, java.io.FilterOutputStream, java.io.OutputStream, java.lang.AutoCloseable, java.util.zip.DeflaterOutputStream
        //@AllowsInterrupt[H]
        void close() { }
    }
}
