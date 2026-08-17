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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestPackageInfo extends CommonTest {

    @Language("java")
    private static final String ANNOT = """
            package a;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.PACKAGE})
            public @interface PackageWide {
                String value();
            }
            """;

    @Language("java")
    private static final String INPUT1 = """
            @PackageWide("?")
            package a.b;
            import a.PackageWide;
            """;

    @Test
    public void test1() {
        TypeInfo pkgInfo = scan(false, "a.PackageWide", ANNOT, "a.b.package-info", INPUT1)
                .get("a.b.package-info");
        assertEquals("a.b.package-info", pkgInfo.fullyQualifiedName());
        assertEquals("@PackageWide(\"?\")", pkgInfo.annotations().getFirst().toString());
    }

    /*
    A package-info.java carrying the two things a real one carries: the project's licence header (a plain
    block comment on the compilation unit) and the package javadoc (which hangs off the PACKAGE DECLARATION,
    see ScanCompilationUnit). Both are read correctly; the printer's package-info branch returned before
    either could be written, so a lever that reprints the file silently reduced it to its package declaration.
     */
    @Language("java")
    private static final String INPUT2 = """
            /*
             * Copyright 2026 Example B.V. Licensed under the Example License 2.0; you may not use this
             * file except in compliance with it.
             */

            /**
             * Documents package a.b, and mentions {@link a.PackageWide}.
             */
            package a.b;
            import a.PackageWide;
            """;

    @Test
    public void licenceHeaderAndPackageJavaDocSurvivePrinting() {
        TypeInfo pkgInfo = scan(false, "a.PackageWide", ANNOT, "a.b.package-info", INPUT2)
                .get("a.b.package-info");
        assertTrue(pkgInfo.typeNature().isPackageInfo());
        String printed = print2(pkgInfo.compilationUnit());

        assertTrue(printed.contains("Example License 2.0"),
                "the licence header must survive a reprint; printed:\n" + printed);
        assertTrue(printed.contains("Documents package a.b"),
                "the package javadoc must survive a reprint; printed:\n" + printed);
        assertTrue(printed.contains("package a.b;"), "printed:\n" + printed);
        // the header is a header: nothing may precede it
        assertTrue(printed.stripLeading().startsWith("/*"),
                "the licence header must come first; printed:\n" + printed);
    }

    /*
    CONTROL for the branch that DOES have to keep working: annotations are what the package-info branch was
    written to emit, and they must still precede the package declaration (a package annotation is not legal
    after it). Without this, a fix that prints comments and drops annotations passes the test above.
     */
    @Test
    public void packageAnnotationStillPrecedesThePackageDeclaration() {
        TypeInfo pkgInfo = scan(false, "a.PackageWide", ANNOT, "a.b.package-info", INPUT1)
                .get("a.b.package-info");
        String printed = print2(pkgInfo.compilationUnit());
        int annotation = printed.indexOf("@PackageWide");
        int packageDecl = printed.indexOf("package a.b;");
        assertTrue(annotation >= 0 && packageDecl > annotation,
                "the package annotation must precede the package declaration; printed:\n" + printed);
    }

}
