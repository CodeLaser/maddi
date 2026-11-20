/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.callgraph;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.INVALID;
import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.REWIRE;
import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.UNCHANGED;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;

/**
 * The re-parse after an edit: the changed primary types are parsed again ({@code INVALID}), their dependents
 * ({@link PrimaryTypeUseGraph#dependentsOf}) are rewired ({@code REWIRE}), the rest is kept. Rewiring a dependent is
 * only right when javac would resolve its names as before, and the re-parse refuses it for the dependents that rely on
 * a changed type's declarations when those moved ({@link JavaInspector.DeclarationsChangedException}: an overload,
 * an override of an inherited method, a supertype, a constant, a member that is gone). Those dependents are parsed
 * again, in a next parse: they are {@code INVALID} there; the types already parsed again, live as their new objects
 * and possibly referring back to the dependents, {@code REWIRE}; the other dependents {@code REWIRE} as before. That
 * parse can refuse again, when a re-parsed dependent's own declarations moved (a constant computed from a changed
 * one): every round parses at least one more type, so the loop ends.
 * <p>
 * The dependents are the transitive closure, so no kept type refers to a re-parsed one, in any parse. A refused
 * parse rewires nothing, so the dependents rewired by the last parse are rewired from the generation before the edit,
 * and its rewire memo ({@link JavaInspector#lastRewireInfoMap}) leads from there: their analysis can be carried.
 */
public final class ReparseWithDependents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReparseWithDependents.class);

    private ReparseWithDependents() {
    }

    /**
     * @param parseResult          the live result
     * @param declarationsChanged  the types whose declarations moved, by name, over every round; empty when every
     *                             dependent was rewired
     * @param reparsedDependents   the dependents that were parsed again, as the objects of the generation before the
     *                             edit: new objects without any analysis data. The other dependents were rewired from
     *                             that generation, and their carried prep and analysis data still describes their code
     */
    public record Outcome(ParseResult parseResult, Set<String> declarationsChanged,
                          Set<TypeInfo> reparsedDependents) {

        public boolean dependentsReparsed() {
            return !reparsedDependents.isEmpty();
        }
    }

    /**
     * @param changed    the primary types whose source changed, as the objects of the current generation
     * @param dependents their dependents, disjoint from {@code changed}
     * @param options    the parse options for a given invalidation
     */
    public static Outcome go(JavaInspector javaInspector, Set<TypeInfo> changed, Set<TypeInfo> dependents,
                             Function<JavaInspector.Invalidated, JavaInspector.ParseOptions> options) {
        assert Collections.disjoint(changed, dependents);
        // by name (Info equality: name and source set): after a refused round, a re-scanned type is live as its new
        // object, a dependent not yet parsed again still as its old one
        Set<String> parsedAgain = new HashSet<>();
        Set<String> invalid = new HashSet<>();
        for (TypeInfo type : changed) invalid.add(key(type));
        Set<String> dependentKeys = new HashSet<>();
        for (TypeInfo type : dependents) dependentKeys.add(key(type));
        Set<String> declarationsChanged = new TreeSet<>();
        // every refused round parses at least one type more; a round past that bound is a defect
        int maxRounds = changed.size() + dependents.size() + 1;
        for (int round = 1; ; ++round) {
            if (round > maxRounds) throw new IllegalStateException("No end to the re-parse rounds after " + maxRounds);
            Set<String> invalidNow = Set.copyOf(invalid);
            Set<String> rewireNow = Set.copyOf(parsedAgain);
            try {
                ParseResult result = javaInspector.parse(options.apply(ti -> {
                    String key = key(ti);
                    if (invalidNow.contains(key)) return INVALID;
                    return rewireNow.contains(key) || dependentKeys.contains(key) ? REWIRE : UNCHANGED;
                })).parseResult();
                Set<TypeInfo> reparsed = new HashSet<>();
                for (TypeInfo type : dependents) {
                    String key = key(type);
                    if (parsedAgain.contains(key) || invalidNow.contains(key)) reparsed.add(type);
                }
                return new Outcome(result, Set.copyOf(declarationsChanged), Set.copyOf(reparsed));
            } catch (JavaInspector.DeclarationsChangedException e) {
                Set<String> affected = new HashSet<>();
                for (TypeInfo type : e.affected()) affected.add(key(type));
                parsedAgain.addAll(invalid);
                // an exception without the affected types is one of an older contract: every dependent
                if (affected.isEmpty()) affected.addAll(dependentKeys);
                affected.removeAll(invalid);
                if (affected.isEmpty()) throw new IllegalStateException("A refused re-parse names no new type", e);
                LOGGER.info("The declarations of {} changed: parsing {} of the {} dependent type(s) again", e.types(),
                        affected.size(), dependents.size());
                declarationsChanged.addAll(e.types());
                invalid = new HashSet<>(affected);
            }
        }
    }

    // Info equality: the name and the source set
    private static String key(TypeInfo type) {
        return type.fullyQualifiedName() + "@" + type.compilationUnit().sourceSet().name();
    }
}
