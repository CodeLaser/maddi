package org.e2immu.analyzer.modification.analyzer.shadow;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.analyzer.modification.prepwork.variable.ObjectCreationVariable;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
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
 * Seeds (P3, "primitive seeding" — see docs/handoff-verification-residue.md §7.5 design A): the
 * converged METHOD_LINKS modified sets seed — EXCEPT, for methods whose body the walk can fully
 * see (no method references, anonymous classes, or local type declarations; lambda bodies ARE
 * walked), the receiver-rooted entries (This, this-scoped FieldReferences, own parameters). Those
 * entries are exactly where the fixpoint's undecided-callee pessimism lives (a recursive pure
 * method marks its own receiver modified on first evaluation and the FALSE is self-consistent
 * forever — TestRecursionThroughAbstract); the pass re-derives them from primitive evidence
 * instead: direct assignments (field rebinding implicates the owner graph, array-element stores
 * implicate the array object), calls to NON-analyzed callees with modifying/@Modified verdicts
 * (jar contracts, the conservative no-information default), undecided abstract callees (the SAM
 * shape — mirrored conservatively at call sites), plus the E1/E2/E6 edges for analyzed callees.
 * Methods the walk cannot
 * fully see keep full summary seeding (the E7 eager creation-site attributions live there).
 * Abstract in-order methods seed from their frozen FALSE only when the E6 edges cannot re-derive
 * it (not all implementations analyzed, or an explicit source @Modified contract). Methods
 * carrying DEGRADED_ANALYSIS_METHOD seed their receiver and all parameters (soundness, plan §9.1).
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
    // P3 discriminator: a callee in the analysis order is re-derived through edges; a callee
    // outside it (jar, shallow, preloaded) is a boundary whose verdict is primitive evidence
    private final Set<MethodInfo> orderMethods = new HashSet<>();
    private int methods, methodsWithoutLinks, edgeCount, callSitesWithoutArgumentLinks, unprojectedReceivers;

    public Report go(List<Info> analysisOrder) {
        List<MethodInfo> methodInfos = analysisOrder.stream()
                .filter(i -> i instanceof MethodInfo).map(i -> (MethodInfo) i).toList();
        orderMethods.addAll(methodInfos);
        List<FieldInfo> fieldInfos = analysisOrder.stream()
                .filter(i -> i instanceof FieldInfo).map(i -> (FieldInfo) i).toList();
        for (MethodInfo mi : methodInfos) {
            buildForMethod(mi);
        }
        if (System.getenv("SHADOW_SEEDS") != null) {
            seeds.forEach(s -> System.out.println("SHADOW SEED " + Report.label(s)
                                                  + " <- " + seedOrigin.getOrDefault(s, "?")));
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
            // frozen property holds the precise union-over-implementations value. P3 refinement, by
            // evidence class: (a) an explicit source @Modified contract is primitive — always seed;
            // (b) an implementation OUTSIDE the analysis order (jar impl, lambda/anonymous impl —
            // never order elements) is a body the graph cannot see — seed from frozen FALSE, and
            // from UNDECIDED too (the engine's call sites treated undecided as modifying);
            // (c) all implementations in-order: the union is fully re-derivable through the E6
            // edges — seeding the frozen FALSE would re-import the fixpoint's recursion pessimism
            // (the TypeInfo.packageName class). No implementations at all: only a frozen FALSE
            // (a materialized contract) seeds; an undecided no-impl abstract stays unseeded, which
            // is what lets the cutover decide it null->TRUE (the Codec class).
            Value.SetOfMethodInfo impls = mi.analysis().getOrDefault(PropertyImpl.IMPLEMENTATIONS,
                    ValueImpl.SetOfMethodInfoImpl.EMPTY);
            boolean anyImpl = !impls.isEmpty();
            // an implementation is E6-re-derivable only when it is in the analysis order AND its
            // overrides() actually carry the edge — a method-reference implementation
            // (this::methodBody registered on a SAM) is in IMPLEMENTATIONS but has no override
            // relation, so its aggregated verdict is invisible to the graph
            boolean outOfOrderImpl = false;
            for (MethodInfo impl : impls.methodInfoSet()) {
                if (!orderMethods.contains(impl) || !impl.overrides().contains(mi)) {
                    outOfOrderImpl = true;
                    break;
                }
            }
            boolean contracted = explicitlyContractedModified(mi);
            Value.Bool nm = mi.analysis().getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
            boolean nmFalse = nm != null && nm.isFalse();
            if (contracted && nmFalse
                || outOfOrderImpl && (nm == null || nmFalse)
                || !anyImpl && nmFalse) {
                seedWithOrigin(mi, mi, "abstract non-modifying " + (nm == null ? "undecided" : "FALSE"));
            }
            for (ParameterInfo pi : mi.parameters()) {
                Value.Bool um = pi.analysis().getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
                boolean umFalse = um != null && um.isFalse();
                if (contracted && umFalse
                    || outOfOrderImpl && (um == null || umFalse)
                    || !anyImpl && umFalse) {
                    seedWithOrigin(pi, mi, "abstract unmodified " + (um == null ? "undecided" : "FALSE"));
                }
            }
            return;
        }
        // P3: a fully-walkable body lets the primitive walk + edges re-derive receiver-rooted
        // modification; only then may the summary's receiver-rooted entries be skipped
        boolean skipReceiverRooted = !bodyIsOpaque(mi);
        // seeds: everything the converged analysis believes modified, projected onto nodes
        for (Variable v : mlv.modified()) {
            int before = seeds.size();
            seedVariable(mi, v, skipReceiverRooted);
            if (seeds.size() > before) {
                seeds.stream().filter(sd -> !seedOrigin.containsKey(sd))
                        .forEach(sd -> seedOrigin.put(sd, mi.fullyQualifiedName() + " modified " + v));
            }
        }
        // E3: field -> parameter linked to that field, with the ENGINE's OWN nature filter
        // (TypeModIndyAnalyzerImpl.relevantLinkForModification): identicalTo with virtual-
        // modification faces, or a non-virtual IS_ASSIGNED_TO. Without the filter, CONTENT-tier
        // links (an element read out of the parameter and added to the field: upper ∋ left ∈
        // this.args) created bogus wake edges — modification of the destination container
        // widened back to the source, precisely what the engine's rule refuses (2026-07-19
        // fernflower cause-chain diagnosis; TestElementFlowWidening pins the precise behavior).
        for (ParameterInfo pi : mi.parameters()) {
            Links links = mlv.ofParameters().get(pi.index());
            for (Link link : links) {
                FieldReference fr = relevantLinkForModification(link);
                if (fr != null && !fr.isIgnoreModifications()) addEdge(fr.fieldInfo(), pi);
            }
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
            if (fr.isIgnoreModifications()) continue; // disclaimed face (engine mirror)
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

    /**
     * P3 opacity: the primitive walk sees plain statements and lambda bodies, but not the deferred
     * or displaced modification channels — method references, anonymous classes, local type
     * declarations. A body containing one keeps full summary seeding (the E7 eager creation-site
     * attributions live in those summaries and only there).
     */
    private boolean bodyIsOpaque(MethodInfo mi) {
        Block body = mi.methodBody();
        if (body == null || body.isEmpty()) return false;
        boolean[] opaque = {false};
        body.visit(e -> {
            if (opaque[0]) return false;
            // only a BOUND method reference (this::m, local::m, field::m) captures a receiver whose
            // deferred modification the walk cannot see; an unbound/static one (Type::m) does not
            if (e instanceof MethodReference mr && !(mr.scope() instanceof org.e2immu.language.cst.api.expression.TypeExpression)
                || e instanceof ConstructorCall cc && cc.anonymousClass() != null
                || e instanceof LocalTypeDeclaration) {
                opaque[0] = true;
                return false;
            }
            return true;
        });
        return opaque[0];
    }

    private static final String MODIFIED_FQN = "org.e2immu.annotation.Modified";

    private static boolean explicitlyContractedModified(MethodInfo mi) {
        return mi.annotations().stream().anyMatch(ShadowModificationPass::isModifiedAnnotation)
               || mi.parameters().stream().anyMatch(pi ->
                pi.annotations().stream().anyMatch(ShadowModificationPass::isModifiedAnnotation));
    }

    private static boolean isModifiedAnnotation(AnnotationExpression ae) {
        return ae.typeInfo() != null && MODIFIED_FQN.equals(ae.typeInfo().fullyQualifiedName());
    }

    private void seedVariable(MethodInfo mi, Variable v, boolean skipReceiverRooted) {
        switch (v) {
            case This thisVar -> {
                // P3: receiver-rooted summary entry of a walkable body — re-derived via primitives + E2
                if (!skipReceiverRooted && mi.typeInfo().isEqualToOrInnerClassOf(thisVar.typeInfo())) {
                    seeds.add(mi);
                }
            }
            case FieldReference fr -> {
                if (!(skipReceiverRooted && fr.scopeIsRecursivelyThis())) seedFieldReference(mi, fr);
            }
            case ParameterInfo pi -> {
                // own parameter of a walkable body: re-derived via E1/boundary seeds/assignment walk;
                // ANOTHER method's parameter is cross-method local evidence with no other channel
                if (!(skipReceiverRooted && pi.methodInfo().equals(mi))) seeds.add(pi);
            }
            case DependentVariable dv -> seedVariable(mi, dv.arrayVariable(), skipReceiverRooted); // a[i] modified => a modified
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
        // mirror MethodModification's Util.variableAndScopes(...).filter(!isIgnoreModifications):
        // modification through a disclaimed face never implicates the field node itself
        if (!fr.isIgnoreModifications()) seeds.add(fr.fieldInfo());
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
                if (e instanceof Lambda lambda) {
                    // P3: lambda bodies are part of the walkable surface (this/captured state inside a
                    // lambda is the enclosing method's — the engine attributes it there eagerly, E7)
                    if (lambda.methodBody() != null) handleBlock(mi, lambda.methodBody());
                    return false;
                }
                if (e instanceof Block) return false; // nested statements handled with their own vd
                if (e instanceof Assignment a && a.variableTarget() != null) {
                    seedAssignmentTarget(mi, vd, a.variableTarget());
                }
                if (e instanceof MethodCall mc) {
                    seedBoundaryCallee(mi, mc.methodInfo());
                    handleCallSite(mi, vd, mc.methodInfo(), mc.analysis(), mc.parameterExpressions());
                    // E2: callee receiver -> caller-side receiver nodes (chained receivers resolved
                    // through the inner callee's return-value summary, P2.2a)
                    for (Object node : projectReceiverChain(mi, vd, mc.object())) {
                        addEdge(mc.methodInfo(), node);
                    }
                } else if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                    seedBoundaryCalleeParameters(mi, cc.constructor());
                    handleCallSite(mi, vd, cc.constructor(), cc.analysis(), cc.parameterExpressions());
                }
                return true;
            });
        }
    }

    /**
     * P3 primitive evidence, boundary calls: a callee OUTSIDE the analysis order (jar, shallow,
     * preloaded) has a verdict the graph cannot re-derive — a contract, an aapi value, or the
     * conservative no-information default. Seeding the callee's own nodes lets the existing E1/E2
     * edges carry the modification to every call site's arguments/receivers, replacing the
     * summary-fold channel the receiver-rooted skip removed.
     */
    private void seedBoundaryCallee(MethodInfo mi, MethodInfo callee) {
        if (callee == null) return;
        if (orderMethods.contains(callee)) {
            // conservative mirror of the engine's undecided-callee default (MethodInfo.isNonModifying:
            // getOrDefault FALSE): an abstract in-order callee whose verdict never decides — a SAM
            // whose only implementations are lambdas, never order elements — was treated as modifying
            // at this very call site; the pass must not read that undecidedness as absence of evidence
            if (callee.isAbstract()) {
                if (!callee.isIgnoreModification()
                    && callee.analysis().getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class) == null) {
                    seedWithOrigin(callee, mi, "undecided abstract callee");
                }
                for (ParameterInfo cpi : callee.parameters()) {
                    if (!cpi.isIgnoreModifications()
                        && cpi.analysis().getOrNull(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class) == null) {
                        seedWithOrigin(cpi, mi, "undecided abstract callee parameter");
                    }
                }
            }
            return;
        }
        if (callee.isModifying() && !callee.isIgnoreModification()) {
            seedWithOrigin(callee, mi, "non-analyzed modifying callee");
        }
        seedBoundaryCalleeParameters(mi, callee);
    }

    private void seedBoundaryCalleeParameters(MethodInfo mi, MethodInfo callee) {
        if (callee == null || orderMethods.contains(callee)) return;
        for (ParameterInfo cpi : callee.parameters()) {
            if (cpi.isModified() && !cpi.isIgnoreModifications()) {
                seedWithOrigin(cpi, mi, "non-analyzed @Modified parameter");
            }
        }
    }

    /**
     * P3 primitive evidence, assignments. Rebinding a field ({@code this.f = x}, {@code p.f = x})
     * modifies the graph of the object HOLDING the field, not the field's own object — so it
     * implicates the receiver/scope nodes, never the field node (fields written only in
     * construction must stay unmodified). An array-element store IS content modification of the
     * array object, construction phase excluded (mirror of seedStatementLevelFieldModifications).
     */
    private void seedAssignmentTarget(MethodInfo mi, VariableData vd, Variable target) {
        switch (target) {
            case FieldReference fr -> {
                if (fr.isIgnoreModifications()) return;
                if (fr.scopeIsRecursivelyThis()) {
                    seedWithOrigin(mi, mi, "assignment to " + fr.fullyQualifiedName());
                } else if (fr.scopeVariable() != null) {
                    for (Object node : project(mi, vd, fr.scopeVariable())) {
                        seedWithOrigin(node, mi, "assignment to " + fr.fullyQualifiedName());
                    }
                }
            }
            case DependentVariable dv -> {
                for (Object node : project(mi, vd, dv.arrayVariable())) {
                    if (node instanceof FieldInfo fi && constructionPhaseWrite(mi, fi)) continue;
                    seedWithOrigin(node, mi, "array-element assignment " + dv.fullyQualifiedName());
                }
            }
            default -> {
                // rebinding a local: no object graph is modified
            }
        }
    }

    private boolean constructionPhaseWrite(MethodInfo mi, FieldInfo fi) {
        if (mi.isConstructor()) return true;
        Value.SetOfInfo poc = fi.owner().analysis().getOrDefault(PART_OF_CONSTRUCTION,
                EMPTY_PART_OF_CONSTRUCTION);
        return poc.infoSet().contains(mi);
    }

    private void handleCallSite(MethodInfo mi, VariableData vd, MethodInfo callee, PropertyValueMap analysis,
                                List<org.e2immu.language.cst.api.expression.Expression> argumentExpressions) {
        if (callee == null || callee.parameters().isEmpty()) return;
        LinkComputer.ListOfLinks list = analysis.getOrNull(LinkComputerImpl.LINKED_VARIABLES_ARGUMENTS,
                LinkComputerImpl.ListOfLinksImpl.class);
        if (list == null) {
            callSitesWithoutArgumentLinks++;
            // P2.2b classification: a missing-link site at an ANALYZED callee is a genuine E1
            // coverage hole — pass-discovered parameter modifications cannot propagate to this
            // caller's nodes. The syntactically projectable argument nodes get an incomplete
            // frontier: they must not receive an optimistic TRUE at cutover (§10.1).
            if (callee.methodBody() != null && !callee.methodBody().isEmpty() && !callee.isAbstract()) {
                missingArgLinkAnalyzedCallees.merge(callee.fullyQualifiedName(), 1, Integer::sum);
                for (org.e2immu.language.cst.api.expression.Expression arg : argumentExpressions) {
                    frontierIncomplete.addAll(projectReceiverChain(mi, vd, arg));
                }
            } else if (!orderMethods.contains(callee)) {
                // P3: the summary fold no longer seeds a walkable caller's own nodes, so a boundary
                // callee's @Modified parameters must reach the syntactically projectable argument
                // nodes directly (the seeded callee-parameter node has no E1 edges here)
                for (ParameterInfo cpi : callee.parameters()) {
                    if (!cpi.isModified() || cpi.isIgnoreModifications()) continue;
                    if (cpi.isVarArgs()) {
                        for (int i = cpi.index(); i < argumentExpressions.size(); i++) {
                            for (Object node : projectReceiverChain(mi, vd, argumentExpressions.get(i))) {
                                addEdge(cpi, node);
                            }
                        }
                    } else if (cpi.index() < argumentExpressions.size()) {
                        for (Object node : projectReceiverChain(mi, vd, argumentExpressions.get(cpi.index()))) {
                            addEdge(cpi, node);
                        }
                    }
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
                            if (!fr.isIgnoreModifications()) out.add(fr.fieldInfo());
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
            // an immutable object cannot be modified: a receiver/argument face of immutable type
            // contributes no nodes and implicates nothing through its scope (the projection-layer
            // mirror of the closure()'s immutable cut and the writer's immutable guard; the engine
            // analogue is TypeModIndyAnalyzerImpl.handleParameter's immutability shortcut)
            if (immutableVariable(v)) continue;
            switch (v) {
                case This thisVar -> {
                    if (mi.typeInfo().isEqualToOrInnerClassOf(thisVar.typeInfo())) out.add(mi);
                }
                case FieldReference fr -> {
                    // engine mirror: a disclaimed (@IgnoreModifications) face never implicates its
                    // field node; the scope chain still projects (Util.variableAndScopes filter)
                    if (!fr.isIgnoreModifications()) out.add(fr.fieldInfo());
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
                              int leftUndecided, int reverseUpgraded, int reverseKept) {
        public String summary() {
            return "modreach wrote: " + downgraded + " TRUE->FALSE downgrades, "
                   + decidedFalse + " null->FALSE, " + decidedTrue + " null->TRUE, "
                   + leftUndecided + " left undecided (frontier), "
                   + reverseUpgraded + " FALSE->TRUE upgrades, " + reverseKept + " reverse kept (tainted)";
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
     * <li>unreached and currently FALSE: upgraded to TRUE when the frontier was constructible —
     *     P3 retires the "reverse divergence = pass bug" doctrine: with primitive seeding, this
     *     class is exactly the fixpoint's undecided-callee recursion pessimism
     *     (TestRecursionThroughAbstract; dogfood 2026-07-22). Tainted nodes keep their FALSE;</li>
     * <li>immutable-typed parameters are never downgraded (their objects cannot be modified;
     *     mirrors TypeModIndyAnalyzerImpl.handleParameter's immutability shortcut).</li>
     * </ul>
     * The caller must freeze the three properties against later writers (TolerantWrite) —
     * Bool's legal overwrite direction is FALSE->TRUE, so any re-derivation writer could
     * silently undo a downgrade.
     */
    public WriteCounts writeVerdicts(List<Info> analysisOrder, Report report) {
        AnalysisHelper analysisHelper = new AnalysisHelper();
        int[] counts = new int[7]; // indexed by the write-result constants
        for (Info info : analysisOrder) {
            switch (info) {
                case MethodInfo mi -> {
                    counts[write(mi.analysis(), PropertyImpl.NON_MODIFYING_METHOD,
                            report.reached().contains(mi), report.frontierIncomplete().contains(mi), false, mi)]++;
                    for (ParameterInfo pi : mi.parameters()) {
                        boolean immutable = analysisHelper.typeImmutable(pi.parameterizedType()).isImmutable();
                        counts[write(pi.analysis(), PropertyImpl.UNMODIFIED_PARAMETER,
                                report.reached().contains(pi), report.frontierIncomplete().contains(pi), immutable, pi)]++;
                    }
                }
                case FieldInfo fi -> {
                    boolean immutable = analysisHelper.typeImmutable(fi.type()).isImmutable();
                    counts[write(fi.analysis(), PropertyImpl.UNMODIFIED_FIELD,
                            report.reached().contains(fi), report.frontierIncomplete().contains(fi), immutable, fi)]++;
                }
                default -> {
                }
            }
        }
        return new WriteCounts(counts[DOWNGRADED], counts[DECIDED_FALSE], counts[DECIDED_TRUE],
                counts[LEFT_UNDECIDED], counts[REVERSE_UPGRADED], counts[REVERSE_KEPT]);
    }

    private static final int NO_CHANGE = 0, DOWNGRADED = 1, DECIDED_FALSE = 2, DECIDED_TRUE = 3,
            LEFT_UNDECIDED = 4, REVERSE_KEPT = 5, REVERSE_UPGRADED = 6;

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
            if (!tainted) {
                // P3: with primitive seeding, complete-frontier unreached is positive evidence of
                // "unmodified" — the same trust the null->TRUE branch above has always exercised.
                // This class is the fixpoint's undecided-callee recursion pessimism.
                analysis.overwrite(property, ValueImpl.BoolImpl.TRUE);
                return REVERSE_UPGRADED;
            }
            LOGGER.warn("modreach reverse-kept (tainted): {} {}", property.key(), element);
            return REVERSE_KEPT;
        }
        return NO_CHANGE; // TRUE and unreached: agreement
    }

    private Set<Object> closure() {
        Set<Object> reached = new HashSet<>(seeds);
        Deque<Object> todo = new ArrayDeque<>(seeds);
        while (!todo.isEmpty()) {
            Object node = todo.poll();
            // an immutable-typed node cannot transmit modification: its object graph cannot change,
            // so any edge out of it is vacuous. Without this cut, chains propagated THROUGH String
            // parameters/fields (the writer's immutable guard stopped the WRITE but not the walk) —
            // fernflower cause-chain diagnosis 2026-07-19.
            if (immutableTyped(node)) continue;
            for (Object next : successors.getOrDefault(node, Set.of())) {
                if (reached.add(next)) {
                    cause.put(next, node);
                    todo.add(next);
                }
            }
        }
        return reached;
    }

    private boolean immutableTyped(Object node) {
        return switch (node) {
            case ParameterInfo pi -> analysisHelper.typeImmutable(pi.parameterizedType()).isImmutable();
            case FieldInfo fi -> analysisHelper.typeImmutable(fi.type()).isImmutable();
            default -> false;
        };
    }

    private boolean immutableVariable(Variable v) {
        return v.parameterizedType() != null
               && analysisHelper.typeImmutable(v.parameterizedType()).isImmutable();
    }

    /**
     * The engine's own filter for "a modified field implicates this parameter"
     * (TypeModIndyAnalyzerImpl.relevantLinkForModification, mirrored verbatim): an identicalTo
     * link between virtual-modification faces, or a non-virtual IS_ASSIGNED_TO onto the field.
     * Content-tier natures (∈ ∋ ~ ...) are excluded — element flow is not modification transfer.
     */
    private static FieldReference relevantLinkForModification(Link link) {
        if (link.linkNature().isIdenticalTo()
            && Util.isVirtualModification(link.from())
            && Util.isVirtualModification(link.to())
            && Util.firstRealVariable(link.to()) instanceof FieldReference fr) return fr;
        if (LinkNatureImpl.IS_ASSIGNED_TO.equals(link.linkNature())
            && !Util.virtual(link.from())
            && !Util.virtual(link.to()) && link.to() instanceof FieldReference fr) return fr;
        return null;
    }
}
