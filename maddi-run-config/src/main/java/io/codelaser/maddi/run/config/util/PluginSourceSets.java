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

package io.codelaser.maddi.run.config.util;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The source set of a <em>build plugin</em>: the Gradle and the Maven plugin both ask their build tool the same
 * two questions -- which directories hold this set's sources, and where does the build compile them to -- and
 * both must answer in the same shape, because the same {@code InputConfiguration} is on the other side.
 * <p>
 * They did not. Each grew its own copy of this construction, and each copy made the {@link #uri} decision below
 * independently and identically wrongly; see {@link #classPathUri}.
 */
public class PluginSourceSets {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSourceSets.class);


    /**
     * ⛔⛔ <b>A SOURCE SET'S {@code uri} IS ITS CLASS OUTPUT, NOT ITS FIRST SOURCE DIRECTORY.</b>
     * <p>
     * The sources are carried by {@code sourceDirectories}; {@code uri} answers a different question. When a
     * source set depends on another that is <em>also</em> parsed from source in the same run (the ordinary
     * {@code test -> main} edge), javac still has to resolve every reference into it, and javac knows nothing of
     * maddi's CST -- so {@code JavaInspectorImpl} puts {@code classOutputOf(dependency)} on javac's
     * {@code CLASS_PATH}, and {@code classOutputOf} is {@code dependency.uri()}.
     * <p>
     * ⚠ <b>THE WRONG ANSWER DOES NOT FAIL, WHICH IS WHY IT SURVIVED IN BOTH PLUGINS.</b> javac's class path
     * doubles as its source path, so pointing it at a source directory makes it find {@code .java} files and
     * compile them <em>implicitly</em> -- a second, silent compilation of the dependency, which usually resolves
     * and so looks like success. It stops looking like success exactly when a type's directory does not match its
     * package: the implicit lookup is by fully-qualified name, so the file is not where the name says it is.
     * <p>
     * ⚠ <b>MEASURED, on fernflower</b> (2026-08-19): {@code DebugPrinter} declares
     * {@code package org.jetbrains.java.decompiler.decompiler.util} while sitting in
     * {@code src/org/jetbrains/java/decompiler/util/}. Through the plugin's configuration that cost one dropped
     * compilation unit and one warning; through {@code --compile-log}, whose source sets carry javac's own
     * {@code -d} directory, it cost nothing. Patching these two URIs -- and nothing else -- made the two
     * configurations parse identically.
     * <p>
     * ⛔ <b>THE DIRECTORY IS NOT PROBED, AND MUST NOT BE.</b> A class output is a <em>declaration</em> of where
     * the build compiles to, not an observation of what is on disk: both plugins compute this configuration
     * before the compile tasks they depend on have run. A first attempt called {@link Files#isDirectory} here to
     * fall back when it was absent, which registered a configuration-time file-system input on
     * {@code build/classes/java/main} -- so Gradle discarded its configuration cache on the next run, the moment
     * compilation created the directory. {@code TestAnalyzerPluginFunctional#configurationCacheCompatible}
     * caught it. Whether the directory exists is a question for parse time, and
     * {@code JavaInspectorImpl#validateOneDependency} already asks it there, where the answer is meaningful and
     * the report names the source set, the directory and the types that will not resolve.
     */
    private static URI classPathUri(List<Path> sourceDirectories, Path classOutput) {
        if (classOutput != null) return classOutput.toAbsolutePath().normalize().toUri();
        return sourceDirectories.getFirst().toUri();
    }

    /**
     * A source set of the project the plugin is applied to.
     * <p>
     * Source directories are made absolute: a relative one yields the opaque {@code file:src} form, which
     * {@code Path.of(URI)} refuses, and it would tie the run to the process's working directory (a Gradle worker
     * does not share the build's).
     *
     * @param sourceRelease the Java API level this set is compiled against, {@code 0} when the build says
     *                      nothing. <b>Per set, never global.</b> {@code InputConfiguration.sourceRelease} can
     *                      state one answer for a whole configuration, so it has to abstain the moment a build
     *                      compiles two of its modules against different releases -- and abstaining means the
     *                      parse runs on whatever JDK it happens to be, where every API removed since reads as
     *                      "cannot find symbol". Per set there is nothing to abstain from.
     * @param addModules  JDK modules outside the default root set, i.e. javac's {@code --add-modules}.
     *                    <p>⛔⛔ <b>NOT COSMETIC, AND NOT COVERED BY {@code jmods}.</b> An INCUBATOR module is
     *                    not in the {@code java.se} closure and is not reachable by widening it either: it has
     *                    to be added to the root set for the compilation that uses it, which is what this is.
     *                    Without it every type in that module reads as "package X is not visible", the
     *                    compilation unit is dropped, and the {@code ParseResult} can be refused.
     *                    <p>⚠ <b>MEASURED, on trino</b> (2026-08-19): {@code core/trino-main} passes
     *                    {@code --add-modules=jdk.incubator.vector}, and {@code --compile-log} records it on 6
     *                    of trino's 209 source sets because it reads javac's own line. <b>Neither plugin set it
     *                    at all</b>, so the same module parsed with 1 error the log route does not have.
     * @return {@code null} when no source directory of this set exists on disk -- a set over nothing contributes
     * no compilation units, and naming an absent path in the configuration only produces an unresolvable entry.
     */
    public static SourceSet sourceSet(String name,
                                      String buildUnit,
                                      List<Path> sourceDirectories,
                                      Path classOutput,
                                      Charset sourceEncoding,
                                      boolean test,
                                      Set<String> restrictToPackages,
                                      int sourceRelease,
                                      List<String> addModules,
                                      List<String> warningFlags) {
        List<Path> existing = sourceDirectories.stream()
                .map(p -> p.toAbsolutePath().normalize())
                .filter(Files::isDirectory)
                .distinct()
                .toList();
        if (existing.isEmpty()) return null;
        return new SourceSetImpl.Builder()
                .setName(name)
                .setBuildUnit(buildUnit)
                .setSourceDirectories(existing)
                .setUri(classPathUri(existing, classOutput))
                .setSourceEncoding(sourceEncoding)
                .setTest(test)
                .setModule(isModularSource(existing))
                .setRestrictToPackages(restrictToPackages)
                .setSourceRelease(sourceRelease)
                .setAddModules(addModules == null ? List.of() : List.copyOf(addModules))
                .setWarningFlags(warningFlags == null ? List.of() : List.copyOf(warningFlags))
                .build();
    }

    /**
     * One entry of a build's class path, as a library part.
     *
     * <p>Both plugins wrote this builder chain themselves, with the same six flags and the same comment about why
     * the part name is what it is -- and they had drifted: only the Gradle one asked whether the artifact is a
     * module, so the Maven plugin could not describe a JPMS dependency at all.
     *
     * @param name  the part's identity, which the serialized configuration resolves dependency edges by. How it
     *              is derived differs per build tool and stays with the caller: a jar identifies itself by its
     *              file name, a project's output directory does not.
     */
    public static SourceSet classPathPart(String name, File file, boolean test, boolean runtimeOnly) {
        return new SourceSetImpl.Builder()
                .setName(name)
                // ⚠ toURI(), not "file:" + path. Both are hierarchical for an absolute path, but only this one
                // stays so for every path the build tool can hand over.
                .setUri(file.getAbsoluteFile().toURI())
                .setTest(test)
                .setLibrary(true)
                .setExternalLibrary(true)
                .setPartOfJdk(false)
                .setModule(isModularArtifact(file))
                .setRuntimeOnly(runtimeOnly)
                .build();
    }

    /**
     * How an artifact presents itself to JPMS, and -- the reason this is three-valued -- whether the build will
     * put it where a named module can read it.
     *
     * <p>⛔⛔ {@code AUTOMATIC} AND {@code NONE} ARE NOT THE SAME ANSWER, AND ONLY ONE OF THEM WORKS. Gradle
     * routes a jar to the module path when it carries a {@code module-info.class} or declares
     * {@code Automatic-Module-Name}, and leaves every other jar on the class path -- where a named module cannot
     * read it, because the class path is the unnamed module. So a {@code requires} on a {@code NONE} artifact
     * compiles to "module not found" however correct the descriptor is.
     *
     * <p>⚠ <b>MEASURED, on jsr305 3.0.2</b> (2026-08-20): its manifest carries an OSGi
     * {@code Bundle-SymbolicName: org.jsr-305} and no {@code Automatic-Module-Name}. JPMS ignores the OSGi
     * header, so this is {@code NONE} -- and it is exactly the artifact that broke the OpenSearch JPMS work.
     *
     * <p>⛔ <b>{@code jar --describe-module} IS NOT THE DISCRIMINATOR, THOUGH IT LOOKS LIKE ONE.</b> On jsr305 it
     * prints "No module descriptor found. Derived automatic module." and then {@code jsr305@3.0.2 automatic} --
     * the JDK derives a name from the FILE NAME and calls the result automatic, so its verdict word is
     * {@code automatic} for {@code AUTOMATIC} and {@code NONE} alike. What separates them is whether the manifest
     * DECLARES the name, which is the only thing a build tool will route on.
     */
    public enum ModuleKind {
        /** Carries a {@code module-info.class}: an explicit module, on the module path. */
        EXPLICIT,
        /** Declares {@code Automatic-Module-Name}: an automatic module with a stable name, on the module path. */
        AUTOMATIC,
        /** Neither. The build leaves it on the class path, so a named module cannot read it. */
        NONE
    }

    /**
     * As {@link #isModularSource}, for a dependency. A directory (a sibling project's compile output) is asked
     * directly; a jar is opened.
     *
     * <p>⚠ A directory is never {@link ModuleKind#AUTOMATIC}: {@code Automatic-Module-Name} is a property of a
     * jar's manifest, and a build tool does not read one out of a compile-output directory.
     *
     * <p>⚠ Only the {@code META-INF/versions/9} spelling of a multi-release descriptor is read, which is what
     * this method has always read. A descriptor added at a later release is answered {@code NONE} here; no
     * corpus has yet produced one, and widening it is a change to which artifacts reach the module path, so it
     * belongs with a run that can see that.
     */
    public static ModuleKind moduleKind(File file) {
        if (file.isDirectory()) {
            return new File(file, "module-info.class").canRead() ? ModuleKind.EXPLICIT : ModuleKind.NONE;
        }
        try (JarFile jarFile = new JarFile(file)) {
            if (!descriptorReleases(jarFile).isEmpty()) return ModuleKind.EXPLICIT;
            Manifest manifest = jarFile.getManifest();
            if (manifest != null && manifest.getMainAttributes().getValue("Automatic-Module-Name") != null) {
                return ModuleKind.AUTOMATIC;
            }
            return ModuleKind.NONE;
        } catch (IOException notAJar) {
            LOGGER.warn("Cannot read {} as a jar, assuming it is not a module: {}", file, notAJar.getMessage());
            return ModuleKind.NONE;
        }
    }

    /** {@link #ROOT} for an unversioned {@code module-info.class}, otherwise the multi-release version it sits under. */
    private static final int ROOT = -1;

    /**
     * Every release at which this jar carries a module descriptor, so that the two questions below can read one
     * scan and disagree about it deliberately rather than by accident.
     *
     * <p>⛔⛔ <b>{@code META-INF/versions/9} IS NOT THE ONLY SPELLING, AND ASSUMING IT WAS COST A WHOLE VENDOR.</b>
     * MEASURED 2026-08-20: <b>netty keeps its descriptor at {@code META-INF/versions/11}</b>
     * ({@code netty-codec-smtp-4.2.17.Final.jar}, {@code jar --describe-module} → {@code releases: 11}) while
     * bouncycastle uses 9. Reading only 9 answered {@code NONE} for every netty jar there is — and netty is in the
     * measured failure list of the OpenSearch JPMS lane's {@code #232}
     * ({@code io.netty.transport requires io.netty.resolver}), which is the report this classification feeds.
     *
     * <p>⚠ The earlier version of this class said the narrow read was safe because <i>"no corpus has yet produced
     * one"</i>. A corpus produced one the same day. <b>An assumption recorded as a limitation is still an
     * assumption</b>; this one is now a measurement.
     */
    private static List<Integer> descriptorReleases(JarFile jarFile) {
        List<Integer> releases = new ArrayList<>();
        if (jarFile.getEntry("module-info.class") != null) releases.add(ROOT);
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
            String name = e.nextElement().getName();
            Matcher m = VERSIONED_DESCRIPTOR.matcher(name);
            if (m.matches()) releases.add(Integer.parseInt(m.group(1)));
        }
        return releases;
    }

    private static final Pattern VERSIONED_DESCRIPTOR =
            Pattern.compile("META-INF/versions/(\\d{1,3})/module-info\\.class");

    /**
     * Whether {@code SourceSet.isModule()} is set for this artifact, and through it whether
     * {@code JavaInspectorImpl} puts it on javac's <em>module path</em> rather than its class path.
     *
     * <p>⛔ <b>DELIBERATELY NARROWER THAN {@link #moduleKind}, AND FROZEN UNTIL A RUN CAN PRICE IT.</b> It answers
     * {@code true} only for a descriptor at the root or under {@code META-INF/versions/9} — exactly what it has
     * always answered — while {@code moduleKind} reads every multi-release spelling and also separates
     * {@link ModuleKind#AUTOMATIC}. Two widenings are on the table here and BOTH move artifacts between javac's
     * two paths:
     * <ul>
     *   <li>including {@link ModuleKind#AUTOMATIC}: an automatic module is one a named consumer can read;</li>
     *   <li>including a descriptor at {@code versions/11}: netty, measured 2026-08-20.</li>
     * </ul>
     * The risk is not the routing itself but its by-product — two artifacts arriving on the MODULE path with a
     * package in common is <i>"package read from both"</i>, where on the class path the first simply wins.
     *
     * <p>⚠ <b>AND THE RUN THAT WOULD PRICE IT IS NOT AVAILABLE TODAY.</b> The branch is only reached when the
     * CONSUMER is modular, so only a modular corpus can see it — fernflower (2 descriptors) and timefold-solver
     * (21) are the two. Both of their {@code inputConfiguration.json} files pin class-path jars by absolute path
     * into {@code ~/.gradle/caches}, and every one of those paths is gone (36 of 36, 537 of 537): the blast radius
     * could not be measured, so it was not changed. <b>Rebuild those two corpora, re-measure, then widen.</b>
     *
     * <p>Ask {@link #moduleKind} when the question is what the BUILD will deliver — which is a different question
     * from what maddi's own javac should be handed, and the whole reason these two are separate methods.
     */
    public static boolean isModularArtifact(File file) {
        if (file.isDirectory()) return new File(file, "module-info.class").canRead();
        try (JarFile jarFile = new JarFile(file)) {
            List<Integer> releases = descriptorReleases(jarFile);
            return releases.contains(ROOT) || releases.contains(9);
        } catch (IOException notAJar) {
            LOGGER.warn("Cannot read {} as a jar, assuming it is not a module: {}", file, notAJar.getMessage());
            return false;
        }
    }

    /**
     * The modules named by {@code --add-modules} among raw javac arguments, in order, without duplicates.
     *
     * <p>⚠ <b>BOTH SPELLINGS, BECAUSE BOTH APPEAR.</b> javac accepts {@code --add-modules=a,b} and
     * {@code --add-modules a,b}, and a build file may use either -- trino writes the {@code =} form into a
     * property and Gradle users typically write the two-argument form into {@code options.compilerArgs}.
     * Reading only one is a silent half-answer, which is worse here than reading none: the modules that ARE
     * found look like the whole story.
     *
     * <p>⚠ {@code ALL-MODULE-PATH} and {@code ALL-DEFAULT} are javac's own pseudo-module names, not modules;
     * they are dropped rather than passed on to something that will look for a jmod by that name.
     */
    public static List<String> addModulesFrom(List<String> compilerArgs) {
        if (compilerArgs == null) return List.of();
        Set<String> modules = new LinkedHashSet<>();
        for (int i = 0; i < compilerArgs.size(); i++) {
            String arg = compilerArgs.get(i);
            if (arg == null) continue;
            String value;
            if (arg.startsWith("--add-modules=")) {
                value = arg.substring("--add-modules=".length());
            } else if ("--add-modules".equals(arg.trim()) && i + 1 < compilerArgs.size()) {
                value = compilerArgs.get(++i);
            } else {
                continue;
            }
            if (value == null) continue;
            for (String module : value.split(",")) {
                String trimmed = module.trim();
                if (!trimmed.isBlank() && !trimmed.startsWith("ALL-")) modules.add(trimmed);
            }
        }
        return List.copyOf(modules);
    }

    /**
     * Whether this javac argument takes part in deciding <em>whether a warning is emitted, and whether it is
     * fatal</em>: {@code -Werror}, {@code -nowarn} and the {@code -Xlint} family.
     *
     * <p>⚠ Deliberately narrow. This is one question with one consumer behind it, not a general bag of compiler
     * arguments: an attribute recorded without a failure demanding it is a guess about the future, and a guess
     * that is wrong fails towards silence. {@code --enable-preview}, {@code --add-exports} and {@code -proc:}
     * are real and are NOT here, because nothing has yet asked.
     */
    public static boolean isWarningFlag(String arg) {
        if (arg == null) return false;
        String trimmed = arg.trim();
        return "-Werror".equals(trimmed) || "-nowarn".equals(trimmed) || trimmed.startsWith("-Xlint");
    }

    /**
     * The {@linkplain #isWarningFlag warning-policy arguments} among raw javac arguments, in order, without
     * duplicates.
     *
     * <p>⛔⛔ <b>THE ANSWER IS THE RESOLVED LIST, WHICH IS WHY THIS IS ASKED OF THE BUILD AND NOT OF A BUILD
     * FILE.</b> OpenSearch adds {@code -Werror} once at the root ({@code build.gradle:280},
     * {@code compile.options.compilerArgs << '-Werror'}) and subtracts it again in 12 files
     * ({@code libs/common/build.gradle:56}, {@code options.compilerArgs -= '-Werror'}). Whether a given set
     * compiles with it is the outcome of both, so a grep over build files can say where the word appears and
     * can never say whether the flag is ON. The list this reads has already had the subtraction applied.
     *
     * <p>⚠ What it is FOR: a {@code requires} that pulls an incubator module into a set's resolution makes
     * javac warn in that compilation -- and {@code -Werror} turns the warning into a build failure in every set
     * that resolves it, whether or not that set uses the API. Which sets those are is a question about the
     * module graph; whether it costs anything is this flag.
     */
    public static List<String> warningFlagsFrom(List<String> compilerArgs) {
        if (compilerArgs == null) return List.of();
        Set<String> flags = new LinkedHashSet<>();
        for (String arg : compilerArgs) {
            if (isWarningFlag(arg)) flags.add(arg.trim());
        }
        return List.copyOf(flags);
    }

    /**
     * {@code "21"}, {@code "1.8"}, {@code "8"} -> {@code 21}, {@code 8}, {@code 8}; anything else -> {@code 0}.
     * Both build tools state the level as a string in more than one spelling, and neither guarantees one is set.
     */
    public static int parseRelease(String release) {
        if (release == null || release.isBlank()) return 0;
        String trimmed = release.trim();
        // the "1.N" spelling stops at 1.8; "1.10" was never a Java version, so this is not ambiguous
        if (trimmed.startsWith("1.")) trimmed = trimmed.substring(2);
        try {
            int value = Integer.parseInt(trimmed);
            return value > 0 ? value : 0;
        } catch (NumberFormatException notAVersion) {
            return 0;
        }
    }

    /**
     * A source set is a Java module when one of its source directories holds a {@code module-info.java}. The
     * distinction is not cosmetic: the openjdk front end puts a module's dependencies on javac's <em>module
     * path</em>, and without the flag every {@code requires}d package comes back as "package X is not visible".
     * <p>
     * ⚠ The Maven plugin never asked this question at all -- it set {@code module} on jmods and nowhere else --
     * so it could not describe a JPMS corpus. Sharing the construction is what gives it the answer.
     */
    public static boolean isModularSource(List<Path> sourceDirectories) {
        return sourceDirectories.stream().anyMatch(p -> Files.isRegularFile(p.resolve("module-info.java")));
    }
}
