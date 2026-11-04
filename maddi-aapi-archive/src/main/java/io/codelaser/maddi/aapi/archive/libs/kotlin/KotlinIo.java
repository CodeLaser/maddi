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

package io.codelaser.maddi.aapi.archive.libs.kotlin;

import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * The annotated API for {@code kotlin.io}: reading a file through a {@link File} leaves the File object as it
 * was. The stream extensions (readBytes, copyTo, reader) DO consume their stream and stay uncontracted.
 * <p>
 * kotlin.* types are named in full, never imported: see {@link KotlinJvm}.
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

        /* writes the FILE, not the File object, which only names it */
        static void writeText(@NotModified File receiver, @NotModified String text, @NotModified Charset charset) {
        }

        static <T> T useLines(@NotModified File receiver, @NotModified Charset charset,
                              kotlin.jvm.functions.Function1<? super kotlin.sequences.Sequence<String>, ? extends T> block) {
            return null;
        }
    }

    class FilesKt__FileTreeWalkKt$ {
        @NotNull
        static kotlin.io.FileTreeWalk walk(@NotModified File receiver, @NotModified kotlin.io.FileWalkDirection direction) {
            return null;
        }
    }
}
