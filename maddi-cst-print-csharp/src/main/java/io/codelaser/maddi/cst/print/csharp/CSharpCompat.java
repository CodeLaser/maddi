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
