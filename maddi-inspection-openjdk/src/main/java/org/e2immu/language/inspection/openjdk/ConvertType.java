package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.function.Function;

public class ConvertType {
    private final Runtime runtime;
    private final ClassSymbolScanner classSymbolScanner;
    private final TypeData typeData;
    private final Function<String, Element> findInElementStack;

    public ConvertType(Runtime runtime,
                       ClassSymbolScanner classSymbolScanner,
                       TypeData typeData,
                       Function<String, Element> findInElementStack) {
        this.runtime = runtime;
        this.classSymbolScanner = classSymbolScanner;
        this.typeData = typeData;
        this.findInElementStack = findInElementStack;
    }


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
            return base.isTypeParameter()
                    ? runtime.newParameterizedType(base.typeParameter(), base.arrays() + 1, null)
                    : runtime.newParameterizedType(base.typeInfo(), base.arrays() + 1, null, List.of());
        }
        if (type instanceof Type.WildcardType wildcardType) {
            if (wildcardType.isUnbound()) {
                return runtime.parameterizedTypeWildcard();
            }
            boolean isExtends = wildcardType.isExtendsBound();
            Wildcard wildCard = isExtends ? runtime.wildcardExtends() : runtime.wildcardSuper();
            ParameterizedType base = convert(wildcardType.type);
            if (base.isTypeParameter()) {
                return runtime.newParameterizedType(base.typeParameter(), 0, wildCard);
            }
            return runtime.newParameterizedType(base.typeInfo(), 0, wildCard, List.of());
        }
        if (type instanceof Type.TypeVar typeVar) {
            String typeParameterName = typeVar.tsym.toString();
            TypeParameter typeParameter;
            if (typeVar.tsym.owner instanceof Symbol.MethodSymbol ms) {
                if (ms.owner instanceof Symbol.ClassSymbol cs) {
                    String typeFqn = cs.fullname.toString();
                    typeParameter = typeData.getTmpMethodTypeParameter(typeFqn, typeParameterName);
                    if (typeParameter == null) {
                        // method must have been completed, look up!
                        // FIXME could be in a super-type (java.lang.Module -> java.lang.reflect.AnnotatedElement)
                        MethodInfo owner = typeData.getMethod(ms);
                        assert owner != null;
                        typeParameter = owner.typeParameters().stream()
                                .filter(tp -> tp.simpleName().equals(typeParameterName))
                                .findFirst().orElseThrow();
                    }
                } else throw new UnsupportedOperationException();
            } else {
                TypeInfo owner = typeData.getType(typeVar.tsym.owner.toString());
                assert owner != null;
                typeParameter = owner.typeParameters().stream()
                        .filter(tp -> tp.simpleName().equals(typeParameterName))
                        .findFirst().orElseThrow();
            }
            return runtime.newParameterizedType(typeParameter, 0, null);
        }
        if ("none".equals(type.toString())) return null; // parent of Object
        throw new UnsupportedOperationException("NYI");
    }

    ParameterizedType convertTree(Tree type) {
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
        if (type instanceof JCTree.JCFieldAccess fieldAccess) {
            // enclosing type notation
            return convert(fieldAccess.type);
        /*    ParameterizedType enclosing = convertTree(fieldAccess.getExpression());
            String name = fieldAccess.name.toString();
            if (enclosing.typeInfo() != null) {
                TypeInfo typeInfo = enclosing.typeInfo().findSubType(name);
                return runtime.newParameterizedType(typeInfo, List.of());
            } else throw new UnsupportedOperationException("NYI");*/
        }
        if (type instanceof JCTree.JCTypeApply apply) {
            ParameterizedType base = convertTree(apply.getType());
            List<ParameterizedType> parameters = apply.getTypeArguments().stream().map(this::convertTree).toList();
            return runtime.newParameterizedType(base.typeInfo(), parameters);
        }
        if (type instanceof JCTree.JCArrayTypeTree att) {
            ParameterizedType base = convertTree(att.elemtype);
            return base.isTypeParameter()
                    ? runtime.newParameterizedType(base.typeParameter(), base.arrays() + 1, null)
                    : runtime.newParameterizedType(base.typeInfo(), base.arrays() + 1, null, List.of());
        }
        throw new UnsupportedOperationException("NYI");
    }

    private ParameterizedType classType(Type.ClassType ct) {
        String fullyQualifiedType = ct.tsym.toString();
        TypeInfo known = typeData.getType(fullyQualifiedType);
        TypeInfo typeInfo;
        if (known == null) {
            // on-demand loading; should be replaced by import handling?
            if (ct.tsym instanceof Symbol.ClassSymbol cs) {
                typeInfo = classSymbolScanner.primaryType(cs);
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
