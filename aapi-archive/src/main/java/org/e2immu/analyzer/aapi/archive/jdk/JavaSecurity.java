/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.analyzer.aapi.archive.jdk;

import org.e2immu.annotation.*;

import java.nio.ByteBuffer;
import java.security.*;

public class JavaSecurity {
    final static String PACKAGE_NAME = "java.security";

    @Independent(hc = true)
    interface PrivilegedAction$<T> {
    }

    @ImmutableContainer
    @Independent
    interface Guard$ {
        // non-modifying, throws security exception
        void checkGuard(Object object);
    }

    @Independent
    class MessageDigest$ {
        static MessageDigest getInstance(String algorithm) { return null; }
        static MessageDigest getInstance(String algorithm, String provider) { return null; }
        static MessageDigest getInstance(String algorithm, @NotModified Provider provider) { return null; }
        Provider getProvider() { return null; }
        @Modified
        void update(byte input) { }
        @Modified
        void update(@NotModified byte[] input, int offset, int len) { }
        @Modified
        void update(@NotModified byte[] input) { }
        @Modified
        void update(@Modified ByteBuffer input) { }
        @Modified
        byte[] digest() { return null; }
        @Modified
        int digest(@NotModified byte[] buf, int offset, int len) { return 0; }
        @Modified
        byte[] digest(@NotModified byte[] input) { return null; }
        static boolean isEqual(@NotModified byte[] digesta, @NotModified byte[] digestb) { return false; }
        @Modified
        void reset() { }
        String getAlgorithm() { return null; }
        int getDigestLength() { return 0; }
    }

    @Immutable(hc = true)
    interface SecureRandomParameters$ {}

    @Independent
    class SecureRandom$ {
        SecureRandom$() { }
        SecureRandom$(@NotModified byte[] seed) { }
        static SecureRandom getInstance(String algorithm) { return null; }
        static SecureRandom getInstance(String algorithm, String provider) { return null; }
        static SecureRandom getInstance(String algorithm, Provider provider) { return null; }
        static SecureRandom getInstance(String algorithm, SecureRandomParameters params) { return null; }

        static SecureRandom getInstance(String algorithm, SecureRandomParameters params, String provider) {
            return null;
        }

        static SecureRandom getInstance(String algorithm, SecureRandomParameters params, Provider provider) {
            return null;
        }

        Provider getProvider() { return null; }
        String getAlgorithm() { return null; }
        public String toString() { return null; }
        SecureRandomParameters getParameters() { return null; }
        @Modified
        void setSeed(@NotModified byte[] seed) { }
        @Modified
        void setSeed(long seed) { }
        @Modified
        void nextBytes(byte[] bytes) { }
        @Modified
        void nextBytes(byte[] bytes, SecureRandomParameters params) { }
        static byte[] getSeed(int numBytes) { return null; }
        byte[] generateSeed(int numBytes) { return null; }
        static SecureRandom getInstanceStrong() { return null; }
        @Modified
        void reseed() { }
        @Modified
        void reseed(SecureRandomParameters params) { }
    }

    static class AccessController$ {
        static <T> T doPrivileged(@Modified PrivilegedAction<T> action) { return null; }
        static <T> T doPrivileged(@Modified PrivilegedExceptionAction<T> action) { return null; }
    }
}
