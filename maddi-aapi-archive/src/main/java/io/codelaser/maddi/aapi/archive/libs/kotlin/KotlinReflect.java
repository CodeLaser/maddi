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

import java.lang.reflect.Type;

/**
 * The annotated API for {@code kotlin.reflect}: the read-only accessors the library-call census found read as
 * modifying. A KClass, KCallable or KType is a description of code, and asking it a question changes nothing.
 * Only members are contracted here; no type-level immutability is claimed for these interfaces.
 * <p>
 * ⚠ {@code X::class.java} is NOT covered: its JVM method is {@code JvmClassMappingKt.getJavaClass} (a @JvmName),
 * the K2 model calls the member {@code getJava}, and a contract matches by the JVM name.
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
