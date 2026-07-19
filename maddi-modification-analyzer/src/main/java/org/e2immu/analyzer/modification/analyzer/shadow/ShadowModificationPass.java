package org.e2immu.analyzer.modification.analyzer.shadow;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.analyzer.modification.prepwork.variable.ObjectCreationVariable;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;

/**
 * PLAN-modification-reachability phase 1: the shadow pass. Computes modification as one-shot
 * worklist reachability over the converged link graph, WITHOUT writing any property, and diffs the
 * result against the frozen NON_MODIFYING_METHOD / UNMODIFIED_PARAMETER / UNMODIFIED_FIELD values.
 * Sound divergences go strictly in one direction: the shadow says "modified" where a prematurely
 * frozen TRUE says "unmodified". A reverse divergence (frozen modified, shadow unreached) is a bug
 * in this pass (incomplete seeds or edges) and is reported separately as a self-check.
 * <p>
 * Nodes: ParameterInfo, FieldInfo, and MethodInfo standing for the method's receiver
 * ("this method modifies its receiver object graph" = !NON_MODIFYING_METHOD).
 * <p>
 * Seeds: every entry of every method's converged METHOD_LINKS modified set — i.e. everything the
 * current analysis already believes modified, including the E7 captured-Result attributions which
 * fold into the creation site's modified set (see TestModificationFunctionalE7). Methods carrying
 * DEGRADED_ANALYSIS_METHOD seed their receiver and all parameters (soundness, plan §9.1).
 * <p>
 * Edges (modification flows from observation site to implicated site):
 * E1 callee parameter -> caller-side nodes of the argument (LINKED_VARIABLES_ARGUMENTS, which
 * requires the analyzer to run with trackObjectCreations); E2 callee receiver -> caller-side nodes
 * of the receiver expression (variable receivers only; others are counted); E3 field -> parameters
 * linked to it (METHOD_LINKS ofParameters); E4/E5 are projection rules: a this-scoped FieldReference
 * implicates the field, its containing fields along the scope chain, and the enclosing method's
 * receiver; E6 override parameter/receiver -> overridden parameter/receiver (the union-over-
 * implementations direction of AbstractMethodAnalyzer); E7 needs no edge here because seeding from
 * the modified sets inherits the engine's eager creation-site attribution. Element natures (plan
 * §7.1) are off: local links are followed through IS_ASSIGNED_FROM only, like the existing
 * consumers.
 */
public class ShadowModificationPass {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShadowModificationPass.class);

    public record Divergence(String property, Info info, String detail) {
        @Override
        public String toString() {
            return property + " " + detail;
        }
    }

    public record Report(Set<Object> reached,
                         List<Divergence> divergences,
                         List<Divergence> reverseDivergences,
                         Map<Object, Object> cause,
                         Map<Object, String> seedOrigin,
                         int methods, int methodsWithoutLinks, int seeds, int edgeCount,
                         int callSitesWithoutArgumentLinks, int unprojectedReceivers,
                         Map<String, Integer> missingArgLinkAnalyzedCallees,
                         Set<Object> frontierIncomplete,
                         int immutableGuardedDivergences) {

        /** the BFS chain from a reached node back to its seed, for divergence classification */
        public String explain(Object node) {
            StringBuilder sb = new StringBuilder(label(node));
            Object at = node;
            while (cause.containsKey(at)) {
                at = cause.get(at);
                sb.append(" <- ").append(label(at));
            }
            String origin = seedOrigin.get(at);
            if (origin != null) sb.append(" (seeded by ").append(origin).append(")");
            return sb.toString();
        }

        private static String label(Object node) {
            if (node instanceof MethodInfo mi) return "receiver:" + mi.fullyQualifiedName();
            if (node instanceof ParameterInfo pi) return "param:" + pi.fullyQualifiedName();
            if (node instanceof FieldInfo fi) return "field:" + fi.fullyQualifiedName();
            return node.toString();
        }
        public String summary() {
            int atAnalyzed = missingArgLinkAnalyzedCallees.values().stream().mapToInt(Integer::intValue).sum();
            String top = missingArgLinkAnalyzedCallees.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(5)
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b).orElse("-");
            return "shadow: " + methods + " methods (" + methodsWithoutLinks + " without links), "
                   + seeds + " seeds, " + edgeCount + " edges, " + reached.size() + " reached; "
                   + divergences.size() + " divergences (" + immutableGuardedDivergences
                   + " immutable-guarded), " + reverseDivergences.size() + " REVERSE divergences; "
                   + callSitesWithoutArgumentLinks + " call sites without argument links ("
                   + atAnalyzed + " at analyzed callees, top: " + top + "), "
                   + unprojectedReceivers + " unprojected receivers; "
                   + frontierIncomplete.size() + " frontier-incomplete nodes (no TRUE at cutover)";
        }

        public List<String> sortedDivergenceStrings() {
            return divergences.stream().map(Divergence::toString).sorted().toList();
        }
    }

    private final Map<Object, Set<Object>> successors = new HashMap<>();
    private final Set<Object> seeds = new HashSet<>();
    private final Map<Object, String> seedOrigin = new HashMap<>();
    private final Map<Object, Object> cause = new HashMap<>();
    private final Map<String, Integer> missingArgLinkAnalyzedCallees = new TreeMap<>();
    private final AnalysisHelper analysisHelper = new AnalysisHelper();
    private int immutableGuardedDivergences;
    // §10.1 invariant support for the cutover writer (PLAN §14 P2.3): nodes whose in-edge frontier
    // could NOT be fully constructed. "Unreached" is only evidence of "unmodified" when every
    // potential in-edge was built; the cutover pass must not write TRUE for tainted nodes (it
    // leaves the existing value). Tainted-and-reached is fine — FALSE needs no complete frontier.
    private final Set<Object> frontierIncomplete = new HashSet<>();
    private int methods, methodsWithoutLinks, edgeCount, callSitesWithoutArgumentLinks, unprojectedReceivers;

    public Report go(List<Info> analysisOrder) {
        List<MethodInfo> methodInfos = analysisOrder.stream()
                .filter(i -> i instanceof MethodInfo).map(i -> (MethodInfo) i).toList();
        List<FieldInfo> fieldInfos = analysisOrder.stream()
                .filter(i -> i instanceof FieldInfo).map(i -> (FieldInfo) i).toList();
        for (MethodInfo mi : methodInfos) {
            buildForMethod(mi);
        }
        Set<Object> reached = closure();
        List<Divergence> divergences = new ArrayList<>();
        List<Divergence> reverse = new ArrayList<>();
        for (MethodInfo mi : methodInfos) {
            diff(divergences, reverse, PropertyImpl.NON_MODIFYING_METHOD.key(), mi.analysis(),
                    PropertyImpl.NON_MODIFYING_METHOD, reached.contains(mi), mi, mi.fullyQualifiedName());
            for (ParameterInfo pi : mi.parameters()) {
                diff(divergences, reverse, PropertyImpl.UNMODIFIED_PARAMETER.key(), pi.analysis(),
                        PropertyImpl.UNMODIFIED_PARAMETER, reached.contains(pi), pi, pi.fullyQualifiedName());
            }
        }
        for (FieldInfo fi : fieldInfos) {
            diff(divergences, reverse, PropertyImpl.UNMODIFIED_FIELD.key(), fi.analysis(),
                    PropertyImpl.UNMODIFIED_FIELD, reached.contains(fi), fi, fi.fullyQualifiedName());
        }
        Report report = new Report(reached, List.copyOf(divergences), List.copyOf(reverse),
                Map.copyOf(cause), Map.copyOf(seedOrigin),
                methods, methodsWithoutLinks, seeds.size(), edgeCount,
                callSitesWithoutArgumentLinks, unprojectedReceivers,
                Map.copyOf(missingArgLinkAnalyzedCallees), Set.copyOf(frontierIncomplete),
                immutableGuardedDivergences);
        LOGGER.debug("{}", report.summary());
        return report;
    }

    private void diff(List<Divergence> divergences, List<Divergence> reverse, String propertyName,
                      PropertyValueMap analysis, org.e2immu.language.cst.api.analysis.Property property,
                      boolean reached, Info info, String detail) {
        Value.Bool frozen = analysis.getOrNull(property, ValueImpl.BoolImpl.class);
        if (frozen == null || !frozen.hasAValue()) return; // undecided: nothing to diff against
        // for all three properties, TRUE is the optimistic "unmodified"/"non-modifying" value
        if (frozen.isTrue() && reached) {
            divergences.add(new Divergence(propertyName, info, detail));
            // union over-approximation can reach immutable-typed nodes (int params via E6 position
            // alignment, String fields via projection); the cutover writer refuses to downgrade
            // those, so under MODREACH the promoted-baseline invariant is:
            // 0 reverse AND divergences == immutableGuarded (frozen == pass output otherwise)
            boolean immutableTyped = switch (info) {
                case ParameterInfo pi -> analysisHelper.typeImmutable(pi.parameterizedType()).isImmutable();
                case FieldInfo fi -> analysisHelper.typeImmutable(fi.type()).isImmutable();
                default -> false;
            };
            if (immutableTyped) immutableGuardedDivergences++;
        } else if (frozen.isFalse() && !reached) {
            reverse.add(new Divergence(propertyName, info, detail));
        }
    }

    // ------------------------------------------------------------------ graph construction

    private void buildForMethod(MethodInfo mi) {
        methods++;
        // E6: overrides, in the union-over-implementations direction
        for (MethodInfo overridden : mi.overrides()) {
            addEdge(mi, overridden);
            int n = Math.min(mi.parameters().size(), overridden.parameters().size());
            for (int i = 0; i < n; i++) {
                addEdge(mi.parameters().get(i), overridden.parameters().get(i));
            }
        }
        // soundness seeds: a degraded body contributes no reliable evidence
        Value.Bool degraded = mi.analysis().getOrNull(PropertyImpl.DEGRADED_ANALYSIS_METHOD, ValueImpl.BoolImpl.class);
        if (degraded != null && degraded.isTrue()) {
            seeds.add(mi);
            seeds.addAll(mi.parameters());
        }
        MethodLinkedVariables mlv = mi.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        if (mlv == null) {
            methodsWithoutLinks++;
            taintUnprojected(mi); // no summary at all: no evidence for this method's own nodes (§10.1)
            return;
        }
        if (mi.isAbstract()) {
            // no body, no local evidence: the shallow mlv conservatively claims "modified" where the
            // frozen property holds the precise union-over-implementations value. Seed only from
            // frozen FALSE (= modified) properties — contracts and aggregations we must not lose;
            // analyzed implementations feed the abstract nodes through the E6 edges instead.
            Value.Bool nm = mi.analysis().getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
            if (nm != null && nm.isFalse()) seedWithOrigin(mi, mi, "frozen non-modifying FALSE");
            for (ParameterInfo pi : mi.parameters()) {
                Value.Bool um = pi.analysis().getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
                if (um != null && um.isFalse()) seedWithOrigin(pi, mi, "frozen unmodified FALSE");
            }
            return;
        }
        // seeds: everything the converged analysis believes modified, projected onto nodes
        for (Variable v : mlv.modified()) {
            int before = seeds.size();
            seedVariable(mi, v);
            if (seeds.size() > before) {
                seeds.stream().filter(sd -> !seedOrigin.containsKey(sd))
                        .forEach(sd -> seedOrigin.put(sd, mi.fullyQualifiedName() + " modified " + v));
            }
        }
        // E3: field -> parameter linked to that field
        for (ParameterInfo pi : mi.parameters()) {
            Links links = mlv.ofParameters().get(pi.index());
            links.stream().forEach(link -> link.to().variableStreamDescend().forEach(v -> {
                if (v instanceof FieldReference fr) {
                    addEdge(fr.fieldInfo(), pi);
                }
            }));
        }
        // E1/E2: call sites
        if (mi.methodBody() != null) {
            handleBlock(mi, mi.methodBody());
        }
        seedStatementLevelFieldModifications(mi);
    }

    /**
     * Mirrors FieldAnalyzerImpl.computeUnmodified: modification of a field through a NON-this scope
     * (local.field on a locally created/held object) never enters any method-level summary — the
     * method's own receiver and parameters are not implicated — but the field analyzer reads it from
     * the last statement's VariableData (UNMODIFIED_VARIABLE == FALSE) and decides
     * UNMODIFIED_FIELD = FALSE. Same exclusions: constructors and part-of-construction methods
     * (construction-time writes don't count), the field's own getter/setter, and only fields of this
     * method's own primary type (the field analyzer scans just its primary type's methods).
     */
    private void seedStatementLevelFieldModifications(MethodInfo mi) {
        if (mi.methodBody() == null || mi.methodBody().isEmpty()) return;
        VariableData vd = VariableDataImpl.of(mi.methodBody().lastStatement());
        if (vd == null) return;
        Value.FieldValue getSet = mi.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                ValueImpl.GetSetValueImpl.EMPTY);
        for (VariableInfo vi : vd.variableInfoIterable()) {
            if (!(vi.variable() instanceof FieldReference fr)) continue;
            FieldInfo fieldInfo = fr.fieldInfo();
            if (fieldInfo == getSet.field()) continue;
            if (fieldInfo.owner().primaryType() != mi.typeInfo().primaryType()) continue;
            Value.Bool unmodified = vi.analysis().getOrNull(VariableInfoImpl.UNMODIFIED_VARIABLE,
                    ValueImpl.BoolImpl.class);
            if (unmodified == null || !unmodified.isFalse()) continue;
            if (mi.isConstructor()) continue;
            Value.SetOfInfo poc = fieldInfo.owner().analysis().getOrDefault(PART_OF_CONSTRUCTION,
                    EMPTY_PART_OF_CONSTRUCTION);
            if (poc.infoSet().contains(mi)) continue;
            seedWithOrigin(fieldInfo, mi, "statement-level unmodified FALSE on " + fr);
        }
    }

    private void seedWithOrigin(Object node, MethodInfo mi, String why) {
        if (seeds.add(node)) seedOrigin.put(node, mi.fullyQualifiedName() + " " + why);
    }

    private void seedVariable(MethodInfo mi, Variable v) {
        switch (v) {
            case This thisVar -> {
                if (mi.typeInfo().isEqualToOrInnerClassOf(thisVar.typeInfo())) seeds.add(mi);
            }
            case FieldReference fr -> seedFieldReference(mi, fr);
            case ParameterInfo pi -> seeds.add(pi); // own or another method's: cross-method local evidence
            case DependentVariable dv -> seedVariable(mi, dv.arrayVariable()); // a[i] modified => a modified
            case LocalVariable lv -> {
                // a method's own locals never appear in the mlv summary; a genuine LocalVariable here
                // is a closure-captured variable of an enclosing method, and copyModificationsIntoMethod
                // counts inClosure modification as receiver modification (methodModified = true).
                // The $_fi/$_ce markers are LocalVariables too but deliberately cross the boundary
                // WITHOUT implying receiver modification — skip them (LinkVariable).
                if (!(lv instanceof LinkVariable) && !(lv instanceof ObjectCreationVariable)) {
                    seeds.add(mi);
                }
            }
            default -> {
                // markers, intermediates: no nodes
            }
        }
    }

    private void seedFieldReference(MethodInfo mi, FieldReference fr) {
        seeds.add(fr.fieldInfo());
        if (fr.scopeIsRecursivelyThis()) seeds.add(mi);
        // E5: modification of a component this.m.i implicates the containing field this.m
        if (fr.scopeVariable() instanceof FieldReference outer) seedFieldReference(mi, outer);
    }

    private void handleBlock(MethodInfo mi, Block block) {
        for (Statement statement : block.statements()) {
            statement.subBlockStream().forEach(sb -> handleBlock(mi, sb));
            VariableData vd = VariableDataImpl.of(statement);
            if (vd == null) continue; // synthetic, e.g. implicit super()
            statement.visit(e -> {
                if (e instanceof Block) return false; // nested statements handled with their own vd
                if (e instanceof MethodCall mc) {
                    handleCallSite(mi, vd, mc.methodInfo(), mc.analysis(), mc.parameterExpressions());
                    // E2: callee receiver -> caller-side receiver nodes (chained receivers resolved
                    // through the inner callee's return-value summary, P2.2a)
                    for (Object node : projectReceiverChain(mi, vd, mc.object())) {
                        addEdge(mc.methodInfo(), node);
                    }
                } else if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                    handleCallSite(mi, vd, cc.constructor(), cc.analysis(), cc.parameterExpressions());
                }
                return true;
            });
        }
    }

    private void handleCallSite(MethodInfo mi, VariableData vd, MethodInfo callee, PropertyValueMap analysis,
                                List<org.e2immu.language.cst.api.expression.Expression> argumentExpressions) {
        if (callee == null || callee.parameters().isEmpty()) return;
        LinkComputer.ListOfLinks list = analysis.getOrNull(LinkComputerImpl.LINKED_VARIABLES_ARGUMENTS,
                LinkComputerImpl.ListOfLinksImpl.class);
        if (list == null) {
            callSitesWithoutArgumentLinks++;
            // P2.2b classification: external/abstract callees carry no NEW facts (the engine folds
            // their annotated/unioned argument modification into the caller's own summary, which
            // seeds); a missing-link site at an ANALYZED callee is a genuine E1 coverage hole —
            // pass-discovered parameter modifications cannot propagate to this caller's nodes.
            // The syntactically projectable argument nodes get an incomplete frontier: they must
            // not receive an optimistic TRUE at cutover (§10.1).
            if (callee.methodBody() != null && !callee.methodBody().isEmpty() && !callee.isAbstract()) {
                missingArgLinkAnalyzedCallees.merge(callee.fullyQualifiedName(), 1, Integer::sum);
                for (org.e2immu.language.cst.api.expression.Expression arg : argumentExpressions) {
                    frontierIncomplete.addAll(projectReceiverChain(mi, vd, arg));
                }
            }
            return;
        }
        for (ParameterInfo pi : callee.parameters()) {
            if (pi.index() >= list.list().size()) break; // varargs tail
            Links links = list.list().get(pi.index());
            Set<Object> targets = new LinkedHashSet<>();
            if (links.primary() != null) {
                // the argument OBJECT and its whole-object aliases; links on component faces
                // (oc.field <- ...) must not widen "argument modified" to "field modified"
                targets.addAll(project(mi, vd, links.primary()));
                String primaryFqn = links.primary().fullyQualifiedName();
                links.stream()
                        .filter(l -> l.linkNature().isAssignedFrom()
                                     && Util.firstRealVariable(l.from()).fullyQualifiedName().equals(primaryFqn))
                        .forEach(l -> targets.addAll(project(mi, vd, Util.firstRealVariable(l.to()))));
            } else {
                links.stream().forEach(link -> targets.addAll(project(mi, vd, Util.firstRealVariable(link.to()))));
            }
            for (Object node : targets) {
                addEdge(pi, node); // E1: callee parameter modified => argument's nodes modified
            }
        }
    }

    /**
     * E2 receiver projection including CHAINED receivers (P2.2a — closes the 'unprojected
     * receivers' coverage caveat of PLAN §13). A variable receiver projects directly. A method-call
     * receiver {@code f(...).mutate()} is resolved through the inner callee's converged
     * return-value summary: a fluent/identity return recurses into the inner receiver; a
     * this-scoped field target implicates that field; a parameter target projects the caller-side
     * argument. A constructor receiver {@code new X(a).mutate()} implicates the caller-side
     * arguments its parameters capture into fields (the deep-capture shape). An empty summary that
     * links the result to nothing caller-side (factories, defensive copies) correctly contributes
     * no nodes; only a missing summary counts as unprojected.
     */
    // memoization is load-bearing: chained/nested receivers re-walk shared sub-expressions once
    // per return-value link, which is EXPONENTIAL in nesting depth without a cache (jenkins-core
    // hung >50 min in this recursion, thread-dump-confirmed 2026-07-19). Expressions are shared
    // immutable CST nodes, each belonging to exactly one statement: identity keying is exact.
    private final Map<org.e2immu.language.cst.api.expression.Expression, Set<Object>> receiverChainCache =
            new IdentityHashMap<>();

    private Set<Object> projectReceiverChain(MethodInfo mi, VariableData vd,
                                             org.e2immu.language.cst.api.expression.Expression receiver) {
        if (receiver == null) return Set.of();
        Set<Object> cached = receiverChainCache.get(receiver);
        if (cached != null) return cached;
        Set<Object> result = computeReceiverChain(mi, vd, receiver);
        receiverChainCache.put(receiver, result);
        return result;
    }

    private Set<Object> computeReceiverChain(MethodInfo mi, VariableData vd,
                                             org.e2immu.language.cst.api.expression.Expression receiver) {
        switch (receiver) {
            case null -> {
                return Set.of();
            }
            case VariableExpression ve -> {
                return project(mi, vd, ve.variable());
            }
            case MethodCall mc -> {
                MethodInfo callee = mc.methodInfo();
                MethodLinkedVariables mlv = callee == null ? null
                        : callee.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                        MethodLinkedVariablesImpl.class);
                if (mlv == null) {
                    unprojectedReceivers++;
                    taintUnprojected(mi);
                    return Set.of();
                }
                Set<Object> out = new LinkedHashSet<>();
                mlv.ofReturnValue().stream().forEach(link -> link.to().variableStreamDescend().forEach(v -> {
                    switch (v) {
                        case This thisVar -> {
                            if (callee.typeInfo().isEqualToOrInnerClassOf(thisVar.typeInfo())) {
                                out.addAll(projectReceiverChain(mi, vd, mc.object()));
                            }
                        }
                        case FieldReference fr -> {
                            out.add(fr.fieldInfo());
                            if (fr.scopeIsRecursivelyThis()) {
                                out.addAll(projectReceiverChain(mi, vd, mc.object()));
                            }
                        }
                        case ParameterInfo pi -> {
                            if (pi.methodInfo() == callee && pi.index() < mc.parameterExpressions().size()) {
                                out.addAll(projectReceiverChain(mi, vd, mc.parameterExpressions().get(pi.index())));
                            }
                        }
                        default -> {
                            // markers, locals of the callee: nothing caller-side
                        }
                    }
                }));
                return out;
            }
            case ConstructorCall cc when cc.constructor() != null -> {
                // a fresh object: modification of its graph implicates the arguments its
                // constructor captures into fields (IS_ASSIGNED_TO this-scoped targets)
                MethodInfo ctor = cc.constructor();
                MethodLinkedVariables mlv = ctor.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                        MethodLinkedVariablesImpl.class);
                if (mlv == null) {
                    unprojectedReceivers++;
                    taintUnprojected(mi);
                    return Set.of();
                }
                Set<Object> out = new LinkedHashSet<>();
                for (ParameterInfo pi : ctor.parameters()) {
                    if (pi.index() >= mlv.ofParameters().size()
                        || pi.index() >= cc.parameterExpressions().size()) break;
                    boolean captured = mlv.ofParameters().get(pi.index()).stream()
                            .anyMatch(link -> link.to().variableStreamDescend()
                                    .anyMatch(v -> v instanceof FieldReference fr && fr.scopeIsRecursivelyThis()));
                    if (captured) {
                        out.addAll(projectReceiverChain(mi, vd, cc.parameterExpressions().get(pi.index())));
                    }
                }
                return out;
            }
            default -> {
                // literals, casts of the above, arbitrary expressions: no resolution attempted here;
                // count only genuinely call-shaped receivers we failed on (handled above)
                return Set.of();
            }
        }
    }

    /**
     * Project a caller-side variable onto graph nodes: parameters of the current method, fields
     * (plus containing fields and the receiver for this-scoped references — E4/E5), the receiver for
     * This; locals are followed through the statement's converged links (IS_ASSIGNED_FROM only,
     * element natures off) until they reach projectable variables.
     */
    private Set<Object> project(MethodInfo mi, VariableData vd, Variable start) {
        Set<Object> out = new LinkedHashSet<>();
        Deque<Variable> todo = new ArrayDeque<>();
        Set<String> done = new HashSet<>();
        push(todo, start);
        while (!todo.isEmpty()) {
            Variable v = todo.poll();
            if (v == null || !done.add(v.fullyQualifiedName())) continue;
            switch (v) {
                case This thisVar -> {
                    if (mi.typeInfo().isEqualToOrInnerClassOf(thisVar.typeInfo())) out.add(mi);
                }
                case FieldReference fr -> {
                    out.add(fr.fieldInfo());
                    if (fr.scopeIsRecursivelyThis()) out.add(mi);
                    push(todo, fr.scopeVariable());
                }
                case ParameterInfo pi -> out.add(pi); // own or another method's: node either way
                case DependentVariable dv -> push(todo, dv.arrayVariable());
                default -> {
                    VariableInfoContainer vic = vd.variableInfoContainerOrNull(v.fullyQualifiedName());
                    if (vic != null) {
                        Links links = vic.best().linkedVariables();
                        if (links != null) {
                            if (links.isEmpty() && links.primary() != null) {
                                push(todo, links.primary());
                            } else {
                                // follow only links whose endpoint is exactly this variable: a link on a
                                // COMPONENT face (td.f <- ...) must not widen "td modified" to "td.f modified" —
                                // the engine distinguishes whole-object from per-field modification
                                links.stream().filter(l -> l.linkNature().isAssignedFrom()).forEach(l -> {
                                    Variable from = Util.firstRealVariable(l.from());
                                    Variable to = Util.firstRealVariable(l.to());
                                    if (from.fullyQualifiedName().equals(v.fullyQualifiedName())) push(todo, to);
                                    if (to.fullyQualifiedName().equals(v.fullyQualifiedName())) push(todo, from);
                                });
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private static void push(Deque<Variable> todo, Variable v) {
        if (v != null) todo.add(v);
    }

    /**
     * A receiver chain we could not resolve (missing inner summary): we cannot name the nodes the
     * chain would have implicated, so conservatively give the OBSERVING method's receiver and
     * parameters an incomplete frontier — cheap over-taint at the measured 1-5 occurrences per
     * corpus (post-P2.2a).
     */
    private void taintUnprojected(MethodInfo mi) {
        frontierIncomplete.add(mi);
        frontierIncomplete.addAll(mi.parameters());
    }

    private void addEdge(Object from, Object to) {
        if (from == to) return;
        if (successors.computeIfAbsent(from, k -> new HashSet<>()).add(to)) edgeCount++;
    }

    // ------------------------------------------------------------------ cutover writer (P2.3)

    public record WriteCounts(int downgraded, int decidedFalse, int decidedTrue,
                              int leftUndecided, int reverseKept) {
        public String summary() {
            return "modreach wrote: " + downgraded + " TRUE->FALSE downgrades, "
                   + decidedFalse + " null->FALSE, " + decidedTrue + " null->TRUE, "
                   + leftUndecided + " left undecided (frontier), " + reverseKept + " reverse kept";
        }
    }

    /**
     * PLAN §14 P2.3a: the single-writer cutover. Applies the reachability verdict to the three
     * frozen properties, post-fixpoint:
     * <ul>
     * <li>reached and not currently FALSE: overwrite FALSE — every such write is exactly a
     *     divergence (frozen optimistic TRUE) or an undecided null;</li>
     * <li>unreached and currently null: TRUE if the node's in-edge frontier was constructible
     *     (§10.1), otherwise left undecided (honest null beats optimistic TRUE);</li>
     * <li>unreached and currently FALSE (the reverse-divergence class, zero on all corpora):
     *     kept — the engine's value stands, conservative;</li>
     * <li>immutable-typed parameters are never downgraded (their objects cannot be modified;
     *     mirrors TypeModIndyAnalyzerImpl.handleParameter's immutability shortcut).</li>
     * </ul>
     * The caller must freeze the three properties against later writers (TolerantWrite) —
     * Bool's legal overwrite direction is FALSE->TRUE, so any re-derivation writer could
     * silently undo a downgrade.
     */
    public WriteCounts writeVerdicts(List<Info> analysisOrder, Report report) {
        AnalysisHelper analysisHelper = new AnalysisHelper();
        int downgraded = 0, decidedFalse = 0, decidedTrue = 0, leftUndecided = 0, reverseKept = 0;
        for (Info info : analysisOrder) {
            switch (info) {
                case MethodInfo mi -> {
                    int r = write(mi.analysis(), PropertyImpl.NON_MODIFYING_METHOD,
                            report.reached().contains(mi), report.frontierIncomplete().contains(mi), false, mi);
                    switch (r) {
                        case DOWNGRADED -> downgraded++;
                        case DECIDED_FALSE -> decidedFalse++;
                        case DECIDED_TRUE -> decidedTrue++;
                        case LEFT_UNDECIDED -> leftUndecided++;
                        case REVERSE_KEPT -> reverseKept++;
                        default -> {
                        }
                    }
                    for (ParameterInfo pi : mi.parameters()) {
                        boolean immutable = analysisHelper.typeImmutable(pi.parameterizedType()).isImmutable();
                        int rp = write(pi.analysis(), PropertyImpl.UNMODIFIED_PARAMETER,
                                report.reached().contains(pi), report.frontierIncomplete().contains(pi), immutable, pi);
                        switch (rp) {
                            case DOWNGRADED -> downgraded++;
                            case DECIDED_FALSE -> decidedFalse++;
                            case DECIDED_TRUE -> decidedTrue++;
                            case LEFT_UNDECIDED -> leftUndecided++;
                            case REVERSE_KEPT -> reverseKept++;
                            default -> {
                            }
                        }
                    }
                }
                case FieldInfo fi -> {
                    boolean immutable = analysisHelper.typeImmutable(fi.type()).isImmutable();
                    int rf = write(fi.analysis(), PropertyImpl.UNMODIFIED_FIELD,
                            report.reached().contains(fi), report.frontierIncomplete().contains(fi), immutable, fi);
                    switch (rf) {
                        case DOWNGRADED -> downgraded++;
                        case DECIDED_FALSE -> decidedFalse++;
                        case DECIDED_TRUE -> decidedTrue++;
                        case LEFT_UNDECIDED -> leftUndecided++;
                        case REVERSE_KEPT -> reverseKept++;
                        default -> {
                        }
                    }
                }
                default -> {
                }
            }
        }
        return new WriteCounts(downgraded, decidedFalse, decidedTrue, leftUndecided, reverseKept);
    }

    private static final int NO_CHANGE = 0, DOWNGRADED = 1, DECIDED_FALSE = 2, DECIDED_TRUE = 3,
            LEFT_UNDECIDED = 4, REVERSE_KEPT = 5;

    private int write(PropertyValueMap analysis, org.e2immu.language.cst.api.analysis.Property property,
                      boolean reached, boolean tainted, boolean immutableType, Object element) {
        Value.Bool current = analysis.getOrNull(property, ValueImpl.BoolImpl.class);
        if (reached) {
            if (immutableType) return NO_CHANGE; // an immutable object cannot be modified: union over-reach
            if (current == null) {
                analysis.set(property, ValueImpl.BoolImpl.FALSE);
                return DECIDED_FALSE;
            }
            if (current.isTrue()) {
                analysis.overwrite(property, ValueImpl.BoolImpl.FALSE);
                return DOWNGRADED;
            }
            return NO_CHANGE; // already FALSE: agreement
        }
        if (current == null) {
            if (immutableType || !tainted) {
                analysis.set(property, ValueImpl.BoolImpl.TRUE);
                return DECIDED_TRUE;
            }
            return LEFT_UNDECIDED;
        }
        if (current.isFalse()) {
            // engine knows more than the graph: keep, conservative — but each of these is a
            // shadow-gap of the reverse-divergence family; named for triage (cf. the original 8)
            LOGGER.warn("modreach reverse-kept: {} {}", property.key(), element);
            return REVERSE_KEPT;
        }
        return NO_CHANGE; // TRUE and unreached: agreement
    }

    private Set<Object> closure() {
        Set<Object> reached = new HashSet<>(seeds);
        Deque<Object> todo = new ArrayDeque<>(seeds);
        while (!todo.isEmpty()) {
            Object node = todo.poll();
            for (Object next : successors.getOrDefault(node, Set.of())) {
                if (reached.add(next)) {
                    cause.put(next, node);
                    todo.add(next);
                }
            }
        }
        return reached;
    }
}
