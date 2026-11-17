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
 * Two shapes from Exposed (maddi#220), in the class-file world:
 * <ul>
 *     <li>{@code n.milliseconds}, imported from {@code Duration.Companion}: an @InlineOnly getter, private in the class
 *     file, inlined by kotlinc to {@code DurationKt.toDuration(n, DurationUnit.MILLISECONDS)};</li>
 *     <li>{@code x::class} on a value typed by a type parameter, or on a lambda's {@code it}: {@code Object.getClass()}
 *     on it, wrapped in {@code Reflection.getOrCreateKotlinClass}.</li>
 * </ul>
 */
public class TestDurationUnitsAndBoundClass {
    @Test
    public void durationUnitsAndBoundClassLiteralsConvert(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import kotlin.time.Duration.Companion.milliseconds
                import kotlin.time.Duration.Companion.nanoseconds
                class K {
                    fun imported(n: Long): kotlin.time.Duration = n.milliseconds
                    fun nanos(n: Long): kotlin.time.Duration = n.nanoseconds
                    fun <S : Any> typed(s: S): String? = s::class.qualifiedName
                    fun inLambda(xs: List<Any>): List<String?> = xs.map { it::class.simpleName }
                }
                """);
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build());
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> "a.K".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        assertEquals(0, PlaceholderCensus.of(parsed.getKotlinTypes()).getTotal(),
                PlaceholderCensus.of(parsed.getKotlinTypes()).getByKind().toString());
        assertEquals("{return DurationKt.toDuration(n,DurationUnit.MILLISECONDS);}", body(k, "imported"));
        assertEquals("{return DurationKt.toDuration(n,DurationUnit.NANOSECONDS);}", body(k, "nanos"));
        assertEquals("{return Reflection.getOrCreateKotlinClass(s.getClass()).qualifiedName;}", body(k, "typed"));
        assertEquals("{return CollectionsKt.map(xs,it->Reflection.getOrCreateKotlinClass(it.getClass()).simpleName);}",
                body(k, "inLambda"));
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
