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

import java.net.URI;
import java.util.List;

public interface CompilationUnit extends Element {

    URI uri();

    String packageName();

    List<ImportStatement> importStatements();

    SourceSet sourceSet();

    FingerPrint fingerPrintOrNull();

    /**
     * Can be set only once! If set during building phase, this method may not be called.
     *
     * @param fingerPrint the fingerprint to be set
     */
    void setFingerPrint(FingerPrint fingerPrint);

    interface Builder extends Element.Builder<Builder> {

        @Fluent
        Builder addImportStatement(ImportStatement importStatement);

        @Fluent
        Builder setURI(URI uri);

        // to avoid having to catch exceptions in PredefinedImpl
        @Fluent
        Builder setURIString(String s);

        @Fluent
        Builder setPackageName(String packageName);

        @Fluent
        Builder setSourceSet(SourceSet sourceSet);

        @Fluent
        Builder setFingerPrint(FingerPrint fingerPrint);

        CompilationUnit build();
    }

    // helper method here, set==null for primitives
    default boolean partOfJdk() {
        SourceSet set = sourceSet();
        return set == null || set.partOfJdk();
    }

    // helper method here, set==null for primitives;  partOfJdk() implies externalLibrary()
    default boolean externalLibrary() {
        SourceSet set = sourceSet();
        assert set != null;
        return set.externalLibrary();
    }
}
