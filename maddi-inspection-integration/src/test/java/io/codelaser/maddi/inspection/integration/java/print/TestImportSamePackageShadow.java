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

package io.codelaser.maddi.inspection.integration.java.print;

import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A single-type import shadows a type of the unit's own package (GitHub CodeLaser/maddi#110): a same-package
 * top-level type the unit references owns its simple name, as a type declared in the unit does.
 */
public class TestImportSamePackageShadow extends CommonTest2 {

    @Language("java")
    private static final String ITERATOR = """
            package p;
            public interface Iterator<T> extends java.util.Iterator<T> {
                static <T> Iterator<T> empty() { return null; }
            }
            """;

    @Language("java")
    private static final String USES = """
            package p;
            public class Uses {
                static Iterator<String> own() { return Iterator.empty(); }
            }
            """;

    @Language("java")
    private static final String ELSEWHERE = """
            package q;
            public class Elsewhere {
                static int one() { return 1; }
            }
            """;

    private String print(ParseResult parseResult, String fqn) {
        TypeInfo typeInfo = parseResult.findType(fqn);
        ImportComputer ic = javaInspector.importComputer(Integer.MAX_VALUE, typeInfo.compilationUnit().sourceSet());
        // as a CST-built reference to java.util.Iterator would: no written qualifier, so an import candidate
        ic.add(javaInspector.compiledTypesManager().type(java.util.Iterator.class));
        return javaInspector.print2(typeInfo.compilationUnit(), (Qualification.Decorator) null, ic);
    }

    @Test
    public void samePackageTypeIsNotShadowedByAnImport() throws IOException {
        ParseResult parseResult = init(Map.of("p.Iterator", ITERATOR, "p.Uses", USES));
        String printed = print(parseResult, "p.Uses");
        assertFalse(printed.contains("import java.util.Iterator;"), printed);
        assertTrue(printed.contains("Iterator.empty()"), printed);
    }

    /** The control: in a unit of another package nothing owns the name, so java.util.Iterator is imported. */
    @Test
    public void elsewhereTheImportStays() throws IOException {
        ParseResult parseResult = init(Map.of("p.Iterator", ITERATOR, "q.Elsewhere", ELSEWHERE));
        String printed = print(parseResult, "q.Elsewhere");
        assertTrue(printed.contains("import java.util.Iterator;"), printed);
    }
}
