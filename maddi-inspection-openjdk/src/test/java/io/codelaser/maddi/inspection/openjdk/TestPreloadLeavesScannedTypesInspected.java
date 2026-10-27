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

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A JDK type that the source scan reaches first, outside every preloaded package, is inspected once the parse
 * returns, whatever was preloaded (maddi#119). With a preload, such a type is loaded on the shared-JDK task.
 */
public class TestPreloadLeavesScannedTypesInspected {

    @DisplayName("no preload")
    @Test
    public void noPreload() throws IOException {
        test("");
    }

    @DisplayName("a preload that does not cover java.security")
    @Test
    public void otherPreload() throws IOException {
        test("java.base::java.util.");
    }

    @DisplayName("a preload that covers java.security")
    @Test
    public void coveringPreload() throws IOException {
        test("java.base::java.util.,java.base::java.security");
    }

    private static void test(String preloads) throws IOException {
        JavaInspector javaInspector = new JavaInspectorImpl();
        Arrays.stream(preloads.split(",")).filter(s -> !s.isEmpty()).forEach(javaInspector::preload);
        javaInspector.initialize(new InputConfigurationImpl.Builder()
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build());
        TypeInfo x = javaInspector.parse(Map.of("a.X", """
                package a;
                import java.security.PrivilegedAction;
                class X {
                    static String m() {
                        PrivilegedAction<String> action = () -> "x";
                        return action.run();
                    }
                }
                """), JavaInspectorImpl.DETAILED_SOURCES).parseResult().primaryTypes().stream().findFirst().orElseThrow();
        // the lambda's own view, before anything looks the type up by name (which may load it on demand)
        AtomicReference<TypeInfo> found = new AtomicReference<>();
        x.findUniqueMethod("m", 0).methodBody().visit(e -> {
            if (e instanceof Lambda lambda) found.set(lambda.abstractFunctionalTypeInfo());
            return found.get() == null;
        });
        TypeInfo privilegedAction = found.get();
        assertEquals("java.security.PrivilegedAction", privilegedAction.fullyQualifiedName());
        assertTrue(privilegedAction.hasBeenInspected(), "preloads: " + preloads);
        assertTrue(privilegedAction.isFunctionalInterface(), "preloads: " + preloads);
        assertEquals("run", privilegedAction.singleAbstractMethod().name());
    }
}
