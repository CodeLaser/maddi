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

import io.codelaser.maddi.run.config.util.ConfigurationChecks;
import io.codelaser.maddi.run.config.util.JavaModules;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Assembles a {@link CompileListToSourceSets.Result} into an {@link InputConfiguration}: the JDK module closure,
 * the source sets, the libraries, and the two repairs neither front-end can make on its own — the generated
 * classes an output directory holds ({@link AnnotationProcessorOutput}) and the TYPE_USE annotations a compile
 * classpath does not close over ({@link TypeUseAnnotationClosure}).
 *
 * <p>⚠ THIS EXISTS BECAUSE THERE WERE TWO COPIES OF IT. {@code ParseJavacList} and {@code ParseKotlincList} each
 * carried the same twenty lines, so anything added to one silently did not hold for the other — and a defect
 * fixed in one reader is not fixed. They now both call this, which is what makes the checks below hold for both
 * front-ends BY CONSTRUCTION rather than by remembering.
 */
public class CompileListToInputConfiguration {
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(CompileListToInputConfiguration.class);


    /**
     * @param result     the source-set graph reconstructed from the compile invocations
     * @param extraJmods JDK modules to add on top of the {@code java.se} closure, each with its own closure
     */
    public static InputConfiguration build(CompileListToSourceSets.Result result, List<String> extraJmods) {
        return build(result, extraJmods, List.of());
    }

    /**
     * @param result               the source-set graph reconstructed from the compile invocations
     * @param extraJmods           JDK modules to add on top of the {@code java.se} closure
     * @param excludedSourceSets   names of source sets to keep OUT of the parse; see {@link #exclude}
     */
    public static InputConfiguration build(CompileListToSourceSets.Result result, List<String> extraJmods,
                                           List<String> excludedSourceSets) {
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        // ⛔⛔ THE ANALYSED TREE MUST NOT BE FOREIGN TO ITSELF. Without this the working directory stays at its
        // default "." -- the JVM's own directory -- and every lever that resolves a path against the project
        // root refuses the corpus it is analysing. Measured on elasticsearch: writeModuleInfo answered
        // "Refusing to write outside the project: .../es-phase3/libs/core resolves outside
        // .../codelaser-refactor-graalpy". The build root is known here; it was simply never passed on.
        if (result.buildRoot() != null && !result.buildRoot().isBlank()) {
            builder.setWorkingDirectory(result.buildRoot());
        }
        Set<String> closure = new HashSet<>(JavaModules.jmodDependencyClosure("java.se"));
        if (extraJmods != null) {
            extraJmods.forEach(jm -> {
                closure.add(jm);
                closure.addAll(JavaModules.jmodDependencyClosure(jm));
            });
        }
        // sorted() so the jmod classpath parts have a deterministic order (a HashSet's iteration order is not
        // stable across runs, which otherwise shuffles the serialized InputConfiguration)
        closure.stream().sorted().forEach(jmod -> builder.addClassPathParts(JavaModules.jmodSourceSet(jmod)));

        List<SourceSet> sourceSets = result.jSourceSets().stream()
                .map(CompileListToSourceSets.JSourceSet::sourceSet).toList();

        // ⚠ EXCLUSION FIRST, so nothing below spends time on a source set that is not going to be parsed --
        // and so an excluded set's own classpath never enters the configuration, which is the point of it.
        Excluded excluded = exclude(sourceSets, excludedSourceSets);
        sourceSets = excluded.kept();

        // An output directory that has become a source set carries the classes an annotation processor generated
        // during that compile, and those belong to nothing at all once the directory leaves the classpath.
        // Detection only: the source sets keep their identity until the closure below has run over them.
        AnnotationProcessorOutput.Result generated = new AnnotationProcessorOutput().materialise(sourceSets);
        List<SourceSet> classPathParts = Stream.of(result.jars(), generated.libraries(), excluded.libraries())
                .flatMap(List::stream).toList();

        // A compile classpath is not a closure over the TYPE_USE annotations its dependencies carry.
        sourceSets = new TypeUseAnnotationClosure().close(sourceSets, classPathParts).sourceSets();
        sourceSets = generated.attach(sourceSets);

        sourceSets.forEach(builder::addSourceSets);
        classPathParts.forEach(builder::addClassPathParts);
        // ⚠ SHARED WITH BOTH BUILD PLUGINS SINCE 2026-08-19. They were private here, so the two producers that
        // had never met a corpus were also the two that ran no check at all.
        ConfigurationChecks.check(sourceSets, classPathParts, closure);
        setSourceRelease(result, builder);
        return builder.build();
    }

    /**
     * Carry the corpus's own {@code javac --release} into the configuration, so the parse compiles against the
     * platform the corpus was built for rather than the platform maddi happens to run on.
     *
     * <p>⛔ <b>ONLY WHEN THE ANSWER IS UNAMBIGUOUS.</b> A reactor may compile its modules against different
     * releases, and there is no single right answer then: picking the maximum hides a removed API from the
     * module that still uses it, picking the minimum invents compile errors in the module that does not.
     * So a mixed corpus is left on today's behaviour and <b>says so</b> — the operator can set
     * {@code --jre}/{@code alternativeJREDirectory} deliberately, which is the more specific instruction and
     * wins in {@code JavaInspectorImpl} either way.
     *
     * <p>⚠ An invocation that passes no {@code --release} and no {@code -source} contributes nothing rather
     * than a zero: absent is not "release 0", and letting it into the set would make every corpus look mixed.
     */
    private static void setSourceRelease(CompileListToSourceSets.Result result,
                                         InputConfigurationImpl.Builder builder) {
        // ⚠ A JSourceSet MAY CARRY A NULL INVOCATION: the source set is the subject, the invocation is how it
        // was discovered, and callers that build a configuration directly (every fixture in
        // TestNamesAreIdentities and TestExcludeSourceSets) pass none. Four green tests turned red on the first
        // run of this method for want of one null check.
        Set<Integer> releases = result.jSourceSets().stream()
                .map(CompileListToSourceSets.JSourceSet::invocation)
                .filter(java.util.Objects::nonNull)
                .map(CompileInvocation::effectiveRelease)
                .filter(r -> r > 0)
                .collect(Collectors.toCollection(TreeSet::new));
        if (releases.size() == 1) {
            int release = releases.iterator().next();
            builder.setSourceRelease(release);
            LOGGER.info("All {} compile invocation(s) that state one target Java release {}; the parse will use"
                        + " it rather than the JDK it runs on ({})", result.jSourceSets().size(), release,
                    Runtime.version().feature());
        } else if (releases.size() > 1) {
            LOGGER.warn("Compile invocations target {} different Java releases {}: leaving sourceRelease unset,"
                        + " so the parse uses the running JDK ({}). If that JDK is newer than the lowest release"
                        + " here, expect 'cannot find symbol' on APIs removed since.", releases.size(), releases,
                    Runtime.version().feature());
        } else {
            LOGGER.info("No compile invocation states a Java release; the parse uses the running JDK ({})",
                    Runtime.version().feature());
        }
    }

    /**
     * @param kept      the source sets that stay in the parse, with their dependency edges re-pointed
     * @param libraries the excluded source sets, demoted to external libraries
     */
    private record Excluded(List<SourceSet> kept, List<SourceSet> libraries) {
    }

    /**
     * Keeps named source sets OUT of the parse — <b>by demoting them to libraries, not by deleting them.</b>
     *
     * <p>⛔⛔ <b>A COMPILE TASK LIST CANNOT EXPRESS THIS, WHICH IS WHY THE PROPERTY EXISTS.</b> Gradle compiles a
     * requested task's dependencies whether you asked for them or not, and every compile emits a javac line, so
     * every dependency becomes a source set. Measured on elasticsearch: a task list naming exactly the 348
     * wanted source sets brought <b>23 unwanted ones</b> in as dependencies — {@code modules/repository-gcs}
     * (whose jars carry four classes that cannot be committed, and one bad compilation unit refuses the whole
     * {@code ParseResult}) pulled in by {@code x-pack/plugin/stateless}, {@code test/fixtures/aws-fixture-utils}
     * by eleven dependents. A list says "do not compile this"; only this can say <b>"compile it, but do not
     * parse it"</b>.
     *
     * <p>⭐ <b>DEMOTED, NOT DROPPED, AND THAT IS THE WHOLE DESIGN.</b> The excluded set's output directory
     * becomes an ordinary external library under the same name, so:
     * <ul>
     *   <li>its types still resolve for everything that depends on it — dependency edges are re-pointed at the
     *       library, so an exclusion does <b>not</b> have to be closed under "who depends on this", which is the
     *       trap the hand-written generator this replaces kept falling into;</li>
     *   <li>its own classpath never enters the configuration — which is exactly what makes excluding
     *       {@code repository-gcs} work: the four uncommittable jars come in with <i>its</i> classpath, and it
     *       no longer has one here;</li>
     *   <li>no lever can edit it, because nothing edits a library.</li>
     * </ul>
     *
     * <p>⛔ <b>AND AN ENTRY THAT MATCHES NOTHING IS REFUSED, NOT IGNORED.</b> An exclusion that silently does
     * nothing gives you a <i>wider</i> parse than you asked for, and the reason it stops matching is almost
     * always a rename — the elasticsearch switch renamed 338 of 348 source sets in one step. A warning in a log
     * is not enough for that: the caller asked for a narrower parse and would get a wider one with a zero exit.
     * The message lists the source sets whose name ends the same way, because after a rename that is the answer.
     */
    private static Excluded exclude(List<SourceSet> sourceSets, List<String> excludedSourceSets) {
        if (excludedSourceSets == null || excludedSourceSets.isEmpty()) return new Excluded(sourceSets, List.of());
        Set<String> wanted = new LinkedHashSet<>(excludedSourceSets);
        Map<String, SourceSet> demoted = new LinkedHashMap<>();
        for (SourceSet sourceSet : sourceSets) {
            if (wanted.contains(sourceSet.name())) demoted.put(sourceSet.name(), asLibrary(sourceSet));
        }
        List<String> unmatched = wanted.stream().filter(n -> !demoted.containsKey(n)).toList();
        if (!unmatched.isEmpty()) {
            throw new IllegalStateException("These source sets were to be excluded and do not exist: " + unmatched
                                            + ". An exclusion that matches nothing widens the parse silently."
                                            + nearMisses(unmatched, sourceSets));
        }
        List<SourceSet> kept = new ArrayList<>(sourceSets.size() - demoted.size());
        for (SourceSet sourceSet : sourceSets) {
            if (demoted.containsKey(sourceSet.name())) continue;
            // ⛔ "HAS ANYTHING CHANGED?" CANNOT BE ASKED WITH equals HERE. A SourceSet's equality is its NAME --
            // the contract says so, and it is why a name is an identity -- so the demoted library compares EQUAL
            // to the source set it replaces, and a `dependencies.equals(old)` guard silently keeps the original.
            boolean demotedDependency = sourceSet.dependencies().stream().anyMatch(d -> demoted.containsKey(d.name()));
            if (demotedDependency) {
                kept.add(sourceSet.withDependencies(sourceSet.dependencies().stream()
                        .map(d -> demoted.getOrDefault(d.name(), d)).toList()));
            } else {
                kept.add(sourceSet);
            }
        }
        LOGGER.info("Excluded {} source set(s), each demoted to a library so its dependents still resolve: {}",
                demoted.size(), demoted.keySet());
        return new Excluded(List.copyOf(kept), List.copyOf(demoted.values()));
    }

    /** After a rename, the set you meant is the one whose name ends the same way. */
    private static String nearMisses(List<String> unmatched, List<SourceSet> sourceSets) {
        StringBuilder sb = new StringBuilder();
        for (String name : unmatched) {
            String leaf = name.substring(name.lastIndexOf('/') + 1);
            String tail = name.contains("/") ? name.substring(name.indexOf('/')) : "/" + leaf;
            List<String> similar = sourceSets.stream().map(SourceSet::name)
                    .filter(n -> n.endsWith(tail) || n.endsWith("/" + leaf) && n.contains(leaf)).sorted().limit(4)
                    .toList();
            if (!similar.isEmpty()) sb.append(" Did you mean, for '").append(name).append("': ").append(similar);
        }
        return sb.toString();
    }

    /**
     * The same output directory, under the same name, as a library: readable by everyone, parsed by nobody.
     * ⚠ Shared with the absorbed-source-set demotion in {@link CompileListToSourceSets}, deliberately — two
     * copies of this is how the two demotions would come to disagree.
     */
    private static SourceSet asLibrary(SourceSet sourceSet) {
        return CompileListToSourceSets.asLibrary(sourceSet);
    }

}
