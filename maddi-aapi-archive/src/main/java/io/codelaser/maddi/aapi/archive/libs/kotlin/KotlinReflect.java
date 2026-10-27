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

import java.lang.reflect.Type;

/**
 * The annotated API for {@code kotlin.reflect}: the read-only accessors the library-call census found read as
 * modifying. A KClass, KCallable or KType is a description of code, and asking it a question changes nothing.
 * Only members are contracted here; no type-level immutability is claimed for these interfaces.
 * <p>
 * {@code X::class.java} is {@code JvmClassMappingKt.getJavaClass} (a @JvmName), in package kotlin.jvm: see
 * {@link KotlinJvm}.
 */
public class KotlinReflect {
    public static final String PACKAGE_NAME = "kotlin.reflect";

    interface KClass$<T> {
        @NotModified
        String getSimpleName();

        @NotModified
        String getQualifiedName();

        @NotModified
        boolean isInstance(Object value);
    }

    interface KCallable$<R> {
        @NotModified
        String getName();
    }

    /* public val KType.javaType: Type */
    class TypesJVMKt$ {
        static Type getJavaType(@NotModified kotlin.reflect.KType receiver) {
            return null;
        }
    }
}
