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

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.java.openjdk.ScanCompilationUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeLaser/maddi#133: every file handed to javac ends as types or as a reported failure. On the gate's audit
 * project two files of a source set whose dependencies lacked a library they imported went missing without any
 * diagnostic, and the analyses behind them read wrong.
 */
public class TestUnaccountedUnits {

    private static void write(Path root, String file, String content) throws IOException {
        Path p = root.resolve(file);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
    }

    @DisplayName("a file importing a type its source set does not depend on is reported, the rest parses")
    @Test
    public void undeclaredDependency(@TempDir Path dir) throws IOException {
        undeclaredDependency(dir, false);
    }

    @DisplayName("the same, with module-info: 'requires a' without the dependency")
    @Test
    public void undeclaredDependencyModule(@TempDir Path dir) throws IOException {
        undeclaredDependency(dir, true);
    }

    private static void undeclaredDependency(Path dir, boolean module) throws IOException {
        Path a = dir.resolve("a"), b = dir.resolve("b");
        write(a, "a/A.java", "package a;\npublic class A { public static int f() { return 1; } }\n");
        write(b, "b/Uses.java", "package b;\nimport a.A;\npublic class Uses { int g() { return A.f(); } }\n");
        write(b, "b/Plain.java", "package b;\npublic class Plain { }\n");
        write(b, "b/Comments.java", "// nothing here\n");
        if (module) {
            write(a, "module-info.java", "module a { exports a; }\n");
            write(b, "module-info.java", "module b { requires a; }\n");
        }
        SourceSet ssA = new SourceSetImpl.Builder().setName("a").setSourceDirectories(List.of(a))
                .setUri(a.toUri()).setModule(module).build();
        SourceSet ssB = new SourceSetImpl.Builder().setName("b").setSourceDirectories(List.of(b))
                .setUri(b.toUri()).setModule(module).build(); // no dependency on a
        InputConfiguration ic = new InputConfigurationImpl.Builder().addSourceSets(ssA, ssB)
                .addClassPath("jmod:java.base").build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);
        Summary summary = javaInspector.parse(Map.of(),
                new JavaInspector.ParseOptions.Builder().setFailFast(false).build());

        assertTrue(summary.types().stream().map(TypeInfo::fullyQualifiedName).anyMatch("b.Plain"::equals));
        URI uses = b.resolve("b/Uses.java").toUri();
        List<Summary.ParseException> all = Stream.concat(summary.parseExceptions().stream(),
                summary.parseWarnings().stream()).toList();
        assertTrue(all.stream().anyMatch(e -> uses.equals(e.uri())), "the dropped unit is reported");
        // #139: an import that is not on the source set's dependencies is unresolved there, a tolerable drop; it
        // used to reach the type of the other source set, and fail as an error on its committed inspection
        assertTrue(summary.parseExceptions().stream().noneMatch(e -> uses.equals(e.uri())),
                () -> summary.parseExceptions().stream().map(Throwable::getMessage).toList().toString());
        // the drop is reported once, by the scan; a file of comments is no loss
        assertTrue(all.stream().noneMatch(e -> e.getMessage().contains("no type was produced")),
                () -> all.stream().map(Throwable::getMessage).toList().toString());
    }

    @DisplayName("a unit that is neither a type's, nor a failure, nor typeless is unaccounted")
    @Test
    public void unaccounted() {
        URI typeless = URI.create("file:/src/b/Comments.java");
        URI lost = URI.create("file:/src/b/Lost.java");
        URI failed = URI.create("file:/src/b/Failed.java");
        ScanCompilationUnits.Result result = new ScanCompilationUnits.Result(List.of(), List.of(), List.of(),
                List.of(new ScanCompilationUnits.CompilationUnitFailure(failed, "detail", true, null)),
                Set.of(typeless));
        List<URI> handed = List.of(typeless, lost, failed, URI.create("file:/src/module-info.java"),
                URI.create("file:/src/b/package-info.java"));
        assertEquals(List.of(lost), JavaInspectorImpl.unaccounted(handed, result));
    }
}
