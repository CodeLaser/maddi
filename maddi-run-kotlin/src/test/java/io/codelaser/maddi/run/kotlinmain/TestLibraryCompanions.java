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
package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A library class NAME used as a value is its companion (`ClassId.fromString(…)`, 18× on detekt;
 * `CompilerConfigurationKey.create(…)`, 12×) or, for an `object`, the object itself. The K2-built model of a Kotlin
 * library class had no static `Companion` field to read it through, so the receiver was a placeholder while the
 * companion's member resolved. And a class name CALLED, `RuleSet(id, rules)` (12× on detekt), is its companion's
 * `operator fun invoke`, not a constructor.
 */
public class TestLibraryCompanions {
    @Test
    public void theNameIsTheCompanion(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import org.jetbrains.kotlin.name.ClassId
                import org.jetbrains.kotlin.name.FqName
                import org.jetbrains.kotlin.config.CompilerConfigurationKey
                class K {
                    fun a(): ClassId = ClassId.fromString("a/B")
                    fun b(): ClassId = ClassId.topLevel(FqName("a.B"))
                    fun c(): CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create<Boolean>("x")
                    fun d(): String = Regex.escape("x")
                    fun e(): Regex = Regex.fromLiteral("x")
                    fun f(): Any = Charsets
                    fun g(): Sized = Sized(listOf("a"))
                    fun h(): Int = Twice(3)
                }
                class Sized(val n: Int) {
                    companion object { operator fun invoke(items: List<String>): Sized = Sized(items.size) }
                }
                object Twice { operator fun invoke(i: Int): Int = i * 2 }
                class Unused {
                }
                """);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        String cp = System.getProperty("maddi.k2.classpath", "");
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet)
                .addClassPath(jar(cp, "kotlin-stdlib-\\d[^-]*\\.jar")).addClassPath(jar(cp, "kotlin-compiler-\\d[^-]*\\.jar")).build());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), String.join("\n", census.dumpLines()));
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> t.simpleName().equals("K")).findFirst().orElseThrow();
        StringBuilder actual = new StringBuilder();
        k.methods().forEach(m -> actual.append(m.name()).append(": ").append(m.methodBody().statements()).append('\n'));
        // ⚠ `fromString`'s omitted default is filled in as a literal: pre-existing, not this route's
        assertEquals("""
                a: [return ClassId.Companion.fromString("a/B",false);]
                b: [return ClassId.Companion.topLevel(new FqName("a.B"));]
                c: [return CompilerConfigurationKey.Companion.create("x");]
                d: [return Regex.Companion.escape("x");]
                e: [return Regex.Companion.fromLiteral("x");]
                f: [return Charsets.INSTANCE;]
                g: [return Sized.Companion.invoke(CollectionsKt__CollectionsJVMKt.listOf("a"));]
                h: [return Twice.INSTANCE.invoke(3);]
                """, actual.toString());
    }

    private static String jar(String classPath, String pattern) {
        return Stream.of(classPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches(pattern)).findFirst().orElseThrow();
    }
}
