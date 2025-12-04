package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.language.cst.api.variable.Variable;

public record LinkImpl(Variable from, LinkNature linkNature, Variable to) implements Link {
}
