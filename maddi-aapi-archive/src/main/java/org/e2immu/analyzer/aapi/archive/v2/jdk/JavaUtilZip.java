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
