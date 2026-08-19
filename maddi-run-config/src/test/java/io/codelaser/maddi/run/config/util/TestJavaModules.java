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

package io.codelaser.maddi.run.config.util;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The JDK class path parts both build plugins contribute. Each used to build them itself, and each
 * stated its own default -- which is how one plugin came to mean {@code java.se} by it and the other
 * {@code java.base}.
 */
public class TestJavaModules {

    /**
     * ⛔ The other half of the shared construction: {@code jmods} unset must mean the whole {@code java.se}
     * closure. It meant {@code java.base} alone in the Gradle plugin and {@code java.se} in the Maven plugin,
     * because each stated its own default.
     */
    @Test
    public void jmodDefaultIsJavaSeInBothPlugins() {
        List<String> unset = names(JavaModules.javaModuleSourceSets(null));
        assertEquals(names(JavaModules.javaModuleSourceSets(JavaModules.DEFAULT_JMODS)), unset);
        assertEquals(names(JavaModules.javaModuleSourceSets("  ")), unset);

        assertTrue(unset.size() > 15, "expected the java.se closure, got " + unset);
        assertTrue(unset.containsAll(List.of("java.base", "java.se", "java.xml", "java.sql", "java.desktop",
                "java.logging", "java.compiler")), unset.toString());
        assertEquals(unset.stream().sorted().toList(), unset, "the order must be stable across runs");

        // an explicit setting is still honoured, and still closed over
        List<String> explicit = names(JavaModules.javaModuleSourceSets("java.base"));
        assertEquals(List.of("java.base"), explicit);
    }

    /** Every JDK part carries the {@code jmod:} scheme JavaInspectorImpl dispatches on, and the JDK flags. */
    @Test
    public void jmodSourceSetShape() {
        SourceSet jmod = JavaModules.jmodSourceSet("java.xml");
        assertEquals("java.xml", jmod.name());
        assertEquals("jmod:java.xml", jmod.uri().toString());
        assertTrue(jmod.partOfJdk());
        assertTrue(jmod.isModule());
        assertTrue(jmod.library());
        assertTrue(jmod.externalLibrary());
        assertEquals(List.of(), jmod.sourceDirectories());
    }

    /** Guards the assumption the tests above rest on: {@link Set} iteration order is not what orders the output. */
    @Test
    public void closureIsASet() {
        assertEquals(Set.copyOf(JavaModules.jmodsFromString("java.se")),
                Set.copyOf(names(JavaModules.javaModuleSourceSets("java.se"))));
    }

    private static List<String> names(List<SourceSet> sets) {
        return sets.stream().map(SourceSet::name).toList();
    }
}
