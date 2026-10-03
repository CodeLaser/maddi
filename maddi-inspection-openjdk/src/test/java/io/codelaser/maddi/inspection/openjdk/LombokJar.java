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
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A Lombok jar as a dependency of a parsed source set. The real one comes from the build ({@code maddi.test.lombokJar},
 * see build.gradle.kts): it is on the class path maddi parses and NOT on the test JVM's, so javac loads the
 * processor from the source set's jar, as in production.
 */
final class LombokJar {

    private LombokJar() {
    }

    /** The real Lombok, from the build. */
    static SourceSet sourceSet(SourceSet javaBase) {
        String jar = System.getProperty("maddi.test.lombokJar");
        if (jar == null || !Files.isRegularFile(Path.of(jar))) {
            throw new IllegalStateException("system property maddi.test.lombokJar must name the Lombok jar; got " + jar);
        }
        return sourceSet(Path.of(jar), javaBase);
    }

    /** Any jar, as maddi sees a library dependency; a name starting with "lombok-" is what makes it run as Lombok. */
    static SourceSet sourceSet(Path jar, SourceSet javaBase) {
        return new SourceSetImpl.Builder().setName(jar.getFileName().toString()).setUri(jar.toUri())
                .setLibrary(true).setExternalLibrary(true).setDependencies(List.of(javaBase)).build();
    }
}
