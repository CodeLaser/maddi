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
 * The annotated API for {@code kotlin.ranges}. An IntRange is three final ints; a membership test reads them.
 */
public class KotlinRanges {
    public static final String PACKAGE_NAME = "kotlin.ranges";

    class IntRange$ {
        @NotModified
        boolean contains(int value) { return false; }

        @NotModified
        boolean isEmpty() { return false; }

        @NotModified
        Integer getStart() { return null; }

        @NotModified
        Integer getEndInclusive() { return null; }
    }

    /* IntRange's parent: first, last and step are final ints, read */
    class IntProgression$ {
        @NotModified
        int getFirst() { return 0; }

        @NotModified
        int getLast() { return 0; }

        @NotModified
        int getStep() { return 0; }
    }
}
