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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.element.SourceImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseAnnotationDeclaration extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public @interface Annot {
              String value() default "";
              int n();
              String k() default "k";
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertEquals("Annot", typeInfo.simpleName());
        assertEquals("a.b.Annot", typeInfo.fullyQualifiedName());
        assertTrue(typeInfo.typeNature().isAnnotation());

        assertEquals(3, typeInfo.methods().size());
        MethodInfo methodInfo = typeInfo.methods().getFirst();
        assertEquals("value", methodInfo.name());
        assertEquals("a.b.Annot.value()", methodInfo.fullyQualifiedName());
        if (methodInfo.source() instanceof SourceImpl source) {
            assertEquals(3, source.beginLine());
            assertEquals(3, source.endLine());
        }
    }
}
