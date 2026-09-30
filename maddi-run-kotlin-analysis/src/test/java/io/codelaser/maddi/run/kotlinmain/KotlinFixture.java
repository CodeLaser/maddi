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
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A mixed Kotlin+Java project written to a temporary directory, parsed and prepped: what a test needs to ask what
 * maddi made of a handful of sources, without a corpus.
 */
record KotlinFixture(Runtime runtime, Set<TypeInfo> primaryTypes, PrepAnalyzer prepAnalyzer, G<Info> callGraph) {

    /** [kotlin] and [java] are file path (e.g. {@code a/K.kt}) to content; prep runs fault-tolerant. */
    static KotlinFixture of(Path tmp, Map<String, String> kotlin, Map<String, String> java) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        write(kDir, kotlin);
        write(jDir, java);

        String stdlibJar = Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(p -> p.matches(".*kotlin-stdlib-[0-9].*\\.jar$")).findFirst()
                .orElseThrow(() -> new AssertionError("kotlin-stdlib jar not on the test classpath"));
        SourceSet stdlib = new SourceSetImpl.Builder()
                .setName(Path.of(stdlibJar).getFileName().toString())
                .setSourceDirectories(List.of()).setUri(URI.create("file:" + stdlibJar))
                .setLibrary(true).setExternalLibrary(true).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(stdlib)).build();
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addClassPathParts(stdlib).addSourceSets(kotlinSet).addSourceSets(javaSet).build();

        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
        // fault-tolerant, as every Kotlin driver here is: a method prep cannot do is isolated and reported,
        // which is what a test about prep failures reads
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        return new KotlinFixture(runtime, primaryTypes, prepAnalyzer,
                prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes));
    }

    private static void write(Path dir, Map<String, String> sources) throws Exception {
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            Path file = dir.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue());
        }
    }

    TypeInfo type(String fqn) {
        return primaryTypes.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in " + primaryTypes.stream()
                        .map(TypeInfo::fullyQualifiedName).sorted().toList()));
    }

    MethodInfo method(String fqn, String name) {
        return type(fqn).methods().stream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError(fqn + "." + name + ", of " + type(fqn).methods().stream()
                        .map(MethodInfo::name).sorted().toList()));
    }
}
