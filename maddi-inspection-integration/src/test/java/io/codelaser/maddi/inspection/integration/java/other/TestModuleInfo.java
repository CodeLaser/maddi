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

package io.codelaser.maddi.inspection.integration.java.other;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Context;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Resolver;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.api.resource.SourceFile;
import io.codelaser.maddi.inspection.impl.parser.ContextImpl;
import io.codelaser.maddi.inspection.impl.parser.ResolverImpl;
import io.codelaser.maddi.inspection.impl.parser.TypeContextImpl;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SummaryImpl;
import io.codelaser.maddi.parser.java.ParseHelperImpl;
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
            open module io.codelaser.maddi.inspection.integration {
                requires io.codelaser.maddi.support;
                requires transitive io.codelaser.maddi.util;
                requires static io.codelaser.maddi.cst.analysis;
                requires transitive static org.slf4j;
                requires java.xml;
            
                exports io.codelaser.maddi.inspection.integration;
                exports a.b to c.d;
            
                /*we must open*/
                opens a.b to c.d;
            
                uses a.b.C;
                // usesComment
                uses d.D;
            
                provides a.b.C with c.d.E;
                provides c.d.D with c.d.F, c.d.E;
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

        TypeContextImpl typeContext = new TypeContextImpl(runtime, javaInspector.compiledTypesManager(), true);
        Context rootContext = ContextImpl.create(runtime, javaInspector.compiledTypesManager(), summary, resolver,
                typeContext, true, false);
        SourceFile sourceFile = new SourceFile("ignore", URI.create("file:ignore"), null, null);
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
        assertEquals("io.codelaser.maddi.inspection.integration", exports.getFirst().packageName());
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
        assertEquals(List.of("c.d.E"), p0.implementations());
        // the home-made (congocc) parser keeps EVERY implementation of a multi-impl 'provides ... with A, B'
        ModuleInfo.Provides p1 = provides.getLast();
        assertEquals("c.d.D", p1.api());
        assertEquals(List.of("c.d.F", "c.d.E"), p1.implementations());
        // each impl name keeps its own detailed source (identity-keyed: pass the model's own string instance)
        assertNotNull(p1.source().detailedSources().detail(p1.implementations().getFirst()));
        assertNotNull(p1.source().detailedSources().detail(p1.implementations().getLast()));

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
        assertEquals("io.codelaser.maddi.inspection.integration", moduleInfo.name());
        ModuleInfo.Requires req0 = moduleInfo.requires().getFirst();
        assertEquals("5-14:5-45", req0.source().detailedSources().detail(req0.name()).compact2());
        assertEquals("""
                        RequiresImpl[source=@5:5-5:46, comments=[], name=io.codelaser.maddi.support, \
                        isStatic=false, isTransitive=false]\
                        """,
                req0.toString());
        assertEquals(15, moduleInfo.requires().size());
        ModuleInfo.Requires lastReq = moduleInfo.requires().get(13);
        assertEquals(" used by DetectJREs, for MacOS", lastReq.comments().getFirst().comment());
    }
}
