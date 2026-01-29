package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.info.MethodInfo;

import java.util.List;
import java.util.Set;

public interface LinkNature {

    boolean isAssignedFrom();

    boolean isDecoration();

    boolean isIdenticalToOrAssignedFromTo();

    boolean isIdenticalTo();

    boolean multiplySymbols();

    int rank();

    List<LinkNature> redundantFromUp();

    List<LinkNature> redundantToUp();

    List<LinkNature> redundantUp();

    LinkNature replaceSubsetSuperset();

    LinkNature reverse();

    boolean valid();

    LinkNature combine(LinkNature other, Set<MethodInfo> block);

    LinkNature best(LinkNature other);

    Set<MethodInfo> pass();
}
