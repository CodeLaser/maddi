package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.Util.virtual;

public class ExpandSlice {

    private record F2(FieldInfo kv, FieldInfo k) {
    }

    /*
    if entry.§kv.§k ∈ map.§vks[-2].§k and entry.§kv.§v ∈ map.§vks[-1].§v then entry ∈ map.§vks

    if 0:map.§$$s[-1].§$~this.map.§$$s[-2].§$ and 0:map.§$$s[-2].§$~this.map.§$$s[-1].§$] then 0:map.§$$s ~ this.map.§$$s
     */
    List<Expand.PC> completeSliceInformation(Map<Expand.V, Map<Expand.V, LinkNature>> graph) {
        Map<Expand.PC, List<List<F2>>> map = new HashMap<>();
        for (Map.Entry<Expand.V, Map<Expand.V, LinkNature>> entry : graph.entrySet()) {
            if (entry.getKey().v() instanceof FieldReference frK && virtual(frK)
                && frK.scopeVariable() instanceof FieldReference frKv && virtual(frKv)) {
                for (Map.Entry<Expand.V, LinkNature> entry2 : entry.getValue().entrySet()) {
                    if (LinkNatureImpl.IS_ELEMENT_OF.equals(entry2.getValue())
                        && entry2.getKey().v() instanceof FieldReference fr2K && virtual(fr2K)
                        && fr2K.scopeVariable() instanceof DependentVariable dv && negative(dv.indexExpression())
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
            if (entry.getKey().v() instanceof FieldReference frK && virtual(frK)
                && frK.scopeVariable() instanceof DependentVariable dvK && negative(dvK.indexExpression())
                && dvK.arrayVariable() instanceof FieldReference frKv && virtual(frKv)) {
                for (Map.Entry<Expand.V, LinkNature> entry2 : entry.getValue().entrySet()) {
                    if ((LinkNatureImpl.SHARES_ELEMENTS.equals(entry2.getValue()) || entry2.getValue().isIdenticalTo())
                        && entry2.getKey().v() instanceof FieldReference fr2K && virtual(fr2K)
                        && fr2K.scopeVariable() instanceof DependentVariable dv && negative(dv.indexExpression())
                        && dv.arrayVariable() instanceof FieldReference fr2Vks && virtual(fr2Vks)) {
                        if (frKv.compareTo(fr2Vks) < 0) {
                            // record only in one direction
                            Expand.PC pc = new Expand.PC(frKv, LinkNatureImpl.SHARES_ELEMENTS, fr2Vks);
                            List<List<F2>> lists = map.computeIfAbsent(pc, _ -> new ArrayList<>());
                            if (lists.isEmpty()) {
                                lists.add(new ArrayList<>());
                                lists.add(new ArrayList<>());
                            }
                            lists.getFirst().add(new F2(frKv.fieldInfo(), frK.fieldInfo()));
                            lists.getLast().add(new F2(fr2Vks.fieldInfo(), fr2K.fieldInfo()));
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
            Set<FieldInfo> subs = kv.type().typeInfo().fields().stream().collect(Collectors.toUnmodifiableSet());
            Set<FieldInfo> concrete = fields.stream().map(F2::k).collect(Collectors.toUnmodifiableSet());
       //     return subs.equals(concrete);
        }
        // FIXME other conditions if not virtual field?
        return true;
    }

    private static boolean negative(Expression expression) {
        return expression.isNumeric() && expression.numericValue() < 0;
    }
}
