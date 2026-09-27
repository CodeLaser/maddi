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

package io.codelaser.maddi.aapi.archive.libs.kotlin;

import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * The annotated API for {@code kotlin.io}: reading a file through a {@link File} leaves the File object as it
 * was. The stream extensions (readBytes, copyTo, reader) DO consume their stream and stay uncontracted.
 */
public class KotlinIo {
    public static final String PACKAGE_NAME = "kotlin.io";

    class FilesKt__UtilsKt$ {
        @NotNull
        static String getExtension(@NotModified File receiver) {
            return null;
        }

        @NotNull
        static String getNameWithoutExtension(@NotModified File receiver) {
            return null;
        }
    }

    class FilesKt__FileReadWriteKt$ {
        @NotNull
        static String readText(@NotModified File receiver, Charset charset) {
            return null;
        }

        @NotNull
        static List<String> readLines(@NotModified File receiver, Charset charset) {
            return null;
        }

        @NotNull
        static byte[] readBytes(@NotModified File receiver) {
            return null;
        }
    }
}
