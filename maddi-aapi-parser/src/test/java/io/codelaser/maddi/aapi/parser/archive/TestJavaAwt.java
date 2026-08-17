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

package io.codelaser.maddi.aapi.parser.archive;

import io.codelaser.maddi.aapi.parser.CommonTest;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.COMMUTABLE_METHODS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaAwt extends CommonTest {

    @Test
    public void testContainerAdd() {
        TypeInfo typeInfo = compiledTypesManager().type(Container.class, mainSources());
        assertNotNull(typeInfo, "Cannot find java.awt.Container");
        MethodInfo methodInfo = typeInfo.findUniqueMethod("add", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testContainerAddWithConstraints() {
        TypeInfo typeInfo = compiledTypesManager().type(Container.class, mainSources());
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
        TypeInfo typeInfo = compiledTypesManager().type(Container.class, mainSources());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("setLayout", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testComponentAddMouseListener() {
        TypeInfo typeInfo = compiledTypesManager().type(Component.class, mainSources());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("addMouseListener", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    private void testCommutable(MethodInfo methodInfo) {
        Value.CommutableData cd = methodInfo.analysis().getOrNull(COMMUTABLE_METHODS, ValueImpl.CommutableData.class);
        assertTrue(cd.isBlankMultiParSeq());
    }

    // These mutable AWT components are not @Container/@Immutable, so their read accessors need explicit
    // @NotModified; only the mutators (setX/paint/add) modify.
    @Test
    public void testComponentGettersNonModifying() {
        TypeInfo component = compiledTypesManager().typeIfLoaded(Component.class);
        for (String g : new String[]{"getWidth", "getHeight", "getX", "getY", "isVisible", "isEnabled"}) {
            assertFalse(component.findUniqueMethod(g, 0).isModifying(), () -> "Component." + g + " must be non-modifying");
        }
        assertTrue(component.findUniqueMethod("setVisible", 1).isModifying(), "setVisible modifies");
    }
}
