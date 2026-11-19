package io.codelaser.maddi.inspection.openjdk;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.EnclosedExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/*
 A field reference whose scope is not a variable (a cast, a call) was named by the scope's source position only:
 `L.variables#scope4-24:4-31`. That position names no file and no method, and variables compare by name, so the same
 read in two methods of two files, at the same line:col, was ONE variable. A dedup run's synthetic loop-body methods
 read `((List<…>) ld).variables` exactly so, and an equals-keyed map handed one method the other's field reference
 (hibernate, cold against hot), one cast to List<Expression>, the other to List<?>. The name now carries the scope's
 shape: what it calls, constructs and casts to.
 */
public class TestFieldReferenceScopeIdentity {
    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String L = """
            package a.b;
            public class L<T> { public int variables; }
            """;

    // A and B: the same layout, so the cast starts at the same line:col in both; one field, two cast types
    @Language("java")
    private static final String A = """
            package a.b;
            public class A {
                int read(Object ld) {
                    return ((L<Integer>) ld).variables;
                }
            }
            """;

    @Language("java")
    private static final String B = """
            package a.b;
            public class B {
                int read(Object ld) {
                    return ((L<Boolean>) ld).variables;
                }
            }
            """;

    @Test
    public void sameScopePositionInTwoMethodsIsTwoVariables() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(
                Map.of(sourceSet, Map.of("a.b.L", L, "a.b.A", A, "a.b.B", B)), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        FieldReference inA = fieldReference(parseResult.findType("a.b.A"));
        FieldReference inB = fieldReference(parseResult.findType("a.b.B"));
        assertEquals(inA.scope().source().compact2(), inB.scope().source().compact2(), "the premise: one position");
        assertNotEquals(inA.fullyQualifiedName(), inB.fullyQualifiedName());
        assertNotEquals(inA, inB);
    }

    /*
     Synthetic code has no source: every scope is at 0-0:0-0, and two generated loop bodies over the same element type
     cast the same way, so position and shape were equal (hibernate's visitOverLoopBody0 and
     visitSelfRenderingExpressionLoopBody0). Such a scope's name carries the methods whose parameters it reads.
     */
    @Test
    public void sameShapeWithoutSourceInTwoMethodsIsTwoVariables() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(
                Map.of(sourceSet, Map.of("a.b.L", L, "a.b.A", A, "a.b.B", B)), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        Runtime runtime = javaInspector.runtime();
        FieldReference inA = fieldReference(parseResult.findType("a.b.A"));
        Cast castInA = (Cast) (inA.scope() instanceof EnclosedExpression ee ? ee.inner() : inA.scope());
        FieldReference synthA = synthetic(runtime, inA, castInA, parseResult.findType("a.b.A").findUniqueMethod("read", 1));
        FieldReference synthB = synthetic(runtime, inA, castInA, parseResult.findType("a.b.B").findUniqueMethod("read", 1));
        assertEquals(synthA.scope().source().compact2(), synthB.scope().source().compact2(), "the premise: one position");
        assertNotEquals(synthA.fullyQualifiedName(), synthB.fullyQualifiedName());
        assertNotEquals(synthA, synthB);
        // a second build in the same method is the same variable
        assertEquals(synthA, synthetic(runtime, inA, castInA, parseResult.findType("a.b.A").findUniqueMethod("read", 1)));
    }

    /** `((L<Integer>) ld).variables`, ld the method's parameter, without a source. */
    private static FieldReference synthetic(Runtime runtime, FieldReference model, Cast cast, MethodInfo method) {
        Cast scope = runtime.newCastBuilder().setSource(runtime.noSource())
                .setExpression(runtime.newVariableExpression(method.parameters().getFirst()))
                .setParameterizedType(cast.parameterizedType()).build();
        return runtime.newFieldReference(model.fieldInfo(), scope, model.parameterizedType());
    }

    private static FieldReference fieldReference(TypeInfo typeInfo) {
        List<FieldReference> found = new ArrayList<>();
        typeInfo.methodStream().forEach(m -> m.methodBody().visit((Element e) -> {
            if (e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
                && "variables".equals(fr.fieldInfo().name())) {
                found.add(fr);
            }
            return true;
        }));
        assertEquals(1, found.size(), () -> typeInfo + ": " + found);
        return found.getFirst();
    }
}
