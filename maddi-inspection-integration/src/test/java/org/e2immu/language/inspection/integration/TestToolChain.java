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

package org.e2immu.language.inspection.integration;

import ch.qos.logback.classic.Level;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestToolChain {

    public static final String JMOD_BASE = "jmod:java.base";
    public static final String BASE = "java.base";

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void testLinux() {
        assertEquals("openjdk-23", ToolChain.extractJdkName(
                "jar:file:/usr/lib/jvm/java-23-openjdk-arm64/jmods/java.base.jmod!/classes/java/io/BufferedInputStream.class"));
    }

    @Test
    public void test() throws IOException {
        InputConfigurationImpl.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addSources("none")
                .addClassPath(JMOD_BASE);
        InputConfiguration inputConfiguration = inputConfigurationBuilder.build();
        SourceSet base = inputConfiguration.classPathParts().getFirst();
        assertEquals(BASE, base.name());
        assertEquals(JMOD_BASE, base.uri().toString());

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);

        List<TypeInfo> typesLoaded = javaInspector.compiledTypesManager().typesLoaded(true);
        assertSame(base, typesLoaded.getFirst().compilationUnit().sourceSet());

        String s = ToolChain.extractLibraryName(typesLoaded, false);
        assertTrue(s.contains("jdk-"), "Have "+s);
    }
}
