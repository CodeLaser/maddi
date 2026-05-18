package org.e2immu.language.java.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.jetbrains.annotations.Nullable;

public interface ConvertType {
    void startCompilationUnit(SourceProvider sourceProvider, ElementStack elementStack);

    // general method, returns null when 'cs' is not a functional type
    @Nullable MethodInfo computeSAM(Type type);

    FieldInfo ensureField(Symbol.VarSymbol vs);

    MethodInfo ensureMethod(Symbol.MethodSymbol methodSymbol);

    record SAMDescriptor(MethodInfo methodInfo, Symbol.MethodSymbol symbol, Type.MethodType instantiatedType) {
    }

    SAMDescriptor findInstantiatedSAM(Type functionalType);

    ParameterizedType convert(Type type);

    ParameterizedType convertTree(Tree type, DetailedSources.Builder detailedSourcesBuilder);

}
