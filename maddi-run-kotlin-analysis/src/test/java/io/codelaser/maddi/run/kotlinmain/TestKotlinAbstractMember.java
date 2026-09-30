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

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same interface written twice, once in Kotlin and once in Java: does maddi model the two the same way?
 * <p>
 * It did not. An abstract Kotlin function was built as a plain method carrying the {@code abstract} MODIFIER, where
 * both Java front ends give it the abstract method TYPE (#35). So {@code isAbstract()} was false for every Kotlin
 * interface member, and prep's {@code addImplementation} -- which filters {@code overrides()} on exactly that -- never
 * registered an implementation for one. The eventual analysis of a call through a Kotlin interface therefore had no
 * implementations to consult, which made it optimistic: on detekt, 674 types were called {@code @Immutable} against
 * 650 once the implementations arrive.
 */
public class TestKotlinAbstractMember {

    private static final String KOTLIN_SRC = """
            package a

            interface KSink {
                fun accept(sb: StringBuilder)
                fun describe(): String = "k"
            }

            class KAppender : KSink {
                override fun accept(sb: StringBuilder) {
                    sb.append("k")
                }
            }
            """;

    private static final String JAVA_SRC = """
            package b;

            public interface JSink {
                void accept(StringBuilder sb);

                default String describe() {
                    return "j";
                }
            }
            """;

    private static final String JAVA_IMPL_SRC = """
            package b;

            public class JAppender implements JSink {
                @Override
                public void accept(StringBuilder sb) {
                    sb.append("j");
                }
            }
            """;

    /**
     * Both declarations are abstract, both members with a body are default methods, and prep registers each
     * implementation against the declaration it implements.
     */
    @Test
    public void aKotlinInterfaceMemberIsModelledAsAJavaOneIs(@TempDir Path tmp) throws Exception {
        Set<TypeInfo> primaryTypes = parseAndPrep(tmp);

        MethodInfo kAccept = method(primaryTypes, "a.KSink", "accept");
        MethodInfo jAccept = method(primaryTypes, "b.JSink", "accept");
        assertTrue(jAccept.isAbstract(), "the Java declaration");
        assertTrue(kAccept.isAbstract(), "the Kotlin declaration");

        assertTrue(method(primaryTypes, "b.JSink", "describe").isDefault(), "the Java member with a body");
        assertTrue(method(primaryTypes, "a.KSink", "describe").isDefault(), "the Kotlin member with a body");

        MethodInfo kImpl = method(primaryTypes, "a.KAppender", "accept");
        MethodInfo jImpl = method(primaryTypes, "b.JAppender", "accept");
        assertFalse(kImpl.isAbstract());
        assertEquals(List.of(jImpl), implementations(jAccept), "the Java implementation, which prep always saw");
        assertEquals(List.of(kImpl), implementations(kAccept), "the Kotlin implementation");
    }

    /** What {@code addImplementation} wrote on the declaration: the implementations the analysis consults. */
    private static List<MethodInfo> implementations(MethodInfo declaration) {
        Value.SetOfMethodInfo set = declaration.analysis()
                .getOrNull(PropertyImpl.IMPLEMENTATIONS, Value.SetOfMethodInfo.class);
        if (set == null) return List.of();
        return StreamSupport.stream(set.methodInfoSet().spliterator(), false).toList();
    }

    private static MethodInfo method(Set<TypeInfo> primaryTypes, String fqn, String name) {
        TypeInfo typeInfo = primaryTypes.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in " + primaryTypes.stream()
                        .map(TypeInfo::fullyQualifiedName).sorted().toList()));
        return typeInfo.methods().stream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError(fqn + "." + name));
    }

    /** The Kotlin and Java sources as two source sets of one mixed project, parsed and prepped. */
    private Set<TypeInfo> parseAndPrep(Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(kDir.resolve("a/KSink.kt"), KOTLIN_SRC);
        Files.writeString(jDir.resolve("b/JSink.java"), JAVA_SRC);
        Files.writeString(jDir.resolve("b/JAppender.java"), JAVA_IMPL_SRC);

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
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> ignored = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        assertTrue(ignored.vertices().iterator().hasNext(), "prep produced no call graph");
        return primaryTypes;
    }
}
