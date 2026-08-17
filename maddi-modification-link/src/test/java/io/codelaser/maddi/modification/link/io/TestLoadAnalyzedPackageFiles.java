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

package io.codelaser.maddi.modification.link.io;

import ch.qos.logback.classic.Level;
import io.codelaser.maddi.modification.common.CommonTest;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults.ANALYZED_RESULTS;
import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.*;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.NotNullImpl.NOT_NULL;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.NotNullImpl.NULLABLE;
import static org.junit.jupiter.api.Assertions.*;

public class TestLoadAnalyzedPackageFiles {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.shallow")).setLevel(Level.DEBUG);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.modification")).setLevel(Level.DEBUG);
    }

    @Test
    public void test1() throws IOException {
        SourceSet sourceSet = SourceSetImpl.testProtocolSourceSet();
        JavaInspector javaInspector = CommonTest.javaInspectorFactory().withSources(sourceSet);
        javaInspector.onlyPreload();
        LoadAnalysisResults lar = new LoadAnalysisResults(javaInspector.runtime(), sourceSet);
        lar.go(ANALYZED_RESULTS);

        TypeInfo object = javaInspector.compiledTypesManager().typeIfLoaded(Object.class);
        assertNotNull(object);
        MethodInfo objectToString = object.findUniqueMethod("toString", 0);
        // assertSame(TRUE, methodInfo.analysis().getOrDefault(CONTAINER_METHOD, FALSE));
        assertSame(NOT_NULL, objectToString.analysis().getOrDefault(NOT_NULL_METHOD, NULLABLE));
        assertFalse(objectToString.isModifying());
        assertSame(IMMUTABLE, objectToString.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
        assertSame(INDEPENDENT, objectToString.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

        TypeInfo list = javaInspector.compiledTypesManager().typeIfLoaded(List.class);
        MethodInfo listIterator = list.findUniqueMethod("iterator", 0);
        assertEquals("java.lang.Iterable.iterator(), java.util.Collection.iterator()",
                listIterator.overrides().stream().map(Object::toString).sorted()
                        .collect(Collectors.joining(", ")));
        assertFalse(listIterator.allowsInterrupts());
        assertFalse(listIterator.isModifying());

    }
}
