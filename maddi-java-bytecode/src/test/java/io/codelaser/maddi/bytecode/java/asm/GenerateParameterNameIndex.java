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

package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.ParameterNameIndex;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.integration.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.integration.ResourcesImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Run-on-demand tool that generates {@link ParameterNameIndex} files from the (faithful) ASM loader, strictly one
 * per JDK module / jar, to be shipped as resources next to {@code analyzedPackageFiles} and consulted by the
 * javac-based loader.
 * <p>
 * Full generation is invoked via {@link #main(String[])} (it is heavy: it loads every type via ASM). All JDK
 * modules are put in scope so cross-module parameter types resolve, but enumeration is filtered by the
 * originating module's {@link SourceFile#sourceSet()}, so each file contains exactly that module's types.
 */
public class GenerateParameterNameIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateParameterNameIndex.class);

    // by default, generate next to the analyzed-package files (relative to the maddi-java-bytecode module dir)
    private static final String DEFAULT_OUTPUT_DIR =
            "maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/parameterNames";
    private static final List<String> DEFAULT_MODULES = List.of("java.base", "java.desktop", "java.net.http");

    private record Setup(Resources resources, CompiledTypesManager ctm, Map<String, SourceSet> sourceSetByModule) {
    }

    private static Setup setup(List<Path> jmodFiles) throws IOException {
        Runtime runtime = new RuntimeImpl();
        Resources cp = new ResourcesImpl(Path.of("."));
        Map<String, SourceSet> byModule = new HashMap<>();
        SourceSet javaBase = null;
        for (Path jmod : jmodFiles) {
            String module = jmod.getFileName().toString().replaceFirst("\\.jmod$", "");
            SourceSet sourceSet = new SourceSetImpl.Builder().setName(module)
                    .setUri(URI.create("file:unknown")).setLibrary(true).setExternalLibrary(true).setPartOfJdk(true)
                    .setModule(true).build();
            sourceSet.computePriorityDependencies();
            byModule.put(module, sourceSet);
            if ("java.base".equals(module)) javaBase = sourceSet;
            URI uri = URI.create("jar:file:" + jmod + "!/");
            cp.addJmod(new SourceFile(uri.getRawSchemeSpecificPart(), uri, sourceSet, null));
        }
        SourceSet primary = javaBase != null ? javaBase : byModule.values().iterator().next();
        CompiledTypesManagerImpl mgr = new CompiledTypesManagerImpl(primary, cp);
        ByteCodeInspectorImpl byteCodeInspector = new ByteCodeInspectorImpl(runtime, mgr, true,
                false);
        mgr.setByteCodeInspector(byteCodeInspector);
        mgr.addToTrie(cp, true);
        mgr.addPredefinedTypeInfoObjects(runtime.predefinedObjects(), primary);
        return new Setup(cp, mgr, byModule);
    }

    private static List<Path> allJmods() throws IOException {
        try (Stream<Path> s = Files.list(Path.of(System.getProperty("java.home"), "jmods"))) {
            return s.filter(p -> p.getFileName().toString().endsWith(".jmod")).sorted().toList();
        }
    }

    private static ParameterNameIndex generateModule(Setup setup, String module) {
        SourceSet sourceSet = setup.sourceSetByModule().get(module);
        if (sourceSet == null) throw new IllegalArgumentException("Module not in setup: " + module);
        // strictly this module's own types (each class file carries its originating jmod's source set)
        return BuildParameterNameIndex.build(setup.ctm(), setup.resources(), sourceSet,
                sf -> module.equals(sf.sourceSet().name()));
    }

    public static void writeToFile(ParameterNameIndex index, Path outputFile) throws IOException {
        index.write(outputFile); // gzip-compressed when the name ends with .gz
    }

    /** args: {@code <outputDir> [module...]}; defaults to the archive parameterNames dir and java.base/desktop/net.http. */
    public static void main(String[] args) throws IOException {
        Path outputDir = Path.of(args.length > 0 ? args[0] : DEFAULT_OUTPUT_DIR);
        List<String> modules = args.length > 1 ? Arrays.asList(args).subList(1, args.length) : DEFAULT_MODULES;
        // every JDK module in scope, so parameter types from other modules resolve; enumeration is per-module
        Setup setup = setup(allJmods());
        for (String module : modules) {
            ParameterNameIndex index = generateModule(setup, module);
            Path out = outputDir.resolve(module + ".paramnames.gz");
            writeToFile(index, out);
            LOGGER.info("Wrote {} ({} methods)", out.toAbsolutePath(), index.size());
        }
    }

    /**
     * These two tests drive the index generator from real {@code .jmod} files on disk (see
     * {@link #setup(List)} and {@link #allJmods()}), so unlike the rest of this module they cannot use the
     * {@code jrt} runtime-image fallback without reworking the generator's API. They therefore skip on a JDK
     * that ships no {@code jmods/} directory (Eclipse Temurin, and hence CI). The skip is explicit and shows
     * up in the test report; it is not a silent pass.
     */
    private static void requireJmodsDirectory() {
        Path jmods = Path.of(System.getProperty("java.home"), "jmods");
        Assumptions.assumeTrue(Files.isDirectory(jmods),
                "generator reads .jmod files directly; this JDK has no " + jmods);
    }

    @Test
    public void testGenerateModuleSliceToFile() throws IOException {
        requireJmodsDirectory();
        // fast: java.base only (self-contained), enumerate the small java.lang.ref package
        Setup setup = setup(List.of(Path.of(System.getProperty("java.home"), "jmods", "java.base.jmod")));
        ParameterNameIndex index = BuildParameterNameIndex.build(setup.ctm(), setup.resources(),
                setup.sourceSetByModule().get("java.base"),
                sf -> sf.fullyQualifiedNameFromPath().startsWith("java.lang.ref."));
        assertTrue(index.size() > 0, "expected some methods, got " + index.size());

        Path out = Path.of("build/test-parameterNames/java.lang.ref.paramnames.gz");
        writeToFile(index, out);
        ParameterNameIndex reloaded = ParameterNameIndex.read(out); // gzip detected by extension
        assertEquals(index.size(), reloaded.size());
    }

    @Test
    public void testMultiModuleStrictlyOneModule() throws IOException {
        requireJmodsDirectory();
        // java.desktop needs java.base in scope to resolve parameter types
        Setup setup = setup(List.of(
                Path.of(System.getProperty("java.home"), "jmods", "java.base.jmod"),
                Path.of(System.getProperty("java.home"), "jmods", "java.desktop.jmod")));
        // enumerate a small slice of java.desktop; the source-set filter is what guarantees "strictly one module"
        ParameterNameIndex index = BuildParameterNameIndex.build(setup.ctm(), setup.resources(),
                setup.sourceSetByModule().get("java.desktop"),
                sf -> "java.desktop".equals(sf.sourceSet().name())
                      && sf.fullyQualifiedNameFromPath().startsWith("java.awt.geom."));
        assertTrue(index.size() > 0, "expected java.awt.geom methods (loaded with java.base in scope)");

        // strictly this module: no java.base type leaked into the file despite being in scope
        StringWriter sw = new StringWriter();
        index.write(sw);
        for (String line : sw.toString().split("\n")) {
            assertTrue(line.startsWith("java.awt.geom."), "unexpected key: " + line);
        }
    }
}
