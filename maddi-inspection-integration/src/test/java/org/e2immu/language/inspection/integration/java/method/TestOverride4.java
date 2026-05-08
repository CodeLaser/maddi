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

package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestOverride4 extends CommonTest2 {
    @Language("java")
    String A = """
            package a;
            
            public interface A {
                void m();
            }
            """;

    @Language("java")
    String B = """
            package a;
            
            public interface B extends A {
                @Override
                void m();
            }
            """;

    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("a.A", A, "a.B", B));
        TypeInfo A = parseResult.findType("a.A");
        MethodInfo am = A.findUniqueMethod("m", 0);
        TypeInfo B = parseResult.findType("a.B");
        MethodInfo bm = B.findUniqueMethod("m", 0);
        assertTrue(bm.overrides().contains(am));
    }

    @Test
    public void test2() throws IOException {
        init(Map.of());
        TypeInfo sb = javaInspector.compiledTypesManager().getOrLoad(StringBuilder.class);
        MethodInfo sbAppend = sb.methodStream()
                .filter(m -> m.parameters().size() == 1
                             && "append".equals(m.name())
                             && m.parameters().getFirst().parameterizedType()
                                     .equals(javaInspector.runtime().charParameterizedType()))
                .findFirst().orElseThrow();
        assertEquals("java.lang.StringBuilder.append(char)", sbAppend.fullyQualifiedName());
        assertEquals("java.lang.AbstractStringBuilder.append(char), java.lang.Appendable.append(char)",
                sbAppend.overrides().stream().map(MethodInfo::fullyQualifiedName).sorted().collect(Collectors.joining(", ")));
    }


    @Test
    public void test3() throws IOException {
        init(Map.of());
        TypeInfo object = javaInspector.runtime().objectTypeInfo();
        TypeInfo object2 = javaInspector.compiledTypesManager().get(Object.class);
        assertSame(object, object2);

        assertEquals(12, object.methods().size());
        TypeInfo sb = javaInspector.compiledTypesManager().getOrLoad(Throwable.class);
        MethodInfo throwableToString = sb.findUniqueMethod("toString", 0);
        assertEquals("java.lang.Throwable.toString()", throwableToString.fullyQualifiedName());
        assertEquals("java.lang.Object.toString()",
                throwableToString.overrides().stream().map(MethodInfo::fullyQualifiedName).sorted().collect(Collectors.joining(", ")));
    }
}
