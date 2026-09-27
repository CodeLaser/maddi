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
import io.codelaser.maddi.cst.api.analysis.Message;
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
    /**
     * Compiled like {@link #LIBRARIES}, but NOT packed into {@code libs.jar} and NOT in
     * {@code LoadAnalysisResults.ANALYZED_RESULTS}: campaign hints for third-party libraries (the analysis-hints
     * campaign) that a consumer preloads explicitly with {@code --preload-analysis-results-dirs
     * .../analyzedPackageFiles/libs/vavr}. {@code libs.jar} goes to every maddi user; a library moves there only on
     * evidence of universal value. Each has a report next to its hints ({@code libs/vavr/VAVR.md}).
     */
    static final List<String> SIDE_LOADED_LIBRARIES = List.of("libs/vavr");
    static final String VAVR_LIBRARY = "libs/vavr";
    /** {@link #RESULTS_BASE} as a path; the directory the committed results live in. */
    static final Path RESULTS_BASE_DIR = Path.of(RESULTS_BASE);
    static final String KOTLIN_LIBRARY = "libs/kotlin";
    static final String JDK_LIBRARY = "jdk";
    /**
     * The JDK feature release the committed {@code jdk/} results (and so {@code openjdk.jar}) were generated on.
     * <p>
     * ⚠ <b>The {@code jdk} library compiles differently on every JDK</b>: its shadows decorate the RUNNING JDK's
     * types, and 27 has members 26 does not. The data can therefore be current for one release only, and the
     * one it is current for is the CI JDK's, recorded here. On another JDK {@link #main} regenerates only the
     * {@code libs/*} libraries and {@link TestAnalysisHintsCompiler} compares only those, so a developer on a
     * newer JDK neither sees a red test nor can overwrite CI's data by regenerating. Moving the data to the
     * running JDK is explicit: {@code gradle :maddi-aapi-parser:compileAnalysisHints -Pmaddi.aapi.moveJdkRelease=true}.
     */
    static final Path JDK_RELEASE_FILE = RESULTS_BASE_DIR.resolve(JDK_LIBRARY).resolve("jdk-release.txt");
    static final String MOVE_JDK_RELEASE = "maddi.aapi.moveJdkRelease";
    // fixed entry timestamp (2020-01-01T00:00:00Z) so a regenerated jar only differs when its content does
    private static final long FIXED_ENTRY_TIME = 1_577_836_800_000L;

    public static void main(String[] args) throws IOException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.aapi")).setLevel(Level.INFO);
        int committed = committedJdkRelease();
        int running = runningJdkRelease();
        boolean withJdk = running == committed || Boolean.getBoolean(MOVE_JDK_RELEASE);
        if (!withJdk) {
            LOGGER.warn("The committed jdk results are for JDK {}, this is JDK {}: regenerating the libs only."
                        + " To move the jdk results to JDK {}, pass -P{}=true", committed, running, running,
                    MOVE_JDK_RELEASE);
        }
        compileAll(RESULTS_BASE_DIR, withJdk);
        packageJars(); // openjdk.jar is repacked from the jdk results on disk, unchanged when they were skipped
        if (withJdk && running != committed) {
            Files.writeString(JDK_RELEASE_FILE, running + "\n");
            LOGGER.warn("Moved the jdk results from JDK {} to JDK {}", committed, running);
        }
    }

    /** The release recorded in {@link #JDK_RELEASE_FILE}. */
    static int committedJdkRelease() throws IOException {
        return Integer.parseInt(Files.readString(JDK_RELEASE_FILE).trim());
    }

    static int runningJdkRelease() {
        return java.lang.Runtime.version().feature();
    }

    /** Compile every configured library into the committed location. */
    public static void compileAll() throws IOException {
        compileAll(RESULTS_BASE_DIR);
    }

    /**
     * Compile every configured library into {@code resultsBase}, laid out as {@code <resultsBase>/<library>}.
     * <p>
     * The directory is a parameter so that {@link TestAnalysisHintsCompiler} can compile into a temporary one and
     * compare: a test must not write into the working tree. Only {@link #main} passes the committed location.
     */
    public static void compileAll(Path resultsBase) throws IOException {
        compileAll(resultsBase, true);
    }

    /** As {@link #compileAll(Path)}; {@code includeJdk} false leaves the {@code jdk} library out (see {@link #JDK_RELEASE_FILE}). */
    public static void compileAll(Path resultsBase, boolean includeJdk) throws IOException {
        // the archive covers java.desktop (swing/awt) and java.net.http, which the lean default omits
        AnalysisHintsCompiler compiler = new AnalysisHintsCompiler(
                javaInspectorFactory("java.desktop", "java.net.http"));
        for (String library : LIBRARIES) {
            if (!includeJdk && JDK_LIBRARY.equals(library)) continue;
            if (KOTLIN_LIBRARY.equals(library)) {
                // Its own compiler, with kotlin-stdlib on the class path. AnalysisHintsParser DECORATES a type it
                // can load rather than minting one from the shadow -- without the jar it logs "Ignoring type
                // 'kotlin.Lazy', cannot load it" and writes an empty library, which is how this was found.
                //
                // Deliberately NOT by widening the shared javaInspectorFactory: that class path is used by every
                // modification-* test, and a jar that merely makes more types resolvable can move verdicts. The
                // javadoc of materializeIgnoreModificationsFromFieldType records exactly that happening on
                // fernflower, where an "inert" change moved ConstantPool.pool from @Independent to @Dependent.
                //
                // The committed jdk results are preloaded: without them the defaults the compiler writes for every
                // UNCONTRACTED member of a shadowed part class see String and CharSequence as mutable (see the
                // AnalysisHintsCompiler constructor). The committed ones, not resultsBase's: a test compiling into
                // a temporary directory without the jdk must see the same defaults as the committed build.
                compile(new AnalysisHintsCompiler(kotlinJavaInspectorFactory(), null,
                        List.of(RESULTS_BASE_DIR.resolve(JDK_LIBRARY).toString())), library, resultsBase);
            } else {
                compile(compiler, library, resultsBase);
            }
        }
        for (String library : SIDE_LOADED_LIBRARIES) {
            // the library's jar must be LOADABLE (the parser decorates types it can load): its own factory, as
            // for kotlin, so that no other library's class path changes
            if (VAVR_LIBRARY.equals(library)) {
                // vavr-match too: io.vavr.Patterns is annotated with io.vavr.match.annotation.Patterns, and without
                // that class file a stub is created and Patterns' $Cons/$Tuple2/... lose their verdicts
                compile(new AnalysisHintsCompiler(libraryJavaInspectorFactory("io.vavr.",
                        io.vavr.Value.class, io.vavr.match.annotation.Patterns.class)), library, resultsBase);
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

    /** As {@link #kotlinJavaInspectorFactory()}, for a library given by its package prefix and one class of each of
     *  its jars (the library's own, and any jar its class files refer to). */
    private static JavaInspectorFactory libraryJavaInspectorFactory(String packagePrefix, Class<?>... anchors) {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet maddiAnnotation = SourceSetImpl.sourceSetOf(Immutable.class);
        List<SourceSet> libraries = java.util.Arrays.stream(anchors).map(SourceSetImpl::sourceSetOf).toList();
        List<SourceSet> classPath = new ArrayList<>(List.of(javaBase, maddiAnnotation));
        classPath.addAll(libraries);
        List<SourceSet> dependencies = new ArrayList<>(List.of(maddiAnnotation));
        dependencies.addAll(libraries);

        return new JavaInspectorFactory() {
            @Override
            public List<SourceSet> dependencies() {
                return dependencies;
            }

            @Override
            public JavaInspector withSources(SourceSet sourceSet) throws IOException {
                JavaInspector javaInspector = new JavaInspectorImpl();
                javaInspector.preload("java.base::java.util.");
                javaInspector.preload("java.base::java.lang.annotation");
                javaInspector.preload("io.codelaser.maddi.annotation.");
                javaInspector.preload(packagePrefix);
                javaInspector.initialize(new InputConfigurationImpl.Builder()
                        .addSourceSets(sourceSet)
                        .addClassPathParts(classPath.toArray(new SourceSet[0]))
                        .build());
                return javaInspector;
            }
        };
    }

    private static void compile(AnalysisHintsCompiler compiler, String library, Path resultsBase) throws IOException {
        AnalysisHints analysisHints = new AnalysisHints.Builder()
                .setLibraryName(library)
                .setAnalysisResultsDir(resultsBase.resolve(library))
                .setHintsPath(Path.of(HINTS_PATH))
                .setPackagePrefix("io.codelaser.maddi.aapi.archive." + library.replace("/", "."))
                .build();
        LOGGER.info("Compiling analysis hints for library '{}'", library);
        // ⛔ do not drop these: they are the shallow analyzer's complaints about the hand-written shadows, and
        // they are the only signal that a contract was parsed but said nothing. The task stays exit 0 either way.
        List<Message> messages = compiler.go(analysisHints);
        messages.forEach(m -> LOGGER.warn("Message while compiling '{}': {}", library, m));
    }

    /**
     * Package the generated result files into the two archive jars (the former copyToJars.sh):
     * {@code openjdk.jar} holds {@code jdk/*.json} at its root, {@code libs.jar} holds {@code libs/<lib>/*.json}
     * keeping the {@code <lib>/} directory. They are loaded from the classpath as {@code resource:.../*.jar}.
     */
    static void packageJars() throws IOException {
        packageJars(RESULTS_BASE_DIR);
    }

    /** As {@link #packageJars()}, but over an arbitrary results directory (the tests pack a temporary one). */
    static void packageJars(Path base) throws IOException {
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
