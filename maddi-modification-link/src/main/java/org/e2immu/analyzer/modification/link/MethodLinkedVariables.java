package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.analysis.Value;

import java.util.List;

public interface MethodLinkedVariables extends Value {
    Link ofReturnValue();

    List<Link> ofParameters();
}
