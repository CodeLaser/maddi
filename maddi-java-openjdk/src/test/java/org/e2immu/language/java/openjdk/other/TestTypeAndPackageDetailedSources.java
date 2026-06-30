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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The openjdk parser must produce detailed-sources for the compilation unit (its package name) and for a type
 * declaration (its simple name).
 */
public class TestTypeAndPackageDetailedSources extends CommonTest {

    @Language("java")
    private static final String A = """
            package a;

            class A {}
            """;

    @DisplayName("compilation unit should carry the package name in its detailed sources")
    @Test
    public void compilationUnitShouldHavePackageNameInDetailedSources() {
        TypeInfo typeA = scan("a.A", A);
        CompilationUnit cu = typeA.compilationUnit();
        assertNotNull(cu.source().detailedSources());
    }

    @DisplayName("type declaration should carry its simple name in its detailed sources")
    @Test
    public void typeShouldHaveSimpleNameInDetailedSources() {
        TypeInfo typeA = scan("a.A", A);
        assertNotNull(typeA.source().detailedSources().detail(typeA.simpleName()));
    }
}
