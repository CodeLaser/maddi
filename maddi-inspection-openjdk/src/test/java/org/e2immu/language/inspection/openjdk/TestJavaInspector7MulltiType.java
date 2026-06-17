package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaInspector7MulltiType {

    private JavaInspector javaInspector;
    private SourceSet sourceSet1;
    private SourceSet sourceSet2;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        sourceSet1 = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/")).build();
        sourceSet2 = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "2").setUri(URI.create("file:/"))
                .setDependencies(List.of(sourceSet1)).build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet1, sourceSet2)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String INPUTX1 = """
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

    @Language("java")
    private static final String INPUTX1_2 = """
            package a.b;
            class X1 {
                void method(String s) {
                    System.out.println("s = "+s);
                }
            }
            """;

    @Language("java")
    private static final String INPUTY = """
            package a.b;
            class Y {
                void method(String s) {
                    new X1().method("abc" + s);
                }
            }
            """;

    @Test
    public void test1() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(Map.of(sourceSet1, Map.of("a.b.X1", INPUTX1),
                                sourceSet2, Map.of("a.b.X1", INPUTX1_2, "a.b.Y", INPUTY)),
                        JavaInspectorImpl.DETAILED_SOURCES)
                .parseResult();

        // test the compiled types manager
        CompiledTypesManager ctm = javaInspector.compiledTypesManager();
        List<TypeInfo> parsed = ctm.typesLoaded(false);
        assertEquals(4, parsed.size());

        TypeInfo X1 = parseResult.firstType();
        assertEquals("test-protocol1::a.b.X1", X1.descriptor());
        assertTrue(X1.hasBeenInspected());
        List<TypeInfo> list = parseResult.typeByFullyQualifiedName("a.b.X1");
        assertEquals(2, list.size());
        assertSame(X1, list.getFirst());
        assertEquals("test-protocol2::a.b.X1", list.getLast().descriptor());

        TypeInfo Y = parseResult.findType("a.b.Y");
        MethodInfo method = Y.findUniqueMethod("method", 1);
        MethodCall mc = ((MethodCall) method.methodBody().statements().getFirst().expression());
        assertEquals("test-protocol2::a.b.X1.method(java.lang.String)", mc.methodInfo().descriptor());
    }
}
