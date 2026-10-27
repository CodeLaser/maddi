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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;

import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

public class JavaNioCharset {
    public static final String PACKAGE_NAME = "java.nio.charset";
    //public abstract class Charset implements Comparable<Charset>
    // immutable value (name/aliases are fixed); abstract, so this becomes @Immutable(hc=true).
    // NOT a container: encode(CharBuffer)/decode(ByteBuffer) advance (modify) their buffer argument.
    // Same shape as java.nio.file.Path.
    @Immutable
    class Charset$ {
        static boolean isSupported(@NotNull String charsetName) { return false; }
        static Charset forName(@NotNull String charsetName) { return null; }
        static Charset forName(@NotNull String charsetName, @Nullable Charset fallback) { return null; }
        //@Independent[T]
        static SortedMap<String, Charset> availableCharsets() { return null; }
        static Charset defaultCharset() { return null; }
        //frequency 4
        String name() { return null; }
        Set<String> aliases() { return null; }
        String displayName() { return null; }
        boolean isRegistered() { return false; }
        String displayName(@NotNull Locale locale) { return null; }
        boolean contains(@NotNull Charset charset) { return false; }
        CharsetDecoder newDecoder() { return null; }
        CharsetEncoder newEncoder() { return null; }
        boolean canEncode() { return false; }
        CharBuffer decode(@NotNull ByteBuffer bb) { return null; }
        ByteBuffer encode(@NotNull CharBuffer cb) { return null; }
        ByteBuffer encode(@NotNull String str) { return null; }
        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull Charset that) { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object ob) { return false; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Charset$(@NotNull java.lang.String p0, @Nullable java.lang.String[] p1) { }
    }
}
