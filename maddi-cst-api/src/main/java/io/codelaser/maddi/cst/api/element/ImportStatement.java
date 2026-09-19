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
