package org.e2immu.analyzer.modification.prepwork.variable;

import java.util.List;

public interface LinkNature {

    boolean isIdenticalTo();

    boolean multiplySymbols();

    int rank();

    List<LinkNature> redundantFromUp();

    List<LinkNature> redundantToUp();

    List<LinkNature> redundantUp();

    LinkNature replaceSubsetSuperset();

    LinkNature reverse();

    boolean valid();

    LinkNature combine(LinkNature other);

    LinkNature best(LinkNature other);
}
