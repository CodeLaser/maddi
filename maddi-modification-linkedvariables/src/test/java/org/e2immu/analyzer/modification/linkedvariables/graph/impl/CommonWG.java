package org.e2immu.analyzer.modification.linkedvariables.graph.impl;


import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.delay.CausesOfDelay;
import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl.*;

public class CommonWG {
    final Runtime runtime = new RuntimeImpl();
    final LV v0 = LINK_STATICALLY_ASSIGNED;
    final LV v1 = LINK_ASSIGNED;
    final LV v2 = LINK_DEPENDENT;
    final LV v4 = LVImpl.createHC(new LinksImpl(0, 0, true));
    final LV delay = LVImpl.delay(CausesOfDelay.DELAY);
    final Indices i0 = new IndicesImpl(0);
    final Indices i1 = new IndicesImpl(1);
    final Indices i2 = new IndicesImpl(2);

    protected Variable makeVariable(String name) {
        CompilationUnit compilationUnit = runtime.newCompilationUnitBuilder().setPackageName("a.b.c").build();
        TypeInfo t = runtime.newTypeInfo(compilationUnit, "T");
        return runtime.newLocalVariable(name, runtime.newParameterizedType(t, 0));
    }
}
