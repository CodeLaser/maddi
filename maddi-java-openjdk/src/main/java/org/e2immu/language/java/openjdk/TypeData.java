package org.e2immu.language.java.openjdk;

import com.sun.tools.javac.code.Symbol;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;

public interface TypeData {

    // extra on top of getType: all types in java.lang, all types in the same package
    TypeInfo getOrAttemptToLoad(String imported);

    TypeInfo getType(String fullyQualifiedName);

    void put(TypeInfo typeInfo);

    void put(String anonymousTypeName, TypeInfo typeInfo);

    void put(Symbol.MethodSymbol methodSymbol, MethodInfo methodInfo);

    MethodInfo getMethod(Symbol.MethodSymbol methodSymbol);

    MethodInfo getOrLoadMethod(Symbol.MethodSymbol methodSymbol);

    void put(Symbol.VarSymbol varSymbol, FieldInfo fieldInfo);

    FieldInfo getField(Symbol.VarSymbol varSymbol);

    FieldInfo getOrLoadField(Symbol.VarSymbol vs);

}
