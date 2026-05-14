package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeKind;
import java.util.*;

public class ConvertType {
    private final Runtime runtime;
    private final ClassSymbolScanner classSymbolScanner;
    private final TypeData typeData;
    private final SourceProvider sourceProvider;
    private final ElementStack elementStack;
    private final Types types;
    private final Deque<Map<String, TypeParameter>> typeParameterStack = new ArrayDeque<>();

    public ConvertType(Runtime runtime,
                       ClassSymbolScanner classSymbolScanner,
                       TypeData typeData,
                       ElementStack elementStack,
                       SourceProvider sourceProvider,
                       Types types) {
        this.runtime = runtime;
        this.classSymbolScanner = classSymbolScanner;
        this.typeData = typeData;
        this.elementStack = elementStack;
        this.sourceProvider = sourceProvider;
        this.types = types;
    }

    // general method, returns null when 'cs' is not a functional type
    public @Nullable MethodInfo computeSAM(Type type) {
        SAMDescriptor sd = findInstantiatedSAM(type);
        return sd == null ? null : sd.methodInfo;
    }

    public FieldInfo ensureField(Symbol.VarSymbol vs) {
        if (vs.owner instanceof Symbol.ClassSymbol cs) {
            TypeInfo owner = convert(cs.type).typeInfo();
            String name = vs.getSimpleName().toString();
            boolean isStatic = vs.isStatic();
            ParameterizedType type = convert(vs.type);
            FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, owner);
            owner.builder().addField(fieldInfo);
            typeData.put(vs, fieldInfo);
            return fieldInfo;
        } else throw new UnsupportedOperationException();
    }

    public MethodInfo ensureMethod(Symbol.MethodSymbol methodSymbol) {
        if (methodSymbol.owner instanceof Symbol.ClassSymbol cs) {
            TypeInfo owner = convert(cs.type).typeInfo();
            return classSymbolScanner.addMethodToType(owner, methodSymbol);
        } else throw new UnsupportedOperationException();
    }

    record SAMDescriptor(MethodInfo methodInfo, Symbol.MethodSymbol symbol, Type.MethodType instantiatedType) {
    }

    SAMDescriptor findInstantiatedSAM(Type functionalType) {
        if (!functionalType.tsym.isInterface()) return null;
        if (!types.isFunctionalInterface(functionalType.tsym)) return null;

        // Symbol — for name, flags, position, enclosing interface
        Symbol.MethodSymbol samSymbol = (Symbol.MethodSymbol) types.findDescriptorSymbol(functionalType.tsym);
        MethodInfo methodInfo = Objects.requireNonNullElseGet(typeData.getMethod(samSymbol),
                () -> ensureMethod(samSymbol));

        // Type — with type variables substituted for concrete args
        // e.g. Comparator<String> -> (String, String) -> int
        Type descriptorType = types.findDescriptorType(functionalType);
        Type.MethodType methodType = switch (descriptorType) {
            case Type.MethodType mt -> mt;
            case Type.ForAll forAll -> (Type.MethodType) forAll.qtype;
            default ->
                    throw new UnsupportedOperationException("unexpected descriptor type: " + descriptorType.getClass());
        };

        return new SAMDescriptor(methodInfo, samSymbol, methodType);
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
            return base.copyWithArrays(base.arrays() + 1);
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
                    TypeParameter tmpTypeParameter = typeData.getTmpMethodTypeParameter(typeFqn, typeParameterName);
                    if (tmpTypeParameter == null) {
                        // method must have been completed, look up!
                        // Note: it could be in a super-type (java.lang.Module -> java.lang.reflect.AnnotatedElement)
                        MethodInfo owner = typeData.getMethod(ms);
                        assert owner != null;
                        typeParameter = findTypeParameter(owner, typeParameterName);
                    } else {
                        typeParameter = tmpTypeParameter;
                    }
                } else throw new UnsupportedOperationException();
            } else if (typeVar.isCaptured()) {
                if (typeVar.getUpperBound() instanceof Type.ClassType ct) {
                    TypeInfo upper = convert(ct).typeInfo();
                    return runtime.newParameterizedType(upper, 0, runtime.wildcardExtends(), List.of());
                } else throw new UnsupportedOperationException();
            } else {
                String fullyQualifiedName = typeVar.tsym.owner.toString();
                TypeInfo owner = typeData.getType(fullyQualifiedName);
                assert owner != null;
                typeParameter = findTypeParameter(owner, typeParameterName);
            }
            return runtime.newParameterizedType(typeParameter, 0, null);
        }
        if ("none".equals(type.toString())) return null; // parent of Object
        throw new UnsupportedOperationException("NYI");
    }

    private @NotNull TypeParameter findTypeParameter(MethodInfo owner, String typeParameterName) {
        TypeParameter inStack = findInTypeParameterStack(typeParameterName);
        if (inStack != null) return inStack;
        TypeParameter typeParameter = owner.typeParameters().stream()
                .filter(tp -> tp.simpleName().equals(typeParameterName))
                .findFirst().orElse(null);
        if (typeParameter != null) return typeParameter;
        return findTypeParameter(owner.typeInfo(), typeParameterName);
    }

    private @NotNull TypeParameter findTypeParameter(TypeInfo owner, String typeParameterName) {
        TypeParameter inStack = findInTypeParameterStack(typeParameterName);
        if (inStack != null) return inStack;
        TypeParameter typeParameter = owner.typeParameters().stream()
                .filter(tp -> tp.simpleName().equals(typeParameterName))
                .findFirst().orElse(null);
        if (typeParameter != null) return typeParameter;
        if (owner.compilationUnitOrEnclosingType().isRight()) {
            return findTypeParameter(owner.compilationUnitOrEnclosingType().getRight(), typeParameterName);
        }
        throw new UnsupportedOperationException();
    }

    ParameterizedType convertTree(Tree type, DetailedSources.Builder detailedSourcesBuilder) {
        ParameterizedType pt = convertTreeDontSet(type, detailedSourcesBuilder);
        assert pt != null;
        detailedSourcesBuilder.put(pt, sourceProvider.sourceForNode(type));
        return pt;
    }

    private ParameterizedType convertTreeDontSet(Tree type, DetailedSources.Builder detailedSourcesBuilder) {
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
                TypeParameter tp = (TypeParameter) elementStack.find(typeParameterName);
                return runtime.newParameterizedType(tp, 0, null);
            } else {
                throw new UnsupportedOperationException("NYI");
            }
        }
        if (type instanceof JCTree.JCFieldAccess fieldAccess) {
            // enclosing type notation
            String name = fieldAccess.name.toString();
            ParameterizedType pt = convert(fieldAccess.type);
            if (!"class".equals(name) && pt.typeInfo() != null) {
                String packageName = pt.typeInfo().packageName();
                detailedSourcesBuilder.put(packageName, sourceProvider.sourceForNode(fieldAccess.getExpression()));
            }// else: java.lang.String.class
            return pt;
        }
        if (type instanceof JCTree.JCTypeApply apply) {
            ParameterizedType base = convertTree(apply.getType(), detailedSourcesBuilder);
            List<ParameterizedType> parameters = apply.getTypeArguments().stream()
                    .map(ta -> convertTree(ta, detailedSourcesBuilder)).toList();
            return runtime.newParameterizedType(base.typeInfo(), parameters);
        }
        if (type instanceof JCTree.JCArrayTypeTree att) {
            ParameterizedType base = convertTree(att.elemtype, detailedSourcesBuilder);
            return base.copyWithArrays(base.arrays() + 1);
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
                typeInfo = classSymbolScanner.type(cs);
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

    void newTypeParameterMap() {
        typeParameterStack.addLast(new HashMap<>());
    }

    void popTypeParameterMap() {
        typeParameterStack.removeLast();
    }

    void putInLastTypeParameterMap(TypeParameter typeParameter) {
        typeParameterStack.getLast().put(typeParameter.simpleName(), typeParameter);
    }

    TypeParameter findInTypeParameterStack(String name) {
        if (!typeParameterStack.isEmpty()) {
            return typeParameterStack.getLast().get(name);
        }
        return null;
    }
}
