package org.e2immu.analyzer.modification.link;

import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

public interface LinkComputer {
    // tests only!
    void doPrimaryType(TypeInfo primaryType);

    Links doField(FieldInfo fieldInfo);

    MethodLinkedVariables doMethod(MethodInfo methodInfo);

    record Options(boolean recurse, boolean forceShallow, boolean checkDuplicateNames) {
        public static final Options TEST = new Options(true, false, true);
        public static final Options PRODUCTION = new Options(true, false, false);
    }
}
