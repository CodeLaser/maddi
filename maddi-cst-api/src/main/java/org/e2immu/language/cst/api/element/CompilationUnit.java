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

package org.e2immu.language.cst.api.element;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.net.URI;
import java.util.List;

/**
 * Represents a single Java source file — the top-level unit of compilation in the CST.
 * <p>
 * A compilation unit declares a package, an optional list of import statements, and one or
 * more top-level type declarations ({@link TypeInfo}). It also carries a {@link SourceSet}
 * that identifies where the source was loaded from, and an optional {@link FingerPrint} for
 * change detection.
 * <p>
 * Top-level types are set after construction via {@link #setTypes} (write-once), because the
 * {@code CompilationUnit} must exist before the types that belong to it can reference it.
 */
public interface CompilationUnit extends Element {

    /** Returns a shallow copy of this compilation unit (same types list, new wrapper object). */
    CompilationUnit copy();

    /** Returns the URI identifying the source file (e.g. a {@code file://} path or test-protocol URI). */
    URI uri();

    /** Returns the declared package name, or an empty string for the default package. */
    String packageName();

    /** Returns the import declarations in source order. */
    List<ImportStatement> importStatements();

    /** Returns the source set this compilation unit was loaded from. */
    SourceSet sourceSet();

    /** Returns the source fingerprint used for change detection, or {@code null} if not yet set. */
    FingerPrint fingerPrintOrNull();

    /** Returns comments that appear after the last type declaration (at the end of the file). */
    List<Comment> trailingComments();

    /**
     * Sets the top-level types declared in this file. Must be called exactly once, after
     * the types have been inspected and linked back to this compilation unit.
     */
    void setTypes(List<TypeInfo> types);

    /** Returns the top-level types declared in this file. {@link #setTypes} must have been called first. */
    List<TypeInfo> types();

    /**
     * Sets the source fingerprint. Can be called only once; calling it again throws.
     * If a fingerprint was already set via the builder, this method must not be called.
     *
     * @param fingerPrint the fingerprint to set
     */
    void setFingerPrint(FingerPrint fingerPrint);

    interface Builder extends Element.Builder<Builder> {

        @Fluent
        Builder addImportStatement(ImportStatement importStatement);

        @Fluent
        Builder setURI(URI uri);

        /** Convenience variant of {@link #setURI} that parses the URI from a string. */
        @Fluent
        Builder setURIString(String s);

        @Fluent
        Builder setPackageName(String packageName);

        @Fluent
        Builder setSourceSet(SourceSet sourceSet);

        @Fluent
        Builder setFingerPrint(FingerPrint fingerPrint);

        @Fluent
        Builder addTrailingComments(List<Comment> comments);

        CompilationUnit build();
    }

    /** Returns {@code true} if this compilation unit belongs to the JDK. The source set is {@code null} for primitives. */
    default boolean partOfJdk() {
        SourceSet set = sourceSet();
        return set == null || set.partOfJdk();
    }

    /** Returns {@code true} if this compilation unit belongs to an external library. {@link #partOfJdk()} implies this. */
    default boolean externalLibrary() {
        SourceSet set = sourceSet();
        assert set != null;
        return set.externalLibrary();
    }
}
