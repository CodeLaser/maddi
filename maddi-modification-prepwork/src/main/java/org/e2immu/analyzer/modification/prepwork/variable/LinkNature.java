package org.e2immu.analyzer.modification.prepwork.variable;

public interface LinkNature {

    boolean important();

    boolean isIdenticalTo();

    boolean multiplySymbols();

    int rank();

    LinkNature replaceSubsetSuperset();

    LinkNature reverse();

    boolean valid();

    LinkNature combine(LinkNature other);

    LinkNature best(LinkNature other);
}
