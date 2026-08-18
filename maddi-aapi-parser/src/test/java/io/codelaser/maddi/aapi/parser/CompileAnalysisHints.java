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

package io.codelaser.maddi.aapi.parser;

import ch.qos.logback.classic.Level;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.integration.JavaInspectorFactory;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static io.codelaser.maddi.modification.common.CommonTest.javaInspectorFactory;

/**
 * Compiles the hand-written analysis hints in {@code maddi-aapi-archive/src/main/java} into the analysis-result
 * (.json) files under {@code .../analyzedPackageFiles/<library>}. This is the same work as
 * {@link TestAnalysisHintsCompiler}, but runnable as a build task ({@code gradle :maddi-aapi-parser:compileAnalysisHints})
 * rather than as a test. Paths are relative to the {@code maddi-aapi-parser} module directory (the task sets that
 * as the working directory).
 */
public class CompileAnalysisHints {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompileAnalysisHints.class);

    static final String HINTS_PATH = "../maddi-aapi-archive/src/main/java";
    static final String RESULTS_BASE =
            "../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/";
    // libs/support is deliberately absent, and has been: its OrgE2immuSupport.json is a generated file that
    // nothing regenerates and nothing loads (it is not in LoadAnalysisResults.ANALYZED_RESULTS either). The
    // pre-rename file name is the giveaway.
    static final List<String> LIBRARIES = List.of("jdk", "libs/test", "libs/log", "libs/kotlin");
    static final String KOTLIN_LIBRARY = "libs/kotlin";
    // fixed entry timestamp (2020-01-01T00:00:00Z) so a regenerated jar only differs when its content does
    private static final long FIXED_ENTRY_TIME = 1_577_836_800_000L;

    public static void main(String[] args) throws IOException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.aapi")).setLevel(Level.INFO);
        compileAll();
        packageJars();
    }

    /** Compile every configured library; reused by {@link TestAnalysisHintsCompiler}. */
    public static void compileAll() throws IOException {
        // the archive covers java.desktop (swing/awt) and java.net.http, which the lean default omits
        AnalysisHintsCompiler compiler = new AnalysisHintsCompiler(
                javaInspectorFactory("java.desktop", "java.net.http"));
        for (String library : LIBRARIES) {
            if (KOTLIN_LIBRARY.equals(library)) {
                // Its own compiler, with kotlin-stdlib on the class path. AnalysisHintsParser DECORATES a type it
                // can load rather than minting one from the shadow -- without the jar it logs "Ignoring type
                // 'kotlin.Lazy', cannot load it" and writes an empty library, which is how this was found.
                //
                // Deliberately NOT by widening the shared javaInspectorFactory: that class path is used by every
                // modification-* test, and a jar that merely makes more types resolvable can move verdicts. The
                // javadoc of materializeIgnoreModificationsFromFieldType records exactly that happening on
                // fernflower, where an "inert" change moved ConstantPool.pool from @Independent to @Dependent.
                compile(new AnalysisHintsCompiler(kotlinJavaInspectorFactory()), library);
            } else {
                compile(compiler, library);
            }
        }
    }

    /**
     * A factory whose class path is java.base + maddi-annotation + kotlin-stdlib: enough to load
     * {@code kotlin.Lazy} and to resolve the annotations the shadow puts on it, and nothing else.
     */
    private static JavaInspectorFactory kotlinJavaInspectorFactory() {
        SourceSet javaBase = SourceSetImpl.javaBase();
        // sourceSetOf, never a hand-rolled Builder: ClassSymbolScanner.ensureSourceSet attributes a loaded
        // 'jar:file:...!/...' class file by the JAR'S FILE NAME looked up among the source-set names, and
        // sourceSetOf is what derives that name. A friendlier name makes every type in the jar off-classpath.
        SourceSet maddiAnnotation = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet kotlinStdlib = SourceSetImpl.sourceSetOf(kotlin.Lazy.class);
        List<SourceSet> classPath = List.of(javaBase, maddiAnnotation, kotlinStdlib);

        return new JavaInspectorFactory() {
            @Override
            public List<SourceSet> dependencies() {
                return List.of(maddiAnnotation, kotlinStdlib);
            }

            @Override
            public JavaInspector withSources(SourceSet sourceSet) throws IOException {
                JavaInspector javaInspector = new JavaInspectorImpl();
                javaInspector.preload("java.base::java.util.");
                javaInspector.preload("java.base::java.lang.annotation");
                javaInspector.preload("io.codelaser.maddi.annotation.");
                javaInspector.initialize(new InputConfigurationImpl.Builder()
                        .addSourceSets(sourceSet)
                        .addClassPathParts(classPath.toArray(new SourceSet[0]))
                        .build());
                return javaInspector;
            }
        };
    }

    private static void compile(AnalysisHintsCompiler compiler, String library) throws IOException {
        AnalysisHints analysisHints = new AnalysisHints.Builder()
                .setLibraryName(library)
                .setAnalysisResultsDir(Path.of(RESULTS_BASE + library))
                .setHintsPath(Path.of(HINTS_PATH))
                .setPackagePrefix("io.codelaser.maddi.aapi.archive." + library.replace("/", "."))
                .build();
        LOGGER.info("Compiling analysis hints for library '{}'", library);
        compiler.go(analysisHints);
    }

    /**
     * Package the generated result files into the two archive jars (the former copyToJars.sh):
     * {@code openjdk.jar} holds {@code jdk/*.json} at its root, {@code libs.jar} holds {@code libs/<lib>/*.json}
     * keeping the {@code <lib>/} directory. They are loaded from the classpath as {@code resource:.../*.jar}.
     */
    static void packageJars() throws IOException {
        Path base = Path.of(RESULTS_BASE);
        Path jdk = base.resolve("jdk");
        try (var stream = Files.list(jdk)) {
            writeJar(base.resolve("openjdk.jar"), jdk, stream.filter(CompileAnalysisHints::isJson).sorted().toList());
        }
        // ⛔ ONLY the libraries in LIBRARIES, never a plain walk of libs/.
        //
        // A walk packages whatever happens to be on disk, and libs/support/OrgE2immuSupport.json is on disk
        // while being compiled by nothing and listed in LoadAnalysisResults.ANALYZED_RESULTS nowhere -- a
        // generated file left behind by an earlier LIBRARIES list. The committed libs.jar predates it and holds
        // only log/ and test/, so the first regeneration silently ADDED it, and maddi-ide-daemon (which loads
        // hints from resource:libs.jar) started reading stale contracts for io.codelaser.maddi.support.*:
        // TestEventualPolarity lost @ImmutableContainer(hc=true) on SetOnce. Packaging from the same list that
        // compiles keeps the jar and the compiled set from drifting apart at all.
        Path libs = base.resolve("libs");
        List<Path> libJson = new ArrayList<>();
        for (String library : LIBRARIES) {
            if (!library.startsWith("libs/")) continue;
            Path dir = base.resolve(library);
            if (!Files.isDirectory(dir)) continue;
            try (var stream = Files.list(dir)) {
                stream.filter(CompileAnalysisHints::isJson).sorted().forEach(libJson::add);
            }
        }
        writeJar(base.resolve("libs.jar"), libs, libJson);
    }

    private static boolean isJson(Path p) {
        return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json");
    }

    private static void writeJar(Path jarFile, Path base, List<Path> jsonFiles) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            // a minimal, fixed-time manifest first (as `jar cf` writes one), then the sorted .json entries
            putEntry(jos, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            for (Path file : jsonFiles) {
                String name = base.relativize(file).toString().replace(File.separatorChar, '/');
                putEntry(jos, name, Files.readAllBytes(file));
            }
        }
        LOGGER.info("Packaged {} ({} entries)", jarFile, jsonFiles.size());
    }

    private static void putEntry(JarOutputStream jos, String name, byte[] content) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(FIXED_ENTRY_TIME);
        jos.putNextEntry(entry);
        jos.write(content);
        jos.closeEntry();
    }
}
