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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The openjdk parser must produce detailed-sources for the compilation unit (its package name) and for a type
 * declaration (its simple name).
 */
public class TestTypeAndPackageDetailedSources extends CommonTest {

    @Language("java")
    private static final String A = """
            package a.b.c;

            class A {}
            """;

    @DisplayName("compilation unit carries the package name in its detailed sources, keyed by cu.packageName()")
    @Test
    public void compilationUnitShouldHavePackageNameInDetailedSources() {
        TypeInfo typeA = scan("a.b.c.A", A);
        CompilationUnit cu = typeA.compilationUnit();
        assertNotNull(cu.source().detailedSources());
        assertEquals("a.b.c", cu.packageName());
        // the detail must be retrievable with the EXACT cu.packageName() instance: DetailedSources is identity-keyed
        assertNotNull(cu.source().detailedSources().detail(cu.packageName()),
                "no detailed source for package name '" + cu.packageName() + "'");
        // 'a.b.c' on line 1: 'package a.b.c;'
        assertEquals("-@1:9-1:13", cu.source().detailedSources().detail(cu.packageName()).toString());
    }

    @DisplayName("type declaration should carry its simple name in its detailed sources")
    @Test
    public void typeShouldHaveSimpleNameInDetailedSources() {
        TypeInfo typeA = scan("a.b.c.A", A);
        assertNotNull(typeA.source().detailedSources().detail(typeA.simpleName()));
    }
}
