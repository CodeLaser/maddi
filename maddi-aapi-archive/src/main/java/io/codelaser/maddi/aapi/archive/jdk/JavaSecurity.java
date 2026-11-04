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
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.crypto.SecretKey;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;

public class JavaSecurity {
    public static final String PACKAGE_NAME = "java.security";

    //public abstract class MessageDigest extends MessageDigestSpi
    @Independent
    class MessageDigest$ {
        static MessageDigest getInstance(@NotNull String algorithm) { return null; }
        static MessageDigest getInstance(@NotNull String algorithm, @NotNull String provider) { return null; }
        static MessageDigest getInstance(@NotNull String algorithm, @NotNull @NotModified Provider provider) { return null; }
        @NotModified @Nullable Provider getProvider() { return null; }
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
        MessageDigest$(@NotNull String p0) { }
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
        @NotModified String getName() { return null; }
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
        @NotModified @Nullable AlgorithmParameterSpec getParams() { return null; }
    }

    //public java.security.CodeSource (shadow created for its nullability, generated)
    class CodeSource$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CodeSource$(@Nullable URL p0, @Nullable Certificate[] p1) { }
        CodeSource$(@Nullable URL p0, @Nullable CodeSigner[] p1) { }
        @NotModified @Nullable Certificate[] getCertificates() { return null; }
        @NotModified @Nullable CodeSigner[] getCodeSigners() { return null; }
        @NotModified @Nullable URL getLocation() { return null; }
        boolean implies(@Nullable CodeSource p0) { return false; }
    }

    //public java.security.DomainLoadStoreParameter (shadow created for its nullability, generated)
    class DomainLoadStoreParameter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable KeyStore.ProtectionParameter getProtectionParameter() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified URI getConfiguration() { return null; }
        @NotModified Map<String, KeyStore.ProtectionParameter> getProtectionParams() { return null; }
    }

    //public java.security.GeneralSecurityException (shadow created for its nullability, generated)
    class GeneralSecurityException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        GeneralSecurityException$(@Nullable String p0) { }
        GeneralSecurityException$(@Nullable String p0, @Nullable Throwable p1) { }
        GeneralSecurityException$(@Nullable Throwable p0) { }
    }

    //public java.security.Key (shadow created for its nullability, generated)
    class Key$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable byte[] getEncoded() { return null; }
        @NotModified @Nullable String getFormat() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getAlgorithm() { return null; }
    }

    //public java.security.KeyStore (shadow created for its nullability, generated)
    class KeyStore$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        KeyStore$(@NotNull KeyStoreSpi p0, Provider p1, @NotNull String p2) { }
        @NotModified boolean containsAlias(@NotNull String p0) { return false; }
        @Modified void deleteEntry(@NotNull String p0) { }
        boolean entryInstanceOf(@NotNull String p0, @NotNull Class<? extends KeyStore.Entry> p1) { return false; }
        @NotModified Set<KeyStore.Entry.Attribute> getAttributes(@NotNull String p0) { return null; }
        @NotModified @Nullable Certificate getCertificate(@NotNull String p0) { return null; }
        @NotModified @Nullable String getCertificateAlias(@NotNull Certificate p0) { return null; }
        @NotModified @Nullable Certificate[] getCertificateChain(@NotNull String p0) { return null; }
        @NotModified @Nullable Date getCreationDate(@NotNull String p0) { return null; }
        @NotModified @Nullable Instant getCreationInstant(@NotNull String p0) { return null; }
        @NotModified @Nullable KeyStore.Entry getEntry(@NotNull String p0, @Nullable KeyStore.ProtectionParameter p1) { return null; }
        static KeyStore getInstance(@NotNull File p0, @Nullable KeyStore.LoadStoreParameter p1) { return null; }
        static KeyStore getInstance(@NotNull File p0, @Nullable char[] p1) { return null; }
        static KeyStore getInstance(@NotNull String p0) { return null; }
        static KeyStore getInstance(@NotNull String p0, @NotNull Provider p1) { return null; }
        static KeyStore getInstance(@NotNull String p0, @NotNull String p1) { return null; }
        @NotModified @Nullable Key getKey(@NotNull String p0, @Nullable char[] p1) { return null; }
        @NotModified boolean isCertificateEntry(@NotNull String p0) { return false; }
        @NotModified boolean isKeyEntry(@NotNull String p0) { return false; }
        @Modified void load(@Nullable InputStream p0, @Nullable char[] p1) { }
        @Modified void load(@Nullable KeyStore.LoadStoreParameter p0) { }
        @Modified void setCertificateEntry(@NotNull String p0, @NotNull Certificate p1) { }
        @Modified void setEntry(@NotNull String p0, @NotNull KeyStore.Entry p1, @Nullable KeyStore.ProtectionParameter p2) { }
        @Modified void setKeyEntry(@NotNull String p0, @NotNull Key p1, @Nullable char[] p2, @Nullable Certificate[] p3) { }
        @Modified void setKeyEntry(@NotNull String p0, @NotNull byte[] p1, @Nullable Certificate[] p2) { }
        void store(@Nullable KeyStore.LoadStoreParameter p0) { }
        void store(@NotNull OutputStream p0, @Nullable char[] p1) { }

        //public java.security.KeyStore.Builder (shadow created for its nullability, generated)
        class Builder {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @NotModified KeyStore.ProtectionParameter getProtectionParameter(@NotNull String p0) { return null; }
            static KeyStore.Builder newInstance(@NotNull File p0, @NotNull KeyStore.ProtectionParameter p1) { return null; }
            static KeyStore.Builder newInstance(@NotNull KeyStore p0, @NotNull KeyStore.ProtectionParameter p1) { return null; }
            static KeyStore.Builder newInstance(@NotNull String p0, @Nullable Provider p1, @NotNull File p2, @NotNull KeyStore.ProtectionParameter p3) { return null; }
            static KeyStore.Builder newInstance(@NotNull String p0, @Nullable Provider p1, @NotNull KeyStore.ProtectionParameter p2) { return null; }
        }
        //public java.security.KeyStore.CallbackHandlerProtection (shadow created for its nullability, generated)
        class CallbackHandlerProtection {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            CallbackHandlerProtection(@NotNull CallbackHandler p0) { }
        }
        //public java.security.KeyStore.LoadStoreParameter (shadow created for its nullability, generated)
        class LoadStoreParameter {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @NotModified @Nullable KeyStore.ProtectionParameter getProtectionParameter() { return null; }
        }
        //public java.security.KeyStore.PasswordProtection (shadow created for its nullability, generated)
        class PasswordProtection {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            PasswordProtection(@Nullable char[] p0) { }
            PasswordProtection(@Nullable char[] p0, @NotNull String p1, @Nullable AlgorithmParameterSpec p2) { }
            @NotModified @Nullable char[] getPassword() { return null; }
            @NotModified @Nullable String getProtectionAlgorithm() { return null; }
            @NotModified @Nullable AlgorithmParameterSpec getProtectionParameters() { return null; }
        }
        //public java.security.KeyStore.PrivateKeyEntry (shadow created for its nullability, generated)
        class PrivateKeyEntry {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            PrivateKeyEntry(@NotNull PrivateKey p0, @NotNull Certificate[] p1) { }
            PrivateKeyEntry(@NotNull PrivateKey p0, @NotNull Certificate[] p1, @NotNull Set<KeyStore.Entry.Attribute> p2) { }
        }
        //public java.security.KeyStore.SecretKeyEntry (shadow created for its nullability, generated)
        class SecretKeyEntry {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            SecretKeyEntry(@NotNull SecretKey p0) { }
            SecretKeyEntry(@NotNull SecretKey p0, @NotNull Set<KeyStore.Entry.Attribute> p1) { }
        }
        //public java.security.KeyStore.TrustedCertificateEntry (shadow created for its nullability, generated)
        class TrustedCertificateEntry {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            TrustedCertificateEntry(@NotNull Certificate p0) { }
            TrustedCertificateEntry(@NotNull Certificate p0, @NotNull Set<KeyStore.Entry.Attribute> p1) { }
        }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Provider getProvider() { return null; }
        @NotModified String getType() { return null; }
        @NotModified int size() { return 0; }
    }

    //public java.security.KeyStoreSpi (shadow created for its nullability, generated)
    class KeyStoreSpi$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable Instant engineGetCreationInstant(String p0) { return null; }
        @Nullable KeyStore.Entry engineGetEntry(String p0, KeyStore.ProtectionParameter p1) { return null; }
    }

    //public java.security.PEM (shadow created for its nullability, generated)
    class PEM$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable byte[] leadingData() { return null; }
    }

    //public java.security.Permission (shadow created for its nullability, generated)
    class Permission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable PermissionCollection newPermissionCollection() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getActions() { return null; }
        @NotModified String getName() { return null; }
    }

    //public java.security.Policy (shadow created for its nullability, generated)
    class Policy$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable Policy.Parameters getParameters() { return null; }
        @NotModified @Nullable Provider getProvider() { return null; }
        @NotModified @Nullable String getType() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified PermissionCollection getPermissions(CodeSource p0) { return null; }
        @NotModified PermissionCollection getPermissions(ProtectionDomain p0) { return null; }
    }

    //public java.security.ProtectionDomain (shadow created for its nullability, generated)
    class ProtectionDomain$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ProtectionDomain$(@Nullable CodeSource p0, @Nullable PermissionCollection p1) { }
        ProtectionDomain$(@Nullable CodeSource p0, @Nullable PermissionCollection p1, @Nullable ClassLoader p2, @Nullable Principal[] p3) { }
        @NotModified @Nullable ClassLoader getClassLoader() { return null; }
        @NotModified @Nullable CodeSource getCodeSource() { return null; }
        @NotModified @Nullable PermissionCollection getPermissions() { return null; }
        boolean implies(@NotNull Permission p0) { return false; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Principal[] getPrincipals() { return null; }
    }

    //public java.security.Provider (shadow created for its nullability, generated)
    class Provider$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified @Nullable Object computeIfPresent(Object p0, BiFunction<? super Object, ? super Object, ? extends Object> p1) { return null; }
        @Nullable Object get(Object p0) { return null; }
        Object getOrDefault(Object p0, @Nullable Object p1) { return null; }
        @Nullable String getProperty(String p0) { return null; }
        @NotModified @Nullable Provider.Service getService(String p0, String p1) { return null; }
        @Modified @Nullable Object put(Object p0, Object p1) { return null; }
        @Modified @Nullable Object putIfAbsent(Object p0, Object p1) { return null; }
        @Modified @Nullable Object remove(Object p0) { return null; }
        @Modified @Nullable Object replace(Object p0, Object p1) { return null; }

        //public java.security.Provider.Service (shadow created for its nullability, generated)
        class Service {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            Service(Provider p0, String p1, String p2, String p3, @Nullable List<String> p4, @Nullable Map<String, String> p5) { }
            @NotModified @Nullable String getAttribute(String p0) { return null; }
            Object newInstance(@Nullable Object p0) { return null; }
            boolean supportsParameter(@Nullable Object p0) { return false; }
        }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getInfo() { return null; }
        @NotModified String getName() { return null; }
        @NotModified Set<Provider.Service> getServices() { return null; }
        @NotModified double getVersion() { return 0; }
        @NotModified String getVersionStr() { return null; }
        @NotModified boolean isConfigured() { return false; }
    }

    //public java.security.SecureClassLoader (shadow created for its nullability, generated)
    class SecureClassLoader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SecureClassLoader$(@Nullable ClassLoader p0) { }
        SecureClassLoader$(@Nullable String p0, @Nullable ClassLoader p1) { }
        Class<?> defineClass(@NotNull String p0, @NotNull ByteBuffer p1, @Nullable CodeSource p2) { return null; }
        Class<?> defineClass(@NotNull String p0, @NotNull byte[] p1, int p2, int p3, @Nullable CodeSource p4) { return null; }
        PermissionCollection getPermissions(@NotNull CodeSource p0) { return null; }
    }

    //public java.security.SecureRandomSpi (shadow created for its nullability, generated)
    class SecureRandomSpi$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable SecureRandomParameters engineGetParameters() { return null; }
    }

    //public java.security.Security (shadow created for its nullability, generated)
    class Security$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable static String getAlgorithmProperty(String p0, String p1) { return null; }
        @Nullable static Provider[] getProviders(Map<String, String> p0) { return null; }
    }

    //public java.security.UnresolvedPermission (shadow created for its nullability, generated)
    class UnresolvedPermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable Certificate[] getUnresolvedCerts() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getActions() { return null; }
        @NotModified String getUnresolvedActions() { return null; }
        @NotModified String getUnresolvedName() { return null; }
        @NotModified String getUnresolvedType() { return null; }
    }

    //public java.security.DrbgParameters (shadow created to hold nested types, generated)
    class DrbgParameters$ {
        //public java.security.DrbgParameters.Instantiation (shadow created for its nullability, generated)
        class Instantiation {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @NotModified @Nullable byte[] getPersonalizationString() { return null; }
        }
        //public java.security.DrbgParameters.NextBytes (shadow created for its nullability, generated)
        class NextBytes {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @NotModified @Nullable byte[] getAdditionalInput() { return null; }
        }
        //public java.security.DrbgParameters.Reseed (shadow created for its nullability, generated)
        class Reseed {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @NotModified @Nullable byte[] getAdditionalInput() { return null; }
        }
    }

    class BasicPermission$ {
        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getActions() { return null; }
    }
}
