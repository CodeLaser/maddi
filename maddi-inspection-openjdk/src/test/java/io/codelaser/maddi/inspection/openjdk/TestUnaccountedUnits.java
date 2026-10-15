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
        Path a = dir.resolve("a"), b = dir.resolve("b");
        write(a, "a/A.java", "package a;\npublic class A { public static int f() { return 1; } }\n");
        write(b, "b/Uses.java", "package b;\nimport a.A;\npublic class Uses { int g() { return A.f(); } }\n");
        write(b, "b/Plain.java", "package b;\npublic class Plain { }\n");
        write(b, "b/Comments.java", "// nothing here\n");
        SourceSet ssA = new SourceSetImpl.Builder().setName("a").setSourceDirectories(List.of(a))
                .setUri(a.toUri()).build();
        SourceSet ssB = new SourceSetImpl.Builder().setName("b").setSourceDirectories(List.of(b))
                .setUri(b.toUri()).build(); // no dependency on a
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
