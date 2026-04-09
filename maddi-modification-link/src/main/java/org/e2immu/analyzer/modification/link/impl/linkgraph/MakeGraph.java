package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.NullConstant;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public record MakeGraph(JavaInspector javaInspector, Runtime runtime, Graph graph) {

    private Variable makeComparableSub(Variable base, Variable sub, Variable target) {
        TranslationMap tm = new VariableTranslationMap(runtime).put(base, target);
        Variable translated = tm.translateVariableRecursively(sub);

        // we definitely want to avoid duplicate fields in cyclic class references
        // mapping the field to the owner also avoids sibling-fields, as would occur when we analyze the
        // Eval code in maddi, which refers to Runtime, and RuntimeImpl refers to EvalImpl which then goes to all
        // EvalXX children (we would have runtime.evalOr.runtime.evalAnd.runtime.evalInline and all sorts of
        // permutations.
        return Util.realTypeStream(translated).count() == Util.realTypeStream(translated).distinct().count()
                ? translated : null;
    }

    private @NotNull Map<Variable, Set<Variable>> computeSubs(Set<Variable> modifiedInThisEvaluation) {
        Map<Variable, Set<Variable>> subs = new LinkedHashMap<>();
        for (Map.Entry<Variable, Map<Variable, LinkNature>> entry : graph.edges()) {
            Variable from = entry.getKey();
            Set<Variable> scopeVariablesFrom = Util.scopeVariables(from);
            for (Variable scopeVariableFrom : scopeVariablesFrom) {
                subs.computeIfAbsent(scopeVariableFrom, _ -> new LinkedHashSet<>()).add(from);
            }
            for (Map.Entry<Variable, LinkNature> entry2 : entry.getValue().entrySet()) {
                Variable vTo = entry2.getKey();
                Set<Variable> scopeVariablesTo = Util.scopeVariables(vTo);
                for (Variable scopeVariableTo : scopeVariablesTo) {
                    subs.computeIfAbsent(scopeVariableTo, _ -> new LinkedHashSet<>()).add(vTo);
                }
            }
            if (modifiedInThisEvaluation.contains(from)
                && Util.firstRealVariable(from).equals(from)
                && Util.hasVirtualFields(from)) {
                // FIXME we should add the current type!
                Value.Immutable immutable = new AnalysisHelper().typeImmutable(from.parameterizedType());
                if (immutable.isMutable()) {
                    // add the mutation field
                    FieldInfo vf = new VirtualFieldComputer(javaInspector)
                            .newMField(VariableTranslationMap.owner(runtime, from.parameterizedType()));
                    FieldReference mutationFr = runtime().newFieldReference(vf, runtime.newVariableExpression(from),
                            vf.type());
                    subs.computeIfAbsent(from, _ -> new LinkedHashSet<>()).add(mutationFr);
                }
            }
        }
        return subs;
    }

    // see TestVarious,9; TestVarious2,5
    // must ensure that there are sufficient array capabilities on the target side when the sub is indexing
    private static boolean ensureArraysWhenSubIsIndex(Variable from, Variable sub, Variable target) {
        if (sub.equals(target)) return false;
        if (from instanceof FunctionalInterfaceVariable || target instanceof FunctionalInterfaceVariable) return false;
        if (sub instanceof DependentVariable dv && Util.scopeVariables(dv).contains(from)
            && (!(dv.indexExpression() instanceof IntConstant ic) || ic.constant() >= 0)) {
            return target.parameterizedType().arrays() == from.parameterizedType().arrays();
        }
        // cycle protection, Test1, Test2
        Set<FieldInfo> subFields = Util.fieldsOf(sub).collect(Collectors.toUnmodifiableSet());
        Set<FieldInfo> targetFields = Util.fieldsOf(target).collect(Collectors.toUnmodifiableSet());
        return Collections.disjoint(subFields, targetFields);
    }

    private static boolean isNotNullConstant(Variable v) {
        return !(v instanceof MarkerVariable mv)
               || !mv.isConstant()
               || !(mv.assignmentExpression() instanceof NullConstant);
    }

    boolean doOneMakeGraphCycle(String statementIndex, Set<Variable> modifiedInThisEvaluation) {
        Map<Variable, Set<Variable>> subs = computeSubs(modifiedInThisEvaluation);
        List<Edge> newLinks = new ArrayList<>();
        for (Map.Entry<Variable, Map<Variable, LinkNature>> entry : graph.edges()) {
            Variable vFrom = entry.getKey();
            for (Map.Entry<Variable, LinkNature> entry2 : entry.getValue().entrySet()) {
                Variable vTo = entry2.getKey();
                LinkNature linkNature = entry2.getValue();
                if (linkNature.isIdenticalToOrAssignedFromTo()) {
                    Set<Variable> subsOfFrom = subs.get(vFrom);
                    if (subsOfFrom != null && vTo.equals(Util.firstRealVariable(vTo)) && isNotNullConstant(vTo)) {
                        for (Variable s : subsOfFrom) {
                            if (ensureArraysWhenSubIsIndex(vFrom, s, vTo)) {
                                Variable sub = makeComparableSub(vFrom, s, vTo);
                                if (sub != null) {
                                    assert !sub.equals(s);
                                    LinkNature ln;
                                    if (s instanceof FieldReference fr && Util.isVirtualModificationField(fr.fieldInfo())) {
                                        ln = LinkNatureImpl.makeIdenticalTo(null);
                                    } else {
                                        ln = linkNature;
                                    }
                                    newLinks.add(new Edge(s, ln, sub));
                                }
                            }
                        }
                    }
                    Set<Variable> subsOfTo = subs.get(vTo);
                    if (subsOfTo != null && vFrom.equals(Util.firstRealVariable(vFrom)) && isNotNullConstant(vFrom)) {
                        for (Variable s : subsOfTo) {
                            if (ensureArraysWhenSubIsIndex(vTo, s, vFrom)) {
                                Variable sub = makeComparableSub(vTo, s, vFrom);
                                if (sub != null) {
                                    assert !sub.equals(s);
                                    LinkNature ln;
                                    if (s instanceof FieldReference fr && Util.isVirtualModificationField(fr.fieldInfo())) {
                                        ln = LinkNatureImpl.makeIdenticalTo(null);
                                    } else {
                                        ln = linkNature;
                                    }
                                    newLinks.add(new Edge(sub, ln, s));
                                }
                            }
                        }
                    }
                }
            }
            if (Util.isSlice(vFrom)) {
                // see TestConsumers,2; necessary to make the connection between filtered and streamed
                newLinks.add(new Edge(vFrom, LinkNatureImpl.IS_IN_OBJECT_GRAPH,
                        ((DependentVariable) vFrom).arrayVariable()));
            }
        }
        boolean change = false;
        for (Edge edge : newLinks) {
            change |= graph.mergeEdgeBi(edge.from(), edge.linkNature(), edge.to(), statementIndex);
        }
        List<Edge> extra = new ExpandSlice(graph).completeSliceInformation();
        for (Edge edge : extra) {
            change |= graph.simpleAddToGraph(edge.from(), edge.linkNature(), edge.to(), statementIndex);
        }
        return change;
    }
}
