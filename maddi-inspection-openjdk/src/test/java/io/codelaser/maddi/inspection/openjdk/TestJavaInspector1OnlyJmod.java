package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;
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
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaInspector1OnlyJmod {

    private JavaInspector javaInspector;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X1 {
                interface I { }
                record RI(String s) implements I { }
                void method(I i) {
                    if(i instanceof RI(String t)) {
                        System.out.println(t);
                    }
                }
            }
            """;

    @Test
    public void test1() {
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.X1", INPUT1), JavaInspectorImpl.DETAILED_SOURCES)
                .parseResult();
        TypeInfo X1 = parseResult.findType("a.b.X1");
        assertEquals("a.b.X1", X1.fullyQualifiedName());
        assertTrue(X1.hasBeenInspected());

        // test the compiled types manager
        CompiledTypesManager ctm = javaInspector.compiledTypesManager();

        List<TypeInfo> parsed = ctm.typesLoaded(false);
        assertEquals(3, parsed.size());

        List<TypeInfo> loaded = ctm.typesLoaded(true);
        assertTrue(loaded.size() > 10);

        TypeInfo printStream = ctm.get("java.io.PrintStream", null);
        assertNotNull(printStream);
        assertTrue(printStream.hasBeenInspected());

        TypeInfo boxedInteger = ctm.get("java.lang.Integer", null);
        assertNotNull(boxedInteger); // pre-load

        TypeInfo varHandle = ctm.get("java.lang.invoke.VarHandle", null);
        assertNull(varHandle); // no pre-load
    }
}
