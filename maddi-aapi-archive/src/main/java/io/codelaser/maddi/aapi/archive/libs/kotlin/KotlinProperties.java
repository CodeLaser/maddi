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
