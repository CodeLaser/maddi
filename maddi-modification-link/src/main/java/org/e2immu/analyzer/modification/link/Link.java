package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.variable.Variable;

public interface Link {
    Variable from();
    LinkNature linkNature();
    Variable to();
}
