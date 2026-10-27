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

package io.codelaser.maddi.cst.print.csharp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The compatibility library, {@code Maddi.JavaCompat}: C# source that goes with translated code that uses it. It has
 * the Java behaviour the BCL lacks (see {@link CSharpBcl}); the translation calls it only where that behaviour is
 * needed, and every use brings in its {@code using}.
 */
public final class CSharpCompat {

    public static final String NAMESPACE = "Maddi.JavaCompat";

    /** The name of the library's file, beside the translated ones. */
    public static final String FILE_NAME = "JavaCompat.cs";

    private CSharpCompat() {
    }

    /** The library's C# source. */
    public static String source() {
        try (InputStream in = CSharpCompat.class.getResourceAsStream(FILE_NAME)) {
            if (in == null) throw new IllegalStateException("no " + FILE_NAME + " beside " + CSharpCompat.class);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
