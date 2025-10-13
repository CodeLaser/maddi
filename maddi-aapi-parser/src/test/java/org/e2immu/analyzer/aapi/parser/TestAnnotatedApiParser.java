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

package org.e2immu.analyzer.aapi.parser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotatedApiParser {
    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException {
        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        annotatedApiParser.initialize(null,
                List.of(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "org/slf4j",
                        JavaInspectorImpl.E2IMMU_SUPPORT),
                List.of("src/test/java/org/e2immu/analyzer/aapi/parser"),
                List.of("example."));
        List<TypeInfo> types = annotatedApiParser.typesParsed();
        assertEquals(2, types.size());
        TypeInfo t1 = types.stream()
                .filter(ti -> "org.e2immu.analyzer.aapi.parser.example.popular.OrgSlf4J".equals(ti.fullyQualifiedName()))
                .findFirst().orElseThrow();
        String uri = t1.compilationUnitOrEnclosingType().getLeft().uri().toString();
        assertTrue(uri.endsWith("example/popular/OrgSlf4J.java"), "Have: "+uri);

        assertEquals(2, annotatedApiParser.getWarnings());

        Runtime runtime = annotatedApiParser.runtime();
        TypeInfo string = runtime.stringTypeInfo();
        TypeInfo charInfo = runtime.charTypeInfo();
        MethodInfo charConstructor = string.constructors().stream()
                .filter(c -> c.parameters().size() == 1 &&
                             c.parameters().getFirst().parameterizedType().typeInfo() == charInfo)
                .findFirst().orElseThrow();
        assertEquals("java.lang.String.<init>(char[])", charConstructor.fullyQualifiedName());
        // the annotations have not been copied, they're in a map!!
        assertEquals(0, charConstructor.annotations().size());
        List<AnnotationExpression> charInfoAnnots = annotatedApiParser.annotations(charConstructor);
        assertEquals(1, charInfoAnnots.size());
        assertEquals("Independent", charInfoAnnots.getFirst().typeInfo().simpleName());
    }
}
