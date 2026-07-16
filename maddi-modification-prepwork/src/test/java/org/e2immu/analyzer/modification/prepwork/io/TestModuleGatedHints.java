package org.e2immu.analyzer.modification.prepwork.io;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.language.cst.api.element.SourceSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Analysis hints (AAPI) must be loaded only for the modules present on the classpath. With java.desktop and
 * java.net.http absent from the classpath, loading the JDK archive must not throw (it used to throw
 * UnsupportedOperationException "Cannot find javax.swing.text.JTextComponent"), must skip the swing/awt/http hints,
 * and must not have loaded those types — while java.util hints still load normally.
 */
public class TestModuleGatedHints {

    private JavaInspector inspectorWithoutDesktop() throws Exception {
        SourceSet javaBase = SourceSetImpl.javaBase();
        JavaInspector ji = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl();
        ji.preload("java.base::java.util.");
        // deliberately NOT preloading java.desktop / java.net.http, and NOT on the classpath below
        InputConfiguration ic = new InputConfigurationImpl.Builder()
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .addClassPathParts(javaBase) // java.base only
                .build();
        ji.initialize(ic);
        ji.onlyPreload();
        return ji;
    }

    @Test
    public void hintsGatedByClasspath() throws Exception {
        JavaInspector ji = inspectorWithoutDesktop();
        LoadAnalysisResults lar = new LoadAnalysisResults(ji.runtime(), SourceSetImpl.testProtocolSourceSet());

        // must NOT throw despite the archive containing javax.swing / java.awt / java.net.http hints
        int loaded = assertDoesNotThrow(() -> lar.go(List.of(LoadAnalysisResults.ANALYZED_RESULTS_JDK)));

        assertTrue(loaded > 0, "some java.base hints must have loaded");
        assertTrue(lar.skippedPrimaryTypes() > 0, "swing/awt/http hints must have been skipped");

        List<TypeInfo> typesLoaded = ji.compiledTypesManager().typesLoaded(true);
        assertTrue(typesLoaded.stream().anyMatch(t -> "java.util.List".equals(t.fullyQualifiedName())),
                "java.util.List must be loaded (hints applied)");
        assertFalse(typesLoaded.stream().anyMatch(t -> t.packageName().startsWith("javax.swing")),
                "no swing type should have been loaded (module absent from classpath)");
    }
}
