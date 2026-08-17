package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*
 Regression test: a method reference whose javac-inferred type is an anonymous class declared elsewhere in the
 same expression (the anonymous 'new Map.Entry<>(){}' in .map(), referenced by 'Map.Entry::getKey' in .collect())
 is resolved before the anonymous's own 'new(){}' node has been scanned. ClassSymbolScanner previously asserted
 on the anonymous class's blank simple name (ElementStack.find("")); it now resolves it to a transient stub.
 Distilled from io.codelaser.jfocus.stdbase.api.encoder.CastData.Builder.build().
 */
public class TestAnonymousForwardReference {
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
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            import java.util.Map;
            import java.util.stream.Collectors;
            public class C {
                Map<String, String> build(List<String> in) {
                    return in.stream()
                            .map(s -> new Map.Entry<String, String>() {
                                public String getKey() { return s; }
                                public String getValue() { return s; }
                                public String setValue(String value) { return null; }
                            })
                            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }
            """;

    @Test
    public void test() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(
                Map.of(sourceSet, Map.of("a.b.C", INPUT)), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        TypeInfo c = parseResult.findType("a.b.C");
        assertNotNull(c);
        assertFalse(c.methodStream().findFirst().isEmpty());
    }
}
