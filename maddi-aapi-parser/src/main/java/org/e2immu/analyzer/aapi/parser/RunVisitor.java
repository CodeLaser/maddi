package org.e2immu.analyzer.aapi.parser;

import org.e2immu.analyzer.modification.common.defaults.DebugVisitor;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.integration.ToolChain;

import java.util.Set;

public interface RunVisitor {

    default void afterAnnotatedApiParsing(JavaInspector javaInspector) {
    }

    default void setContext(String libIn, String libOut, ToolChain.JRE jre) {
    }

    default DebugVisitor debugVisitor() {
        return null;
    }

    default void writeAnalysis(Set<TypeInfo> writeOut) {
    }
}
