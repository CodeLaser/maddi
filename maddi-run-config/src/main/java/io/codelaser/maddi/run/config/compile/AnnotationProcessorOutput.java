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
package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Recovers the <b>annotation-processor products</b> that {@link CompileListToSourceSets} drops, by declaring them
 * as an ordinary external library.
 *
 * <p>⛔⛔ <b>THE HOLE, AND IT IS SILENT.</b> {@code CompileListToSourceSets} removes an invocation's {@code -d}
 * destination from every classpath, because that destination has just become a source set. That is right for
 * everything the invocation compiled from source, and wrong for everything else in the same directory: a
 * {@code .class} whose compilation unit is on none of this invocation's source roots then belongs to
 * <i>nothing</i> — not source, not library. Those are the classes an annotation processor generated during the
 * compile. On {@code :x-pack:plugin:esql:compute} plus {@code :x-pack:plugin:esql} that is <b>2 077 classes</b>,
 * and the failure it produced was 178 dropped compilation units and a {@code ParseResult} refused with
 * <i>"70 parse error(s)"</i> — <b>naming the consumer, never the cause.</b>
 *
 * <p>⛔ <b>{@code -s} IS NOT THE ANSWER, MEASURED RATHER THAN ARGUED.</b> The obvious repair is javac's
 * {@code -s} ({@code generatedSourceFilesDestination}) — point the parse at the generated <i>sources</i>. On
 * elasticsearch's {@code esql:compute}, {@code -s} is Gradle's unused default
 * ({@code build/generated/sources/annotationProcessor/java/main}, empty), while the processor writes its 495
 * {@code .java} into {@code src/main/generated}, committed to git. One {@code ls} settles it. The signal that
 * does work needs nothing but the compile line and the build outputs it already produced: <b>the orphan test on
 * the destination</b>.
 *
 * <p>▶ <b>THE TEST, AND ITS PREFILTER CAN ONLY BE WRONG IN THE SAFE DIRECTION.</b>
 * <pre>
 * for every source set S with a compiled destination D,
 *     for every top-level name T with class files in D,
 *         T is an annotation-processor product  iff  T's COMPILATION UNIT is on none of S's source roots.
 * </pre>
 * The cheap half is a file-existence check for {@code <root>/<package>/<T>.java}: when it hits, T has a source
 * and nothing further is read. It cannot produce a false negative — a processor may not generate a type that
 * also has a source, javac refuses the duplicate — so it is a prefilter, and only what it flags costs a class
 * file parse.
 *
 * <p>⛔ <b>AND THE SECOND HALF IS NOT OPTIONAL: THE FILE-NAME RULE MANUFACTURES ORPHANS.</b> A top-level class
 * that is not {@code public} need not be in a file of its own name, and then no {@code <T>.java} exists although
 * the source is right there in the parse. Measured on timefold-solver:
 * {@code Target_com_networknt_schema_regex_RegularExpression} and its anonymous inner class are declared inside
 * {@code JsonSchemaSubstitutions.java}, and the file-name rule calls both of them generated. Copying them into a
 * library would put the same fully qualified name in two places at once — <b>a manufactured duplicate is worse
 * than the silence it replaces.</b> So a flagged name is settled by the {@code SourceFile} attribute of its own
 * bytecode, which names the compilation unit exactly. That is what takes timefold's count from 2 to 0, while
 * elasticsearch's 2 077 stay 2 077 — the rule is strictly sharper, not merely different.
 *
 * <p>⚠ <b>NESTED CLASSES TRAVEL WITH THEIR TOP-LEVEL NAME BY CONSTRUCTION</b> ({@code Foo$Bar.class} is decided
 * by {@code Foo}), and a class is judged against <b>its own source set's</b> roots only: a type whose source
 * lives in a different source set is not generated, it is elsewhere.
 *
 * <p>⭐ <b>WHY A LIBRARY AND NOT A SOURCE ROOT.</b> Declaring the generated <i>sources</i> as a source root is
 * simpler and it is currently unsafe: a lever edited generated source, the build regenerated it, and the edit
 * vanished — {@code SourceSet} has no read-only/generated flag to prevent that. As a library the classes are
 * visible to every reader and editable by none. The cost is a file copy, and the copy is rebuilt from scratch
 * on every run, because a stale copy is the stale-jar failure under a new name.
 *
 * <p>⚠ <b>WHAT CANNOT BE PLACED IS REPORTED</b>, which is the whole point: the defect this replaces was not that
 * the classes were missing, it was that nothing said so.
 */
public class AnnotationProcessorOutput {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProcessorOutput.class);

    /** The directory name the copies live in, inside the build tool's output directory. */
    public static final String APT_DIRECTORY = "maddi-apt";

    /** The suffix that turns a source set's name into its generated-classes library's name. */
    public static final String APT_SUFFIX = "-apt";

    /**
     * @param sourceSet the source set whose destination held the classes
     * @param why       what stopped this source set from being judged, or its orphans from being placed
     * @param classes   the number of class files involved, or {@code -1} when that is not known either
     */
    public record NotPlaced(String sourceSet, String why, int classes) {
    }

    /**
     * A type whose compilation unit is in the parse, at a path its package does not predict — so the file-name
     * rule calls it generated and it is not.
     *
     * @param type    the top-level binary name, as a path
     * @param unit    the compilation unit, from the class file's {@code SourceFile} attribute
     * @param foundAt where that unit actually is
     * @param classes how many class files the name covers
     */
    public record SourceElsewhere(String sourceSet, String type, String unit, String foundAt, int classes) {
    }

    /**
     * @param libraries        one library per source set that has generated classes, in source-set order
     * @param owners           library name per owning source set name
     * @param notPlaced        every source set that could not be judged, or whose orphans could not be written
     * @param sourceElsewhere  types whose compilation unit is in the parse at a path its package does not
     *                         predict; a defect in the corpus's own layout, reported because the file-name rule
     *                         calls exactly these generated
     * @param classes          the number of class files copied in total
     * @param destinationsRead how many source sets had a compiled destination to look at
     * @param sourceBacked     ⚠ THE DENOMINATOR OF THE DISCRIMINATOR: top-level names the cheap file-name rule
     *                         flagged and the {@code SourceFile} attribute then settled as source-backed. It is
     *                         the count of duplicates that were NOT manufactured, and without it a run reporting
     *                         zero generated classes cannot be told from a run where the sharp rule never fired.
     */
    public record Result(List<SourceSet> libraries,
                         Map<String, SourceSet> owners,
                         List<NotPlaced> notPlaced,
                         List<SourceElsewhere> sourceElsewhere,
                         int classes,
                         int destinationsRead,
                         int sourceBacked) {

        /**
         * Adds each generated-classes library to the source set that owns it <b>and to every source set that
         * depends on that source set</b>. The consumer edge is not a nicety: rule 2 turned the producer's output
         * directory into a source-set dependency carrying only the source-backed types, so a consumer sees the
         * hand-written half of its dependency and not the generated half.
         *
         * <p>⚠ Matched by NAME rather than by identity, so it composes with any pass that has already replaced
         * source-set objects with copies ({@link TypeUseAnnotationClosure} does).
         */
        public List<SourceSet> attach(List<SourceSet> sourceSets) {
            if (owners.isEmpty()) return sourceSets;
            List<SourceSet> result = new ArrayList<>(sourceSets.size());
            for (SourceSet sourceSet : sourceSets) {
                List<SourceSet> extra = new ArrayList<>();
                SourceSet own = owners.get(sourceSet.name());
                if (own != null) extra.add(own);
                for (SourceSet dependency : sourceSet.dependencies()) {
                    SourceSet library = owners.get(dependency.name());
                    if (library != null && !extra.contains(library)) extra.add(library);
                }
                if (extra.isEmpty()) {
                    result.add(sourceSet);
                } else {
                    List<SourceSet> dependencies = new ArrayList<>(sourceSet.dependencies());
                    dependencies.addAll(extra);
                    result.add(sourceSet.withDependencies(List.copyOf(dependencies)));
                    LOGGER.debug("Generated classes: {} += {}", sourceSet.name(),
                            extra.stream().map(SourceSet::name).toList());
                }
            }
            return List.copyOf(result);
        }
    }

    /**
     * Detects the generated classes of every source set and copies them to a side directory declared as a library.
     * The source sets themselves are left untouched here; see {@link Result#attach(List)}.
     */
    public Result materialise(List<SourceSet> sourceSets) {
        List<SourceSet> libraries = new ArrayList<>();
        Map<String, SourceSet> owners = new LinkedHashMap<>();
        Report report = new Report();
        List<NotPlaced> notPlaced = report.notPlaced;
        int classes = 0;
        int destinationsRead = 0;
        int noDestination = 0;
        for (SourceSet sourceSet : sourceSets) {
            if (sourceSet.library()) continue;
            Path destination = destinationOf(sourceSet);
            if (destination == null || !Files.isDirectory(destination)) {
                // A destination that is not on disk is the normal state of a configuration built from an old log,
                // or of a hand-made one: it is counted and reported once, not warned about 348 times.
                ++noDestination;
                continue;
            }
            ++destinationsRead;
            if (sourceSet.sourceDirectories().isEmpty()) {
                // ⛔ WITHOUT SOURCE ROOTS EVERY CLASS IS AN ORPHAN, so the "repair" would copy the whole
                // destination and duplicate every type in it. Refuse, and say so.
                notPlaced.add(new NotPlaced(sourceSet.name(),
                        "the source set declares no source directories, so nothing can be judged against them", -1));
                continue;
            }
            List<String> orphans = orphans(sourceSet, destination, report);
            if (orphans.isEmpty()) continue;
            Path target = sideDirectory(destination, sourceSet.name());
            try {
                copy(destination, orphans, target);
            } catch (IOException io) {
                notPlaced.add(new NotPlaced(sourceSet.name(),
                        "cannot write the generated classes to " + target + ": " + io.getMessage(), orphans.size()));
                continue;
            }
            SourceSet library = new SourceSetImpl.Builder()
                    .setName(sourceSet.name() + APT_SUFFIX)
                    .setBuildUnit(sourceSet.buildUnit())
                    .setSourceDirectories(List.of())
                    // ⛔⛔ toUri(), NOT "file:" + path. This threw
                    // `IllegalArgumentException: Illegal character in path at index 90` the first time a BUILD
                    // PLUGIN fed this class a source set, because a plugin's names come from the build model:
                    // langchain4j-core's is `LangChain4j :: Core/test`, so the directory below has spaces and
                    // colons in it and concatenation produces a string that is not a URI. `--compile-log` never
                    // saw it -- its names are path-shaped (`core/main`) by construction.
                    // ⚠ THE SAME SENTENCE IS ALREADY WRITTEN IN `PluginSourceSets.classPathPart`, one package
                    // away: "toURI(), not 'file:' + path ... only this one stays hierarchical for every path the
                    // build tool can hand over". Two producers of one URI, and the lesson landed in one of them.
                    .setUri(target.toUri())
                    .setLibrary(true)
                    .setExternalLibrary(true)
                    .build();
            libraries.add(library);
            owners.put(sourceSet.name(), library);
            classes += orphans.size();
            LOGGER.info("Generated classes with no source in the parse: {} {} -> {}", sourceSet.name(),
                    orphans.size(), library.name());
        }
        if (noDestination > 0) {
            LOGGER.warn("{} of {} source set(s) have no compiled output on disk, so their generated classes cannot"
                        + " be detected; a type an annotation processor produced will be missing without further"
                        + " notice", noDestination, sourceSets.size());
        }
        // NO SILENT CAPS. The defect being repaired here was a SILENT drop, so a drop this pass cannot repair is
        // stated in full rather than left to be rediscovered from a parse error four steps downstream.
        for (NotPlaced np : notPlaced) {
            LOGGER.warn("Generated classes stay unplaced in {}{}: {}", np.sourceSet(),
                    np.classes() < 0 ? "" : " (" + np.classes() + " class file(s))", np.why());
        }
        // ⚠ A DIFFERENT REPORT, BECAUSE IT IS A DIFFERENT FACT: nothing is missing from the parse here, the
        // corpus's own source layout disagrees with its packages. It is named because it is precisely the set a
        // file-name rule would have copied, and a copy of a type that already has a source is a duplicate.
        for (SourceElsewhere se : report.sourceElsewhere) {
            LOGGER.warn("{} in {} is compiled from {}, which is on a source root at {}: its directory does not"
                        + " match its package, so it is source-backed and NOT an annotation-processor product"
                        + " ({} class file(s))", se.type(), se.sourceSet(), se.unit(), se.foundAt(), se.classes());
        }
        LOGGER.info("Generated-class libraries: {} over {} source set(s) with compiled output, {} class file(s);"
                    + " {} top-level name(s) had no .java of their own name and were settled as source-backed by"
                    + " their SourceFile attribute", libraries.size(), destinationsRead, classes,
                report.sourceBacked);
        return new Result(List.copyOf(libraries), Map.copyOf(owners), List.copyOf(notPlaced),
                List.copyOf(report.sourceElsewhere), classes, destinationsRead, report.sourceBacked);
    }

    /** What a pass accumulates besides its answer; one object so a new kind of report is not a new parameter. */
    private static final class Report {
        private final List<NotPlaced> notPlaced = new ArrayList<>();
        private final List<SourceElsewhere> sourceElsewhere = new ArrayList<>();
        private int sourceBacked;
    }

    /** The {@code -d} destination is the source set's URI; anything not on the local file system is skipped. */
    private static Path destinationOf(SourceSet sourceSet) {
        URI uri = sourceSet.uri();
        if (uri == null || !"file".equals(uri.getScheme())) return null;
        String path = uri.getPath();
        return path == null ? null : Path.of(path);
    }

    /**
     * The class files under {@code destination}, as paths relative to it, whose top-level name has no compilation
     * unit on the source set's source roots.
     */
    private List<String> orphans(SourceSet sourceSet, Path destination, Report report) {
        // top-level name (package path + simple name, no '$') -> its class files, relative to the destination
        Map<String, List<String>> byTopLevel = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(destination)) {
            for (Path file : walk.filter(p -> p.getFileName().toString().endsWith(".class")).toList()) {
                String relative = destination.relativize(file).toString();
                byTopLevel.computeIfAbsent(topLevelOf(relative), t -> new ArrayList<>()).add(relative);
            }
        } catch (IOException io) {
            report.notPlaced.add(new NotPlaced(sourceSet.name(),
                    "cannot read the compiled output at " + destination + ": " + io.getMessage(), -1));
            return List.of();
        }
        List<Path> roots = sourceSet.sourceDirectories();
        Map<String, List<String>> byFileName = null; // built lazily: only a flagged name needs it
        List<String> orphans = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byTopLevel.entrySet()) {
            String topLevel = entry.getKey();
            if (exists(roots, topLevel + ".java")) continue; // the prefilter, and it cannot miss a real orphan
            String unit = compilationUnit(destination, entry.getValue());
            if (unit == null) {
                // -g:none, or bytecode this reader cannot parse: the file-name rule is all there is, and it is
                // the rule that manufactures duplicates, so say which classes were decided by it.
                report.notPlaced.add(new NotPlaced(sourceSet.name(), "no SourceFile attribute on " + topLevel
                                                                    + ", so it was judged by its file name alone",
                        entry.getValue().size()));
                orphans.addAll(entry.getValue());
                continue;
            }
            int lastSlash = topLevel.lastIndexOf('/');
            String packagePath = lastSlash < 0 ? "" : topLevel.substring(0, lastSlash + 1);
            if (exists(roots, packagePath + unit)) { // declared in a file of a different name: NOT generated
                ++report.sourceBacked;
                continue;
            }
            // ⛔ THE LAST CHANCE, AND IT IS EXACT RATHER THAN BY NAME. A source root whose directories do not
            // match its packages is a corpus defect, not a reason to duplicate a type: elasticsearch keeps seven
            // esql files under .../xpack/compute/... that declare package org.elasticsearch.compute..., and a
            // file-name rule copies all seven into a library the source set already provides. So the candidates
            // are read, and only a candidate declaring THIS class's package counts.
            if (byFileName == null) byFileName = sourceFilesByName(roots);
            String packageName = lastSlash < 0 ? "" : topLevel.substring(0, lastSlash).replace('/', '.');
            String declaredHere = byFileName.getOrDefault(unit, List.of()).stream()
                    .filter(candidate -> packageName.equals(packageOf(Path.of(candidate))))
                    .findFirst().orElse(null);
            if (declaredHere != null) {
                ++report.sourceBacked;
                report.sourceElsewhere.add(new SourceElsewhere(sourceSet.name(), topLevel, unit, declaredHere,
                        entry.getValue().size()));
                continue;
            }
            orphans.addAll(entry.getValue());
        }
        return orphans;
    }

    /** The package a source file declares, {@code ""} for the unnamed package, {@code null} when unreadable. */
    private static String packageOf(Path sourceFile) {
        try {
            Matcher m = PACKAGE.matcher(Files.readString(sourceFile));
            return m.find() ? m.group(1) : "";
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("Cannot read {}: {}", sourceFile, e.toString());
            return null;
        }
    }

    /** As in {@link CompileListToSourceSets}: the trailing {@code ;} is optional so Kotlin matches too. */
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;?");

    /** {@code p/q/Foo$Bar$1.class} -> {@code p/q/Foo}: a nested class is decided by its top-level name. */
    private static String topLevelOf(String relative) {
        String withoutSuffix = relative.substring(0, relative.length() - ".class".length());
        int separator = withoutSuffix.lastIndexOf(java.io.File.separatorChar);
        int dollar = withoutSuffix.indexOf('$', separator + 1);
        return (dollar < 0 ? withoutSuffix : withoutSuffix.substring(0, dollar))
                .replace(java.io.File.separatorChar, '/');
    }

    private static boolean exists(List<Path> roots, String relative) {
        for (Path root : roots) {
            if (Files.isRegularFile(root.resolve(relative))) return true;
        }
        return false;
    }

    /**
     * The {@code SourceFile} attribute of a top-level name, read from its own class file when that is present and
     * otherwise from any of its members — they all carry the same value. {@code null} when nothing could be read.
     */
    private static String compilationUnit(Path destination, List<String> classFiles) {
        List<String> ordered = new ArrayList<>(classFiles);
        ordered.sort(Comparator.comparingInt(String::length)); // the top-level class file is the shortest name
        for (String relative : ordered) {
            String sourceFile = sourceFileAttribute(destination.resolve(relative));
            if (sourceFile != null) return sourceFile;
        }
        return null;
    }

    private static String sourceFileAttribute(Path classFile) {
        String[] holder = new String[1];
        try {
            // ⚠ NOT SKIP_DEBUG: that is exactly the flag that drops the SourceFile attribute. SKIP_CODE is where
            // the saving is anyway, and this only ever runs for a name the prefilter already flagged.
            new ClassReader(Files.readAllBytes(classFile))
                    .accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitSource(String source, String debug) {
                            holder[0] = source;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("Cannot read {}: {}", classFile, e.toString());
        }
        return holder[0];
    }

    /** File name -> the relative paths it occupies under the source roots. Built once, and only when needed. */
    private static Map<String, List<String>> sourceFilesByName(List<Path> roots) {
        Map<String, List<String>> byName = new HashMap<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.getFileName().toString().endsWith(".java"))
                        .forEach(p -> byName.computeIfAbsent(p.getFileName().toString(), n -> new ArrayList<>())
                                .add(p.toString()));
            } catch (IOException io) {
                LOGGER.debug("Cannot walk source root {}: {}", root, io.getMessage());
            }
        }
        return byName;
    }

    /**
     * Where the copies go: inside the build tool's own output directory, as
     * {@code .../build/maddi-apt/<name>} or {@code .../target/maddi-apt/<name>}, so that the project's own
     * {@code clean} removes them. When no build output directory is recognised, a sibling of the destination —
     * which is a compiler output directory by definition, and therefore never a source tree.
     */
    static Path sideDirectory(Path destination, String sourceSetName) {
        // ⚠ THE LEAF IS A NAME WE INVENT, SO IT MUST BE ONE EVERY FILESYSTEM AND EVERY URI ACCEPTS. Replacing
        // only the separators was enough while every caller was `--compile-log`, whose source-set names are
        // path-shaped already; a build plugin's name is whatever the build model says, and Maven's is the POM's
        // <name> -- `LangChain4j :: Core/test`, `Camel :: Core Model/main`, `ActiveMQ :: Broker/main`. Anything
        // outside this set becomes '-', and runs collapse, so the result stays readable rather than escaped.
        String leaf = sourceSetName.replaceAll("[^A-Za-z0-9._-]+", "-");
        Path buildOutput = CompileListToSourceSets.buildOutputDirectory(destination.toString());
        if (buildOutput != null) return buildOutput.resolve(APT_DIRECTORY).resolve(leaf);
        return destination.resolveSibling(destination.getFileName() + "-" + APT_DIRECTORY + "-" + leaf);
    }

    private static void copy(Path destination, List<String> relatives, Path target) throws IOException {
        // ⛔ REBUILT FROM SCRATCH. This directory is derived from the build outputs AS THEY ARE NOW; keeping a
        // class from a previous run is the stale-jar failure, where rung 6 poisons rung 1.
        deletePreviousCopy(target);
        for (String relative : relatives) {
            Path to = target.resolve(relative);
            Files.createDirectories(to.getParent());
            Files.copy(destination.resolve(relative), to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * ⚠ Deletes only a directory this class made: the guard is the {@link #APT_DIRECTORY} marker in the path,
     * because the alternative — trusting the caller's path — deletes a build output the first time the naming
     * changes.
     */
    private static void deletePreviousCopy(Path target) throws IOException {
        if (!Files.isDirectory(target)) return;
        boolean ours = APT_DIRECTORY.equals(String.valueOf(target.getParent().getFileName()))
                       || target.getFileName().toString().contains("-" + APT_DIRECTORY + "-");
        if (!ours) throw new IOException(target + " is not a " + APT_DIRECTORY + " directory; refusing to remove it");
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
