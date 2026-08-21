package io.codelaser.maddi.java.openjdk;

import com.sun.tools.javac.code.Symbol;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;

public interface TypeData {

    TypeInfo getType(String fullyQualifiedName);

    void put(TypeInfo typeInfo);

    void put(String anonymousTypeName, TypeInfo typeInfo);

    void put(Symbol.MethodSymbol methodSymbol, MethodInfo methodInfo);

    MethodInfo getMethod(Symbol.MethodSymbol methodSymbol);

    MethodInfo getOrLoadMethod(Symbol.MethodSymbol methodSymbol);

    void put(Symbol.VarSymbol varSymbol, FieldInfo fieldInfo);

    FieldInfo getField(Symbol.VarSymbol varSymbol);

    FieldInfo getOrLoadField(Symbol.VarSymbol vs);

    /**
     * Claim a type's "setup block" (access, type parameters, parent class, interfaces, annotations) for the caller,
     * returning {@code true} the first time and {@code false} on every later attempt. Backed by the shared
     * {@code InfoByFqn} registry, so the claim outlives any one scanner instance.
     * <p>
     * The source scan claims what it is about to build, so a class-file load of the same {@code TypeInfo} — before
     * or after, in this parse or the next — cannot build it a second time. See
     * {@code ScanCompilationUnit#continueType} and {@code ClassSymbolScanner#loadType}.
     */
    boolean markClassScannerSetupDone(TypeInfo typeInfo);

}
