package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.Util.virtual;

public class ExpandSlice {

    private record F2(FieldInfo kv, FieldInfo k) {
    }

    /*
    typeLink/TestMap,2
    if entry.§kv.§k ∈ map.§vks[-2] and entry.§kv.§v ∈ map.§vks[-1] then entry ∈ map.§vks

    FIXME isIdenticalTo example?

    TestForEachLambda,7
    if 0:map.§$$s[-1]~this.map.§$$s[-2] and 0:map.§$$s[-2]~this.map.§$$s[-1]] then 0:map.§$$s ~ this.map.§$$s
     */
    List<Expand.PC> completeSliceInformation(Map<Expand.V, Map<Expand.V, LinkNature>> graph) {
        Map<Expand.PC, List<List<F2>>> map = new HashMap<>();
        for (Map.Entry<Expand.V, Map<Expand.V, LinkNature>> entry : graph.entrySet()) {
            if (entry.getKey().v() instanceof FieldReference frK && virtual(frK)
                && frK.scopeVariable() instanceof FieldReference frKv && virtual(frKv)) {
                for (Map.Entry<Expand.V, LinkNature> entry2 : entry.getValue().entrySet()) {
                    if (LinkNatureImpl.IS_ELEMENT_OF.equals(entry2.getValue())
                        && entry2.getKey().v() instanceof DependentVariable dv
                        && negative(dv.indexExpression()) >= 0
                        && dv.arrayVariable() instanceof FieldReference fr2Vks && virtual(fr2Vks)) {
                        Expand.PC pc = new Expand.PC(frKv.scopeVariable(), LinkNatureImpl.IS_ELEMENT_OF, fr2Vks);
                        List<List<F2>> lists = map.computeIfAbsent(pc, _ -> new ArrayList<>());
                        if (lists.isEmpty()) lists.add(new ArrayList<>());
                        lists.getFirst().add(new F2(frKv.fieldInfo(), frK.fieldInfo()));
                    }
                    if (entry2.getValue().isIdenticalTo()
                        && entry2.getKey().v() instanceof FieldReference fr2K && virtual(fr2K)
                        && fr2K.scopeVariable() instanceof FieldReference fr2kv && virtual(fr2kv)) {
                        if (frKv.compareTo(fr2kv) < 0) {
                            Expand.PC pc = new Expand.PC(frKv, LinkNatureImpl.SHARES_ELEMENTS, fr2kv);
                            List<List<F2>> lists = map.computeIfAbsent(pc, _ -> new ArrayList<>());
                            if (lists.isEmpty()) {
                                lists.add(new ArrayList<>());
                                lists.add(new ArrayList<>());
                            }
                            lists.getFirst().add(new F2(frKv.fieldInfo(), frK.fieldInfo()));
                            lists.getLast().add(new F2(fr2kv.fieldInfo(), fr2K.fieldInfo()));
                        } // do only one direction
                    }
                }
            }
            int index;
            if (entry.getKey().v() instanceof DependentVariable dvK && (index = negative(dvK.indexExpression())) >= 0) {
                for (Map.Entry<Expand.V, LinkNature> entry2 : entry.getValue().entrySet()) {
                    int index1;
                    if ((LinkNatureImpl.SHARES_ELEMENTS.equals(entry2.getValue()) || entry2.getValue().isIdenticalTo())
                        && entry2.getKey().v() instanceof DependentVariable dv
                        && (index1 = negative(dv.indexExpression())) >= 0) {
                        if (dvK.indexExpression().compareTo(dv.indexExpression()) < 0) {
                            // record only in one direction
                            Variable frKv = dvK.arrayVariable();
                            Variable fr2Vks = dv.arrayVariable();

                            Expand.PC pc = new Expand.PC(frKv, LinkNatureImpl.SHARES_ELEMENTS, fr2Vks);
                            List<List<F2>> lists = map.computeIfAbsent(pc, _ -> new ArrayList<>());
                            if (lists.isEmpty()) {
                                lists.add(new ArrayList<>());
                                lists.add(new ArrayList<>());
                            }
                            FieldInfo frKvFieldInfo = frKv.parameterizedType().typeInfo().fields().get(index);
                            FieldInfo frKFieldInfo = ((FieldReference) dvK.arrayVariable()).fieldInfo();
                            FieldInfo fr2VksFieldInfo = fr2Vks.parameterizedType().typeInfo().fields().get(index1);
                            lists.getFirst().add(new F2(frKFieldInfo, frKvFieldInfo));
                            FieldInfo fr2KFieldInfo = ((FieldReference) dv.arrayVariable()).fieldInfo();
                            lists.getLast().add(new F2(fr2KFieldInfo, fr2VksFieldInfo));
                        }
                    }
                }
            }
        }
        return map.entrySet().stream()
                .filter(e -> e.getValue().stream().allMatch(ExpandSlice::complete))
                .map(Map.Entry::getKey).toList();
    }

    private static boolean complete(List<F2> fields) {
        assert !fields.isEmpty();
        FieldInfo kv = fields.getFirst().kv;
        if (fields.stream().skip(1).anyMatch(f2 -> !f2.kv.equals(kv))) return false;
        if (kv.type().typeInfo() != null && VirtualFieldComputer.VIRTUAL_FIELD == kv.type().typeInfo().typeNature()) {
            Set<ParameterizedType> subs = kv.type().typeInfo().fields().stream()
                    .map(FieldInfo::type)
                    .collect(Collectors.toUnmodifiableSet());
            Set<ParameterizedType> concrete = fields.stream()
                    .map(f2 -> f2.k.type()).collect(Collectors.toUnmodifiableSet());
            return kv.type().typeInfo().fields().size() == fields.size() && subs.equals(concrete);
        }
        throw new UnsupportedOperationException("NYI");
    }

    private static int negative(Expression expression) {
        if (expression.isNumeric()) {
            Double d = expression.numericValue();
            if (d != null) {
                return -(int) (double) d - 1;
            }
        }
        return -1;
    }
}
