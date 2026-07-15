package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A compilation unit's fingerprint is the MD5 of its source text — the prerequisite for
 * {@link JavaInspector#reloadSources}, which decides "did this file change?" by comparing the fingerprint it holds
 * against a freshly computed one. Must agree with the in-house inspector, which fingerprints the same way.
 */
public class TestFingerPrint {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class X {
                public int method(int i) { return i + 1; }
            }
            """;

    private ParseResult parse(boolean computeFingerPrints) throws IOException {
        JavaInspector javaInspector = new JavaInspectorImpl(computeFingerPrints, false);
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/")).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
        return javaInspector.parseMultiSourceSet(Map.of(sourceSet, Map.of("a.b.X", INPUT)),
                JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    @DisplayName("with fingerprints on: the compilation unit carries the MD5 of its source")
    @Test
    public void testFingerPrintComputed() throws IOException {
        TypeInfo x = parse(true).findType("a.b.X");
        assertNotNull(x);
        FingerPrint fp = x.compilationUnit().fingerPrintOrNull();
        assertNotNull(fp, "no fingerprint on the compilation unit");
        assertFalse(fp.isNoFingerPrint(), "expected a real fingerprint, not NO_FINGERPRINT");
        // the fingerprint is exactly the MD5 of the source text javac read — the same rule the in-house inspector
        // applies, so both inspectors agree on whether a given file changed
        assertEquals(MD5FingerPrint.compute(INPUT).toString(), fp.toString());
    }

    @DisplayName("with fingerprints off (the default): none is computed")
    @Test
    public void testFingerPrintNotComputed() throws IOException {
        TypeInfo x = parse(false).findType("a.b.X");
        assertNotNull(x);
        FingerPrint fp = x.compilationUnit().fingerPrintOrNull();
        assertTrue(fp == null || fp.isNoFingerPrint(), "expected no fingerprint, got " + fp);
    }
}
