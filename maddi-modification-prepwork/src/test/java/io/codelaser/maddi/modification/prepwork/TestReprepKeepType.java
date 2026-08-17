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

package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tier-2 incremental reparse re-runs {@code E2ImmuPrep.make()} over ALL primary types, but the types from source
 * sets that did not change (the KEEP set) are the SAME objects carried unchanged from the previous full prep — they
 * already hold {@code VariableData} at method / statement / field-initializer level. Re-prepping such a type used to
 * throw {@code IllegalArgumentException: Trying to overwrite a value for property variableData} (the method-level set
 * at {@code MethodAnalyzer:346} is guarded, but the statement- and field-initializer-level sets are not), which the
 * service caught by discarding the partial and falling back to a full cold analysis — defeating the incremental win
 * on every multi-source-set edit.
 * <p>
 * {@link PrepAnalyzer#doType} now skips a type that already carries {@code PART_OF_CONSTRUCTION} (the per-type
 * "already processed" marker). This test asserts a second prep over the very same objects is a no-op that neither
 * throws nor disturbs the VariableData set by the first.
 */
public class TestReprepKeepType extends CommonTest {

    @BeforeEach
    @Override
    public void beforeEach() {
        // this test builds its own inspector, mirroring TestImplementationsAfterRewire
    }

    @Language("java")
    private static final String X = """
            package a.b;
            public class X {
                private final int base;
                private int total = 7;
                X(int base) { this.base = base; }
                public int compute(int n) {
                    int acc = base;
                    for (int i = 0; i < n; i++) {
                        acc = acc + i;
                    }
                    return acc + total;
                }
            }
            """;

    @DisplayName("re-prepping a KEEP type (same objects, already prepped) is a no-op, not a variableData overwrite")
    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of(ABX, X));
        TypeInfo x = parseResult.findType(ABX);
        MethodInfo compute = x.findUniqueMethod("compute", 1);

        // first (full) prep: sets VariableData at method + statement level, and PART_OF_CONSTRUCTION on the type
        new PrepAnalyzer(runtime).doPrimaryTypes(Set.of(x));
        assertTrue(x.analysis().haveAnalyzedValueFor(ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION),
                "the full prep must mark the type processed");
        VariableData methodVd = compute.analysis().getOrNull(VariableDataImpl.VARIABLE_DATA, VariableDataImpl.class);
        assertNotNull(methodVd, "the method must have VariableData after the first prep");
        Statement firstStatement = compute.methodBody().statements().getFirst();
        VariableData stmtVd = firstStatement.analysis()
                .getOrNull(VariableDataImpl.VARIABLE_DATA, VariableDataImpl.class);
        assertNotNull(stmtVd, "statement-level VariableData is what used to be double-set");

        // second prep over the SAME objects — exactly what the reparse does to the KEEP set. Before the guard this
        // threw "Trying to overwrite a value for property variableData"; now it must be a silent no-op.
        assertDoesNotThrow(() -> new PrepAnalyzer(runtime).doPrimaryTypes(Set.of(x)),
                "re-prepping an already-prepped (KEEP) type must not overwrite its VariableData");

        // and the carried values are untouched — same objects, byte-identical prep output
        assertSame(methodVd, compute.analysis().getOrNull(VariableDataImpl.VARIABLE_DATA, VariableDataImpl.class));
        assertSame(stmtVd, firstStatement.analysis().getOrNull(VariableDataImpl.VARIABLE_DATA, VariableDataImpl.class));
    }

    private ParseResult init(Map<String, String> sourcesByFqn) throws IOException {
        Map<String, String> sourcesByURIString = sourcesByFqn.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        sourcesByURIString.keySet().forEach(builder::addSources);
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).setIgnoreModule(true).build();
        Summary summary = javaInspector.parse(sourcesByURIString, parseOptions);
        return summary.parseResult();
    }
}
