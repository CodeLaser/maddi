package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;

import java.util.List;

public interface InternalMethodModAnalyzer {
    // for recursions

    VariableData doBlock(MethodInfo methodInfo,
                         Block block,
                         VariableData vdOfParent);

    VariableData doStatement(MethodInfo methodInfo,
                             Statement statement,
                             VariableData previous,
                             boolean first);

    boolean trackObjectCreations();
}
