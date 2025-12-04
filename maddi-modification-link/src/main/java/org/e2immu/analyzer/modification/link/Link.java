package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

public interface Link {
    Variable from();
    LinkNature linkNature();

    Link replaceThis(Runtime runtime, Variable variable, TypeInfo thisType);

    Variable to();
}
