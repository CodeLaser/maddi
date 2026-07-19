package org.e2immu.analyzer.modification.analyzer.shadow;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
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
                         int callSitesWithoutArgumentLinks, int unprojectedReceivers) {

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
            return "shadow: " + methods + " methods (" + methodsWithoutLinks + " without links), "
                   + seeds + " seeds, " + edgeCount + " edges, " + reached.size() + " reached; "
                   + divergences.size() + " divergences, " + reverseDivergences.size() + " REVERSE divergences; "
                   + callSitesWithoutArgumentLinks + " call sites without argument links, "
                   + unprojectedReceivers + " unprojected receivers";
        }

        public List<String> sortedDivergenceStrings() {
            return divergences.stream().map(Divergence::toString).sorted().toList();
        }
    }

    private final Map<Object, Set<Object>> successors = new HashMap<>();
    private final Set<Object> seeds = new HashSet<>();
    private final Map<Object, String> seedOrigin = new HashMap<>();
    private final Map<Object, Object> cause = new HashMap<>();
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
                callSitesWithoutArgumentLinks, unprojectedReceivers);
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
                    handleCallSite(mi, vd, mc.methodInfo(), mc.analysis());
                    if (mc.object() instanceof VariableExpression ve) {
                        // E2: callee receiver -> caller-side receiver nodes
                        for (Object node : project(mi, vd, ve.variable())) {
                            addEdge(mc.methodInfo(), node);
                        }
                    } else if (mc.object() instanceof MethodCall || mc.object() instanceof ConstructorCall) {
                        unprojectedReceivers++;
                    }
                } else if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                    handleCallSite(mi, vd, cc.constructor(), cc.analysis());
                }
                return true;
            });
        }
    }

    private void handleCallSite(MethodInfo mi, VariableData vd, MethodInfo callee, PropertyValueMap analysis) {
        if (callee == null || callee.parameters().isEmpty()) return;
        LinkComputer.ListOfLinks list = analysis.getOrNull(LinkComputerImpl.LINKED_VARIABLES_ARGUMENTS,
                LinkComputerImpl.ListOfLinksImpl.class);
        if (list == null) {
            callSitesWithoutArgumentLinks++;
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

    private void addEdge(Object from, Object to) {
        if (from == to) return;
        if (successors.computeIfAbsent(from, k -> new HashSet<>()).add(to)) edgeCount++;
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
