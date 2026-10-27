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

package io.codelaser.maddi.cst.api.element;

import io.codelaser.maddi.annotation.Fluent;

/**
 * Represents a single {@code import} declaration inside a {@link CompilationUnit}.
 */
public interface ImportStatement extends Element {

    /**
     * Returns the fully-qualified name (or wildcard name) being imported,
     * e.g. {@code "java.util.List"} or {@code "java.util.*"}.
     */
    String importString();

    /**
     * Returns {@code true} if this is a {@code static} import. Always {@code false} for Kotlin, which has no
     * such modifier: one {@code import} reaches a class, a top-level function and a companion member alike.
     */
    boolean isStatic();

    /**
     * The name this import binds when it differs from the last segment of {@link #importString()} --
     * Kotlin's {@code import a.b.C as D}. {@code null} when there is none, which is always the case for Java.
     * <p>
     * ⛔ A consumer that REWRITES an import line must carry this over, or {@code as D} is dropped and every use
     * of {@code D} in the file stops resolving. {@link #print} is the Java printer and ignores it.
     */
    default String alias() {
        return null;
    }

    /** Returns {@code true} if this is a wildcard import (the import string ends with {@code .*}). */
    default boolean isStar() {
        return importString().endsWith(".*");
    }

    interface Builder extends Element.Builder<Builder> {

        @Fluent
        Builder setImport(String importString);

        @Fluent
        Builder setIsStatic(boolean isStatic);

        /** Kotlin's {@code as D}; leave unset for Java. */
        @Fluent
        Builder setAlias(String alias);

        ImportStatement build();
    }
}
