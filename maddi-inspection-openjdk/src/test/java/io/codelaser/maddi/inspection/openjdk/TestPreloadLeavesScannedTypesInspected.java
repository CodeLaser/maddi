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
