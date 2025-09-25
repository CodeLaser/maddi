package org.e2immu.language.inspection.integration.java.other;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Resolver;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.impl.parser.*;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.parser.java.ParseHelperImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModuleInfo {
    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Language("java")
    private static final String MODULE_INFO = """
            open module org.e2immu.language.inspection.integration {
                requires org.e2immu.util.external.support;
                requires transitive org.e2immu.util.internal.util;
                requires static org.e2immu.language.cst.analysis;
                requires transitive static org.slf4j;
                requires java.xml;
            
                exports org.e2immu.language.inspection.integration;
                exports a.b to c.d;
            
                /*we must open*/
                opens a.b to c.d;
            
                uses a.b.C;
                // usesComment
                uses d.D;
            
                provides a.b.C with c.d.E;
                provides c.d.D with c.d.F;
            }
            """;

    @Test
    public void test0() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources(InputConfigurationImpl.MAVEN_MAIN)
                .addRestrictSourceToPackages(".")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        JavaInspectorImpl javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        Runtime runtime = javaInspector.runtime();
        Summary summary = new SummaryImpl(true); // once stable, change to false
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime), false);

        TypeContextImpl typeContext = new TypeContextImpl(runtime, javaInspector.compiledTypesManager(),
                new SourceTypeMapImpl(), true);
        Context rootContext = ContextImpl.create(runtime, javaInspector.compiledTypesManager(), summary, resolver,
                typeContext, true, false);
        SourceFile sourceFile = new SourceFile("/", URI.create("file:/"), null, null);
        ModuleInfo moduleInfo = javaInspector.parseModuleInfo(MODULE_INFO, sourceFile, rootContext);

        List<ModuleInfo.Requires> requires = moduleInfo.requires();
        assertEquals(5, requires.size());
        assertTrue(requires.get(1).isTransitive());
        assertFalse(requires.get(1).isStatic());
        assertTrue(requires.get(2).isStatic());
        assertFalse(requires.get(2).isTransitive());
        assertEquals("org.slf4j", requires.get(3).name());
        assertTrue(requires.get(3).isStatic());
        assertTrue(requires.get(3).isTransitive());

        List<ModuleInfo.Exports> exports = moduleInfo.exports();
        assertEquals(2, exports.size());
        assertEquals("org.e2immu.language.inspection.integration", exports.getFirst().packageName());
        assertNull(exports.getFirst().toPackageNameOrNull());
        assertEquals("c.d", exports.getLast().toPackageNameOrNull());

        List<ModuleInfo.Opens> opens = moduleInfo.opens();
        assertEquals(1, opens.size());
        ModuleInfo.Opens o0 = opens.getFirst();
        assertEquals("a.b", o0.packageName());
        assertEquals("c.d", o0.toPackageNameOrNull());
        assertEquals("12-11:12-13", o0.source().detailedSources().detail(o0.packageName()).compact2());
        assertEquals("12-18:12-20", o0.source().detailedSources().detail(o0.toPackageNameOrNull()).compact2());
        assertEquals("we must open", o0.comments().getFirst().comment());

        List<ModuleInfo.Uses> uses = moduleInfo.uses();
        assertEquals(2, uses.size());
        assertTrue(uses.getFirst().comments().isEmpty());
        assertEquals(" usesComment", uses.getLast().comments().getFirst().comment());

        List<ModuleInfo.Provides> provides = moduleInfo.provides();
        assertEquals(2, provides.size());
        ModuleInfo.Provides p0 = provides.getFirst();
        assertEquals("a.b.C", p0.api());
        assertEquals("c.d.E", p0.implementation());

        assertTrue(moduleInfo.open());
    }

    @Test
    public void test() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources(InputConfigurationImpl.MAVEN_MAIN)
                .addRestrictSourceToPackages(".")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        JavaInspector.ParseOptions options = JavaInspectorImpl.DETAILED_SOURCES;
        ParseResult parseResult = javaInspector.parse(options).parseResult();
        assertEquals(1, parseResult.sourceSetsByName().size());
        SourceSet sourceSet = parseResult.sourceSetsByName().values().stream().findFirst().orElseThrow();
        ModuleInfo moduleInfo = parseResult.moduleInfo(sourceSet);
        assertFalse(moduleInfo.open());
        assertEquals("[multiLineComment@1-1:3-3]", moduleInfo.comments().toString());
        assertEquals("org.e2immu.language.inspection.integration", moduleInfo.name());
        ModuleInfo.Requires req0 = moduleInfo.requires().getFirst();
        assertEquals("5-14:5-45", req0.source().detailedSources().detail(req0.name()).compact2());
        assertEquals("""
                        RequiresImpl[source=@5:5-5:46, comments=[], name=org.e2immu.util.external.support, \
                        isStatic=false, isTransitive=false]\
                        """,
                req0.toString());
        assertEquals(15, moduleInfo.requires().size());
        ModuleInfo.Requires lastReq = moduleInfo.requires().get(13);
        assertEquals(" used by DetectJREs, for MacOS", lastReq.comments().getFirst().comment());
    }
}
