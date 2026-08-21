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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What must be true of an {@link io.codelaser.maddi.inspection.api.resource.InputConfiguration} before anything
 * reads it, whichever of the three producers built it.
 *
 * <p>⛔⛔ <b>ONLY ONE PRODUCER RAN THESE, AND IT IS NOT THE ONE THAT NEEDED THEM MOST.</b> They were written for
 * {@code --compile-log}, which reconstructs a configuration out of javac command lines and can therefore get it
 * subtly wrong — so the checks lived as private methods of {@code CompileListToInputConfiguration} and neither
 * build plugin could reach them. That is backwards: the log route sees a whole reactor at once and is the route
 * with a reference to be diffed against, while a plugin sees one module, is invoked in isolation, and until
 * 2026-08-19 had never been pointed at a corpus at all. Every one of the five defects found in the Gradle plugin
 * and the three found in the Maven one was invisible to every automated gate.
 *
 * <p>⚠ <b>AND THESE WOULD NOT HAVE CAUGHT THEM, WHICH IS WORTH STATING RATHER THAN GLOSSING.</b> The sibling
 * drop is the instructive case: three projects all named {@code classes}, so the configuration contained one
 * part, every edge pointed at it, and both checks below are satisfied by a graph that is simply missing two
 * thirds of its class path. What catches THAT is the name-clash warning at the point of construction, in each
 * plugin's own {@code ComputeSourceSets}. These checks cover the family next door — a name that means two
 * things, an edge that means nothing — which no producer had ever verified either, and which the log route
 * has been measured to hit twice.
 *
 * <p>⚠ <b>THEY THROW, AND THAT IS DELIBERATE FOR A PLUGIN TOO.</b> A caller cannot do anything sensible with an
 * ambiguous or dangling graph, and the symptom if it is let through does not point at the cause: an ambiguous
 * name surfaces as a phantom dependency cycle, and a dangling edge as "package X does not exist" in the victim's
 * sources, a day and 214 seconds into a run. A build that stops with the name of the clash is strictly cheaper.
 *
 * <p>Cost, measured on the largest configuration in the corpus (elasticsearch, 348 source sets / 785 class-path
 * parts / ~27,000 edges): one pass over ~1,100 strings and one over the edges.
 */
public class ConfigurationChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationChecks.class);

    /**
     * Every check, in the order their failures make sense in: an ambiguous name first, because a dangling edge is
     * one of the things an ambiguous name causes.
     *
     * @param extraKnownNames names that resolve but appear in neither list. {@code --compile-log} keeps its JDK
     *                        modules apart until the very end and passes them here; both plugins have already
     *                        put theirs into {@code classPathParts}, and pass nothing.
     */
    public static void check(List<SourceSet> sourceSets, List<SourceSet> classPathParts,
                             Set<String> extraKnownNames) {
        checkNamesAreIdentities(sourceSets, classPathParts, extraKnownNames);
        checkEveryDependencyResolves(sourceSets, classPathParts, extraKnownNames);
        checkDependencyReleases(sourceSets);
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
    static void checkEveryDependencyResolves(List<SourceSet> sourceSets, List<SourceSet> classPathParts,
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

    static void checkNamesAreIdentities(List<SourceSet> sourceSets, List<SourceSet> jars,
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

    /**
     * A set may not be compiled against an OLDER Java release than something it depends on.
     * <p>
     * ⚠ <b>A WARNING, NOT A REFUSAL, AND ON PURPOSE.</b> The rule is real — class files of release N are not
     * consumable by a compilation at a release below N, so a build in that shape could not have produced the
     * log this configuration was scraped from — which is exactly why the finding is far more likely to be a
     * MIS-SCRAPE than a real corpus. Refusing would turn a scrape defect into "maddi cannot read this project";
     * saying it loudly turns it into one line naming the two sets. It is also the only cross-set statement the
     * per-set releases make, so if the scrape ever attributes a release to the wrong set, this is what notices.
     * <p>
     * Sets that state nothing ({@code <= 0}) are skipped on both sides: absent is not release 0, and a corpus
     * where nothing states a release must not produce a wall of warnings about it.
     */
    static void checkDependencyReleases(List<SourceSet> sourceSets) {
        for (SourceSet consumer : sourceSets) {
            int consumerRelease = consumer.sourceRelease();
            if (consumerRelease <= 0) continue;
            for (SourceSet dependency : consumer.dependencies()) {
                int dependencyRelease = dependency.sourceRelease();
                if (dependencyRelease <= 0 || dependencyRelease <= consumerRelease) continue;
                LOGGER.warn("Source set '{}' states release {} but depends on '{}', which states {}."
                            + " A compilation cannot consume class files from a newer release, so the build this"
                            + " configuration describes could not have run: expect a mis-scraped release rather"
                            + " than a real one.",
                        consumer.name(), consumerRelease, dependency.name(), dependencyRelease);
            }
        }
    }
}
