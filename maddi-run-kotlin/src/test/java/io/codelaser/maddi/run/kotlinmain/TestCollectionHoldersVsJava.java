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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The type verdict of a holder of a collection: an interface whose property is a {@code Collection}, a final class
 * whose constructor property is a {@code Map} or a {@code List}. detekt's {@code ConfigSpec}, {@code RuleSet} and
 * {@code Issue} are these shapes; on 2026-09-24 about twenty such types moved from IMMUTABLE_HC to FINAL_FIELDS in
 * one merge of engine changes. Whatever the right level is, it is the one Java's equivalent gets.
 */
public class TestCollectionHoldersVsJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCollectionHoldersVsJava.class);

    private static final String KOTLIN = """
            package a
            interface KSpec {
                val flag: Boolean
                val resources: Collection<String>
            }
            class KRuleSet(val id: String, val rules: Map<String, String>)
            class KIssue(val message: String, val references: List<String>)
            """;

    private static final String JAVA_SPEC = """
            package b;
            public interface JSpec {
                boolean getFlag();
                java.util.Collection<String> getResources();
            }
            """;
    private static final String JAVA_RULE_SET = """
            package b;
            public final class JRuleSet {
                private final String id;
                private final java.util.Map<String, String> rules;
                public JRuleSet(String id, java.util.Map<String, String> rules) { this.id = id; this.rules = rules; }
                public String getId() { return id; }
                public java.util.Map<String, String> getRules() { return rules; }
            }
            """;
    private static final String JAVA_ISSUE = """
            package b;
            public final class JIssue {
                private final String message;
                private final java.util.List<String> references;
                public JIssue(String message, java.util.List<String> references) {
                    this.message = message; this.references = references;
                }
                public String getMessage() { return message; }
                public java.util.List<String> getReferences() { return references; }
            }
            """;

    private static final List<String> SHAPES = List.of("Spec", "RuleSet", "Issue");

    @Test
    public void aCollectionHolderGetsTheVerdictItsJavaTwinGets(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(kDir.resolve("a/K.kt"), KOTLIN);
        Files.writeString(jDir.resolve("b/JSpec.java"), JAVA_SPEC);
        Files.writeString(jDir.resolve("b/JRuleSet.java"), JAVA_RULE_SET);
        Files.writeString(jDir.resolve("b/JIssue.java"), JAVA_ISSUE);

        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(javaSet)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(javaSet).addSourceSets(kotlinSet).build();

        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
        PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
        assertEquals(0, census.getTotal(), "unread Kotlin would make the comparison vacuous: " + census.dumpLines());
        new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);

        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        assertEquals(List.of(), prepAnalyzer.exceptions().stream().map(String::valueOf).toList());
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        new IteratingAnalyzerImpl(parsed.getJavaInspector(), new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(30).setStopWhenCycleDetectedAndNoImprovements(true).setFaultTolerant(true)
                .build()).analyze(order, callGraph);

        StringBuilder report = new StringBuilder("\n");
        StringBuilder kotlinSide = new StringBuilder();
        StringBuilder javaSide = new StringBuilder();
        for (String shape : SHAPES) {
            String kv = immutability(type(primaryTypes, "a.K" + shape));
            String jv = immutability(type(primaryTypes, "b.J" + shape));
            report.append(String.format("%-10s kotlin: %-14s java: %s%n", shape, kv, jv));
            kotlinSide.append(shape).append(' ').append(kv).append('\n');
            javaSide.append(shape).append(' ').append(jv).append('\n');
        }
        LOGGER.info("collection holders:{}", report);
        assertEquals(javaSide.toString(), kotlinSide.toString(), report.toString());
    }

    private static TypeInfo type(Set<TypeInfo> types, String fqn) {
        return types.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn));
    }

    private static String immutability(TypeInfo type) {
        Value.Immutable immutable = type.analysis()
                .getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
        return immutable.isImmutable() ? "IMMUTABLE" : immutable.isAtLeastImmutableHC() ? "IMMUTABLE_HC"
                : immutable.isFinalFields() ? "FINAL_FIELDS" : "MUTABLE";
    }
}
