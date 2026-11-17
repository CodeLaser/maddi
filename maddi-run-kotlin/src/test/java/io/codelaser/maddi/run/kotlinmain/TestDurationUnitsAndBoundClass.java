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
 * Three shapes from Exposed (maddi#220), in the class-file world:
 * <ul>
 *     <li>{@code n.milliseconds}, imported from {@code Duration.Companion}: an @InlineOnly getter, private in the class
 *     file, inlined by kotlinc to {@code DurationKt.toDuration(n, DurationUnit.MILLISECONDS)};</li>
 *     <li>{@code Duration.parse(s)}, with the Java side naming {@code Duration.Companion} and minting a stub for it,
 *     which K2 does not take for its model (CodeLaser/maddi#76);</li>
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
                    fun parsed(s: String): kotlin.time.Duration = kotlin.time.Duration.parse(s)
                    fun <S : Any> typed(s: S): String? = s::class.qualifiedName
                    fun inLambda(xs: List<Any>): List<String?> = xs.map { it::class.simpleName }
                }
                """);
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\n// names Duration.Companion: without the stdlib on its own class path, the Java side mints a STUB for it\npublic class Trivial { public Object c(kotlin.time.Duration.Companion c) { return c; } }\n");
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
        // a stub the Java side minted for Duration.Companion is not K2's model (CodeLaser/maddi#76)
        assertEquals("{return Duration.Companion.parse(s);}", body(k, "parsed"));
        assertEquals("{return Reflection.getOrCreateKotlinClass(s.getClass()).qualifiedName;}", body(k, "typed"));
        assertEquals("{return CollectionsKt.map(xs,it->Reflection.getOrCreateKotlinClass(it.getClass()).simpleName);}",
                body(k, "inLambda"));
    }

    /**
     * The same call in a Kotlin-only project: there {@code Duration.Companion} is the stdlib CLASS FILE's type, whose
     * method is {@code parse-UwyO8pc} -- kotlinc mangles a function that takes or returns an inline class.
     */
    @Test
    public void durationParseInAKotlinOnlyProject(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Files.createDirectories(kDir.resolve("a"));
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import kotlin.time.Duration
                class K {
                    fun parsed(s: String): Duration = Duration.parse(s)
                    fun orNull(s: String): Duration? = Duration.parseOrNull(s)
                    fun dflt(value: Any): Duration = when (value) { is String -> Duration.parse(value); else -> Duration.ZERO }
                }
                """);
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).build();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build());
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> "a.K".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        assertEquals(0, PlaceholderCensus.of(parsed.getKotlinTypes()).getTotal(),
                PlaceholderCensus.of(parsed.getKotlinTypes()).getByKind().toString());
        // the class file's JVM name: what the call binds to
        assertEquals("{return Duration.Companion.parse-UwyO8pc(s);}", body(k, "parsed"));
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
