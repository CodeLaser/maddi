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
 * Destructuring, in a declaration and in a lambda's parameter list. A destructured lambda parameter `(a, b) ->` is
 * one JVM parameter whose entries the body reads first (detekt's `.partition { (_, rule) -> … }`: `rule`,
 * `ruleInstance`, `key`, `value` were ~50 unresolved references). `Map.Entry`'s `component1/2` are stdlib
 * `@InlineOnly` extensions, compiled to `getKey()`/`getValue()`; a `List`'s are `get(N-1)`.
 */
public class TestDestructuring {
    @Test
    public void entriesAreReadFromTheValue(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                data class P(val a: Int, val b: String)
                class K {
                    fun lam(p: P, f: (P) -> String): String = f(p)
                    fun useLam(p: P): String = lam(p) { (a, b) -> b + a }
                    fun useUnder(p: P): String = lam(p) { (_, b) -> b }
                    fun entry(e: Map.Entry<String, Int>, f: (Map.Entry<String, Int>) -> String): String = f(e)
                    fun useEntry(e: Map.Entry<String, Int>): String = entry(e) { (k, v) -> k + v }
                    fun decl(e: Map.Entry<String, Int>): String { val (k, v) = e; return k + v }
                    fun list(l: List<String>): String { val (x, y) = l; return x + y }
                }
                """);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        String cp = System.getProperty("maddi.k2.classpath", "");
        String stdlib = Stream.of(cp.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar")).findFirst().orElseThrow();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(stdlib).build());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> t.simpleName().equals("K")).findFirst().orElseThrow();
        StringBuilder actual = new StringBuilder();
        for (String name : List.of("useLam", "useUnder", "useEntry", "decl", "list")) {
            actual.append(name).append(": ").append(k.findUniqueMethod(name, 1).methodBody().statements()).append('\n');
        }
        assertEquals(0, census.getTotal(), String.join("\n", census.dumpLines()));
        // one creation holding every entry, each variable with its OWN type (the printer shows the first's)
        assertEquals("""
                useLam: [return lam(p,$dstr0->{int a=$dstr0.component1(),b=$dstr0.component2();return b+a;});]
                useUnder: [return lam(p,$dstr0->{String b=$dstr0.component2();return b;});]
                useEntry: [return entry(e,$dstr0->{String k=$dstr0.getKey(),v=$dstr0.getValue();return k+v;});]
                decl: [String k=e.getKey(),v=e.getValue();, return k+v;]
                list: [String x=l.get(0),y=l.get(1);, return x+y;]
                """, actual.toString());
    }
}
