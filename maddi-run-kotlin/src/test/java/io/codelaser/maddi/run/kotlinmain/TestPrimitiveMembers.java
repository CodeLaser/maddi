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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.MethodCall;
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
 * A member of a Kotlin primitive (`i.toString()` 72x on detekt, `b.not()` 17x) has no Java declaration to bind to; each
 * becomes the Java a human writes, which is also what kotlinc compiles (javap, 2.4.0) but for `compareTo`
 * (kotlinc: `Intrinsics.compare`) and `equals` (kotlinc boxes both sides). A nullable receiver is boxed, and keeps
 * binding to the box's own member. `arrayOf(…)` (an intrinsic, no bytecode) is `new T[]{…}`.
 */
public class TestPrimitiveMembers {
    @Test
    public void eachBecomesTheJavaAHumanWrites(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                class K {
                    fun d(i: Int): Double = i.toDouble()
                    fun l(i: Int): Long = i.toLong()
                    fun s(i: Int): String = i.toString()
                    fun sd(x: Double): String = x.toString()
                    fun sb(b: Boolean): String = b.toString()
                    fun n(b: Boolean): Boolean = b.not()
                    fun c(i: Int, j: Int): Int = i.compareTo(j)
                    fun p(i: Int, j: Int): Int = i.plus(j)
                    fun h(i: Int): Int = i.hashCode()
                    fun e(i: Int, j: Int): Boolean = i.equals(j)
                    fun sn(i: Int?): String = i.toString()
                    fun cs(c: Char): String = c.toString()
                    fun ci(c: Char): Int = c.code
                    fun arr(): Array<String> = arrayOf("a", "b")
                    fun ints(): IntArray = intArrayOf(1, 2)
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
        assertEquals(0, census.getTotal(), String.join("\n", census.dumpLines()));
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> t.simpleName().equals("K")).findFirst().orElseThrow();
        StringBuilder actual = new StringBuilder();
        k.methods().forEach(m -> {
            StringBuilder calls = new StringBuilder();
            m.methodBody().visit((Element e) -> {
                if (e instanceof MethodCall mc) calls.append(" -> ").append(mc.methodInfo().fullyQualifiedName());
                return true;
            });
            actual.append(m.name()).append(": ").append(m.methodBody().statements()).append(calls).append('\n');
        });
        assertEquals("""
                d: [return (double)i;]
                l: [return (long)i;]
                s: [return String.valueOf(i);] -> java.lang.String.valueOf(int)
                sd: [return String.valueOf(x);] -> java.lang.String.valueOf(double)
                sb: [return String.valueOf(b);] -> java.lang.String.valueOf(boolean)
                n: [return !b;]
                c: [return Integer.compare(i,j);] -> java.lang.Integer.compare(int,int)
                p: [return i+j;]
                h: [return Integer.hashCode(i);] -> java.lang.Integer.hashCode(int)
                e: [return i==j;]
                sn: [return i.toString();] -> java.lang.Integer.toString()
                cs: [return String.valueOf(c);] -> java.lang.String.valueOf(char)
                ci: [return CharCodeKt.getCode(c);] -> kotlin.CharCodeKt.getCode(char)
                arr: [return new String[]{"a","b"};]
                ints: [return new int[]{1,2};]
                """, actual.toString());
    }
}
