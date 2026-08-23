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

package io.codelaser.maddi.ide.daemon;

import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.support.SetOnce;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the plain-JSON {@link DaemonProtocol.AnalyzeConfig} the plugin sends into a maddi
 * {@link InputConfiguration}, using the string-based builder (Style A). {@code Builder.build()}
 * auto-wires each source set's dependencies to all classpath parts and all earlier source sets, so
 * the daemon does not have to compute the inter-source-set graph itself — correct for whole-project
 * analysis. Mirrors {@code Main.inputConfiguration} / {@code AnalyzerPropertyComputer}.
 */
public class InputConfigurationAssembler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InputConfigurationAssembler.class);


    public InputConfiguration build(DaemonProtocol.AnalyzeConfig config) {
        List<DaemonProtocol.ModuleSourceSet> perModule = config.moduleSourceSets();
        if (perModule != null && !perModule.isEmpty()) {
            return buildPerModule(config, perModule);
        }
        return buildFlat(config);
    }

    /**
     * Build from per-module source sets, using the builder's OBJECT style
     * ({@code addSourceSets}/{@code addClassPathParts}) — never the string style.
     * <p>
     * ⛔ The two styles must not be mixed for source sets. {@code InputConfigurationImpl.Builder.build()}
     * turns every string-style source directory into a set whose dependencies are
     * {@code classPathParts + all earlier sourceSets} — the auto-wiring this method exists to avoid. Adding one
     * {@code addSource(...)} alongside these objects reintroduces it for that set and, worse, wires it to the
     * object sets too.
     * <p>
     * ⚠ A source set's {@code uri} is its OUTPUT location and its identity. It is deliberately not added to any
     * other set's class path: doing that is what made every FQN in the project reachable both as source and as
     * bytecode.
     */
    private InputConfiguration buildPerModule(DaemonProtocol.AnalyzeConfig config,
                                              List<DaemonProtocol.ModuleSourceSet> specs) {
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        if (notBlank(config.workingDirectory())) builder.setWorkingDirectory(config.workingDirectory());
        if (notBlank(config.sdkHome())) builder.setAlternativeJREDirectory(config.sdkHome());
        if (notBlank(config.sourceEncoding())) builder.setSourceEncoding(config.sourceEncoding());
        Charset charset = notBlank(config.sourceEncoding())
                ? Charset.forName(config.sourceEncoding()) : StandardCharsets.UTF_8;

        // ---- the shared pool of libraries: one SourceSet per distinct path, referenced by many modules ----
        Map<String, SourceSet> libraries = new LinkedHashMap<>();
        for (String jmod : config.jmods() == null ? List.<String>of() : config.jmods()) {
            if (notBlank(jmod)) libraries.computeIfAbsent("jmod:" + jmod, InputConfigurationAssembler::jmodPart);
        }
        for (DaemonProtocol.ModuleSourceSet spec : specs) {
            for (String part : spec.classPath() == null ? List.<String>of() : spec.classPath()) {
                if (notBlank(part)) libraries.computeIfAbsent(part, InputConfigurationAssembler::libraryPart);
            }
        }
        // The e2immu annotations and support types, and java.base as a first-class set — as in the flat path.
        List<SourceSet> always = new ArrayList<>(List.of(
                SourceSetImpl.sourceSetOf(Immutable.class),
                SourceSetImpl.sourceSetOf(SetOnce.class),
                SourceSetImpl.javaBase()));
        builder.addClassPathParts(libraries.values());
        builder.addClassPathParts(always);

        // ---- the source sets, in dependency order: SourceSetImpl is immutable, so a dependency must exist
        // as an object before the set that names it can be built ----
        Map<String, DaemonProtocol.ModuleSourceSet> byName = new LinkedHashMap<>();
        for (DaemonProtocol.ModuleSourceSet spec : specs) byName.put(spec.name(), spec);
        Map<String, SourceSet> built = new LinkedHashMap<>();
        for (String name : topologicalOrder(byName)) {
            DaemonProtocol.ModuleSourceSet spec = byName.get(name);
            List<SourceSet> dependencies = new ArrayList<>(always);
            for (String part : spec.classPath() == null ? List.<String>of() : spec.classPath()) {
                SourceSet lib = libraries.get(part);
                if (lib != null) dependencies.add(lib);
            }
            for (String jmod : config.jmods() == null ? List.<String>of() : config.jmods()) {
                SourceSet lib = libraries.get("jmod:" + jmod);
                if (lib != null) dependencies.add(lib);
            }
            for (String dep : spec.dependencies() == null ? List.<String>of() : spec.dependencies()) {
                SourceSet other = built.get(dep);
                // absent only when the cycle-breaker below dropped this edge; it is logged there
                if (other != null) dependencies.add(other);
            }
            List<Path> sourceDirs = new ArrayList<>();
            for (String dir : spec.sourceDirectories() == null ? List.<String>of() : spec.sourceDirectories()) {
                if (notBlank(dir)) sourceDirs.add(Path.of(dir));
            }
            SourceSet set = new SourceSetImpl.Builder()
                    .setName(spec.name())
                    .setSourceDirectories(sourceDirs)
                    .setUri(uriOf(spec.outputPath(), sourceDirs))
                    .setSourceEncoding(charset)
                    .setTest(spec.test())
                    .setSourceRelease(spec.sourceRelease())
                    // a set carrying module-info.java is a JPMS module; the javac-log route records this too,
                    // and without it the set's requires/exports are not honoured
                    .setModule(hasModuleInfo(sourceDirs))
                    .setDependencies(List.copyOf(dependencies))
                    .build();
            built.put(spec.name(), set);
        }
        builder.addSourceSets(built.values());
        LOGGER.info("per-module configuration: {} source set(s), {} shared library part(s)",
                built.size(), libraries.size());
        return builder.build();
    }

    /**
     * Dependency-first order. A cycle cannot be expressed by immutable source sets, so the back-edge is dropped
     * and named rather than throwing: an IDE model can contain one (a test fixture pair, a mis-imported build),
     * and losing one edge is a weaker analysis, where refusing is no analysis at all.
     */
    private static List<String> topologicalOrder(Map<String, DaemonProtocol.ModuleSourceSet> byName) {
        List<String> order = new ArrayList<>();
        Set<String> done = new HashSet<>();
        Set<String> onPath = new LinkedHashSet<>();
        for (String name : byName.keySet()) visit(name, byName, done, onPath, order);
        return order;
    }

    private static void visit(String name, Map<String, DaemonProtocol.ModuleSourceSet> byName,
                              Set<String> done, Set<String> onPath, List<String> order) {
        if (done.contains(name) || !byName.containsKey(name)) return;
        if (!onPath.add(name)) {
            LOGGER.warn("dependency cycle through source set '{}'; dropping the edge that closes it", name);
            return;
        }
        List<String> deps = byName.get(name).dependencies();
        if (deps != null) for (String dep : deps) visit(dep, byName, done, onPath, order);
        onPath.remove(name);
        if (done.add(name)) order.add(name);
    }

    private static SourceSet libraryPart(String path) {
        return new SourceSetImpl.Builder().setName(nameOfPart(path)).setUri(fileUri(path))
                .setLibrary(true).setExternalLibrary(true).build();
    }

    private static SourceSet jmodPart(String prefixed) {
        String name = prefixed.substring("jmod:".length());
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create(prefixed))
                .setLibrary(true).setExternalLibrary(true).setPartOfJdk(true).setModule(true).build();
    }

    /** Jars are named by their file name, as the javac-log route names them; directories keep their path. */
    private static String nameOfPart(String path) {
        int slash = path.lastIndexOf('/');
        return path.endsWith(".jar") && slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static URI fileUri(String path) {
        return path.contains(":") && !path.startsWith("/") ? URI.create(path) : URI.create("file:" + path);
    }

    /**
     * A set's identity. Falls back to the first source directory when the IDE reports no compiler output — an
     * unbuilt module still has to be analysable, and a name is all the runtime needs.
     */
    private static URI uriOf(String outputPath, List<Path> sourceDirs) {
        if (notBlank(outputPath)) return fileUri(outputPath);
        return URI.create("file:" + (sourceDirs.isEmpty() ? "." : sourceDirs.getFirst().toString()));
    }

    private static boolean hasModuleInfo(List<Path> sourceDirs) {
        for (Path dir : sourceDirs) {
            if (Files.isRegularFile(dir.resolve("module-info.java"))) return true;
        }
        return false;
    }

    /** The original flat form: one source list, one class path, and {@code build()}'s auto-wiring. */
    private InputConfiguration buildFlat(DaemonProtocol.AnalyzeConfig config) {
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();

        if (notBlank(config.workingDirectory())) builder.setWorkingDirectory(config.workingDirectory());
        if (notBlank(config.sdkHome())) builder.setAlternativeJREDirectory(config.sdkHome());
        if (notBlank(config.sourceEncoding())) builder.setSourceEncoding(config.sourceEncoding());

        if (config.sources() != null) {
            for (DaemonProtocol.SourceRoot root : config.sources()) {
                if (root.test()) {
                    builder.addTestSource(root.name(), root.path());
                } else {
                    builder.addSource(root.name(), root.path());
                }
            }
        }

        if (config.classpath() != null) {
            for (DaemonProtocol.ClasspathEntry entry : config.classpath()) {
                String scope = entry.scope() == null ? "compile" : entry.scope();
                switch (scope) {
                    case "test" -> builder.addTestClassPath(entry.path());
                    case "runtime" -> builder.addRuntimeClassPath(entry.path());
                    case "test-runtime" -> builder.addTestRuntimeClassPath(entry.path());
                    default -> builder.addClassPath(entry.path()); // "compile"
                }
            }
        }

        List<String> jmods = config.jmods();
        if (jmods != null) {
            for (String jmod : jmods) {
                if (notBlank(jmod)) builder.addJmodToClassPath(jmod);
            }
        }

        List<String> restrict = config.restrictToPackages();
        if (restrict != null && !restrict.isEmpty()) {
            builder.addRestrictSourceToPackages(restrict.toArray(new String[0]));
        }

        // Supply the annotation types (@Immutable, @Container, @NotModified, …) and the support types
        // (SetOnce, Either, EventuallyFinal) as real classpath parts, located from the daemon's own classpath
        // (the distribution bundles both). Required so DecoratorImpl resolves them, so projects that do NOT
        // depend on the annotations still get hints, and so a project that DOES use them parses those imports.
        // (The openjdk inspector does not support the jar-on-classpath: scheme that
        // withMaddiSupportFromClasspath() uses.)
        //
        // TWO parts since the 0.9.1 split: sourceSetOf() resolves the ARTIFACT containing the named class, and
        // the annotations moved to maddi-annotation while the support types stayed in maddi-support. Naming only
        // Immutable here leaves an analyzed project that imports SetOnce unable to resolve it, which silently
        // yields no verdicts at all rather than an error.
        builder.addClassPathParts(SourceSetImpl.sourceSetOf(Immutable.class));
        builder.addClassPathParts(SourceSetImpl.sourceSetOf(SetOnce.class));
        // Provide java.base as a first-class source set (like the analyzer's test harness) so the runtime
        // registers its types for hint resolution — otherwise LoadAnalysisResults reports "type not on the
        // classpath" even though the openjdk parser preloaded them.
        builder.addClassPathParts(SourceSetImpl.javaBase());
        return builder.build();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
