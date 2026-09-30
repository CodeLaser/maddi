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
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A contract in the kotlin archive must reach the method a Kotlin call resolves to. It did not for any generic
 * signature: the K2-built stdlib model created library functions WITHOUT their type parameters, so
 * {@code listOf(vararg T)} was {@code listOf(Object[])}, the archive's {@code listOf(11,T[])} token matched nothing,
 * and the loader skipped it with a WARN. {@code listOf}, {@code setOf}, {@code mutableListOf}, {@code arrayListOf},
 * {@code toMap(.., M)} and more were written, compiled, shipped -- and never applied. Found by the library-call
 * census ({@code -Dmaddi.libraryCallDump}): 46 detekt/javalin calls to a "contracted" {@code listOf(vararg)} read
 * as DEFAULT.
 * <p>
 * Asserted per call site on the callee's {@code ANNOTATED_API}, which every loaded contract carries (a static has no
 * receiver, so {@code NON_MODIFYING_METHOD} is no marker for one). And the generics: a stdlib call keeps its type
 * parameters, so {@code listOf("a")} is a {@code List<T>} of the callee, not a {@code List<Object>}.
 */
public class TestKotlinArchiveAttaches {
    @Test
    public void everyContractedCallReachesItsContract(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(jDir.resolve("b/J.java"), "package b;\npublic class J {}\n");
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                class K {
                    fun l1() = listOf("a")
                    fun l2() = listOf("a", "b")
                    fun l0() = emptyList<String>()
                    fun s1() = setOf(1)
                    fun s2() = setOf(1, 2)
                    fun s0() = emptySet<Int>()
                    fun m2() = mapOf("a" to 1, "b" to 2)
                    fun m0() = emptyMap<String, Int>()
                    fun map(l: List<String>) = l.map { it.length }
                    fun filter(l: List<String>) = l.filter { it.length > 0 }
                    fun each(l: List<String>) { l.forEach { it.hashCode() } }
                    fun any(l: List<String>) = l.any { it.length == 0 }
                    fun all(l: List<String>) = l.all { it.length == 0 }
                    fun none(l: List<String>) = l.none { it.length == 0 }
                    fun has(l: List<String>, s: String?) = s in l
                }
                """);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        String cp = System.getProperty("maddi.k2.classpath", "");
        String jar = Stream.of(cp.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst().orElseThrow(() -> new AssertionError("no kotlin-stdlib on -Dmaddi.k2.classpath"));
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(jar).build());
        new LoadAnalysisResults(parsed.getRuntime(), kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);

        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> t.simpleName().equals("K")).findFirst().orElseThrow();
        TreeMap<String, String> kotlinCallees = new TreeMap<>();
        List<String> uncontracted = new ArrayList<>();
        for (MethodInfo m : k.methods()) {
            m.methodBody().visit(e -> {
                if (e instanceof MethodCall mc && mc.methodInfo().typeInfo().primaryType().packageName() != null
                    && mc.methodInfo().typeInfo().primaryType().packageName().startsWith("kotlin")) {
                    MethodInfo callee = mc.methodInfo();
                    kotlinCallees.putIfAbsent(m.name(), callee.name() + " -> " + callee.returnType());
                    if (!callee.analysis().haveAnalyzedValueFor(PropertyImpl.ANNOTATED_API)) {
                        uncontracted.add(m.name() + ": " + callee.fullyQualifiedName());
                    }
                }
                return true;
            });
        }
        assertEquals(List.of(), uncontracted, "a call the kotlin archive contracts reached an uncontracted method");
        // the generics survive: the callee's own type parameters, not Object
        assertEquals("listOf -> Type java.util.List<T>", kotlinCallees.get("l2"));
        assertEquals("setOf -> Type java.util.Set<T>", kotlinCallees.get("s2"));
        assertEquals("mapOf -> Type java.util.Map<K,V>", kotlinCallees.get("m2"));
        assertEquals("map -> Type java.util.List<R>", kotlinCallees.get("map"));
    }
}
