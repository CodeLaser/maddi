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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;

public class JavaNioCharset {
    public static final String PACKAGE_NAME = "java.nio.charset";
    //public abstract class Charset implements Comparable<Charset>
    class Charset$ {
        static boolean isSupported(String charsetName) { return false; }
        static Charset forName(String charsetName) { return null; }
        static Charset forName(String charsetName, Charset fallback) { return null; }
        //@Independent[T]
        static SortedMap<String, Charset> availableCharsets() { return null; }
        static Charset defaultCharset() { return null; }
        //frequency 4
        String name() { return null; }
        Set<String> aliases() { return null; }
        String displayName() { return null; }
        boolean isRegistered() { return false; }
        String displayName(Locale locale) { return null; }
        boolean contains(Charset charset) { return false; }
        CharsetDecoder newDecoder() { return null; }
        CharsetEncoder newEncoder() { return null; }
        boolean canEncode() { return false; }
        CharBuffer decode(ByteBuffer bb) { return null; }
        ByteBuffer encode(CharBuffer cb) { return null; }
        ByteBuffer encode(String str) { return null; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ Charset that) { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object ob) { return false; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }
}
