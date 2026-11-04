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

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.byname.ByNameDangling;
import io.codelaser.maddi.inspection.api.byname.ByNameReference;
import io.codelaser.maddi.inspection.api.byname.ByNameSink;
import io.codelaser.maddi.inspection.api.parser.ParseResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The call graph kept per primary type, across runs: each primary type's vertices, edges and recursive methods are
 * its own contribution, and {@link ComputeCallGraph#go(CallGraphContributions)} unions them. After a partial re-parse
 * the {@code INVALID} and {@code REWIRE} types are new objects, so only they are computed again; a kept type's
 * contribution is reused (docs/design/incremental-and-overlay.md §7 item 5).
 * <p>
 * Why a contribution can be reused: every edge a type's walk adds starts at one of its own members (the inverted
 * field edge starts at its own field), and the walk reads only the edges it added itself. So a contribution depends on
 * the type's own objects, on the objects its references resolve to, and on which types are accepted. Each of those is
 * checked before a contribution is reused (see {@link #prepare}): the key is the type object itself; every source type
 * a contribution points into must still be in the universe, the same object; a universe that gained a type, or another
 * external predicate, clears everything.
 * <p>
 * With by-name sinks declared ({@link ComputeCallGraph#withByNameSinks}), a type's by-name rows are part of its
 * contribution: its references, danglings and unresolved sink calls. A row depends on two more things, and both are
 * checked too. The binary names it read must resolve now as they did then, to the same type objects (a type that was
 * removed, re-parsed, or added as a nested type under a kept name changes that); and the {@code static final} field a
 * name came through, possibly in another type, is a target like any other. Another list of sinks clears everything.
 * <p>
 * Not thread-safe. Hold one per sequence of parses of the same inspector: a contribution holds the objects of the
 * generation it was computed on, and is dropped when its type leaves the universe.
 */
public class CallGraphContributions {

    record Contribution(G.Builder<Info> builder, Set<MethodInfo> recursive, Set<TypeInfo> targets,
                        List<ByNameReference> byNameReferences, List<ByNameDangling> byNameDanglings,
                        int unresolvedSinkCalls, Map<String, List<TypeInfo>> resolutions) {
    }

    private final Map<TypeInfo, Contribution> byType = new IdentityHashMap<>();
    private Set<String> lastFullyQualifiedNames;
    private Predicate<TypeInfo> lastExternalsToAccept;
    private List<ByNameSink> lastByNameSinks;
    private ParseResult byNameParseResult;
    private int reused;
    private int computed;
    private int stale;

    /** Of the last run: how many primary types' contributions were reused. */
    public int reused() {
        return reused;
    }

    /** Of the last run: how many primary types' contributions were computed. */
    public int computed() {
        return computed;
    }

    /**
     * Of the last run: how many kept types' contributions were dropped because they pointed into a replaced type, and
     * were walked again (counted in {@link #computed()} too). Not 0 means the invalidation kept a type that refers to
     * one it replaced.
     */
    public int stale() {
        return stale;
    }

    public int size() {
        return byType.size();
    }

    public void clear() {
        byType.clear();
        lastFullyQualifiedNames = null;
        lastExternalsToAccept = null;
        lastByNameSinks = null;
        byNameParseResult = null;
    }

    /* Drop what no longer holds for this universe; see the class comment. */
    void prepare(Set<TypeInfo> primaryTypes, Predicate<TypeInfo> externalsToAccept, List<ByNameSink> byNameSinks,
                 ParseResult byNameParseResult) {
        reused = 0;
        computed = 0;
        stale = 0;
        Set<String> fqns = new HashSet<>();
        for (TypeInfo pt : primaryTypes) fqns.add(pt.fullyQualifiedName());
        if (externalsToAccept != lastExternalsToAccept
            || lastFullyQualifiedNames != null && !lastFullyQualifiedNames.containsAll(fqns)
            || lastByNameSinks != null && !lastByNameSinks.equals(byNameSinks)) {
            byType.clear();
        }
        lastFullyQualifiedNames = fqns;
        lastExternalsToAccept = externalsToAccept;
        lastByNameSinks = byNameSinks;
        this.byNameParseResult = byNameParseResult;
        Set<TypeInfo> universe = Collections.newSetFromMap(new IdentityHashMap<>());
        universe.addAll(primaryTypes);
        // stale: a target named like a source type of this universe that is not that type's object. A target
        // outside the universe (a library type: an explicit constructor invocation's edge, an accepted external)
        // was outside when the walk ran too, and the predicate is the same one
        byType.keySet().removeIf(type -> !universe.contains(type));
        byType.values().removeIf(c -> {
            boolean isStale = c.targets().stream().anyMatch(t -> !universe.contains(t)
                                                                 && fqns.contains(t.fullyQualifiedName()))
                              || !c.resolutions().isEmpty() && (byNameParseResult == null
                                                                || !sameResolutions(c.resolutions(), byNameParseResult));
            if (isStale) ++stale;
            return isStale;
        });
    }

    /* every binary name resolves to the same type objects, in the same order; a dangling name still to none */
    private static boolean sameResolutions(Map<String, List<TypeInfo>> resolutions, ParseResult parseResult) {
        for (Map.Entry<String, List<TypeInfo>> e : resolutions.entrySet()) {
            List<TypeInfo> now = parseResult.typeByBinaryName(e.getKey());
            List<TypeInfo> then = e.getValue();
            if (now.size() != then.size()) return false;
            for (int i = 0; i < now.size(); i++) if (now.get(i) != then.get(i)) return false;
        }
        return true;
    }

    Contribution get(TypeInfo primaryType) {
        Contribution c = byType.get(primaryType);
        if (c != null) ++reused;
        return c;
    }

    Contribution put(TypeInfo primaryType, G.Builder<Info> builder, Set<MethodInfo> recursive,
                     List<ByNameReference> byNameReferences, List<ByNameDangling> byNameDanglings,
                     int unresolvedSinkCalls) {
        Set<TypeInfo> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Info vertex : builder.vertices()) {
            TypeInfo type = vertex instanceof TypeInfo ti ? ti : vertex.typeInfo();
            TypeInfo owner = type == null ? null : type.primaryType();
            if (owner != null && owner != primaryType) targets.add(owner);
        }
        for (MethodInfo mi : recursive) {
            TypeInfo owner = mi.typeInfo().primaryType();
            if (owner != primaryType) targets.add(owner);
        }
        Map<String, List<TypeInfo>> resolutions = new HashMap<>();
        for (ByNameReference r : byNameReferences) {
            addOwner(targets, r.targetType(), primaryType);
            addOwner(targets, r.siteOwner(), primaryType);
            addOwner(targets, r.targetMember(), primaryType);
            resolutions.computeIfAbsent(r.binaryName(), this::resolve);
        }
        for (ByNameDangling d : byNameDanglings) {
            addOwner(targets, d.siteOwner(), primaryType);
            resolutions.computeIfAbsent(d.binaryName(), this::resolve);
        }
        Contribution c = new Contribution(builder, Set.copyOf(recursive), targets, List.copyOf(byNameReferences),
                List.copyOf(byNameDanglings), unresolvedSinkCalls, Map.copyOf(resolutions));
        byType.put(primaryType, c);
        ++computed;
        return c;
    }

    private List<TypeInfo> resolve(String binaryName) {
        return List.copyOf(Objects.requireNonNull(byNameParseResult, "by-name rows without a parse result")
                .typeByBinaryName(binaryName));
    }

    private static void addOwner(Set<TypeInfo> targets, Info info, TypeInfo primaryType) {
        if (info == null) return;
        TypeInfo type = info instanceof TypeInfo ti ? ti : info.typeInfo();
        TypeInfo owner = type == null ? null : type.primaryType();
        if (owner != null && owner != primaryType) targets.add(owner);
    }
}
