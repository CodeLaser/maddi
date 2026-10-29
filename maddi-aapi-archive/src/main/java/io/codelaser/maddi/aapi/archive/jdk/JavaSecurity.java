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
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;

import java.nio.ByteBuffer;
import java.security.*;
import javax.security.auth.Subject;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaSecurity {
    public static final String PACKAGE_NAME = "java.security";

    //public abstract class MessageDigest extends MessageDigestSpi
    @Independent
    class MessageDigest$ {
        static MessageDigest getInstance(@NotNull String algorithm) { return null; }
        static MessageDigest getInstance(@NotNull String algorithm, @NotNull String provider) { return null; }
        static MessageDigest getInstance(@NotNull String algorithm, @NotNull @NotModified Provider provider) { return null; }
        @Nullable Provider getProvider() { return null; }
        @Modified void update(byte input) { }
        @Modified void update(@NotNull @NotModified byte [] input, int offset, int len) { }
        @Modified void update(@NotNull @NotModified byte [] input) { }
        @Modified void update(@NotNull ByteBuffer input) { }
        @Modified byte [] digest() { return null; }
        @Modified int digest(@NotNull @NotModified byte [] buf, int offset, int len) { return 0; }
        @Modified byte [] digest(@NotNull @NotModified byte [] input) { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        static boolean isEqual(@Nullable @NotModified byte [] digesta, @Nullable @NotModified byte [] digestb) { return false; }
        @Modified void reset() { }
        @NotModified
        String getAlgorithm() { return null; }
        @NotModified
        int getDigestLength() { return 0; }
        //override from java.lang.Object, java.security.MessageDigestSpi
        //@Immutable(hc=true)[T] @Independent(hc=true)[H]
        protected Object clone() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        MessageDigest$(@NotNull java.lang.String p0) { }
    }

    //public interface Principal
    // read-only identity: getName()/implies(Subject) never modify a parameter
    @Container
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
        @Modified void setSeed(@NotModified byte [] seed) { }
        //override from java.util.Random
        @Modified void setSeed(long seed) { }

        //override from java.util.Random, java.util.random.RandomGenerator
        //frequency 1
        @Modified void nextBytes(/*@NotModified[H]*/ byte [] bytes) { }
        @Modified void nextBytes(byte [] bytes, SecureRandomParameters params) { }
        //@Independent[T]
        @NotModified
        static byte [] getSeed(int numBytes) { return null; }
        byte [] generateSeed(int numBytes) { return null; }
        static SecureRandom getInstanceStrong() { return null; }
        void reseed() { }
        void reseed(SecureRandomParameters params) { }
    }

    //public java.security.AsymmetricKey (shadow created for its nullability, generated)
    class AsymmetricKey$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.security.spec.AlgorithmParameterSpec getParams() { return null; }
    }

    //public java.security.CodeSource (shadow created for its nullability, generated)
    class CodeSource$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CodeSource$(@Nullable java.net.URL p0, @Nullable java.security.cert.Certificate[] p1) { }
        CodeSource$(@Nullable java.net.URL p0, @Nullable java.security.CodeSigner[] p1) { }
        @Nullable java.security.cert.Certificate[] getCertificates() { return null; }
        @Nullable java.security.CodeSigner[] getCodeSigners() { return null; }
        @Nullable java.net.URL getLocation() { return null; }
        boolean implies(@Nullable java.security.CodeSource p0) { return false; }
    }

    //public java.security.DomainLoadStoreParameter (shadow created for its nullability, generated)
    class DomainLoadStoreParameter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.security.KeyStore.ProtectionParameter getProtectionParameter() { return null; }
    }

    //public java.security.GeneralSecurityException (shadow created for its nullability, generated)
    class GeneralSecurityException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        GeneralSecurityException$(@Nullable java.lang.String p0) { }
        GeneralSecurityException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
        GeneralSecurityException$(@Nullable java.lang.Throwable p0) { }
    }

    //public java.security.Key (shadow created for its nullability, generated)
    class Key$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable byte[] getEncoded() { return null; }
        @Nullable java.lang.String getFormat() { return null; }
    }

    //public java.security.KeyStore (shadow created for its nullability, generated)
    class KeyStore$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        KeyStore$(@NotNull java.security.KeyStoreSpi p0, java.security.Provider p1, @NotNull java.lang.String p2) { }
        boolean containsAlias(@NotNull java.lang.String p0) { return false; }
        @Modified void deleteEntry(@NotNull java.lang.String p0) { }
        boolean entryInstanceOf(@NotNull java.lang.String p0, @NotNull java.lang.Class<? extends java.security.KeyStore.Entry> p1) { return false; }
        java.util.Set<java.security.KeyStore.Entry.Attribute> getAttributes(@NotNull java.lang.String p0) { return null; }
        @Nullable java.security.cert.Certificate getCertificate(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.String getCertificateAlias(@NotNull java.security.cert.Certificate p0) { return null; }
        @Nullable java.security.cert.Certificate[] getCertificateChain(@NotNull java.lang.String p0) { return null; }
        @Nullable java.util.Date getCreationDate(@NotNull java.lang.String p0) { return null; }
        @Nullable java.time.Instant getCreationInstant(@NotNull java.lang.String p0) { return null; }
        @Nullable java.security.KeyStore.Entry getEntry(@NotNull java.lang.String p0, @Nullable java.security.KeyStore.ProtectionParameter p1) { return null; }
        static java.security.KeyStore getInstance(@NotNull java.io.File p0, @Nullable java.security.KeyStore.LoadStoreParameter p1) { return null; }
        static java.security.KeyStore getInstance(@NotNull java.io.File p0, @Nullable char[] p1) { return null; }
        static java.security.KeyStore getInstance(@NotNull java.lang.String p0) { return null; }
        static java.security.KeyStore getInstance(@NotNull java.lang.String p0, @NotNull java.security.Provider p1) { return null; }
        static java.security.KeyStore getInstance(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { return null; }
        @Nullable java.security.Key getKey(@NotNull java.lang.String p0, @Nullable char[] p1) { return null; }
        boolean isCertificateEntry(@NotNull java.lang.String p0) { return false; }
        boolean isKeyEntry(@NotNull java.lang.String p0) { return false; }
        @Modified void load(@Nullable java.io.InputStream p0, @Nullable char[] p1) { }
        @Modified void load(@Nullable java.security.KeyStore.LoadStoreParameter p0) { }
        @Modified void setCertificateEntry(@NotNull java.lang.String p0, @NotNull java.security.cert.Certificate p1) { }
        @Modified void setEntry(@NotNull java.lang.String p0, @NotNull java.security.KeyStore.Entry p1, @Nullable java.security.KeyStore.ProtectionParameter p2) { }
        @Modified void setKeyEntry(@NotNull java.lang.String p0, @NotNull java.security.Key p1, @Nullable char[] p2, @Nullable java.security.cert.Certificate[] p3) { }
        @Modified void setKeyEntry(@NotNull java.lang.String p0, @NotNull byte[] p1, @Nullable java.security.cert.Certificate[] p2) { }
        void store(@Nullable java.security.KeyStore.LoadStoreParameter p0) { }
        void store(@NotNull java.io.OutputStream p0, @Nullable char[] p1) { }

        //public java.security.KeyStore.Builder (shadow created for its nullability, generated)
        class Builder {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            java.security.KeyStore.ProtectionParameter getProtectionParameter(@NotNull java.lang.String p0) { return null; }
            static java.security.KeyStore.Builder newInstance(@NotNull java.io.File p0, @NotNull java.security.KeyStore.ProtectionParameter p1) { return null; }
            static java.security.KeyStore.Builder newInstance(@NotNull java.security.KeyStore p0, @NotNull java.security.KeyStore.ProtectionParameter p1) { return null; }
            static java.security.KeyStore.Builder newInstance(@NotNull java.lang.String p0, @Nullable java.security.Provider p1, @NotNull java.io.File p2, @NotNull java.security.KeyStore.ProtectionParameter p3) { return null; }
            static java.security.KeyStore.Builder newInstance(@NotNull java.lang.String p0, @Nullable java.security.Provider p1, @NotNull java.security.KeyStore.ProtectionParameter p2) { return null; }
        }
        //public java.security.KeyStore.CallbackHandlerProtection (shadow created for its nullability, generated)
        class CallbackHandlerProtection {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            CallbackHandlerProtection(@NotNull javax.security.auth.callback.CallbackHandler p0) { }
        }
        //public java.security.KeyStore.LoadStoreParameter (shadow created for its nullability, generated)
        class LoadStoreParameter {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable java.security.KeyStore.ProtectionParameter getProtectionParameter() { return null; }
        }
        //public java.security.KeyStore.PasswordProtection (shadow created for its nullability, generated)
        class PasswordProtection {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            PasswordProtection(@Nullable char[] p0) { }
            PasswordProtection(@Nullable char[] p0, @NotNull java.lang.String p1, @Nullable java.security.spec.AlgorithmParameterSpec p2) { }
            @Nullable char[] getPassword() { return null; }
            @Nullable java.lang.String getProtectionAlgorithm() { return null; }
            @Nullable java.security.spec.AlgorithmParameterSpec getProtectionParameters() { return null; }
        }
        //public java.security.KeyStore.PrivateKeyEntry (shadow created for its nullability, generated)
        class PrivateKeyEntry {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            PrivateKeyEntry(@NotNull java.security.PrivateKey p0, @NotNull java.security.cert.Certificate[] p1) { }
            PrivateKeyEntry(@NotNull java.security.PrivateKey p0, @NotNull java.security.cert.Certificate[] p1, @NotNull java.util.Set<java.security.KeyStore.Entry.Attribute> p2) { }
        }
        //public java.security.KeyStore.SecretKeyEntry (shadow created for its nullability, generated)
        class SecretKeyEntry {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            SecretKeyEntry(@NotNull javax.crypto.SecretKey p0) { }
            SecretKeyEntry(@NotNull javax.crypto.SecretKey p0, @NotNull java.util.Set<java.security.KeyStore.Entry.Attribute> p1) { }
        }
        //public java.security.KeyStore.TrustedCertificateEntry (shadow created for its nullability, generated)
        class TrustedCertificateEntry {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            TrustedCertificateEntry(@NotNull java.security.cert.Certificate p0) { }
            TrustedCertificateEntry(@NotNull java.security.cert.Certificate p0, @NotNull java.util.Set<java.security.KeyStore.Entry.Attribute> p1) { }
        }
    }

    //public java.security.KeyStoreSpi (shadow created for its nullability, generated)
    class KeyStoreSpi$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.time.Instant engineGetCreationInstant(java.lang.String p0) { return null; }
        @Nullable java.security.KeyStore.Entry engineGetEntry(java.lang.String p0, java.security.KeyStore.ProtectionParameter p1) { return null; }
    }

    //public java.security.PEM (shadow created for its nullability, generated)
    class PEM$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable byte[] leadingData() { return null; }
    }

    //public java.security.Permission (shadow created for its nullability, generated)
    class Permission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.security.PermissionCollection newPermissionCollection() { return null; }
    }

    //public java.security.Policy (shadow created for its nullability, generated)
    class Policy$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.security.Policy.Parameters getParameters() { return null; }
        @Nullable java.security.Provider getProvider() { return null; }
        @Nullable java.lang.String getType() { return null; }
    }

    //public java.security.ProtectionDomain (shadow created for its nullability, generated)
    class ProtectionDomain$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ProtectionDomain$(@Nullable java.security.CodeSource p0, @Nullable java.security.PermissionCollection p1) { }
        ProtectionDomain$(@Nullable java.security.CodeSource p0, @Nullable java.security.PermissionCollection p1, @Nullable java.lang.ClassLoader p2, @Nullable java.security.Principal[] p3) { }
        @Nullable java.lang.ClassLoader getClassLoader() { return null; }
        @Nullable java.security.CodeSource getCodeSource() { return null; }
        @Nullable java.security.PermissionCollection getPermissions() { return null; }
        boolean implies(@NotNull java.security.Permission p0) { return false; }
    }

    //public java.security.Provider (shadow created for its nullability, generated)
    class Provider$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified @Nullable java.lang.Object computeIfPresent(java.lang.Object p0, java.util.function.BiFunction<? super java.lang.Object, ? super java.lang.Object, ? extends java.lang.Object> p1) { return null; }
        @Nullable java.lang.Object get(java.lang.Object p0) { return null; }
        java.lang.Object getOrDefault(java.lang.Object p0, @Nullable java.lang.Object p1) { return null; }
        @Nullable java.lang.String getProperty(java.lang.String p0) { return null; }
        @Nullable java.security.Provider.Service getService(java.lang.String p0, java.lang.String p1) { return null; }
        @Modified @Nullable java.lang.Object put(java.lang.Object p0, java.lang.Object p1) { return null; }
        @Modified @Nullable java.lang.Object putIfAbsent(java.lang.Object p0, java.lang.Object p1) { return null; }
        @Modified @Nullable java.lang.Object remove(java.lang.Object p0) { return null; }
        @Modified @Nullable java.lang.Object replace(java.lang.Object p0, java.lang.Object p1) { return null; }

        //public java.security.Provider.Service (shadow created for its nullability, generated)
        class Service {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            Service(java.security.Provider p0, java.lang.String p1, java.lang.String p2, java.lang.String p3, @Nullable java.util.List<java.lang.String> p4, @Nullable java.util.Map<java.lang.String, java.lang.String> p5) { }
            @Nullable java.lang.String getAttribute(java.lang.String p0) { return null; }
            java.lang.Object newInstance(@Nullable java.lang.Object p0) { return null; }
            boolean supportsParameter(@Nullable java.lang.Object p0) { return false; }
        }
    }

    //public java.security.SecureClassLoader (shadow created for its nullability, generated)
    class SecureClassLoader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SecureClassLoader$(@Nullable java.lang.ClassLoader p0) { }
        SecureClassLoader$(@Nullable java.lang.String p0, @Nullable java.lang.ClassLoader p1) { }
        java.lang.Class<?> defineClass(@NotNull java.lang.String p0, @NotNull java.nio.ByteBuffer p1, @Nullable java.security.CodeSource p2) { return null; }
        java.lang.Class<?> defineClass(@NotNull java.lang.String p0, @NotNull byte[] p1, int p2, int p3, @Nullable java.security.CodeSource p4) { return null; }
        java.security.PermissionCollection getPermissions(@NotNull java.security.CodeSource p0) { return null; }
    }

    //public java.security.SecureRandomSpi (shadow created for its nullability, generated)
    class SecureRandomSpi$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.security.SecureRandomParameters engineGetParameters() { return null; }
    }

    //public java.security.Security (shadow created for its nullability, generated)
    class Security$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable static java.lang.String getAlgorithmProperty(java.lang.String p0, java.lang.String p1) { return null; }
        @Nullable static java.security.Provider[] getProviders(java.util.Map<java.lang.String, java.lang.String> p0) { return null; }
    }

    //public java.security.UnresolvedPermission (shadow created for its nullability, generated)
    class UnresolvedPermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.security.cert.Certificate[] getUnresolvedCerts() { return null; }
    }

    //public java.security.DrbgParameters (shadow created to hold nested types, generated)
    class DrbgParameters$ {
        //public java.security.DrbgParameters.Instantiation (shadow created for its nullability, generated)
        class Instantiation {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable byte[] getPersonalizationString() { return null; }
        }
        //public java.security.DrbgParameters.NextBytes (shadow created for its nullability, generated)
        class NextBytes {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable byte[] getAdditionalInput() { return null; }
        }
        //public java.security.DrbgParameters.Reseed (shadow created for its nullability, generated)
        class Reseed {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable byte[] getAdditionalInput() { return null; }
        }
    }
}
