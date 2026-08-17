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

}
