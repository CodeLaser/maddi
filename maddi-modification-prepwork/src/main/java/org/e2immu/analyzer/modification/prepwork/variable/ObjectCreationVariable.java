package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.variable.Variable;

public interface ObjectCreationVariable extends Variable {

    MethodInfo methodInfo();
}
