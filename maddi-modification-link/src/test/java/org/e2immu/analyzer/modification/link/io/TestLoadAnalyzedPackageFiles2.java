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

package org.e2immu.analyzer.modification.link.io;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NOT_NULL;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.NotNullImpl.NULLABLE;
import static org.junit.jupiter.api.Assertions.*;

public class TestLoadAnalyzedPackageFiles2 extends CommonTest {

    public TestLoadAnalyzedPackageFiles2() {
        super(true);
    }

    @Test
    public void test1() throws IOException {
        TypeInfo object = javaInspector.compiledTypesManager().get(Object.class);
        MethodInfo objectToString = object.findUniqueMethod("toString", 0);
        // assertSame(TRUE, methodInfo.analysis().getOrDefault(CONTAINER_METHOD, FALSE));
        assertSame(NOT_NULL, objectToString.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertFalse(objectToString.isModifying());
        assertSame(IMMUTABLE, objectToString.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(INDEPENDENT, objectToString.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        MethodInfo listIterator = list.findUniqueMethod("iterator", 0);
        assertEquals("java.lang.Iterable.iterator(), java.util.Collection.iterator()",
                listIterator.overrides().stream().map(Object::toString).sorted()
                        .collect(Collectors.joining(", ")));
        assertFalse(listIterator.allowsInterrupts());
        assertFalse(listIterator.isModifying());

    }
}
