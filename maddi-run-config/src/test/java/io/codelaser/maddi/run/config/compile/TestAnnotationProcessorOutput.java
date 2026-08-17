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

package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ⛔⛔ THE DEFECT: an output directory that becomes a source set stops being a classpath entry, so a
 * {@code .class} in it whose compilation unit is on none of that invocation's source roots belongs to nothing —
 * not source, not library. Those are the annotation processor's products, and on elasticsearch's esql pair that
 * is 2 077 classes lost in silence, surfacing four steps later as <i>"70 parse error(s)"</i>.
 * <p>
 * ⚠ THE FIXTURE IS COMPILED, NOT HAND-WRITTEN, because the question this pass gets wrong is a question about
 * BYTECODE: {@code javac} writes a {@code SourceFile} attribute naming the compilation unit, and that attribute
 * is the only thing that separates a generated class from a class whose source is right there under a different
 * file name. A fixture of empty files could not tell the two apart, which is precisely the bug.
 */
public class TestAnnotationProcessorOutput {

    /**
     * ⛔⛔ THE CASE THAT REFUTES THE FILE-NAME RULE, and it is real code: a non-public top-level class does not
     * need a file of its own name. {@code Secondary} and its anonymous inner class have no {@code Secondary.java},
     * so "is there a .java called like me" calls both generated — measured on timefold-solver, where
     * {@code Target_com_networknt_schema_regex_RegularExpression} lives in {@code JsonSchemaSubstitutions.java}.
     * Copying it into a library would put one fully qualified name in two places at once.
     */
    private static final String HANDWRITTEN = """
            package p;
            public class Handwritten {
                Runnable r = new Runnable() { public void run() {} };
                static class Inner {}
            }
            class Secondary {
                Runnable r = new Runnable() { public void run() {} };
            }
            """;

    /** Stands in for what an annotation processor writes: compiled into the destination, source on no root. */
    private static final String GENERATED = """
            package p;
            public class Generated {
                static class Inner {}
            }
            """;

    private record Fixture(Path source, Path generatedSource, Path destination) {
    }

    /**
     * Compiles {@code src/p/Handwritten.java} and {@code gen/p/Generated.java} into one destination — the shape a
     * single javac invocation with an annotation processor leaves behind — and returns the roots. Only {@code src}
     * is ever declared as a source directory.
     */
    private static Fixture compile(Path root) throws IOException {
        Path source = Files.createDirectories(root.resolve("src/p"));
        Path generatedSource = Files.createDirectories(root.resolve("gen/p"));
        Path destination = Files.createDirectories(root.resolve("build/classes/java/main"));
        Files.writeString(source.resolve("Handwritten.java"), HANDWRITTEN);
        Files.writeString(generatedSource.resolve("Generated.java"), GENERATED);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "no system java compiler: this test needs a JDK, not a JRE");
        int rc = compiler.run(null, null, null, "-d", destination.toString(),
                source.resolve("Handwritten.java").toString(), generatedSource.resolve("Generated.java").toString());
        assertEquals(0, rc, "the fixture must compile");
        return new Fixture(root.resolve("src"), root.resolve("gen"), destination);
    }

    private static SourceSet sourceSet(String name, Path destination, List<Path> sourceDirectories,
                                       List<SourceSet> dependencies) {
        return new SourceSetImpl.Builder()
                .setName(name)
                .setSourceDirectories(sourceDirectories)
                .setUri(URI.create("file:" + destination))
                .setDependencies(dependencies)
                .build();
    }

    private static List<String> relativeClassFiles(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(p -> p.toString().endsWith(".class"))
                    .map(p -> directory.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                    .sorted().toList();
        }
    }

    /**
     * ⛔⛔ THE HEADLINE. Exactly the two {@code Generated} class files leave, and the four written by hand —
     * including the two the file-name rule would have taken — stay.
     */
    @DisplayName("the generated classes become a library; a secondary top-level class does NOT")
    @Test
    public void generatedClassesBecomeALibrary(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        // the CONTROL for the discriminator: nothing named Secondary.java exists, so the file-name rule fires
        assertFalse(Files.exists(fixture.source().resolve("p/Secondary.java")));
        assertEquals(List.of("p/Generated$Inner.class", "p/Generated.class", "p/Handwritten$1.class",
                        "p/Handwritten$Inner.class", "p/Handwritten.class", "p/Secondary$1.class",
                        "p/Secondary.class"),
                relativeClassFiles(fixture.destination()));

        SourceSet main = sourceSet("demo/main", fixture.destination(), List.of(fixture.source()), List.of());
        AnnotationProcessorOutput.Result result = new AnnotationProcessorOutput().materialise(List.of(main));

        assertEquals(1, result.libraries().size());
        SourceSet library = result.libraries().getFirst();
        assertEquals("demo/main-apt", library.name());
        assertTrue(library.library());
        assertTrue(library.externalLibrary());
        assertEquals(2, result.classes());
        assertEquals(List.of("p/Generated$Inner.class", "p/Generated.class"),
                relativeClassFiles(Path.of(library.uri().getPath())));
        assertEquals(List.of(), result.notPlaced());
        // ⚠ AND THE DISCRIMINATOR IS MEASURED, NOT ASSUMED TO HAVE RUN: Secondary is exactly the one name the
        // file-name rule flagged and the SourceFile attribute rescued. Without this, "Secondary is not in the
        // library" would also pass if the flagging had never happened.
        assertEquals(1, result.sourceBacked());
    }

    /** The copies land inside the build tool's own output directory, so the project's {@code clean} removes them. */
    @DisplayName("the side directory sits in the build output directory")
    @Test
    public void sideDirectoryIsInsideTheBuildOutput(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        SourceSet main = sourceSet("demo/main", fixture.destination(), List.of(fixture.source()), List.of());

        SourceSet library = new AnnotationProcessorOutput().materialise(List.of(main)).libraries().getFirst();

        assertEquals(root.resolve("build/maddi-apt/demo-main"), Path.of(library.uri().getPath()));
    }

    /** ⚠ CONTROL: with the generated source root declared too, there is nothing to recover and no library. */
    @DisplayName("CONTROL: every class has a source, so no library is created")
    @Test
    public void everyClassHasASource(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        SourceSet main = sourceSet("demo/main", fixture.destination(),
                List.of(fixture.source(), fixture.generatedSource()), List.of());

        AnnotationProcessorOutput.Result result = new AnnotationProcessorOutput().materialise(List.of(main));

        assertEquals(List.of(), result.libraries());
        assertEquals(0, result.classes());
        assertEquals(1, result.destinationsRead());
        assertEquals(1, result.sourceBacked(), "Secondary is still settled by its SourceFile attribute");
        assertFalse(Files.exists(root.resolve("build/maddi-apt")));
    }

    /**
     * ⛔ A CONSUMER OF THE PRODUCER'S OUTPUT NEEDS THE LIBRARY TOO. Rule 2 turned that output directory into a
     * source-set dependency carrying only the source-backed half, so without this edge the consumer sees the
     * hand-written types of its dependency and not the generated ones — the elasticsearch failure exactly.
     */
    @DisplayName("a dependent source set gets the library as well")
    @Test
    public void consumersSeeTheLibrary(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        SourceSet producer = sourceSet("demo/main", fixture.destination(), List.of(fixture.source()), List.of());
        SourceSet consumer = sourceSet("other/main", root.resolve("other/build/classes/java/main"),
                List.of(root.resolve("other/src")), List.of(producer));

        AnnotationProcessorOutput.Result result =
                new AnnotationProcessorOutput().materialise(List.of(producer, consumer));
        List<SourceSet> attached = result.attach(List.of(producer, consumer));

        assertEquals(List.of("demo/main-apt"), names(attached.getFirst().dependencies()));
        assertEquals(List.of("demo/main", "demo/main-apt"), names(attached.get(1).dependencies()));
    }

    private static List<String> names(List<SourceSet> sourceSets) {
        return sourceSets.stream().map(SourceSet::name).toList();
    }

    /**
     * ⛔ WITHOUT SOURCE ROOTS EVERY CLASS LOOKS GENERATED, so the "repair" would copy the whole destination and
     * duplicate every type in it. It refuses, and it says so — a silent refusal here is the original defect.
     */
    @DisplayName("a source set with no source directories is refused, and reported")
    @Test
    public void noSourceDirectories(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        SourceSet main = sourceSet("demo/main", fixture.destination(), List.of(), List.of());

        AnnotationProcessorOutput.Result result = new AnnotationProcessorOutput().materialise(List.of(main));

        assertEquals(List.of(), result.libraries());
        assertEquals(1, result.notPlaced().size());
        assertEquals("demo/main", result.notPlaced().getFirst().sourceSet());
        assertTrue(result.notPlaced().getFirst().why().contains("no source directories"),
                result.notPlaced().getFirst().why());
    }

    /**
     * ⛔ A STALE COPY IS THE STALE-JAR FAILURE UNDER A NEW NAME. The directory is derived from the build outputs
     * as they are now, so a class from a previous run must not survive into this one.
     */
    @DisplayName("the side directory is rebuilt from scratch, not added to")
    @Test
    public void previousCopyIsRemoved(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        Path stale = root.resolve("build/maddi-apt/demo-main/p/Removed.class");
        Files.createDirectories(stale.getParent());
        Files.writeString(stale, "not a class file, and it must not survive");
        SourceSet main = sourceSet("demo/main", fixture.destination(), List.of(fixture.source()), List.of());

        SourceSet library = new AnnotationProcessorOutput().materialise(List.of(main)).libraries().getFirst();

        assertFalse(Files.exists(stale));
        assertEquals(List.of("p/Generated$Inner.class", "p/Generated.class"),
                relativeClassFiles(Path.of(library.uri().getPath())));
    }

    /**
     * ⚠ THE OTHER HALF OF THE SAME QUESTION, and it is not hypothetical: elasticsearch keeps seven esql files
     * under {@code .../xpack/compute/...} that declare {@code package org.elasticsearch.compute...}. A
     * compilation unit that IS in the parse, at a path its package does not predict, must not be copied —
     * that duplicates a type the source set already provides — and the disagreement is reported.
     */
    @DisplayName("a source file whose directory does not match its package is source, and reported")
    @Test
    public void compilationUnitAtAnUnexpectedPath(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        // move the only source file to a path that does not match the package p it declares
        Files.move(fixture.source().resolve("p/Handwritten.java"), fixture.source().resolve("Handwritten.java"));
        SourceSet main = sourceSet("demo/main", fixture.destination(), List.of(fixture.source()), List.of());

        AnnotationProcessorOutput.Result result = new AnnotationProcessorOutput().materialise(List.of(main));

        // Generated is still recovered; the two names compiled from the misplaced file are reported, not copied
        assertEquals(List.of("p/Generated$Inner.class", "p/Generated.class"),
                relativeClassFiles(Path.of(result.libraries().getFirst().uri().getPath())));
        assertEquals(List.of(), result.notPlaced());
        assertEquals(List.of("p/Handwritten", "p/Secondary"),
                result.sourceElsewhere().stream().map(AnnotationProcessorOutput.SourceElsewhere::type).sorted()
                        .toList());
        assertTrue(result.sourceElsewhere().stream()
                        .allMatch(s -> s.foundAt().equals(fixture.source().resolve("Handwritten.java").toString())),
                result.sourceElsewhere().toString());
    }

    /**
     * ⛔ AND THE DISCRIMINATOR FOR THAT RULE: matching a source file by NAME is not enough. A decoy
     * {@code q/Generated.java} shares the file name and declares another package, so it is a different type and
     * {@code p.Generated} is still a product of the processor. Without reading the candidate's {@code package}
     * declaration, this class would go missing from the parse — silently, which is the whole defect.
     */
    @DisplayName("a same-named source file in another package does NOT make a class source-backed")
    @Test
    public void sameFileNameInAnotherPackage(@TempDir Path root) throws IOException {
        Fixture fixture = compile(root);
        Path decoy = Files.createDirectories(fixture.source().resolve("q"));
        Files.writeString(decoy.resolve("Generated.java"), "package q;\npublic class Generated {}\n");
        SourceSet main = sourceSet("demo/main", fixture.destination(), List.of(fixture.source()), List.of());

        AnnotationProcessorOutput.Result result = new AnnotationProcessorOutput().materialise(List.of(main));

        assertEquals(List.of("p/Generated$Inner.class", "p/Generated.class"),
                relativeClassFiles(Path.of(result.libraries().getFirst().uri().getPath())));
        assertEquals(List.of(), result.sourceElsewhere());
    }
}
