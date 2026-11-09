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
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;

/**
 * The re-parse after an edit: the changed primary types are parsed again ({@code INVALID}), their dependents
 * ({@link PrimaryTypeUseGraph#dependentsOf}) are rewired ({@code REWIRE}), the rest is kept. Rewiring a dependent is
 * only right when javac would resolve its names as before, and the re-parse refuses it when the declarations of a
 * changed type moved ({@link JavaInspector.DeclarationsChangedException}: an overload, an override of an inherited
 * method, a supertype, a constant, a member that is gone). Then the dependents are parsed again as well, in a second
 * parse: they are {@code INVALID} there, and the changed types, already live as their new objects and possibly
 * referring back to the dependents, {@code REWIRE}.
 * <p>
 * The dependents are the transitive closure, so no kept type refers to a re-parsed one, in either parse.
 */
public final class ReparseWithDependents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReparseWithDependents.class);

    private ReparseWithDependents() {
    }

    /**
     * @param parseResult          the live result
     * @param declarationsChanged  the changed types whose declarations moved, by name; empty when the dependents were
     *                             rewired, and their carried prep and analysis data still describes their code
     * @param dependentsReparsed   whether the dependents were parsed again: they are new objects without any analysis
     *                             data, and the rewire memo ({@link JavaInspector#lastRewireInfoMap}) is that of the
     *                             second parse, which does not lead from the generation before the edit
     */
    public record Outcome(ParseResult parseResult, Set<String> declarationsChanged, boolean dependentsReparsed) {
    }

    /**
     * @param changed    the primary types whose source changed, as the objects of the current generation
     * @param dependents their dependents, disjoint from {@code changed}
     * @param options    the parse options for a given invalidation
     */
    public static Outcome go(JavaInspector javaInspector, Set<TypeInfo> changed, Set<TypeInfo> dependents,
                             Function<JavaInspector.Invalidated, JavaInspector.ParseOptions> options) {
        assert Collections.disjoint(changed, dependents);
        try {
            ParseResult rewired = javaInspector.parse(options.apply(
                    ti -> changed.contains(ti) ? INVALID : dependents.contains(ti) ? REWIRE : UNCHANGED)).parseResult();
            return new Outcome(rewired, Set.of(), false);
        } catch (JavaInspector.DeclarationsChangedException e) {
            LOGGER.info("The declarations of {} changed: parsing the {} dependent type(s) again", e.types(),
                    dependents.size());
            // the changed types are live as new objects, known here by name only; the dependents are as they were
            Set<String> changedNames = new HashSet<>();
            for (TypeInfo type : changed) changedNames.add(key(type));
            ParseResult reparsed = javaInspector.parse(options.apply(
                    ti -> dependents.contains(ti) ? INVALID : changedNames.contains(key(ti)) ? REWIRE : UNCHANGED))
                    .parseResult();
            return new Outcome(reparsed, e.types(), true);
        }
    }

    // Info equality: the name and the source set
    private static String key(TypeInfo type) {
        return type.fullyQualifiedName() + "@" + type.compilationUnit().sourceSet().name();
    }
}
