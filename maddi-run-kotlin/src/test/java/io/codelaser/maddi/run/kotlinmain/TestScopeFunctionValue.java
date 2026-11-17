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
 * A scope function given a FUNCTION VALUE instead of a literal (CodeLaser/maddi#98): `x?.let(::URI)`,
 * `s?.let(xs::add)`, `sb.apply(f)`. kotlinc inlines the one call the value makes, so this does too, through the same
 * lowering as a literal: a modification the function makes of the receiver is then the receiver's, not a lambda
 * parameter's. A property reference (`String::length`) stays a call.
 */
public class TestScopeFunctionValue {
    @Test
    public void aFunctionValueIsCalledDirectly(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Files.createDirectories(kDir.resolve("a"));
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import java.net.URI
                class K(val sb: StringBuilder, val xs: MutableList<String>) {
                    fun ctor(s: String?): URI? = s?.let(::URI)
                    fun bound(s: String?): Boolean? = s?.let(xs::add)
                    fun self(s: String): Int = s.let(this::len)
                    fun len(s: String): Int = s.length
                    fun unbound(s: String): Int = s.let(String::length)
                    fun variable(s: String, f: (String) -> Int): Int = s.let(f)
                    fun runs(f: StringBuilder.() -> Unit) { sb.run(f) }
                    fun applies(f: StringBuilder.() -> Unit): StringBuilder = sb.apply(f)
                    fun also(s: String) { s.also(xs::add) }
                    fun withs(f: StringBuilder.() -> Int): Int = with(sb, f)
                }
                """);
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).build();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build());
        assertEquals(0, PlaceholderCensus.of(parsed.getKotlinTypes()).getTotal(),
                PlaceholderCensus.of(parsed.getKotlinTypes()).getByKind().toString());
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> "a.K".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        assertEquals(String.join("\n", List.of(
                "ctor: {URI $let0=null;if(!(s==null)){$let0=new URI(s);}return $let0;}",
                "bound: {Boolean $let1=null;if(!(s==null)){$let1=this.xs.add(s);}return $let1;}",
                "self: {int $let2;{$let2=this.len(s);}return $let2;}",
                "unbound: {return StandardKt.let(s,String::length);}",
                "variable: {int $let3;{$let3=f.invoke(s);}return $let3;}",
                "runs: {{f.invoke(this.sb);}}",
                "applies: {{f.invoke(this.sb);}return this.sb;}",
                "also: {{this.xs.add(s);}}",
                "withs: {int $with4;{$with4=f.invoke(this.sb);}return $with4;}")),
                String.join("\n", List.of("ctor", "bound", "self", "unbound", "variable", "runs", "applies", "also", "withs").stream().map(n -> n + ": " + body(k, n)).toList()));
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
