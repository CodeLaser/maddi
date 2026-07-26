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
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
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
 * Prepping primary types <b>one at a time</b> (a fresh {@link PrepAnalyzer} per type, as
 * {@code jfocus-stdbase}'s viewer {@code Processor} and jfocus-standardize's intake used to do) must give the
 * same result as one batch {@link PrepAnalyzer#doPrimaryTypes} over all of them.
 * <p>
 * It did not: {@link PrepAnalyzer#doType}'s idempotency guard used {@code PART_OF_CONSTRUCTION} as the per-type
 * "already prepped" marker, but {@code ComputePartOfConstructionFinalField.go} stamps that marker on every
 * primary type reachable <em>through the call graph</em>, not only on the types actually passed to {@code doType}.
 * So prepping {@code DD} stamped its callee {@code F}, and the later {@code doPrimaryType(F)} returned
 * immediately — leaving {@code F}'s statements without {@code VariableData} while {@code F} stayed fully
 * reachable, so the link computer descended into them and tripped
 * {@code LinkComputerImpl$SourceMethodComputer.doStatement}'s bare {@code assert vd != null}.
 * <p>
 * The failure is order-dependent: it needs the caller to be prepped before the callee, which is why
 * {@link #callerFirst()} and {@link #calleeFirst()} are separate tests over identical code.
 */
public class TestPrepOnePrimaryTypeAtATime extends CommonTest {

    @BeforeEach
    @Override
    public void beforeEach() {
        // this test builds its own inspector, mirroring TestReprepKeepType
    }

    // DD calls F; DD sorts first, so prepping in name order preps the caller first
    @Language("java")
    private static final String DD = """
            package proj;
            public class DD {
                public int from = 0;
                @Override public String toString() {
                    String sep = proj.F.sep();
                    return "from: " + from + sep;
                }
            }
            """;

    @Language("java")
    private static final String F = """
            package proj;
            public class F {
                public static String sep() {
                    String s = System.lineSeparator();
                    return s;
                }
            }
            """;

    @DisplayName("caller prepped first: the callee must still be prepped, not skipped as 'already processed'")
    @Test
    public void callerFirst() throws IOException {
        ParseResult parseResult = init();
        TypeInfo dd = parseResult.findType("proj.DD");
        TypeInfo f = parseResult.findType("proj.F");

        // one call per primary type, fresh analyzer each — the shape that breaks
        new PrepAnalyzer(runtime).doPrimaryType(dd);
        new PrepAnalyzer(runtime).doPrimaryType(f);

        assertHasVariableData(dd, "toString", 0);
        assertHasVariableData(f, "sep", 0);
    }

    @DisplayName("callee prepped first: passed even before the fix, hence the pair")
    @Test
    public void calleeFirst() throws IOException {
        ParseResult parseResult = init();
        TypeInfo dd = parseResult.findType("proj.DD");
        TypeInfo f = parseResult.findType("proj.F");

        new PrepAnalyzer(runtime).doPrimaryType(f);
        new PrepAnalyzer(runtime).doPrimaryType(dd);

        assertHasVariableData(dd, "toString", 0);
        assertHasVariableData(f, "sep", 0);
    }

    @DisplayName("one batch call over both types: the reference behaviour the per-type calls must match")
    @Test
    public void batch() throws IOException {
        ParseResult parseResult = init();
        TypeInfo dd = parseResult.findType("proj.DD");
        TypeInfo f = parseResult.findType("proj.F");

        new PrepAnalyzer(runtime).doPrimaryTypes(Set.of(dd, f));

        assertHasVariableData(dd, "toString", 0);
        assertHasVariableData(f, "sep", 0);
    }

    // ------------------------------------------------------------------------------------------------------
    // the second half of the same defect: what ComputePartOfConstructionFinalField COMPUTES for a type it was
    // never asked about. FINAL_FIELD is a soundness gate for DynamicImmutabilityInference, not a hint, and
    // isAssigned() cannot tell "no assignment" from "no body analyzed". go() is now gated on PREPPED: a type
    // whose bodies we do not have is left UNDECIDED rather than decided on manufactured evidence.

    @Language("java")
    private static final String CALLER = """
            package proj;
            public class Caller {
                public String go() { return Callee.sep(); }
            }
            """;

    // counter must be PRIVATE: computeEffectivelyFinalFields starts a private field at "effectively final" and can
    // only knock it down on evidence of an assignment outside construction. A public field starts at not-final, so
    // missing that evidence would be invisible. bump() supplies exactly that evidence — when it is in the graph.
    @Language("java")
    private static final String CALLEE = """
            package proj;
            public class Callee {
                private static int counter;
                public static void bump() { counter = counter + 1; }
                public static String sep() { return "x" + counter; }
            }
            """;

    @DisplayName("effective finality of the callee's field must not depend on who was prepped first")
    @Test
    public void finalFieldOfAReachedType() throws IOException {
        ParseResult perType = init(Map.of("proj.Caller", CALLER, "proj.Callee", CALLEE));
        TypeInfo callerA = perType.findType("proj.Caller");
        TypeInfo calleeA = perType.findType("proj.Callee");

        new PrepAnalyzer(runtime).doPrimaryType(callerA);
        // Callee is in Caller's call graph (via sep()) but was not prepped: undecided, not decided-on-no-evidence
        assertFalse(calleeA.analysis()
                        .haveAnalyzedValueFor(ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION),
                "a merely-reached type must be left undecided, not computed from bodies we never analyzed");
        assertFalse(calleeA.getFieldByName("counter", true).analysis()
                        .haveAnalyzedValueFor(PropertyImpl.FINAL_FIELD),
                "and its fields with it — FINAL_FIELD is write-once, so a wrong value here would be permanent");

        new PrepAnalyzer(runtime).doPrimaryType(calleeA);
        boolean perTypeFinal = isFinal(calleeA, "counter");

        // same code, one batch call: the reference answer
        ParseResult batched = init(Map.of("proj.Caller", CALLER, "proj.Callee", CALLEE));
        TypeInfo callerB = batched.findType("proj.Caller");
        TypeInfo calleeB = batched.findType("proj.Callee");
        new PrepAnalyzer(runtime).doPrimaryTypes(Set.of(callerB, calleeB));
        boolean batchFinal = isFinal(calleeB, "counter");

        assertFalse(batchFinal, "bump() assigns counter outside construction, so it is not effectively final");
        assertEquals(batchFinal, perTypeFinal,
                "prepping one primary type at a time must not change the verdict on the callee's field:"
                + " a FINAL_FIELD that is true only because the assigning method had no VariableData yet is"
                + " unsound, and DynamicImmutabilityInference gates on it");
    }

    private static boolean isFinal(TypeInfo typeInfo, String fieldName) {
        FieldInfo fieldInfo = typeInfo.getFieldByName(fieldName, true);
        Value.Bool value = fieldInfo.analysis()
                .getOrDefault(PropertyImpl.FINAL_FIELD, ValueImpl.BoolImpl.FALSE);
        return value.isTrue();
    }

    private static void assertHasVariableData(TypeInfo typeInfo, String methodName, int parameters) {
        MethodInfo methodInfo = typeInfo.findUniqueMethod(methodName, parameters);
        VariableData methodVd = methodInfo.analysis()
                .getOrNull(VariableDataImpl.VARIABLE_DATA, VariableDataImpl.class);
        assertNotNull(methodVd, () -> "no method-level VariableData on " + methodInfo);
        Statement firstStatement = methodInfo.methodBody().statements().getFirst();
        VariableData stmtVd = firstStatement.analysis()
                .getOrNull(VariableDataImpl.VARIABLE_DATA, VariableDataImpl.class);
        assertNotNull(stmtVd, () -> "no statement-level VariableData on " + methodInfo + " statement 0;"
                                    + " the type was marked processed without being processed");
    }

    private ParseResult init() throws IOException {
        return init(Map.of("proj.DD", DD, "proj.F", F));
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
