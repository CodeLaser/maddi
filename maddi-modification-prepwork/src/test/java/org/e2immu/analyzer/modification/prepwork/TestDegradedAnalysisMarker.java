package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #36 (degradation visibility): a type isolated by fault-tolerant prep stamps
 * DEGRADED_ANALYSIS_METHOD on all its methods, so per-call consumers (VL2O / extract-interface)
 * know to treat them pessimistically. The trigger is SYNTHETIC: a committed non-abstract method
 * without a body — the shape a half-built (e.g. lazily class-scanner-loaded) type presents to prep.
 * (The original trigger, the task-#33 parse shape, was fixed on 2026-07-18 and no longer degrades.)
 */
public class TestDegradedAnalysisMarker extends CommonTest {

    @DisplayName("fault-tolerant prep isolation stamps DEGRADED_ANALYSIS_METHOD")
    @Test
    public void test() {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("a.b")
                .setSourceSet(javaInspector.mainSources()) // call-graph phase asserts a source set
                .build();
        TypeInfo broken = runtime.newTypeInfo(cu, "Broken");
        cu.setTypes(List.of(broken));
        MethodInfo m = runtime.newMethod(broken, "noBody", runtime.methodTypeMethod());
        // deliberately NO setMethodBody: a committed, non-abstract, body-less method is the
        // half-built shape that must trip prep's fault isolation
        m.builder()
                .setReturnType(runtime.stringParameterizedType())
                .addMethodModifier(runtime.methodModifierPublic())
                .setAccess(runtime.accessPublic())
                .commitParameters().commit();
        broken.builder().addMethod(m)
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .computeAccess().commit();

        PrepAnalyzer prep = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder()
                .setFaultTolerant(true).build());
        prep.doPrimaryTypes(Set.of(broken));
        assertFalse(prep.exceptions().isEmpty(), "the body-less method must trigger isolation");
        boolean anyStamped = broken.recursiveSubTypeStream()
                .flatMap(TypeInfo::constructorAndMethodStream)
                .anyMatch(mi -> mi.analysis().getOrDefault(PropertyImpl.DEGRADED_ANALYSIS_METHOD,
                        ValueImpl.BoolImpl.FALSE).isTrue());
        assertTrue(anyStamped, "isolated type's methods must carry the degradation marker");
    }
}
