package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.function.Function;

public record ConvertType(Runtime runtime,
                          ClassSymbolScanner classSymbolScanner,
                          TypeData typeData,
                          Function<String, Element> findInElementStack) {

    ParameterizedType convert(Type type) {
        if (type instanceof Type.JCPrimitiveType primitiveType) {
            return primitiveType(primitiveType.getKind());
        }
        if (type instanceof Type.ClassType ct) {
            return classType(ct);
        }
        if (type instanceof Type.JCVoidType) {
            return runtime.voidParameterizedType();
        }
        if (type instanceof Type.ArrayType at) {
            ParameterizedType base = convert(at.elemtype);
            return runtime.newParameterizedType(base.typeInfo(), 1);
        }
        throw new UnsupportedOperationException("NYI");
    }

    ParameterizedType convert(Tree type) {
        if (type == null) return runtime.voidParameterizedType();
        if (type instanceof JCTree.JCPrimitiveTypeTree ptt) {
            TypeKind primitiveTypeKind = ptt.typetag.getPrimitiveTypeKind();
            if (primitiveTypeKind != null) {
                return primitiveType(primitiveTypeKind);
            }
        }
        if (type instanceof JCTree.JCIdent identifier) {
            if (identifier.type instanceof Type.ClassType ct) {
                return classType(ct);
            } else if (identifier.type instanceof Type.TypeVar) {
                String typeParameterName = identifier.getName().toString();
                TypeParameter tp = (TypeParameter) findInElementStack.apply(typeParameterName);
                return runtime.newParameterizedType(tp, 0, null);
            } else {
                throw new UnsupportedOperationException("NYI");
            }
        }
        if (type instanceof JCTree.JCTypeApply apply) {
            ParameterizedType base = convert(apply.getType());
            List<ParameterizedType> parameters = apply.getTypeArguments().stream().map(this::convert).toList();
            return runtime.newParameterizedType(base.typeInfo(), parameters);
        }
        throw new UnsupportedOperationException("NYI");
    }

    private ParameterizedType classType(Type.ClassType ct) {
        String fullyQualifiedType = ct.toString();
        TypeInfo known = typeData.getType(fullyQualifiedType);
        TypeInfo typeInfo;
        if (known == null) {
            // on-demand loading; should be replaced by import handling?
            if (ct.tsym instanceof Symbol.ClassSymbol cs) {
                typeInfo = classSymbolScanner.typeInfo(cs);
                typeData.put(typeInfo);
            } else throw new UnsupportedOperationException("NYI");
        } else {
            typeInfo = known;
        }
        if (ct.getTypeArguments().isEmpty()) {
            return typeInfo.asSimpleParameterizedType();
        }
        List<ParameterizedType> typeParameters = ct.getTypeArguments().stream().map(this::convert).toList();
        return runtime.newParameterizedType(typeInfo, typeParameters);
    }

    private ParameterizedType primitiveType(TypeKind primitiveTypeKind) {
        return switch (primitiveTypeKind) {
            case VOID -> runtime.voidParameterizedType();
            case BYTE -> runtime.byteParameterizedType();
            case INT -> runtime.intParameterizedType();
            case DOUBLE -> runtime.doubleParameterizedType();
            case LONG -> runtime.longParameterizedType();
            case FLOAT -> runtime.floatParameterizedType();
            case SHORT -> runtime.shortParameterizedType();
            case BOOLEAN -> runtime.booleanParameterizedType();
            case CHAR -> runtime.charParameterizedType();
            default -> throw new UnsupportedOperationException();
        };
    }

}
