package org.e2immu.analyzer.aapi.archive.v2.jdk;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.Independent;
import org.e2immu.annotation.NotModified;

import java.nio.ByteBuffer;
import java.security.*;
import javax.security.auth.Subject;

public class JavaSecurity {
    public static final String PACKAGE_NAME = "java.security";

    //public abstract class MessageDigest extends MessageDigestSpi
    @Independent
    class MessageDigest$ {
        static MessageDigest getInstance(String algorithm) { return null; }
        static MessageDigest getInstance(String algorithm, String provider) { return null; }
        static MessageDigest getInstance(String algorithm, @NotModified Provider provider) { return null; }
        Provider getProvider() { return null; }
        void update(byte input) { }
        void update(@NotModified byte [] input, int offset, int len) { }
        void update(@NotModified byte [] input) { }
        void update(ByteBuffer input) { }
        byte [] digest() { return null; }
        int digest(@NotModified byte [] buf, int offset, int len) { return 0; }
        byte [] digest(@NotModified byte [] input) { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        static boolean isEqual(@NotModified byte [] digesta, @NotModified byte [] digestb) { return false; }
        void reset() { }
        @NotModified
        String getAlgorithm() { return null; }
        @NotModified
        int getDigestLength() { return 0; }
        //override from java.lang.Object, java.security.MessageDigestSpi
        //@Immutable(hc=true)[T] @Independent(hc=true)[H]
        protected Object clone() { return null; }
    }

    //public interface Principal
    class Principal$ {
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object object) { return false; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //frequency 1
        String getName() { return null; }
        boolean implies(Subject subject) { return false; }
    }

    //public class SecureRandom extends Random
    @Independent
    class SecureRandom$ {
        SecureRandom$() { }
        SecureRandom$(@NotModified byte [] seed) { }
        static SecureRandom getInstance(String algorithm) { return null; }
        static SecureRandom getInstance(String algorithm, String provider) { return null; }
        static SecureRandom getInstance(String algorithm, Provider provider) { return null; }
        static SecureRandom getInstance(String algorithm, SecureRandomParameters params) { return null; }
        static SecureRandom getInstance(String algorithm, SecureRandomParameters params, String provider) { return null; }

        static SecureRandom getInstance(String algorithm, SecureRandomParameters params, Provider provider) { return null; }
        @NotModified
        Provider getProvider() { return null; }
        @NotModified
        String getAlgorithm() { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        @NotModified
        SecureRandomParameters getParameters() { return null; }
        void setSeed(@NotModified byte [] seed) { }
        //override from java.util.Random
        void setSeed(long seed) { }

        //override from java.util.Random, java.util.random.RandomGenerator
        //frequency 1
        void nextBytes(/*@NotModified[H]*/ byte [] bytes) { }
        void nextBytes(byte [] bytes, SecureRandomParameters params) { }
        //@Independent[T]
        @NotModified
        static byte [] getSeed(int numBytes) { return null; }
        byte [] generateSeed(int numBytes) { return null; }
        static SecureRandom getInstanceStrong() { return null; }
        void reseed() { }
        void reseed(SecureRandomParameters params) { }
    }
}
