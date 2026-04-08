package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

record Edge(Variable from, LinkNature linkNature, Variable to) {
    @Override
    public @NotNull String toString() {
        return Util.simpleName(from) + " " + linkNature + " " + Util.simpleName(to);
    }
}
