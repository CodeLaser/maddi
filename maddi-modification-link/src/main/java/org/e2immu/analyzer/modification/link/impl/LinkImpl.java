package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

public record LinkImpl(Variable from, LinkNature linkNature, Variable to) implements Link {
    @Override
    public @NotNull String toString() {
        return from.toString() + linkNature + to;
    }
}
