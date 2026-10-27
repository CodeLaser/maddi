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
import kotlin.reflect.KProperty;

/**
 * The annotated API for {@code kotlin.properties}: reading a delegated property.
 * <p>
 * ⚠ A DECISION, NOT A FACT (Bart, 2026-09-26). {@code val x by delegate} compiles to
 * {@code x$delegate.getValue(this, $$delegatedProperties[i])}, and uncontracted that call modifies the delegate
 * field, {@code this} and the property reference -- 259 calls in detekt alone, each capping its type. detekt's
 * delegates MEMOIZE: getValue writes a private cache on the first read. Declaring the read non-modifying is the
 * bargain {@link Kotlin}'s Lazy contract makes (an idempotent slot whose write is observationally invisible), taken
 * here for the interface as a whole: nothing in ReadOnlyProperty promises it, so a delegate that counts its reads
 * or otherwise changes observable state is misread. setValue stays modifying.
 */
public class KotlinProperties {
    public static final String PACKAGE_NAME = "kotlin.properties";

    interface ReadOnlyProperty$<T, V> {
        @NotModified
        V getValue(@NotModified T thisRef, @NotModified KProperty<?> property);
    }

    interface ReadWriteProperty$<T, V> {
        @NotModified
        V getValue(@NotModified T thisRef, @NotModified KProperty<?> property);
    }
}
