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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A package whose {@code package-info} annotation reaches a type of that package, read from class files, with that
 * type the first one asked for: micronaut's {@code io.micronaut.http.client} is {@code @Requires(beans =
 * HttpClientRegistry.class)}, and the registry is {@code HttpClientRegistry<T extends HttpClient>} ("type
 * io.micronaut.http.client.HttpClient committed twice from jar:…HttpClient.class", under -ea, 2026-10-09). Loading
 * the type builds its unit, which reads the package's annotations, whose registry loads the type again before the
 * first load registered it: the inner load registered one instance, the outer one a second from the same class
 * file. The outer load now gets the inner one's instance (InfoByFqn.putIfAbsentFromSameOrigin).
 */
public class TestPackageAnnotationNamesItsOwnType extends CommonTest {

    @TempDir
    private Path tempRoot;

    @Test
    public void typeNamedByItsPackageAnnotationLoadsOnce() throws IOException {
        boolean assertionsOn = false;
        assert assertionsOn = true;
        assertTrue(assertionsOn, "run with -ea, or this test asserts nothing");

        classPathOverride = List.of(fixture().toFile());
        TypeInfo x = scan("a.b.X", "package a.b; public class X { lib.Client c; lib.Other o; }");
        TypeInfo client = x.fields().getFirst().type().typeInfo();
        assertEquals("lib.Client", client.fullyQualifiedName());
        TypeInfo other = x.fields().get(1).type().typeInfo();
        // the rest of the package has its annotations; they name the one Client instance
        assertEquals("[ann.Requires]", other.compilationUnit().packageAnnotations().stream()
                .map(ae -> ae.typeInfo().fullyQualifiedName()).toList().toString());
    }

    private Path fixture() throws IOException {
        Path dir = Files.createTempDirectory(tempRoot, "maddi-package-names-own-type");
        Path ann = Files.createDirectories(dir.resolve("src").resolve("ann"));
        Path lib = Files.createDirectories(dir.resolve("src").resolve("lib"));
        Path out = Files.createDirectory(dir.resolve("classes"));
        Files.writeString(ann.resolve("Requires.java"), """
                package ann;
                import java.lang.annotation.*;
                @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.PACKAGE, ElementType.TYPE})
                public @interface Requires { Class<?>[] beans() default {}; }
                """);
        Files.writeString(lib.resolve("package-info.java"), "@ann.Requires(beans = lib.Registry.class) package lib;");
        Files.writeString(lib.resolve("Client.java"), """
                package lib;
                public interface Client extends AutoCloseable {
                    String retrieve(String uri);
                    @Override default void close() { }
                }
                """);
        Files.writeString(lib.resolve("Registry.java"), "package lib; public interface Registry<T extends Client> { T client(String id); }");
        Files.writeString(lib.resolve("Other.java"), "package lib; public class Other { }");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null, "-d", out.toString(), ann.resolve("Requires.java").toString(),
                lib.resolve("package-info.java").toString(), lib.resolve("Client.java").toString(),
                lib.resolve("Registry.java").toString(), lib.resolve("Other.java").toString());
        assertEquals(0, rc, "fixture must compile");
        return out;
    }
}
