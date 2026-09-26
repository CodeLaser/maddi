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
 * Javalin's Java-interop shapes in the class-file world (the k2 unit fixture builds JDK types from K2, where a Java
 * property is a field, so it cannot see these): assigning a Java synthetic property whose setter is OVERLOADED
 * (jetty's `keyStorePath = …` has `setKeyStorePath(String)` and `(Path)`; kotlinc picks the one taking the getter's
 * type), on an explicit and on an implicit `this` receiver; never the same-named PRIVATE field, which Kotlin cannot
 * see (the fixture's `path` converted to a field write before); and calling the default constructor javac generates for a
 * Java class that declares none (javalin's `WsConfig()`); a Java static imported under an ALIAS (javalin's
 * `import java.lang.Enum.valueOf as enumValueOf`), which is looked up by its declared name.
 */
public class TestJavaSetterOverloads {
    @Test
    public void theSetterTakingTheGettersType(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(jDir.resolve("s/Factory.java"), """
                package s;
                public class Factory {
                    private String path;
                    public String getPath() { return path; }
                    public void setPath(java.nio.file.Path p) { this.path = p.toString(); }
                    public void setPath(String p) { this.path = p; }
                }
                """);
        Files.writeString(jDir.resolve("s/Config.java"), "package s;\npublic class Config { public int n; }\n");
        Files.writeString(kDir.resolve("a/K.kt"), """
                package a
                import s.Config
                import s.Factory
                import java.lang.Integer.parseInt as parse
                class K : Factory() {
                    init { path = "i" }
                    fun a(): Factory = Factory().apply { path = "x" }
                    fun b(f: Factory) { f.path = "y" }
                    fun c(k: java.security.PublicKey) { java.security.cert.X509CertSelector().subjectPublicKey = k }
                    fun d(): Config = Config()
                    fun e(f: Factory): String = f.path
                    fun g(s: String): Int = parse(s)
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
        k.constructors().forEach(m -> actual.append("<init>: ").append(m.methodBody().statements()).append('\n'));
        k.methods().forEach(m -> actual.append(m.name()).append(": ").append(m.methodBody().statements()).append('\n'));
        assertEquals("""
                <init>: [super();, {this.setPath("i");}]
                a: [return StandardKt__StandardKt.apply(new Factory(),$receiver->$receiver.setPath("x"));]
                b: [f.setPath("y");]
                c: [new X509CertSelector().setSubjectPublicKey(k);]
                d: [return new Config();]
                e: [return f.getPath();]
                g: [return Integer.parseInt(s);]
                """, actual.toString());
    }

    private static String jar(String classPath, String pattern) {
        return Stream.of(classPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches(pattern)).findFirst().orElseThrow();
    }
}
