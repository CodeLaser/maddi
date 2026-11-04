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

import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

/**
 * The annotated API for {@code kotlin.io.path}: like {@link KotlinIo}, a Path only names a file, and writing or
 * walking the file leaves the Path as it was. Each vararg array of options is read, never written.
 * <p>
 * kotlin.* types are named in full, never imported: see {@link KotlinJvm}.
 */
public class KotlinIoPath {
    public static final String PACKAGE_NAME = "kotlin.io.path";

    class PathsKt__PathReadWriteKt$ {
        static void writeText(@NotModified Path receiver, @NotModified CharSequence text, @NotModified Charset charset,
                              @NotModified OpenOption... options) {
        }
    }

    class PathsKt__PathUtilsKt$ {
        @NotNull
        static Path createParentDirectories(@NotModified Path receiver, @NotModified FileAttribute<?>... attributes) {
            return null;
        }

        @NotNull
        static kotlin.sequences.Sequence<Path> walk(@NotModified Path receiver,
                                                    @NotModified kotlin.io.path.PathWalkOption... options) {
            return null;
        }
    }
}
