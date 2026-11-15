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

/**
 * The annotated API for {@code kotlin.uuid}. {@code Uuid} is a value (two longs); its companion holds no state. The
 * random generators draw from a shared secure random, which is no state of the companion. Imports nothing from
 * {@code kotlin.*} (see {@link KotlinCoroutines}).
 */
public class KotlinUuid {
    public static final String PACKAGE_NAME = "kotlin.uuid";

    class Uuid$ {
        class Companion {
            @NotModified
            kotlin.uuid.Uuid parseHexOrNull(String hexString) { return null; }

            @NotModified
            kotlin.uuid.Uuid generateV4() { return null; }

            @NotModified
            kotlin.uuid.Uuid generateV7() { return null; }
        }
    }

    /* public fun Uuid.toJavaUuid(): java.util.UUID -- a new UUID from the two longs */
    class UuidKt__UuidJVMKt$ {
        static java.util.UUID toJavaUuid(@NotModified kotlin.uuid.Uuid receiver) { return null; }
    }
}
