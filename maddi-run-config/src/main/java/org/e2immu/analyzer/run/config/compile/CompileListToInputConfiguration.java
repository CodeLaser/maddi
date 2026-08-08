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
package org.e2immu.analyzer.run.config.compile;

import org.e2immu.analyzer.run.config.util.JavaModules;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        Set<String> closure = new HashSet<>(JavaModules.jmodDependencyClosure("java.se"));
        if (extraJmods != null) {
            extraJmods.forEach(jm -> {
                closure.add(jm);
                closure.addAll(JavaModules.jmodDependencyClosure(jm));
            });
        }
        // sorted() so the jmod classpath parts have a deterministic order (a HashSet's iteration order is not
        // stable across runs, which otherwise shuffles the serialized InputConfiguration)
        closure.stream().sorted().forEach(jmod -> builder.addClassPathParts(
                new SourceSetImpl.Builder().setName(jmod)
                        .setSourceDirectories(List.of())
                        .setUri(URI.create("jmod:" + jmod))
                        .setLibrary(true)
                        .setExternalLibrary(true)
                        .setPartOfJdk(true)
                        .setModule(true)
                        .build()));

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
        checkNamesAreIdentities(sourceSets, classPathParts, closure);
        checkEveryDependencyResolves(sourceSets, classPathParts, closure);
        return builder.build();
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

    /**
     * ⛔⛔ A NAME IS THE IDENTITY, SO A DUPLICATE NAME IS A WRONG ANSWER RATHER THAN AN UNTIDY ONE.
     * {@code SourceSet}'s own contract says it: <i>"source sets are identified by their name() throughout the
     * system, including in serialized dependency references"</i> — {@code buildUnit()} is deliberately excluded
     * from {@code equals}. A serialized {@code dependencies: ["core/main"]} therefore has exactly one reading,
     * and two entries answering to one name make it a coin toss.
     * <p>
     * ⚠ THE SYMPTOM IS NOT A DUPLICATE-NAME ERROR, WHICH IS THE WHOLE REASON TO CHECK HERE. When a library took a
     * declared source set's name in the Elasticsearch parse config, what came out was a <b>phantom dependency
     * cycle</b> — a source set that appeared to depend on itself — and the diagnosis was the expensive part. This
     * is the only place that sees the source sets, the jars and the jmods together, so it is the only place the
     * question can be asked at all.
     * <p>
     * ⚠ MEASURED, and it does not currently fire: today's Elasticsearch configuration has 348 source sets and 785
     * class-path parts with <b>0</b> shared names, and 0 duplicates within either group. This is prevention with a
     * denominator, not a repair — and it costs one pass over ~1,100 strings. It throws rather than warns because a
     * caller cannot do anything sensible with an ambiguous graph, and because a warning here was already tried:
     * {@code handleJarInClasspath} logs {@code "Name clash"} and keeps the first jar.
     */
    /**
     * ⛔⛔ <b>A DEPENDENCY NAMES SOMETHING, OR THE PACKAGES IT PROVIDES SIMPLY DO NOT EXIST.</b> A serialized
     * configuration refers to dependencies by name, so a name present on an edge and absent from both lists is
     * not an untidy reference — it is a set of types that will not resolve, and the parse says so in a way that
     * points at the victim rather than the cause.
     * <p>
     * ⚠ <b>MEASURED, AND IT WOULD HAVE FIRED.</b> On the Elasticsearch configuration generated 2026-08-08,
     * {@code libs/native/main} was named by <b>208 of 348</b> source sets and existed nowhere: two invocations
     * compile its 38 files (one real, one {@code -proc:only}) and the containment rule kept only one
     * destination. What surfaced, a day later and 214 s into a run, was
     * <i>"package org.elasticsearch.nativeaccess does not exist"</i> in {@code Spawner.java}, one dropped
     * compilation unit, and {@code Summary.parseResult()} refusing the whole {@code ParseResult}.
     * ▶ <b>THE COST OF THE MISSING CHECK WAS NOT THE DEFECT, IT WAS THE DISTANCE FROM THE DEFECT.</b> Six
     * reconciliation checks passed over that configuration — names, counts, test flags, a topological sort,
     * class-path parts, generated classes — and not one of them asked whether an edge pointed at anything.
     * <p>
     * ⚠ It runs over the FINAL lists, after the exclusion demotion, the generated-class attachment and the
     * TYPE_USE closure, because each of those rewrites edges. Cost: one pass over ~27,000 edges.
     */
    private static void checkEveryDependencyResolves(List<SourceSet> sourceSets, List<SourceSet> classPathParts,
                                                     Set<String> jmodNames) {
        Set<String> known = new HashSet<>(jmodNames);
        sourceSets.forEach(ss -> known.add(ss.name()));
        classPathParts.forEach(part -> known.add(part.name()));
        Map<String, List<String>> dangling = new LinkedHashMap<>();
        for (SourceSet sourceSet : sourceSets) {
            for (SourceSet dependency : sourceSet.dependencies()) {
                if (!known.contains(dependency.name())) {
                    dangling.computeIfAbsent(dependency.name(), n -> new ArrayList<>()).add(sourceSet.name());
                }
            }
        }
        if (!dangling.isEmpty()) {
            StringBuilder sb = new StringBuilder("These dependencies name nothing in the configuration, so the"
                                                 + " packages they provide will not resolve and the compilation"
                                                 + " units using them are dropped:");
            dangling.forEach((name, users) -> sb.append("\n  '").append(name).append("' <- ").append(users.size())
                    .append(" source set(s), e.g. ").append(users.stream().limit(3).toList()));
            throw new IllegalStateException(sb.toString());
        }
    }

    private static void checkNamesAreIdentities(List<SourceSet> sourceSets, List<SourceSet> jars,
                                                Set<String> jmodNames) {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (SourceSet ss : sourceSets) if (!seen.add(ss.name())) duplicates.add("source set " + ss.name());
        for (SourceSet jar : jars) if (!seen.add(jar.name())) duplicates.add("library " + jar.name());
        for (String jmod : jmodNames) if (!seen.add(jmod)) duplicates.add("jmod " + jmod);
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException("A name identifies a source set, so these are ambiguous"
                                            + " dependency references: " + duplicates
                                            + ". Expect the symptom to look like a dependency cycle rather than"
                                            + " like a name clash.");
        }
    }
}
