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

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * A preload must survive a class-path part it cannot attribute.
 * <p>
 * {@link io.codelaser.maddi.java.openjdk.ClassSymbolScanner}{@code .ensureSourceSet} attributes a loaded
 * {@code jar:file:…!/…} class file by looking the JAR'S FILE NAME up among the configured source-set names —
 * which is why {@link SourceSetImpl#sourceSetOf} derives the name with {@code tail(uri)}, and why
 * {@link TestJavaInspector2JarOnClasspath} takes such care to use the real file name including its version.
 * A class-path part built by hand with a friendlier name puts the jar on javac's class path while leaving it
 * unattributable to maddi, and every type in it resolves to "off-classpath".
 * <p>
 * That is a supported outcome: {@code lazilyLoadPrimaryTypeFromClassFile} returns {@code null} so callers can
 * treat it as a miss. {@code ScanCompilationUnits.preload} did not check, and dereferenced it — so a merely
 * misnamed class-path entry surfaced as
 * {@code NullPointerException: Cannot invoke "TypeInfo.hasBeenInspected()" because "newTypeInfo" is null},
 * thrown from a stack that names neither the package nor the misconfiguration. Found while wiring a mixed
 * Java+Kotlin fixture, where it cost a long detour into whether the mixed pipeline could read source contracts
 * at all — it can; the source set was misnamed.
 * <p>
 * The misnaming itself stays a caller error. This test only pins that it is reported as a skip rather than as
 * an NPE from inside the scanner.
 */
public class TestPreloadOffClasspath {

    private static URI artifactOf(Class<?> classInThatJar) {
        try {
            return classInThatJar.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new AssertionError("cannot locate the artifact of " + classInThatJar, e);
        }
    }

    @DisplayName("preloading a package whose jar is on the class path under the wrong source-set name skips it")
    @Test
    public void misnamedClassPathPartIsSkippedNotDereferenced() {
        JavaInspector javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();

        // Deliberately NOT the jar's file name: this is the misconfiguration under test. Using
        // SourceSetImpl.sourceSetOf(ImmutableContainer.class) here would name it correctly and attribute fine.
        SourceSet misnamed = new SourceSetImpl.Builder()
                .setName("maddi-annotation")
                .setUri(artifactOf(io.codelaser.maddi.annotation.ImmutableContainer.class))
                .setExternalLibrary(true).build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPathParts(misnamed)
                .build();

        javaInspector.preload("io.codelaser.maddi.annotation."); // before initialize, as the harnesses do
        assertDoesNotThrow(() -> {
            javaInspector.initialize(inputConfiguration);
            javaInspector.onlyPreload();
        });
    }
}
