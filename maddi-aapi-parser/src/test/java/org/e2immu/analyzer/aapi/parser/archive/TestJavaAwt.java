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

package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.COMMUTABLE_METHODS;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaAwt extends CommonTest {

    @Test
    public void testContainerAdd() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Container.class, mainSources());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("add", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testContainerAddWithConstraints() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Container.class, mainSources());
        MethodInfo methodInfo = typeInfo.methods().stream()
                                        .filter(m -> m.simpleName().equals("add")
                                                             && m.parameters().size() == 2
                                                             && m.parameters().get(1)
                                                                        .parameterizedType()
                                                                        .equals(runtime().newParameterizedType(runtime().objectTypeInfo(), 0)))
                                        .findFirst().orElseThrow();
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testContainerSetLayout() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Container.class, mainSources());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("setLayout", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testComponentAddMouseListener() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Component.class, mainSources());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("addMouseListener", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    private void testCommutable(MethodInfo methodInfo) {
        Value.CommutableData cd = methodInfo.analysis().getOrNull(COMMUTABLE_METHODS, ValueImpl.CommutableData.class);
        assertTrue(cd.isBlankMultiParSeq());
    }
}
