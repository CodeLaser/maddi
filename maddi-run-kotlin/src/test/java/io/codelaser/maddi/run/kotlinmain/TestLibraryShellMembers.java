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
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ⛔ A class-file type the Java front end had registered as a SHELL -- loaded hierarchy-only because another type's
 * signature named it ({@code File.toPath()} names {@code java.nio.file.Path}) -- reached the Kotlin converter with no
 * members at all. The Java front end completes its shells in a batch when its parse commits; in a mixed project the
 * Kotlin parse runs after that. So {@code p.toUri()}, {@code URI("x")}, {@code Path::toUri} and {@code ::URI} were
 * all placeholders, while {@code java.util} types, preloaded whole, resolved -- which is why the fixtures that use
 * {@code ArrayList} never showed it. Fixed by completing a shell where its members are looked up
 * ({@code CompiledTypesManager.typeWithMembers}).
 *
 * <p>Also the extension-function reference {@code String::toRegex}: the facade's static method, receiver first.
 */
public class TestLibraryShellMembers {

    private static final String KOTLIN = """
            package a
            import java.nio.file.Path
            import java.net.URI
            class K {
                fun call(p: Path): URI = p.toUri()
                fun ctor(): URI = URI("x")
                fun refMember(l: List<Path>): List<URI> = l.map(Path::toUri)
                fun refCtor(l: List<String>): List<URI> = l.map(::URI)
                fun refExtension(l: List<String>): List<Regex> = l.map(String::toRegex)
            }
            """;

    @Test
    public void membersOfAClassFileShellResolve(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("s"));
        Files.writeString(kDir.resolve("a/K.kt"), KOTLIN);
        Files.writeString(jDir.resolve("s/Trivial.java"), "package s;\npublic class Trivial { public int n; }\n");
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).setDependencies(List.of(javaSet)).build();
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build());

        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), "every shape must convert: " + census.dumpLines());

        // ⛔ identity: each must name the REAL member, not merely something that is not a placeholder
        TypeInfo k = parsed.getKotlinTypes().stream().filter(t -> "a.K".equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow();
        assertEquals(List.of("java.nio.file.Path.toUri()"), targets(k, "call"));
        assertEquals(List.of("java.net.URI.<init>(String)"), targets(k, "ctor"));
        assertEquals(List.of("java.nio.file.Path.toUri()"), targets(k, "refMember"));
        assertEquals(List.of("java.net.URI.<init>(String)"), targets(k, "refCtor"));
        assertEquals(List.of("kotlin.text.StringsKt__RegexExtensionsKt.toRegex(String)"), targets(k, "refExtension"));
    }

    /** The methods a method's body calls, constructs or references, in order. */
    private static List<String> targets(TypeInfo type, String name) {
        MethodInfo m = type.methods().stream().filter(x -> name.equals(x.name())).findFirst().orElseThrow();
        List<String> found = new ArrayList<>();
        m.methodBody().visit(e -> {
            if (e instanceof MethodCall mc && !"map".equals(mc.methodInfo().name())) found.add(mc.methodInfo().fullyQualifiedName());
            if (e instanceof ConstructorCall cc && cc.constructor() != null) found.add(cc.constructor().fullyQualifiedName());
            if (e instanceof MethodReference mr) found.add(mr.methodInfo().fullyQualifiedName());
            return true;
        });
        return found;
    }

    private static String kotlinStdlibJar() {
        String cp = System.getProperty("maddi.k2.classpath", "");
        return Stream.of(cp.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst().orElseThrow(() -> new AssertionError("no kotlin-stdlib jar on " + cp));
    }
}
