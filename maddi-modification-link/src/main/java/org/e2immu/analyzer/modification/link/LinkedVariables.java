package org.e2immu.analyzer.modification.link;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;

public interface LinkedVariables extends Iterable<Map.Entry<Variable, Link>>, Value {

    boolean isEmpty();

    int size();

    LinkedVariables merge(LinkedVariables other);
}
