package io.codelaser.maddi.java.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;

public interface ConvertType {
    // must be set before scanning of compilation unit starts
    void setTopLevelClassSymbolsOfSources(IdentityHashMap<Symbol.ClassSymbol, Boolean> topLevelClassSymbolsOfSources);

    // must be set as soon as the element stack is known
    void startCompilationUnit(SourceProvider sourceProvider, ElementStack elementStack);

    // general method, returns null when 'cs' is not a functional type
    @Nullable MethodInfo computeSAM(Type type);

    FieldInfo ensureField(Symbol.VarSymbol vs);

    MethodInfo ensureMethod(Symbol.MethodSymbol methodSymbol, boolean synthetic);

    record SAMDescriptor(MethodInfo methodInfo, Symbol.MethodSymbol symbol, Type.MethodType instantiatedType) {
    }

    SAMDescriptor findInstantiatedSAM(Type functionalType);

    ParameterizedType convert(Type type);

    ParameterizedType convertTree(Tree type, DetailedSources.Builder detailedSourcesBuilder);

}
