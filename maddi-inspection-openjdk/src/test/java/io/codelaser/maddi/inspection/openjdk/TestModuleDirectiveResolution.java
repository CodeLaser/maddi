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

package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Gap ledger {@code #201}. A {@code uses}/{@code provides} directive names a service type, and
 * {@code ComputeCallGraph} turns each resolved one into a module→type edge. <b>Three defects stood between the
 * written directive and that edge, stacked, each invisible behind the one in front of it.</b>
 *
 * <ol>
 *   <li><b>The imports were parsed and thrown away.</b> {@code ParseModuleInfo} skipped every import
 *       declaration on its way to the {@code module} keyword — with a comment saying what they are for — and
 *       was handed an already-built {@code CompilationUnit}, so the descriptor's import list was always empty.</li>
 *   <li><b>Resolution treated the written text as an FQN.</b> The parse stores {@code apiNode.getSource()}
 *       deliberately (a refactoring rewrites a directive the way its author wrote it), so a short name resolved
 *       to nothing.</li>
 *   <li><b>And the inspector every real run uses did not resolve at all.</b> The congocc inspector had
 *       {@code resolveModuleInfo}; the <b>openjdk</b> one — this one — never called {@code setApiResolved}, so
 *       {@code apiResolved()} was null for <em>every</em> directive, short or qualified.</li>
 * </ol>
 *
 * ⛔⛔ <b>NOTHING COULD SEE ANY OF IT.</b> {@code ComputeCallGraph} skips a null api and
 * {@code implementationsResolved()} defaults to an empty list, so the consequence is a call graph that is
 * quietly missing edges — the rule-1 family: a wrong result no gate can see. Measured on Elasticsearch's 119
 * descriptors: <b>215 service names</b> (19 {@code uses}, 70 {@code provides} apis, 126 implementations),
 * <b>37 of them written short</b>, and not one edge among them.
 * <p>
 * ⚠ This test lives in the <b>openjdk</b> inspector on purpose: layer 3 means a test anywhere else would have
 * passed while the production path stayed broken.
 */
public class TestModuleDirectiveResolution {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setSourceDirectories(List.of()).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String SERVICE = "package a.b; public interface Service { void go(); }";
    @Language("java")
    private static final String IMPL = "package a.b; public class Impl implements Service { public void go() { } }";
    @Language("java")
    private static final String OTHER = "package x.y; public interface Other { }";

    /**
     * Both forms in one descriptor, which is what the corpus looks like: {@code Service} short through an
     * import, {@code x.y.Other} qualified, and one short name with <b>no</b> import at all.
     */
    @Language("java")
    private static final String MODULE_INFO = """
            import a.b.Service;
            import a.b.Impl;
            module m {
                uses Service;
                uses x.y.Other;
                uses NotImported;
                provides Service with Impl;
            }
            """;

    private ModuleInfo parse() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(false).setIgnoreModule(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of(
                "a.b.Service", SERVICE, "a.b.Impl", IMPL, "x.y.Other", OTHER,
                "module-info", MODULE_INFO), options).parseResult();
        ModuleInfo moduleInfo = parseResult.moduleInfo(sourceSet);
        assertNotNull(moduleInfo, "the descriptor itself must parse");
        return moduleInfo;
    }

    /** Layer 1, alone: without the imports in the compilation unit there is nothing to resolve THROUGH. */
    @DisplayName("a module descriptor's own imports are in its compilation unit")
    @Test
    public void theImportsSurviveTheParse() {
        ModuleInfo moduleInfo = parse();

        assertEquals(Map.of("Service", "a.b.Service", "Impl", "a.b.Impl"), moduleInfo.importedShortNames());
        assertEquals("a.b.Service", moduleInfo.resolveDirectiveName("Service"));
        assertEquals("x.y.Other", moduleInfo.resolveDirectiveName("x.y.Other"), "a qualified name is itself");
        assertEquals("NotImported", moduleInfo.resolveDirectiveName("NotImported"),
                "an unimportable short name stays itself: this resolves, it never guesses a package");
    }

    @DisplayName("a short-form `uses` resolves through the import; the written form is untouched")
    @Test
    public void aShortUsesResolves() {
        ModuleInfo moduleInfo = parse();
        ModuleInfo.Uses shortForm = moduleInfo.uses().getFirst();

        assertEquals("Service", shortForm.api(), "the WRITTEN text is kept -- a refactoring rewrites it as written");
        assertNotNull(shortForm.apiResolved(), "and it resolves anyway, through the descriptor's own import");
        assertEquals("a.b.Service", shortForm.apiResolved().fullyQualifiedName());
    }

    /**
     * ⚠ CONTROL, and it is the one that catches layer 3 on its own: the qualified form needed no import rule and
     * was ALSO unresolved, because this inspector never resolved anything.
     */
    @DisplayName("CONTROL: a qualified `uses` resolves too — the reader had none at all")
    @Test
    public void aQualifiedUsesResolves() {
        ModuleInfo moduleInfo = parse();
        ModuleInfo.Uses qualified = moduleInfo.uses().get(1);

        assertEquals("x.y.Other", qualified.api());
        assertNotNull(qualified.apiResolved(), "nothing about this one is short-form; it was simply never resolved");
        assertEquals("x.y.Other", qualified.apiResolved().fullyQualifiedName());
    }

    /** ⚠ CONTROL in the other direction: a name that resolves nowhere stays unresolved rather than invented. */
    @DisplayName("CONTROL: a short name with no import resolves to nothing, and nothing is guessed")
    @Test
    public void anUnimportedShortNameStaysUnresolved() {
        ModuleInfo moduleInfo = parse();
        ModuleInfo.Uses unresolvable = moduleInfo.uses().get(2);

        assertEquals("NotImported", unresolvable.api());
        assertNull(unresolvable.apiResolved());
    }

    /**
     * ⛔⛔ THE OTHER PARSER, AND THE PATH THE DESCRIPTOR WRITERS USE. {@code parseModuleInfo(Path)} reads a
     * {@code module-info.java} that is <b>not part of any parsed source set</b> — {@code addExportTargets} and
     * {@code addRequires} rely on exactly this to edit a descriptor in a project nobody asked to refactor. It
     * goes through the congocc {@code ParseModuleInfo}, which skipped every import declaration on its way to the
     * {@code module} keyword, so the descriptor came back with an EMPTY import list and a short name in it could
     * not be resolved by anything.
     * <p>
     * ⚠ THIS IS A SEPARATE TEST BECAUSE THE OTHER ONES DO NOT REACH THAT CODE: javac scans the in-memory
     * descriptor itself and its scanner always did collect the imports. A fix and a test that never meet look
     * exactly like a fix that works.
     */
    @DisplayName("parseModuleInfo(Path) — the foreign-descriptor path — keeps the imports too")
    @Test
    public void theStandaloneDescriptorParserKeepsItsImports() throws IOException {
        Path dir = Files.createTempDirectory("moduleinfo");
        Path file = dir.resolve("module-info.java");
        Files.writeString(file, MODULE_INFO);

        ModuleInfo moduleInfo = javaInspector.parseModuleInfo(file);

        assertNotNull(moduleInfo);
        assertEquals(Map.of("Service", "a.b.Service", "Impl", "a.b.Impl"), moduleInfo.importedShortNames(),
                "the imports of a descriptor read from disk, which is the only way to resolve its short names");
        assertEquals("a.b.Service", moduleInfo.resolveDirectiveName(moduleInfo.uses().getFirst().api()));
    }

    @DisplayName("`provides X with Y`: both the api and the implementation resolve through imports")
    @Test
    public void providesResolvesApiAndImplementations() {
        ModuleInfo moduleInfo = parse();
        ModuleInfo.Provides provides = moduleInfo.provides().getFirst();

        assertEquals("Service", provides.api());
        assertNotNull(provides.apiResolved());
        assertEquals("a.b.Service", provides.apiResolved().fullyQualifiedName());
        assertEquals(List.of("Impl"), provides.implementations(), "written short, and kept that way");
        assertEquals(List.of("a.b.Impl"), provides.implementationsResolved().stream()
                .map(t -> t.fullyQualifiedName()).toList());
    }
}
