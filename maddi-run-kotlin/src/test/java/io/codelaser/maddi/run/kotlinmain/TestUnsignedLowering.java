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
 * <b>Unsigned types, with {@code kotlin.UInt} a class-file shell</b> -- as it is once Java code names it, which is
 * how Exposed's unsigned column types meet it (maddi#220). A {@code UInt} is an {@code int} on the JVM, and its
 * conversions and comparisons are {@code @InlineOnly}: no member to bind to in either model. Each is lowered to the
 * arithmetic kotlinc inlines, on the value's bits (a cast to the primitive of its width); a constant is its signed
 * bit pattern, typed as the unsigned class.
 */
public class TestUnsignedLowering {
    @Test
    public void unsignedConstantsConversionsAndComparisonsAreLowered(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                class K {
                    fun c1(): UInt = 1u
                    fun c2(): Any = 1uL
                    fun c3(): UShort = 7u
                    fun l(v: UInt): Long = v.toLong()
                    fun i(v: UShort): Int = v.toInt()
                    fun s(v: UByte): Short = v.toShort()
                    fun cmp(v: ULong): Boolean = v >= 0uL
                    fun list(): List<Any> = listOf(1u)
                }
                """);
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\n// names the unsigned classes, so that they are class-file shells for the Kotlin side\npublic class Trivial { public Object u(kotlin.UInt x, kotlin.UShort y, kotlin.UByte z, kotlin.ULong w) { return x; } }\n");
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build());
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> "a.K".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        assertEquals(0, PlaceholderCensus.of(parsed.getKotlinTypes()).getTotal(),
                PlaceholderCensus.of(parsed.getKotlinTypes()).getByKind().toString());
        assertEquals("{return (UInt)1;}", body(k, "c1"));
        assertEquals("{return (ULong)1L;}", body(k, "c2"));
        assertEquals("{return (UShort)7;}", body(k, "c3"));
        assertEquals("{return Integer.toUnsignedLong((int)v);}", body(k, "l"));
        assertEquals("{return Short.toUnsignedInt((short)v);}", body(k, "i"));
        assertEquals("{return (short)Byte.toUnsignedInt((byte)v);}", body(k, "s"));
        assertEquals("{return Long.compareUnsigned((long)v,(long)(ULong)0L)>=0;}", body(k, "cmp"));
        assertEquals("{return CollectionsKt.listOf((UInt)1);}", body(k, "list"));
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
