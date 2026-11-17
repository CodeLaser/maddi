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
package io.codelaser.maddi.run.kotlinmain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;

/**
 * {@code ::LinkedHashSet}: a constructor reference through a typealias, {@code kotlin.collections.LinkedHashSet} for
 * {@code java.util.LinkedHashSet}. Its owner is the aliased class. Retrofit's
 * {@code elementToReferencedTypes.getOrPut(serviceType, ::LinkedHashSet)} (CodeLaser/maddi#220).
 */
public class TestTypeAliasConstructorReference {
    @Test
    public void theAliasedClassIsTheOwner(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Files.createDirectories(kDir.resolve("a"));
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                class K {
                    fun refs(m: MutableMap<String, MutableSet<String>>, k: String): MutableSet<String> =
                        m.getOrPut(k, ::LinkedHashSet)
                }
                """);
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).build();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build());
        assertEquals(0, PlaceholderCensus.of(parsed.getKotlinTypes()).getTotal(),
                PlaceholderCensus.of(parsed.getKotlinTypes()).getByKind().toString());
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> "a.K".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        assertEquals("{return MapsKt.getOrPut(m,k,LinkedHashSet::new);}", body(k, "refs"));
    }

    private static String body(TypeInfo type, String name) {
        return type.methods().stream().filter(m -> name.equals(m.name())).findFirst().orElseThrow().methodBody().toString();
    }

    private static String kotlinStdlibJar() {
        String cp = System.getProperty("maddi.k2.classpath", "");
        return Stream.of(cp.split(File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst().orElseThrow(() -> new AssertionError("no kotlin-stdlib jar on " + cp));
    }
}
