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

import lombok.Data;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static io.codelaser.maddi.inspection.resource.SourceSetImpl.sourceSetOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * javac's default stop policy: an error BEFORE attribution -- a syntax error, an annotation processor's error, a
 * duplicate class -- stops attribution of EVERY compilation unit of the task, not only the faulty one. Every unit of
 * the source set then reaches the scanner unattributed and is dropped, so one bad file (pulsar-broker's tests: Lombok
 * 1.18.48 rejecting one {@code @Builder(builderClassName = "Builder")}) cost the whole source set (835 units).
 * {@code -XDshould-stop.ifError=FLOW} attributes every unit anyway; only the faulty ones are dropped.
 */
public class TestStopPolicy {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet lombok = sourceSetOf(Data.class, javaBase);
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase, lombok)).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath("jmod:java.base")
                .addClassPathParts(lombok)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String HEALTHY = """
            package a;
            public class Healthy {
                int twice(int i) {
                    return 2 * i;
                }
            }
            """;

    @Language("java")
    private static final String SYNTAX_ERROR = """
            package a;
            public class Broken {
                int oops( {
            }
            """;

    // the pulsar shape: the processor refuses the unqualified name, reports an error, and generates nothing
    @Language("java")
    private static final String PROCESSOR_ERROR = """
            package a;
            import lombok.Builder;
            @Builder(builderClassName = "Builder")
            public class Rejected {
                private int x;
            }
            """;

    @Language("java")
    private static final String CALLER = """
            package a;
            public class Caller {
                Rejected make() {
                    return Rejected.builder().x(1).build();
                }
                int plain() {
                    return 3;
                }
            }
            """;

    @Test
    public void callerOfMissingGeneratedMember() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(false).setDetailedSources(true).setLombok(true).build();
        Summary summary = javaInspector.parseMultiSourceSet(Map.of(sourceSet, Map.of("a.Healthy", HEALTHY,
                "a.Rejected", PROCESSOR_ERROR, "a.Caller", CALLER)), options);
        // Caller's call of the builder the processor never generated does not resolve: the unit is dropped, as
        // a WARNING (a symbol that does not resolve is routine on a partial class path), not an error
        assertTrue(summary.types().stream().noneMatch(t -> "a.Caller".equals(t.fullyQualifiedName())));
        assertTrue(summary.parseExceptions().isEmpty(), summary.parseExceptions().toString());
        assertTrue(summary.parseWarnings().stream().anyMatch(w -> "compilation unit".equals(w.where())
                                                                  && w.uri().toString().endsWith("a/Caller.java")),
                summary.parseWarnings().toString());
        assertTrue(summary.types().stream().anyMatch(t -> "a.Healthy".equals(t.fullyQualifiedName())));
    }

    private Summary parse(String fqn, String source) {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(false).setDetailedSources(true).setLombok(true).build();
        return javaInspector.parseMultiSourceSet(
                Map.of(sourceSet, Map.of("a.Healthy", HEALTHY, fqn, source)), options);
    }

    private static void assertHealthyParsed(Summary summary) {
        assertTrue(summary.types().stream().anyMatch(t -> "a.Healthy".equals(t.fullyQualifiedName())),
                "a.Healthy is dropped: " + summary.parseExceptions());
        ParseResult parseResult = summary.parseResultIgnoringErrors();
        TypeInfo healthy = parseResult.findType("a.Healthy");
        MethodInfo twice = healthy.findUniqueMethod("twice", 1);
        assertFalse(twice.methodBody().isEmpty(), "the body of a.Healthy.twice is parsed");
    }

    @Test
    public void syntaxErrorDropsOnlyItsUnit() {
        Summary summary = parse("a.Broken", SYNTAX_ERROR);
        assertTrue(summary.haveErrors());
        assertHealthyParsed(summary);
    }

    @Test
    public void processorErrorDropsOnlyItsUnit() {
        Summary summary = parse("a.Rejected", PROCESSOR_ERROR);
        assertHealthyParsed(summary);
        // the unit whose processor failed is attributed too, and kept: a source type WITHOUT the members the
        // processor would have generated, the error a warning like any other javac error. A unit calling those
        // members is dropped: callerOfMissingGeneratedMember.
        TypeInfo rejected = summary.parseResultIgnoringErrors().findType("a.Rejected");
        assertTrue(summary.types().contains(rejected));
        assertTrue(rejected.methodStream().noneMatch(m -> "builder".equals(m.name())));
        assertTrue(summary.parseWarnings().stream().anyMatch(w -> w.toString().contains("Rejected.java")));
    }
}
