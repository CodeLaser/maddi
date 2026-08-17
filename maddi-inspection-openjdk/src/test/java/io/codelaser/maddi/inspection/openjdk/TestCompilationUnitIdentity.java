package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Two top-level types in the same source file must share a single {@link CompilationUnit} <em>instance</em>: every
 * type carrying a {@code CompilationUnit} that {@code equals()} another but is not {@code ==} to it violates the
 * contract that a compilation unit is one object. This mirrors the output of {@code IsolateMethod}, which puts the
 * frame and its (package-private) supertype as two top-level types in one compilation unit.
 */
public class TestCompilationUnitIdentity {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/")).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    // the public type comes first and forward-references the (package-private) supertype declared below it, exactly
    // as IsolateMethod prints its frame ahead of the generated supertype
    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class Frame extends Frame_super { }
            class Frame_super { }
            """;

    @Test
    public void test() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(
                        Map.of(sourceSet, Map.of("a.b.Frame", INPUT)),
                        JavaInspectorImpl.DETAILED_SOURCES)
                .parseResult();

        TypeInfo frame = parseResult.findType("a.b.Frame");
        TypeInfo superType = parseResult.findType("a.b.Frame_super");
        assertNotNull(frame);
        assertNotNull(superType);

        CompilationUnit cuFrame = frame.compilationUnit();
        CompilationUnit cuSuper = superType.compilationUnit();
        assertEquals(cuFrame, cuSuper, "the two compilation units must at least be equal");
        assertSame(cuFrame, cuSuper,
                "two top-level types in one source file must share the same CompilationUnit instance, not just equal ones");
    }
}
