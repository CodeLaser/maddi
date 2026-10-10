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
