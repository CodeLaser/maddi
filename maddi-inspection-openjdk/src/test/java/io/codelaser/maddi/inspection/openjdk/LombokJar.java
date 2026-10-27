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
