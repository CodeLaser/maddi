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
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * detekt's {@code DetektError.kt}, the whole file, on its own: the shape behind #34. {@code IssuesFound} and
 * {@code InvalidConfig} are character-identical apart from their name.
 * <p>
 * On the corpus the subclasses were {@code @Mutable} in most runs and {@code @FinalFields} in about 1 in 4. The breaking
 * pass floors an undecided supertype at FINAL_FIELDS, so a subclass computed before {@code DetektError} was decided got
 * {@code @FinalFields}; one computed after saw its parent at FINAL_FIELDS, which then sank it to MUTABLE; and the
 * refused downgrade froze whichever came first. Iterations run on 8 threads there; this file is below the parallel
 * threshold, so here the parent always came first and the verdict was always {@code @Mutable}.
 * <p>
 * The parent caps a subtype, it does not sink it (road to immutability, 050: deriving from a class cannot increase the
 * immutability level). A subclass adding only final fields to a {@code @FinalFields} parent is {@code @FinalFields},
 * whatever the order. A {@code @Mutable} parent still makes its subclass {@code @Mutable}.
 */
public class TestSealedErrorHierarchy {

    private static final String DETEKT_ERROR = """
            package a

            sealed class DetektError(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

            class IssuesFound(message: String) : DetektError(message)

            class InvalidConfig(message: String) : DetektError(message)

            class UnexpectedError(override val cause: Throwable) : DetektError(null, cause)

            open class Counter { var count = 0 }

            class Named(val name: String) : Counter()
            """;

    private static final List<String> JDK_ANNOTATED_APIS = List.of(
            "../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk");

    @Test
    public void aFinalFieldsParentCapsItsSubclassesAndAMutableOneSinksThem(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin/a");
        Files.createDirectories(kDir);
        Files.writeString(kDir.resolve("DetektError.kt"), DETEKT_ERROR);

        String stdlibJar = Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(p -> p.matches(".*kotlin-stdlib-[0-9].*\\.jar$")).findFirst()
                .orElseThrow(() -> new AssertionError("kotlin-stdlib jar not on the test classpath"));
        SourceSet stdlib = new SourceSetImpl.Builder()
                .setName(Path.of(stdlibJar).getFileName().toString())
                .setSourceDirectories(List.of()).setUri(URI.create("file:" + stdlibJar))
                .setLibrary(true).setExternalLibrary(true).build();
        Path srcRoot = tmp.resolve("src/main/kotlin");
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(srcRoot)).setUri(srcRoot.toUri())
                .setDependencies(List.of(stdlib)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addClassPathParts(stdlib).addSourceSets(kotlinSet).build();

        // the committed instrument (#34) is how the verdicts are read back: no API of its own to maintain
        Path dump = tmp.resolve("verdicts.txt");
        String previous = System.setProperty("maddi.verdictDump", dump.toString());
        try {
            new RunMixedPrepAnalyzer().go(config, true, JDK_ANNOTATED_APIS);
        } finally {
            if (previous == null) System.clearProperty("maddi.verdictDump");
            else System.setProperty("maddi.verdictDump", previous);
        }

        Map<String, String> verdicts = Files.readAllLines(dump).stream()
                .collect(Collectors.toMap(l -> l.substring(l.indexOf(' ') + 1), l -> l.substring(0, l.indexOf(' '))));
        assertEquals("@FinalFields", verdicts.get("a.DetektError"), verdicts::toString);
        for (String subclass : List.of("a.IssuesFound", "a.InvalidConfig", "a.UnexpectedError")) {
            assertEquals("@FinalFields", verdicts.get(subclass), () -> subclass + ": a @FinalFields parent caps, it does"
                    + " not sink; all: " + verdicts);
        }
        assertEquals("@Mutable", verdicts.get("a.Counter"), verdicts::toString);
        assertEquals("@Mutable", verdicts.get("a.Named"), () -> "a @Mutable parent still sinks: " + verdicts);
    }
}
