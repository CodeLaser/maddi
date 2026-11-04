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
