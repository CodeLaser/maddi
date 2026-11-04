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

package io.codelaser.maddi.run.openjdkmain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * Only a partial re-parse carries the intrinsic tier (prep's output) onto a rewired copy
 * ({@code InfoMapView.carryIntrinsic}). A rewire that follows a transform must not: the transform builds new bodies
 * out of the old, prepped, statements and preps the result, so a carried value is written a second time ("Trying to
 * overwrite a value for property variableData"), and rewiring it can already fail on a type the transform changed
 * (a lambda whose implementation type gained a method). Both broke the jfocus deduplication pipeline
 * (transform, {@code newInfoMap(types).rewireAll()}, prep) at 0.9.7-alpha.4. The other carried tiers are not affected.
 */
public class TestRewireIntrinsicCarry {

    private static final Property INTRINSIC = new PropertyImpl("testIntrinsicCarried", ValueImpl.BoolImpl.FALSE,
            Property.AnalysisTier.INTRINSIC, true);
    private static final Property PARSE_TIME = new PropertyImpl("testParseTimeCarried", ValueImpl.BoolImpl.FALSE,
            true);

    @Language("java")
    private static final String SOURCE = """
            package a;
            public class T {
                private final int k = 3;
                public int m(int n) {
                    int j = n + k;
                    return j;
                }
            }
            """;

    @TempDir
    Path root;
    private JavaInspector javaInspector;
    private TypeInfo type;

    @BeforeEach
    public void parse() throws IOException {
        Path src = Files.createDirectories(root.resolve("src/a"));
        Files.writeString(src.resolve("T.java"), SOURCE);
        var main = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(root.resolve("src")))
                .setUri(root.resolve("src").toUri()).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder().addSourceSets(main)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();
        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration);
        type = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult().findType("a.T");
        maps(type).forEach(map -> {
            map.set(INTRINSIC, ValueImpl.BoolImpl.TRUE);
            map.set(PARSE_TIME, ValueImpl.BoolImpl.TRUE);
        });
    }

    // the type, the method, its first statement, and the field's initializer
    private static List<PropertyValueMap> maps(TypeInfo typeInfo) {
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        Statement first = m.methodBody().statements().getFirst();
        FieldInfo k = typeInfo.getFieldByName("k", true);
        return List.of(typeInfo.analysis(), m.analysis(), first.analysis(), k.analysisOfInitializer());
    }

    @DisplayName("a plain rewire (a transform's) drops the intrinsic tier, and keeps the other carried tiers")
    @Test
    public void testPlainRewire() {
        InfoMap infoMap = javaInspector.runtime().newInfoMap(Set.of(type));
        assertFalse(infoMap.carryIntrinsic());
        TypeInfo rewired = infoMap.rewireAll().iterator().next();
        assertNotSame(type, rewired);
        List<PropertyValueMap> maps = maps(rewired);
        for (int i = 0; i < maps.size(); i++) {
            assertFalse(maps.get(i).haveAnalyzedValueFor(INTRINSIC), "map " + i);
            assertTrue(maps.get(i).haveAnalyzedValueFor(PARSE_TIME), "map " + i);
        }
        // as prep would do on the result: a second write must not meet a carried value
        maps.forEach(map -> assertDoesNotThrow(() -> map.set(INTRINSIC, ValueImpl.BoolImpl.TRUE)));
    }

    @DisplayName("a partial re-parse's rewire carries the intrinsic tier")
    @Test
    public void testReparseRewire() {
        InfoMap infoMap = javaInspector.runtime().newInfoMap(Set.of(type), Set.of(), true);
        assertTrue(infoMap.carryIntrinsic());
        TypeInfo rewired = infoMap.rewireAll().iterator().next();
        assertNotSame(type, rewired);
        List<PropertyValueMap> maps = maps(rewired);
        for (int i = 0; i < maps.size(); i++) {
            assertTrue(maps.get(i).haveAnalyzedValueFor(INTRINSIC), "map " + i);
            assertTrue(maps.get(i).haveAnalyzedValueFor(PARSE_TIME), "map " + i);
        }
    }
}
