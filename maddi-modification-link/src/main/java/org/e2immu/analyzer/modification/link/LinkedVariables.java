package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;

// Primarily a helper type to carry around multiple Links objects. Not a "deliverable".
public interface LinkedVariables extends Iterable<Map.Entry<Variable, Links>> {

    boolean isEmpty();

    Map<Variable, Links> map();

    LinkedVariables merge(LinkedVariables other);
}
