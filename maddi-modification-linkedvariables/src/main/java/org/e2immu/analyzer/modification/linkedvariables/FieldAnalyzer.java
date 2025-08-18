package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.annotation.Modified;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;

import java.util.Set;

/*
Level 2:

is a field @Modified?

- we need to know the @Modified value for the field in each method of the primary type

Is a field @Independent?

- we need to know if the field links to any of the parameters of each method in the primary type.
- so we compute this linking first, then write the @Independent property if all data is present

Independence and modification of parameters is directly influenced by the values computed here,
but the actual computation for parameters is done in the next phase.

This analyzer does not concern itself with solving internal cycles.
It writes out results, if any, in the field's analysis() object.
 */
public interface FieldAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {
        Set<Info> waitFor();
    }

    Output go(@Modified FieldInfo fieldInfo, boolean cycleBreakingActive);
}
