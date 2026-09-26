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
 * An {@code @InlineOnly} stdlib member has no method in the class file -- kotlinc inlines its body -- so the kotlin
 * archive has nothing to contract, and called as K2 names it the call reads as an UNCONTRACTED method: modifying its
 * receiver and arguments. The library-call census found 492 such calls with a mutable argument over detekt, coil
 * and javalin. Each becomes the class-file call its inlined body makes, which an archive contracts:
 * {@code c.isNotEmpty()} is {@code !c.isEmpty()}, {@code x.matches(r)} is {@code r.matches(x)}, {@code error(m)} a
 * throw, {@code require(c) { m }} an {@code if (!c) throw}, {@code c += x} is {@code c.add(x)}.
 */
public class TestInlineOnlyLowering {
    @Test
    public void theCallKotlincInlines(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import java.nio.file.Path
                import kotlin.io.path.absolute
                import kotlin.io.path.exists
                class K(private val s: List<String>, private val m: Map<String, Int>) {
                    fun ne(): Boolean = s.isNotEmpty()
                    fun oe(l: List<String>?): List<String> = l.orEmpty()
                    fun ino(l: List<String>?): Boolean = l.isNullOrEmpty()
                    fun has(k: String): Boolean = k in m
                    fun fd(): String? = s.find { it.length > 1 }
                    fun mt(r: Regex, x: String): Boolean = x.matches(r)
                    fun ct(r: Regex, x: String): Boolean = x.contains(r)
                    fun rp(r: Regex, x: String): String = x.replace(r, "y")
                    fun fm(x: Int): String = "%d".format(x)
                    fun lc(x: String): String = x.lowercase()
                    fun uc(x: String, l: java.util.Locale): String = x.uppercase(l)
                    fun pl(x: Any) { println(x) }
                    fun ex(p: Path): Boolean = p.exists()
                    fun ab(p: Path): Path = p.absolute()
                    fun er(x: String?): String = x ?: error("none")
                    fun rq(x: Int) { require(x > 0) { "positive $x" } }
                    fun ck(x: Int) { check(x > 0) }
                    fun rn(x: String?): String = requireNotNull(x)
                    fun pa(c: MutableList<String>) { c += "x" }
                    fun ma(c: MutableSet<String>) { c -= "x" }
                    fun ce(x: CharSequence): Boolean = x.isEmpty()
                    fun cn(x: CharSequence): Boolean = x.isNotEmpty()
                    fun cq(x: String?): Boolean = x?.isNotEmpty() == true
                    fun sn(x: String?): Boolean = x.isNullOrEmpty()
                    fun ci(r: Regex, x: CharSequence): Boolean = r in x
                    fun cs(r: Regex, x: String?): Boolean = x?.contains(r) ?: false
                    fun af(a: Array<String>): String? = a.find { it.length > 1 }
                    fun eb() { error("boom") }
                    fun td(): Int = TODO()
                }
                """);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        String cp = System.getProperty("maddi.k2.classpath", "");
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet)
                .addClassPath(jar(cp, "kotlin-stdlib-\\d[^-]*\\.jar")).build());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), String.join("\n", census.dumpLines()));
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> t.simpleName().equals("K")).findFirst().orElseThrow();
        StringBuilder actual = new StringBuilder();
        k.methods().stream().filter(m -> !m.isSynthetic() && !m.name().startsWith("get"))
                .forEach(m -> actual.append(m.name()).append(": ").append(m.methodBody().statements()).append('\n'));
        assertEquals("""
                ne: [return !this.s.isEmpty();]
                oe: [return l==null?CollectionsKt__CollectionsKt.emptyList():l;]
                ino: [return l==null||l.isEmpty();]
                has: [return this.m.containsKey(k);]
                fd: [return CollectionsKt___CollectionsKt.firstOrNull(this.s,it->it.length()>1);]
                mt: [return r.matches(x);]
                ct: [return r.containsMatchIn(x);]
                rp: [return r.replace(x,"y");]
                fm: [return String.format("%d",x);]
                lc: [return x.toLowerCase(Locale.ROOT);]
                uc: [return x.toUpperCase(l);]
                pl: [System.out.println(x);]
                ex: [return Files.exists(p);]
                ab: [return p.toAbsolutePath();]
                er: [if(x==null){throw new IllegalStateException(String.valueOf("none"));}, return x;]
                rq: [if(!(x>0)){throw new IllegalArgumentException(String.valueOf("positive "+x));}]
                ck: [if(!(x>0)){throw new IllegalStateException("Check failed.");}]
                rn: [return Objects.requireNonNull(x);]
                pa: [c.add("x");]
                ma: [c.remove("x");]
                ce: [return x.length()==0;]
                cn: [return !(x.length()==0);]
                cq: [return (x==null?null:!(x.length()==0)).equals(true);]
                sn: [return x==null||x.length()==0;]
                ci: [return r.containsMatchIn(x);]
                cs: [Boolean $nullSafe0=x==null?null:r.containsMatchIn(x);, return $nullSafe0==null?false:$nullSafe0;]
                af: [return ArraysKt___ArraysKt.firstOrNull(a,it->it.length()>1);]
                eb: [throw new IllegalStateException(String.valueOf("boom"));]
                td: [throw new NotImplementedError("An operation is not implemented.");]
                """, actual.toString());
    }

    private static String jar(String classPath, String pattern) {
        return Stream.of(classPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches(pattern)).findFirst().orElseThrow();
    }
}
