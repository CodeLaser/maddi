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
import java.util.List;
import java.util.Set;

/**
 * Assembles a {@link CompileListToSourceSets.Result} into an {@link InputConfiguration}: the JDK module closure,
 * the source sets, the libraries.
 *
 * <p>⚠ THIS EXISTS BECAUSE THERE WERE TWO COPIES OF IT. {@code ParseJavacList} and {@code ParseKotlincList} each
 * carried the same twenty lines, so anything added to one silently did not hold for the other — and a defect
 * fixed in one reader is not fixed. They now both call this, which is what makes the checks below hold for both
 * front-ends BY CONSTRUCTION rather than by remembering.
 */
public class CompileListToInputConfiguration {

    /**
     * @param result     the source-set graph reconstructed from the compile invocations
     * @param extraJmods JDK modules to add on top of the {@code java.se} closure, each with its own closure
     */
    public static InputConfiguration build(CompileListToSourceSets.Result result, List<String> extraJmods) {
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
        // A compile classpath is not a closure over the TYPE_USE annotations its dependencies carry.
        sourceSets = new TypeUseAnnotationClosure().close(sourceSets, result.jars()).sourceSets();

        sourceSets.forEach(builder::addSourceSets);
        result.jars().forEach(builder::addClassPathParts);
        checkNamesAreIdentities(sourceSets, result.jars(), closure);
        return builder.build();
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
