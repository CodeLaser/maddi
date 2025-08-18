package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;

import java.util.*;
import java.util.stream.Stream;

public class TypeInspectionImpl extends InspectionImpl implements TypeInspection {
    private final Set<TypeModifier> typeModifiers;
    private final List<MethodInfo> methods;
    private final List<MethodInfo> constructors;
    private final List<FieldInfo> fields;
    private final ParameterizedType parentClass;
    private final TypeNature typeNature;
    private final MethodInfo singleAbstractMethod;
    private final List<ParameterizedType> interfacesImplemented;
    private final List<TypeParameter> typeParameters;
    private final List<TypeInfo> subTypes;
    private final boolean fieldsAccessedInRestOfPrimaryType;
    private final MethodInfo enclosingMethod;
    private final List<TypeInfo> permittedWhenSealed;
    private final Set<TypeInfo> superTypesExcludingJavaLangObject;
    private final int anonymousTypes;

    public TypeInspectionImpl(Inspection inspection,
                              Set<TypeModifier> typeModifiers,
                              List<MethodInfo> methods,
                              List<MethodInfo> constructors,
                              List<FieldInfo> fields,
                              ParameterizedType parentClass,
                              TypeNature typeNature,
                              MethodInfo singleAbstractMethod,
                              List<ParameterizedType> interfacesImplemented,
                              List<TypeParameter> typeParameters,
                              List<TypeInfo> subTypes,
                              boolean fieldsAccessedInRestOfPrimaryType,
                              MethodInfo enclosingMethod,
                              List<TypeInfo> permittedWhenSealed,
                              Set<TypeInfo> superTypesExcludingJavaLangObject,
                              int anonymousTypes) {
        super(inspection.access(), inspection.comments(), inspection.source(), inspection.isSynthetic(),
                inspection.annotations(), inspection.javaDoc());
        this.typeModifiers = typeModifiers;
        this.methods = methods;
        this.constructors = constructors;
        this.parentClass = parentClass;
        this.typeNature = typeNature;
        this.singleAbstractMethod = singleAbstractMethod;
        this.interfacesImplemented = interfacesImplemented;
        this.subTypes = subTypes;
        this.fields = fields;
        this.typeParameters = typeParameters;
        this.fieldsAccessedInRestOfPrimaryType = fieldsAccessedInRestOfPrimaryType;
        this.enclosingMethod = enclosingMethod;
        this.permittedWhenSealed = permittedWhenSealed;
        this.superTypesExcludingJavaLangObject = superTypesExcludingJavaLangObject;
        this.anonymousTypes = anonymousTypes;
    }

    @Override
    public Set<TypeInfo> superTypesExcludingJavaLangObject() {
        return superTypesExcludingJavaLangObject;
    }

    @Override
    public List<TypeInfo> permittedWhenSealed() {
        return permittedWhenSealed;
    }

    @Override
    public int anonymousTypes() {
        return anonymousTypes;
    }

    @Override
    public boolean isFinal() {
        return typeModifiers.contains(TypeModifierEnum.FINAL);
    }

    @Override
    public List<TypeParameter> typeParameters() {
        return typeParameters;
    }

    @Override
    public Set<TypeModifier> modifiers() {
        return typeModifiers;
    }

    @Override
    public Stream<MethodInfo> methodStream() {
        return this.methods.stream();
    }

    @Override
    public List<MethodInfo> constructors() {
        return constructors;
    }

    @Override
    public TypeNature typeNature() {
        return typeNature;
    }

    @Override
    public ParameterizedType parentClass() {
        return parentClass;
    }

    @Override
    public List<ParameterizedType> interfacesImplemented() {
        return interfacesImplemented;
    }

    @Override
    public MethodInfo singleAbstractMethod() {
        return singleAbstractMethod;
    }

    @Override
    public List<TypeInfo> subTypes() {
        return subTypes;
    }

    @Override
    public List<FieldInfo> fields() {
        return fields;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract(typeNature, typeModifiers);
    }

    public static class Builder extends InspectionImpl.Builder<TypeInfo.Builder> implements TypeInspection, TypeInfo.Builder {
        private final Set<TypeModifier> typeModifiers = new HashSet<>();
        private final List<MethodInfo> methods = new ArrayList<>();
        private final List<MethodInfo> constructors = new ArrayList<>();
        private final List<FieldInfo> fields = new ArrayList<>();
        private final List<ParameterizedType> interfacesImplemented = new ArrayList<>();
        private final List<TypeInfo> subTypes = new ArrayList<>();
        private final List<TypeParameter> typeParameters = new ArrayList<>();
        private final List<TypeInfo> permittedWhenSealed = new ArrayList<>();
        private int anonymousTypes;

        private ParameterizedType parentClass;
        private TypeNature typeNature;
        private MethodInfo singleAbstractMethod;
        private final TypeInfoImpl typeInfo;
        private boolean fieldsAccessedInRestOfPrimaryType;
        private MethodInfo enclosingMethod;

        private volatile boolean hierarchyDone;

        @Override
        public Set<TypeInfo> superTypesExcludingJavaLangObject() {
            Set<TypeInfo> set = new HashSet<>();
            recursivelyComputeSuperTypesExcludingJLO(typeInfo, set);
            return set;
        }

        private void recursivelyComputeSuperTypesExcludingJLO(TypeInfo type, Set<TypeInfo> superTypes) {
            ParameterizedType parentPt = type.parentClass();
            if (parentPt != null) {
                TypeInfo parent = parentPt.typeInfo();
                if (!parent.isJavaLangObject() && superTypes.add(parent)) {
                    recursivelyComputeSuperTypesExcludingJLO(parent, superTypes);
                }
            }
            for (ParameterizedType interfaceImplemented : type.interfacesImplemented()) {
                TypeInfo i = interfaceImplemented.typeInfo();
                if (superTypes.add(i)) {
                    recursivelyComputeSuperTypesExcludingJLO(i, superTypes);
                }
            }
        }

        @Override
        public List<TypeInfo> permittedWhenSealed() {
            return permittedWhenSealed;
        }

        @Override
        public boolean fieldsAccessedInRestOfPrimaryType() {
            return fieldsAccessedInRestOfPrimaryType;
        }

        public Builder(TypeInfoImpl typeInfo) {
            this.typeInfo = typeInfo;
        }

        @Override
        public Builder setSingleAbstractMethod(MethodInfo singleAbstractMethod) {
            this.singleAbstractMethod = singleAbstractMethod;
            return this;
        }

        @Override
        public TypeInfo.Builder addSubType(TypeInfo subType) {
            subTypes.add(subType);
            return this;
        }

        @Override
        public TypeInfo.Builder addTypeModifier(TypeModifier typeModifier) {
            typeModifiers.add(typeModifier);
            return this;
        }

        @Override
        public List<TypeParameter> typeParameters() {
            return typeParameters;
        }

        @Override
        public Set<TypeModifier> modifiers() {
            return typeModifiers;
        }

        @Override
        public TypeInfo.Builder addMethod(MethodInfo methodInfo) {
            methods.add(methodInfo);
            return this;
        }

        @Override
        public TypeInfo.Builder addConstructor(MethodInfo constructor) {
            constructors.add(constructor);
            return this;
        }

        @Override
        public TypeInfo.Builder addField(FieldInfo field) {
            fields.add(field);
            return this;
        }

        @Override
        public List<FieldInfo> fields() {
            return fields;
        }

        @Override
        public TypeInfo.Builder setTypeNature(TypeNature typeNature) {
            this.typeNature = typeNature;
            return this;
        }

        @Override
        public TypeInfo.Builder setParentClass(ParameterizedType parentClass) {
            this.parentClass = parentClass;
            return this;
        }

        @Override
        public TypeInfo.Builder addInterfaceImplemented(ParameterizedType interfaceImplemented) {
            this.interfacesImplemented.add(interfaceImplemented);
            return this;
        }

        @Override
        public TypeInfo.Builder addOrSetTypeParameter(TypeParameter typeParameter) {
            if (typeParameters.size() <= typeParameter.getIndex()) {
                typeParameters.add(typeParameter);
            } else {
                typeParameters.set(typeParameter.getIndex(), typeParameter);
            }
            return this;
        }

        @Override
        public TypeInfo.Builder addPermittedType(TypeInfo typeInfo) {
            permittedWhenSealed.add(typeInfo);
            return this;
        }

        @Override
        public void commit() {
            List<TypeInfo> sortedSubTypes = subTypes.stream().sorted(Comparator.comparing(TypeInfo::simpleName)).toList();
            TypeInspection ti = new TypeInspectionImpl(this, Set.copyOf(typeModifiers), List.copyOf(methods),
                    List.copyOf(constructors), List.copyOf(fields), parentClass, typeNature, singleAbstractMethod,
                    List.copyOf(interfacesImplemented), List.copyOf(typeParameters), sortedSubTypes,
                    fieldsAccessedInRestOfPrimaryType, enclosingMethod, List.copyOf(permittedWhenSealed),
                    superTypesExcludingJavaLangObject(), anonymousTypes);
            if (ti.parentClass() == null
                && !typeInfo.isJavaLangObject()
                && typeNature != TypeNatureEnum.PRIMITIVE) {
                throw new UnsupportedOperationException("Cannot commit. Type " + typeInfo + " has a null parent class, and it is not JLO. Its type nature is " + ti.typeNature());
            }
            assert !ti.typeNature().isEnum()
                   || "java.lang.Enum".equals(ti.parentClass().typeInfo().fullyQualifiedName());
            assert !ti.typeNature().isAnnotation()
                   || "java.lang.annotation.Annotation".equals(ti.interfacesImplemented().getFirst().typeInfo().fullyQualifiedName());
            typeInfo.commit(ti);
        }

        @Override
        public Stream<MethodInfo> methodStream() {
            return this.methods.stream();
        }

        @Override
        public List<MethodInfo> constructors() {
            return constructors;
        }

        @Override
        public TypeNature typeNature() {
            return typeNature;
        }

        @Override
        public ParameterizedType parentClass() {
            return parentClass;
        }

        @Override
        public List<ParameterizedType> interfacesImplemented() {
            return interfacesImplemented;
        }

        @Override
        public MethodInfo singleAbstractMethod() {
            return singleAbstractMethod;
        }

        @Override
        public List<TypeInfo> subTypes() {
            return subTypes;
        }

        @Override
        public boolean isAbstract() {
            return TypeInspectionImpl.isAbstract(typeNature, typeModifiers);
        }

        @Override
        public List<MethodInfo> methods() {
            return methods;
        }

        @Override
        public Builder computeAccess() {
            Access fromModifiers = accessFromModifiers();
            if (typeInfo.compilationUnitOrEnclosingType().isLeft()) {
                setAccess(fromModifiers);
            } else {
                TypeInfo enclosingType = typeInfo.compilationUnitOrEnclosingType().getRight();
                Access fromEnclosing = enclosingType.access();
                if (fromEnclosing == null) {
                    throw new UnsupportedOperationException("Trying to compute access of " + typeInfo
                                                            + " (from modifiers: " + fromModifiers
                                                            + "), but access of enclosing type "
                                                            + enclosingType + " not yet set.");
                }
                Access combined = fromEnclosing.combine(fromModifiers);
                setAccess(combined);
            }
            return this;
        }

        private Access accessFromModifiers() {
            for (TypeModifier typeModifier : typeModifiers) {
                if (typeModifier.isPublic()) return AccessEnum.PUBLIC;
                if (typeModifier.isPrivate()) return AccessEnum.PRIVATE;
                if (typeModifier.isProtected()) return AccessEnum.PROTECTED;
            }
            return AccessEnum.PACKAGE;
        }

        @Override
        public boolean hasBeenCommitted() {
            return typeInfo.hasBeenInspected();
        }

        @Override
        public Builder setEnclosingMethod(MethodInfo enclosingMethod) {
            this.enclosingMethod = enclosingMethod;
            return this;
        }

        @Override
        public MethodInfo enclosingMethod() {
            return enclosingMethod;
        }

        @Override
        public int getAndIncrementAnonymousTypes() {
            return anonymousTypes++;
        }

        @Override
        public Builder setAnonymousTypes(int anonymousTypes) {
            this.anonymousTypes = anonymousTypes;
            return this;
        }

        @Override
        public int anonymousTypes() {
            return anonymousTypes;
        }

        @Override
        public String toString() {
            return "TypeInspectionImpl.Builder of " + typeInfo;
        }

        @Override
        public boolean hierarchyNotYetDone() {
            return !hierarchyDone;
        }

        @Override
        public Builder hierarchyIsDone() {
            this.hierarchyDone = true;
            return this;
        }

        @Override
        public boolean isFinal() {
            return modifiers().contains(TypeModifierEnum.FINAL);
        }
    }

    @Override
    public boolean fieldsAccessedInRestOfPrimaryType() {
        return fieldsAccessedInRestOfPrimaryType;
    }

    private static boolean isAbstract(TypeNature typeNature, Set<TypeModifier> typeModifiers) {
        return typeNature.isInterface() || typeModifiers.stream().anyMatch(TypeModifier::isAbstract);
    }

    @Override
    public MethodInfo enclosingMethod() {
        return enclosingMethod;
    }
}
