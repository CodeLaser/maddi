package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandSlice {

    private record F2(FieldInfo kv, FieldInfo k) {
    }

    /*
    if entry.§kv.§k ∈ map.§vks[-2].§k and entry.§kv.§v ∈ map.§vks[-1].§v then entry ∈ map.§vks

    if 0:map.§$$s[-1].§$~this.map.§$$s[-2].§$ and 0:map.§$$s[-2].§$~this.map.§$$s[-1].§$] then 0:map.§$$s ~ this.map.§$$s
     */
    List<Expand.PC> completeSliceInformation(Map<Expand.V, Map<Expand.V, LinkNature>> graph) {
        Map<Expand.PC, List<F2>> map = new HashMap<>();
        for (Map.Entry<Expand.V, Map<Expand.V, LinkNature>> entry : graph.entrySet()) {
            if (entry.getKey().v() instanceof FieldReference frK && virtual(frK.fieldInfo())
                && frK.scopeVariable() instanceof FieldReference frKv && virtual(frKv.fieldInfo())) {
                for (Map.Entry<Expand.V, LinkNature> entry2 : entry.getValue().entrySet()) {
                    if (LinkNatureImpl.IS_ELEMENT_OF.equals(entry2.getValue())
                        && entry2.getKey().v() instanceof FieldReference fr2K && virtual(fr2K.fieldInfo())
                        && fr2K.scopeVariable() instanceof DependentVariable dv && negative(dv.indexExpression())
                        && dv.arrayVariable() instanceof FieldReference fr2Vks && virtual(fr2Vks.fieldInfo())) {
                        Expand.PC pc = new Expand.PC(frKv.scopeVariable(), LinkNatureImpl.IS_ELEMENT_OF, fr2Vks);
                        map.computeIfAbsent(pc, _ -> new ArrayList<>())
                                .add(new F2(frKv.fieldInfo(), frK.fieldInfo()));
                    }
                }
            }
            if (entry.getKey().v() instanceof FieldReference frK && virtual(frK.fieldInfo())
                && frK.scopeVariable() instanceof DependentVariable dvK && negative(dvK.indexExpression())
                && dvK.arrayVariable() instanceof FieldReference frKv && virtual(frKv.fieldInfo())) {
                for (Map.Entry<Expand.V, LinkNature> entry2 : entry.getValue().entrySet()) {
                    if (LinkNatureImpl.SHARES_ELEMENTS.equals(entry2.getValue())
                        && entry2.getKey().v() instanceof FieldReference fr2K && virtual(fr2K.fieldInfo())
                        && fr2K.scopeVariable() instanceof DependentVariable dv && negative(dv.indexExpression())
                        && dv.arrayVariable() instanceof FieldReference fr2Vks && virtual(fr2Vks.fieldInfo())) {
                        Expand.PC pc = new Expand.PC(frKv, LinkNatureImpl.SHARES_ELEMENTS, fr2Vks);
                        map.computeIfAbsent(pc, _ -> new ArrayList<>())
                                .add(new F2(frKv.fieldInfo(), frK.fieldInfo()));
                    }
                }
            }
        }
        return map.entrySet().stream()
                .filter(e -> complete(e.getValue()))
                .map(Map.Entry::getKey).toList();
    }

    private static boolean complete(List<F2> fields) {
        assert !fields.isEmpty();
        FieldInfo kv = fields.getFirst().kv;
        if (fields.stream().skip(1).anyMatch(f2 -> !f2.kv.equals(kv))) return false;
        // FIXME now how do we know that fields.k are the constituent parts of kv? do some legwork
        // FIXME complete for the 2nd one is more complicated
        return true;
    }

    private static boolean negative(Expression expression) {
        return expression.isNumeric() && expression.numericValue() < 0;
    }

    private static boolean virtual(FieldInfo fieldInfo) {
        return fieldInfo.name().startsWith("§");
    }

}
