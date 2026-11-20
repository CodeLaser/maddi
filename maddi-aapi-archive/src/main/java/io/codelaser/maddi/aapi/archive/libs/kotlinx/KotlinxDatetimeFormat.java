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
package io.codelaser.maddi.aapi.archive.libs.kotlinx;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.NotModified;

/**
 * The annotated API for {@code kotlinx.datetime.format}. A {@code DateTimeFormat} is a compiled, immutable format
 * (the {@code java.time.format.DateTimeFormatter} of kotlinx.datetime): formatting and parsing read it. Its
 * {@code DateTimeFormatBuilder} is a builder and is deliberately left uncontracted (see {@link KotlinxDatetime}).
 */
public class KotlinxDatetimeFormat {
    public static final String PACKAGE_NAME = "kotlinx.datetime.format";

    @ImmutableContainer(hc = true)
    interface DateTimeFormat$<T> {
        @NotModified
        String format(@NotModified T value);

        @NotModified
        T parse(@NotModified CharSequence input);

        @NotModified
        T parseOrNull(@NotModified CharSequence input);
    }
}
