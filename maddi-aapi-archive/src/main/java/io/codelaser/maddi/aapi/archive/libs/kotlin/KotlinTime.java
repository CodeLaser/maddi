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
 * The annotated API for {@code kotlin.time}. {@code Instant} is a value: two longs, and every method reads them.
 * Its companion holds no state. Imports nothing from {@code kotlin.*} (see {@link KotlinCoroutines}).
 */
public class KotlinTime {
    public static final String PACKAGE_NAME = "kotlin.time";

    class Instant$ {
        @NotModified
        long toEpochMilliseconds() { return 0L; }

        @NotModified
        long getEpochSeconds() { return 0L; }

        @NotModified
        int getNanosecondsOfSecond() { return 0; }

        class Companion {
            @NotModified
            kotlin.time.Instant fromEpochMilliseconds(long epochMilliseconds) { return null; }

            @NotModified
            kotlin.time.Instant fromEpochSeconds(long epochSeconds, long nanosecondAdjustment) { return null; }
        }
    }
}
