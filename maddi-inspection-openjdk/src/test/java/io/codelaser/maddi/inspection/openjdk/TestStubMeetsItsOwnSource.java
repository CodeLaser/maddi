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

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ⛔⛔ <b>A STUB MET THE SOURCE IT STANDS IN FOR, AND THE SOURCE LOST.</b> {@code ClassSymbolScanner} mints a
 * stub — a {@code TypeInfo} on a {@code CompilationUnit} with <b>no source set</b>, GAP #163 — when a class file
 * names a type absent from the CURRENT source set's class path. Source sets are scanned in turn, each with its
 * own class path, so the very same type is routinely a SOURCE type of a LATER source set. When that later scan
 * reached its own declaration, {@code ScanCompilationUnit#visitClass} asked {@code typeData} what it already
 * knew of the name, got the stub, and called {@code .sourceSet().equals(...)} on it.
 * <p>
 * ⚠ The rule it failed to apply is not new, and is stated twice already: {@code InfoByFqn#isStub} — <i>"a real
 * type supersedes a stub, and a stub never supersedes a real type"</i> — and, since the trino measurement of
 * 2026-08-13, {@code ClassSymbolScanner#classTypeInfo}. {@code visitClass} is the third site that has to know
 * it, and the only one that had no test.
 * <p>
 * <b>Measured on the CodeLaser tree, 2026-08-21</b>, whole-repository parse, 162 source sets. Two stubs were
 * minted while scanning {@code codelaser-metrics-textindex/main}, which reads {@code ProjectData} from a class
 * file and does not have {@code codelaser-metrics-common} on its class path:
 * <pre>
 * WARN  MaddiDiagnosticCollector -- class file for io.codelaser.jfocus.metrics.common.Prepwork not found
 * WARN  ClassSymbolScanner       -- Creating stub type for io.codelaser.jfocus.metrics.common.Prepwork
 * ... 1.9 seconds later, scanning codelaser-metrics-common/main itself:
 * ERROR ScanCompilationUnit -- Caught exception in type Prepwork
 * java.lang.NullPointerException: Cannot invoke "SourceSet.equals(Object)" because the return value of
 *   "CompilationUnit.sourceSet()" is null   at ScanCompilationUnit.visitClass(ScanCompilationUnit.java:365)
 * </pre>
 * {@code Prepwork.java} and {@code MethodFlow.java} were dropped, and every later type of those two source sets
 * that named them then failed a second way ({@code CompilationUnit.uri()} null, from {@code classTypeInfo}) —
 * <b>a consequence, not a second defect</b>: the dropped unit is why the real type never got registered.
 * {@code codelaser-metrics-common} and {@code codelaser-metrics-dataflow} were unparseable for as long as this
 * held, and with them the six build units downstream of them.
 * <p>
 * ⚠ The fixture below is that shape and nothing more: {@code svc.ProjectData} is compiled against
 * {@code common.Prep} and published as a class file; the consumer source set gets {@code ProjectData} and NOT
 * {@code Prep}; and a second source set holds {@code Prep}'s own source. The gap is what mints the stub, so
 * {@link #theClassPathGapIsReal()} is the control — without it the parse is clean, which is what makes the
 * warning in the gap run evidence rather than noise.
 */
public class TestStubMeetsItsOwnSource {

    /** The type that goes missing: its class file is deliberately kept off the consumer's class path. */
    @Language("java")
    private static final String PREP = """
            package common;
            public class Prep {
                private final String name;
                public Prep(String name) { this.name = name; }
                public String name() { return name; }
            }
            """;

    /** Compiled against Prep, and it LEAKS it: prep() is on the interface, so completing it needs Prep. */
    @Language("java")
    private static final String PROJECT_DATA = """
            package svc;
            import common.Prep;
            public interface ProjectData {
                Prep prep();
                String id();
            }
            """;

    /** Reads ProjectData from its class file, never touches prep(); this is what a real consumer looks like. */
    @Language("java")
    private static final String USE = """
            package use;
            import svc.ProjectData;
            public class Use {
                public String go(ProjectData data) { return data.id(); }
            }
            """;

    /** What one run produced, so the gap run and its control can be compared field by field. */
    private record Observation(boolean haveErrors, List<String> parseExceptions, String prepSourceSet,
                               List<String> prepMethods, String prepAsSeenByProjectData) {
    }

    /**
     * ⭐ The regression. {@code common.Prep} is a source type of the second source set, so after the parse it
     * must be the real type: on a compilation unit that HAS a source set, carrying the method it declares.
     */
    @DisplayName("a stub minted by an earlier source set does not displace the source it stands in for")
    @Test
    public void aStubDoesNotDisplaceTheSourceItStandsInFor() throws Exception {
        Observation o = run(true);
        assertEquals(TEST_PROTOCOL + "B", o.prepSourceSet(),
                "common.Prep must come out of the source set that DECLARES it, not off the stub");
        assertEquals(List.of("name"), o.prepMethods(), "and it must be the real type, members and all");
        assertTrue(o.parseExceptions().isEmpty(), "nothing may be dropped: " + o.parseExceptions());
        assertFalse(o.haveErrors(), "the parse succeeds");
    }

    /**
     * ⛔ The control, and it is what makes the run above mean something. Give the consumer {@code common.Prep}
     * on its class path and no stub is minted at all — so the gap run's warning is the gap, not background.
     */
    @DisplayName("control: with Prep on the consumer's class path the same parse is clean")
    @Test
    public void theClassPathGapIsReal() throws Exception {
        Observation gap = run(true);
        Observation control = run(false);
        assertEquals(TEST_PROTOCOL + "B", control.prepSourceSet());
        assertEquals(List.of("name"), control.prepMethods());
        assertEquals("(stub, no source set)", gap.prepAsSeenByProjectData(),
                "with the gap the consumer resolves ProjectData.prep() to a stub -- if this ever stops being"
                + " true the fixture has stopped minting one and the test above proves nothing");
        // ⚠ Asserted as "a real source set", not by name. The control answers java.base, which is NOT the
        // 'prep-classes' set it was given: ensureSourceSet's directory-prefix lookup takes the first
        // sourceSetDirPrefixes entry whose key the path starts with, and that map is unordered. Noted as an
        // observation, not fixed here -- what this control has to establish is that the gap run's stub is the
        // gap, and a type on a real source set is exactly that.
        assertNotEquals("(stub, no source set)", control.prepAsSeenByProjectData(),
                "without the gap Prep resolves to a real source set, so no stub is minted at all");
    }

    private Observation run(boolean withGap) throws Exception {
        Path tmp = Files.createTempDirectory("stub-meets-source-");
        try {
            Path prepClasses = compile(tmp, "prep", Map.of("common.Prep", PREP));
            Path svcClasses = compile(tmp, "svc", Map.of("svc.ProjectData", PROJECT_DATA), prepClasses);

            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet svcLib = new SourceSetImpl.Builder()
                    .setName("svc-classes").setUri(svcClasses.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();
            SourceSet prepLib = new SourceSetImpl.Builder()
                    .setName("prep-classes").setUri(prepClasses.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();

            // ⛔ THE GAP: svc.ProjectData is on the class path, common.Prep -- which its signature names -- is not.
            SourceSet consumer = new SourceSetImpl.Builder().setName(TEST_PROTOCOL)
                    .setUri(URI.create("file:/consumer/"))
                    .setDependencies(withGap ? List.of(javaBase, svcLib) : List.of(javaBase, svcLib, prepLib))
                    .build();
            // ...and here is Prep's own source. Declared second, and computeScanOrder keeps declaration order
            // between source sets that do not depend on each other, so the consumer really is scanned first.
            SourceSet prepMain = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "B")
                    .setUri(URI.create("file:/prep/"))
                    .setDependencies(List.of(javaBase)).build();

            InputConfigurationImpl.Builder icb = new InputConfigurationImpl.Builder();
            if (withGap) icb.addClassPathParts(javaBase, svcLib);
            else icb.addClassPathParts(javaBase, svcLib, prepLib);
            InputConfiguration ic = icb.addSourceSets(consumer, prepMain).build();

            JavaInspector javaInspector = new JavaInspectorImpl();
            javaInspector.initialize(ic);

            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(consumer, Map.of("use.Use", USE));
            sources.put(prepMain, Map.of("common.Prep", PREP));

            Summary summary = javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
            TypeInfo prep = summary.parseResult().findType("common.Prep");
            SourceSet prepSet = prep == null ? null : prep.compilationUnit().sourceSet();
            Observation o = new Observation(summary.haveErrors(),
                    summary.parseExceptions().stream().map(e -> String.valueOf(e.getMessage())).toList(),
                    prep == null ? "(absent)" : prepSet == null ? "(stub, no source set)" : prepSet.name(),
                    prep == null ? List.of() : prep.methods().stream().map(m -> m.name()).sorted().toList(),
                    describe(prepReturnedByProjectData(summary)));
            System.out.println("stub-meets-source " + (withGap ? "WITH THE GAP" : "CONTROL, no gap")
                               + " >>> " + o + "\n<<<");
            return o;
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted((x, y) -> y.getNameCount() - x.getNameCount()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    /**
     * The consumer's own view of the missing type, reached entirely through the parse result: {@code use.Use}'s
     * parameter is {@code svc.ProjectData}, loaded from its class file while the consumer was scanned, and
     * {@code prep()}'s return type is the type that could not be resolved there. This is the field that tells the
     * gap run apart from its control — the stub minting itself only reaches a logger.
     */
    private static TypeInfo prepReturnedByProjectData(Summary summary) {
        TypeInfo use = summary.parseResult().findType("use.Use");
        if (use == null) return null;
        return use.methods().stream()
                .filter(m -> "go".equals(m.name())).findFirst()
                .map(m -> m.parameters().getFirst().parameterizedType().typeInfo())
                .flatMap(pd -> pd.methods().stream().filter(m -> "prep".equals(m.name())).findFirst())
                .map(m -> m.returnType().typeInfo())
                .orElse(null);
    }

    private static String describe(TypeInfo typeInfo) {
        if (typeInfo == null) return "(absent)";
        SourceSet sourceSet = typeInfo.compilationUnit().sourceSet();
        return sourceSet == null ? "(stub, no source set)" : sourceSet.name();
    }

    private static Path compile(Path tmp, String name, Map<String, String> sources, Path... classpath)
            throws IOException {
        Path src = Files.createDirectories(tmp.resolve(name + "-src"));
        Path classes = Files.createDirectories(tmp.resolve(name + "-classes"));
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path javaFile = src.resolve(e.getKey().replace('.', '/') + ".java");
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, e.getValue());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        List<String> args = new ArrayList<>(List.of("-d", classes.toString()));
        if (classpath.length > 0) {
            args.add("-cp");
            args.add(Stream.of(classpath).map(Path::toString)
                    .reduce((x, y) -> x + File.pathSeparator + y).orElseThrow());
        }
        try (Stream<Path> walk = Files.walk(src)) {
            walk.filter(p -> p.toString().endsWith(".java")).map(Path::toString).forEach(args::add);
        }
        int rc = compiler.run(null, null, null, args.toArray(String[]::new));
        assertEquals(0, rc, "the fixture's own compilation must succeed: " + name);
        return classes;
    }
}
