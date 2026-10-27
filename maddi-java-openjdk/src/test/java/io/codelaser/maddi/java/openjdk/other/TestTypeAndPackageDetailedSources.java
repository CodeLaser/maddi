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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
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
