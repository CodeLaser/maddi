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

/**
 * The annotated API for {@code kotlin.jvm}: the bridge between a {@code KClass} and its {@code java.lang.Class}.
 * {@code X::class.java} is a property whose getter kotlinc names {@code getJavaClass} ({@code @get:JvmName}); the
 * front end builds it under that name since #15, so a contract keyed on it now reaches a Kotlin caller. Asking a
 * class description for another description changes neither.
 * <p>
 * kotlin.* types are named in full, never imported, as in {@link KotlinReflect}: such a unit survives a parse on the
 * shared factory, which has no kotlin-stdlib (TestParseAnalyzeWrite counts it).
 */
public class KotlinJvm {
    public static final String PACKAGE_NAME = "kotlin.jvm";

    /*
    public val <T> KClass<T>.java: Class<T>               @get:JvmName("getJavaClass")
    public val <T : Any> Class<T>.kotlin: KClass<T>      @get:JvmName("getKotlinClass")
    */
    class JvmClassMappingKt$ {
        static <T> Class<T> getJavaClass(@NotModified kotlin.reflect.KClass<T> receiver) {
            return null;
        }

        static <T> kotlin.reflect.KClass<T> getKotlinClass(@NotModified Class<T> receiver) {
            return null;
        }
    }
}
