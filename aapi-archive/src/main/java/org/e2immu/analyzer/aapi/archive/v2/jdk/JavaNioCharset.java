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
