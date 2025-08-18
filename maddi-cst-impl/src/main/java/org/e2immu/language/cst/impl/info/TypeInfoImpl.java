package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.translate.TranslationMapImpl;
import org.e2immu.language.cst.impl.type.ParameterizedTypeImpl;
import org.e2immu.language.cst.impl.type.TypeParameterImpl;
import org.e2immu.support.Either;
import org.e2immu.support.EventuallyFinalOnDemand;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypeInfoImpl extends InfoImpl implements TypeInfo {
    public static final String JAVA_LANG_OBJECT = "java.lang.Object";

    private final String fullyQualifiedName;
    private final String simpleName;
    private final Either<CompilationUnit, TypeInfo> compilationUnitOrEnclosingType;
    private final EventuallyFinalOnDemand<TypeInspection> inspection = new EventuallyFinalOnDemand<>();

    public TypeInfoImpl(CompilationUnit compilationUnit, String simpleName) {
        String packageName = compilationUnit.packageName();
        fullyQualifiedName = packageName == null || packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        this.simpleName = simpleName;
        compilationUnitOrEnclosingType = Either.left(compilationUnit);
        inspection.setVariable(new TypeInspectionImpl.Builder(this));
    }

    public TypeInfoImpl(TypeInfo enclosing, String simpleName) {
        fullyQualifiedName = enclosing.fullyQualifiedName() + "." + simpleName;
        this.simpleName = simpleName;
        compilationUnitOrEnclosingType = Either.right(enclosing);
        inspection.setVariable(new TypeInspectionImpl.Builder(this));
    }

    public TypeInfoImpl(TypeInfo enclosingType, int index) {
        this(enclosingType, "$" + index);
    }

    public TypeInfoImpl(MethodInfo enclosingMethod, String simpleName, int index) {
        this.simpleName = simpleName;
        fullyQualifiedName = enclosingMethod.typeInfo().fullyQualifiedName() + "."
                             + index + "$" + enclosingMethod.name() + "$" + simpleName;
        this.compilationUnitOrEnclosingType = Either.right(enclosingMethod.typeInfo());
        inspection.setVariable(new TypeInspectionImpl.Builder(this));
    }

    @Override
    public String info() {
        return "type";
    }

    /*
    It is possible to have multiple types with the same fully qualified name.
    They must reside in different source sets, however.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeInfoImpl typeInfo)) return false;
        return fullyQualifiedName.equals(typeInfo.fullyQualifiedName) &&
               // note: the primitives have no source set
               Objects.equals(compilationUnit().sourceSet(), typeInfo.compilationUnit().sourceSet());
    }

    @Override
    public int hashCode() {
        return fullyQualifiedName.hashCode();
    }

    @Override
    public TypeInfo typeInfo() {
        return this;
    }

    @Override
    public String packageName() {
        if (compilationUnitOrEnclosingType.isLeft()) return compilationUnitOrEnclosingType.getLeft().packageName();
        return compilationUnitOrEnclosingType.getRight().packageName();
    }

    @Override
    public List<AnnotationExpression> annotations() {
        return inspection.get().annotations();
    }

    @Override
    public String fullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public String toString() {
        return fullyQualifiedName;
    }

    @Override
    public ParameterizedType asSimpleParameterizedType() {
        return new ParameterizedTypeImpl(this);
    }

    @Override
    public MethodInfo findUniqueMethod(String methodName, int numberOfParameters) {
        List<MethodInfo> list = methods().stream()
                .filter(mi -> methodName.equals(mi.name()) && mi.parameters().size() == numberOfParameters)
                .toList();
        if (list.size() != 1) {
            throw new NoSuchElementException("Cannot find a unique method named '" + methodName
                                             + "', with " + numberOfParameters + " parameters, in type "
                                             + fullyQualifiedName);
        }
        return list.getFirst();
    }

    @Override
    public MethodInfo findConstructor(int numberOfParameters) {
        List<MethodInfo> list = constructors().stream()
                .filter(constructor -> constructor.parameters().size() == numberOfParameters)
                .toList();
        if (list.size() != 1) {
            throw new NoSuchElementException("Found " + list.size() + " constructors with "
                                             + numberOfParameters + " parameters");
        }
        return list.getFirst();
    }

    @Override
    public MethodInfo findConstructor(TypeInfo typeOfFirstParameter) {
        List<MethodInfo> list = constructors().stream()
                .filter(constructor -> !constructor.parameters().isEmpty()
                                       && typeOfFirstParameter.equals(constructor.parameters().getFirst().parameterizedType().typeInfo()))
                .toList();
        if (list.size() != 1) {
            throw new NoSuchElementException("Found " + list.size() + " constructors with "
                                             + typeOfFirstParameter + " as type of the first parameter");
        }
        return list.getFirst();
    }

    @Override
    public TypeInfo findSubType(String simpleName) {
        List<TypeInfo> subTypes = subTypes().stream().filter(st -> simpleName.equals(st.simpleName())).toList();
        if (subTypes.size() != 1) throw new NoSuchElementException();
        return subTypes.getFirst();
    }

    @Override
    public TypeInfo findSubType(String simpleName, boolean complain) {
        List<TypeInfo> subTypes = subTypes().stream().filter(st -> simpleName.equals(st.simpleName())).toList();
        if (subTypes.size() != 1 && complain) throw new NoSuchElementException();
        return subTypes.isEmpty() ? null : subTypes.getFirst();
    }

    @Override
    public FieldInfo getFieldByName(String name, boolean complain) {
        Optional<FieldInfo> optional = fields().stream().filter(fieldInfo -> name.equals(fieldInfo.name())).findFirst();
        return complain ? optional.orElseThrow() : optional.orElse(null);
    }

    @Override
    public MethodInfo findUniqueMethod(String name, TypeInfo typeInfoOfFirstParameter) {
        List<MethodInfo> list = methods().stream()
                .filter(mi -> name.equals(mi.name())
                              && !mi.parameters().isEmpty()
                              && typeInfoOfFirstParameter.equals(mi.parameters().getFirst().parameterizedType().typeInfo()))
                .toList();
        if (list.size() != 1) throw new NoSuchElementException();
        return list.getFirst();
    }

    @Override
    public ParameterizedType parentClass() {
        return inspection.get().parentClass();
    }

    @Override
    public List<ParameterizedType> interfacesImplemented() {
        return inspection.get().interfacesImplemented();
    }

    @Override
    public List<TypeInfo> permittedWhenSealed() {
        return inspection.get().permittedWhenSealed();
    }

    @Override
    public boolean isFinal() {
        return inspection.get().isFinal();
    }

    @Override
    public boolean isNonSealed() {
        return typeModifiers().contains(TypeModifierEnum.NON_SEALED);
    }

    @Override
    public Stream<MethodInfo> methodStream() {
        return inspection.get().methodStream();
    }

    @Override
    public Stream<MethodInfo> recursiveMethodStream() {
        Stream<MethodInfo> mine = methodStream();
        Stream<MethodInfo> descend = subTypes().stream().flatMap(TypeInfo::recursiveMethodStream);
        return Stream.concat(mine, descend);
    }

    @Override
    public List<MethodInfo> constructors() {
        return inspection.get().constructors();
    }

    @Override
    public List<FieldInfo> fields() {
        return inspection.get().fields();
    }

    @Override
    public boolean doesNotRequirePackage() {
        return "java.lang".equals(packageName());
    }

    @Override
    public String fromPrimaryTypeDownwards() {
        if (compilationUnitOrEnclosingType.isLeft()) {
            return simpleName;
        }
        return compilationUnitOrEnclosingType.getRight().fromPrimaryTypeDownwards() + "." + simpleName;
    }

    @Override
    public Either<CompilationUnit, TypeInfo> compilationUnitOrEnclosingType() {
        return compilationUnitOrEnclosingType;
    }

    @Override
    public Set<TypeInfo> superTypesExcludingJavaLangObject() {
        return inspection.get().superTypesExcludingJavaLangObject();
    }

    @Override
    public Set<TypeModifier> typeModifiers() {
        return inspection.get().modifiers();
    }

    @Override
    public ParameterizedType asParameterizedType() {
        List<ParameterizedType> typeParameters = typeParameters()
                .stream().map(NamedType::asParameterizedType)
                .collect(Collectors.toList());
        return new ParameterizedTypeImpl(this, typeParameters);
    }

    @Override
    public List<TypeParameter> typeParameters() {
        return inspection.get().typeParameters();
    }

    @Override
    public boolean isStatic() {
        return typeNature().isStatic()  // interface, enum, etc.. otherwise: CLASS
               || isPrimaryType() // otherwise: subtype
               || inspection.get().modifiers().stream().anyMatch(TypeModifier::isStatic);
    }

    @Override
    public boolean isInterface() {
        TypeNature typeNature = typeNature();
        assert typeNature != null : "Type nature of " + fullyQualifiedName + " has not been set";
        return typeNature.isInterface();
    }

    @Override
    public String simpleName() {
        return simpleName;
    }

    @Override
    public TypeInfo primaryType() {
        MethodInfo methodInfo = enclosingMethod();
        if (methodInfo != null) return methodInfo.primaryType();
        return compilationUnitOrEnclosingType.isLeft() ? this : compilationUnitOrEnclosingType.getRight().primaryType();
    }

    @Override
    public boolean isNumeric() {
        return isInt() || isInteger() ||
               isLong() || isBoxedLong() ||
               isShort() || isBoxedShort() ||
               isByte() || isBoxedByte() ||
               isFloat() || isBoxedFloat() ||
               isDouble() || isBoxedDouble() ||
               isChar() || isCharacter();
    }

    @Override
    public boolean isBoxedExcludingVoid() {
        return isBoxedByte() || isBoxedShort() || isInteger() || isBoxedLong()
               || isCharacter() || isBoxedFloat() || isBoxedDouble() || isBoxedBoolean();
    }

    @Override
    public boolean isFunctionalInterface() {
        return inspection.get().singleAbstractMethod() != null;
    }

    public boolean allowInImport() {
        return isNotJavaLang() && !isPrimitiveExcludingVoid() && !isVoid();
    }

    public boolean packageIsExactlyJavaLang() {
        return "java.lang".equals(packageName());
    }

    public boolean isNotJavaLang() {
        return !this.fullyQualifiedName.startsWith("java.lang.");
    }

    public boolean needsParent() {
        return fullyQualifiedName.indexOf('.') > 0 && !fullyQualifiedName.startsWith("java.lang");
    }

    @Override
    public boolean isJavaLangObject() {
        return JAVA_LANG_OBJECT.equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isJavaLangString() {
        return "java.lang.String".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isJavaLangClass() {
        return "java.lang.Class".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isJavaLangVoid() {
        return "java.lang.Void".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isJavaIoSerializable() {
        return "java.io.Serializable".equals(fullyQualifiedName);
    }

    @Override
    public boolean isVoid() {
        return "void".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isBoxedFloat() {
        return "java.lang.Float".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isFloat() {
        return "float".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isBoxedDouble() {
        return "java.lang.Double".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isDouble() {
        return "double".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isBoxedByte() {
        return "java.lang.Byte".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isByte() {
        return "byte".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isBoxedShort() {
        return "java.lang.Short".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isShort() {
        return "short".equals(this.fullyQualifiedName);
    }

    public boolean isBoxedLong() {
        return "java.lang.Long".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isLong() {
        return "long".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isBoxedBoolean() {
        return "java.lang.Boolean".equals(this.fullyQualifiedName);
    }

    @Override
    public List<TypeInfo> subTypes() {
        return inspection.get().subTypes();
    }

    @Override
    public boolean isChar() {
        return "char".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isInteger() {
        return "java.lang.Integer".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isInt() {
        return "int".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isBoolean() {
        return "boolean".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isCharacter() {
        return "java.lang.Character".equals(this.fullyQualifiedName);
    }

    @Override
    public boolean isPrimitiveExcludingVoid() {
        return this.isByte() || this.isShort() || this.isInt() || this.isLong() ||
               this.isChar() || this.isFloat() || this.isDouble() || this.isBoolean();
    }

    @Override
    public JavaDoc javaDoc() {
        return inspection.get().javaDoc();
    }

    @Override
    public boolean isPublic() {
        return inspection.get().isPublic();
    }

    @Override
    public boolean isPubliclyAccessible() {
        if (!isPublic()) return false;
        if (compilationUnitOrEnclosingType.isRight())
            return compilationUnitOrEnclosingType.getRight().isPubliclyAccessible();
        return true;
    }

    @Override
    public void setOnDemandInspection(Consumer<TypeInfo> inspector) {
        assert inspection.isVariable();
        inspection.setOnDemand(() -> inspector.accept(this));
    }

    @Override
    public TypeInfo.Builder builder() {
        assert inspection.isVariable() : "Inspection of " + fullyQualifiedName + " has already been committed";
        return (TypeInfo.Builder) inspection.get();
    }

    @Override
    public TypeNature typeNature() {
        return inspection.get().typeNature();
    }

    @Override
    public int anonymousTypes() {
        return inspection.get().anonymousTypes();
    }

    @Override
    public int complexity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return print(qualification, true);
    }

    @Override
    public OutputBuilder print(Qualification qualification, boolean doTypeDeclaration) {
        return new TypePrinterImpl(this, false)
                .print(new ImportComputerImpl(), qualification, doTypeDeclaration);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<TypeReference> typesReferenced() {
        Stream<TypeReference> fromParent = parentClass().isJavaLangObject() ? Stream.empty()
                : parentClass().typesReferencedMadeExplicit();
        Stream<TypeReference> fromInterfaces = interfacesImplemented().stream()
                .flatMap(ParameterizedType::typesReferencedMadeExplicit);
        Stream<TypeReference> fromPermits = permittedWhenSealed().stream()
                .map(ti -> new ElementImpl.TypeReference(ti, true));
        Stream<TypeReference> fromAnnotations = annotations().stream().flatMap(AnnotationExpression::typesReferenced);
        Stream<TypeReference> fromMethods = methods().stream().flatMap(MethodInfo::typesReferenced);
        Stream<TypeReference> fromConstructors = constructors().stream().flatMap(MethodInfo::typesReferenced);
        Stream<TypeReference> fromFields = fields().stream().flatMap(FieldInfo::typesReferenced);
        Stream<TypeReference> fromSubTypes = subTypes().stream().flatMap(TypeInfo::typesReferenced);
        Stream<TypeReference> fromTypeParameters = typeParameters().stream()
                .flatMap(tp -> tp.typesReferenced(true, new HashSet<>()));
        Stream<TypeReference> fromJavaDoc = javaDoc() == null ? Stream.of() : javaDoc().typesReferenced();
        return Stream.concat(fromParent,
                Stream.concat(fromInterfaces,
                        Stream.concat(fromPermits,
                                Stream.concat(fromAnnotations, Stream.concat(fromMethods,
                                        Stream.concat(fromConstructors, Stream.concat(fromFields,
                                                Stream.concat(fromSubTypes,
                                                        Stream.concat(fromTypeParameters, fromJavaDoc)))))))));
    }

    @Override
    public Access access() {
        return inspection.get().access();
    }

    @Override
    public MethodInfo singleAbstractMethod() {
        return inspection.get().singleAbstractMethod();
    }

    public void commit(TypeInspection ti) {
        inspection.setFinal(ti);
    }

    @Override
    public List<Comment> comments() {
        return inspection.get().comments();
    }

    @Override
    public Source source() {
        return inspection.get().source();
    }

    @Override
    public boolean isPrivate() {
        return inspection.get().access().isPrivate();
    }

    @Override
    public boolean isAbstract() {
        return inspection.get().isAbstract();
    }

    @Override
    public boolean isAtLeastImmutableHC() {
        return analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isAtLeastImmutableHC();
    }

    @Override
    public boolean fieldsAccessedInRestOfPrimaryType() {
        return inspection.get().fieldsAccessedInRestOfPrimaryType();
    }

    @Override
    public boolean isAnnotation() {
        return inspection.get().typeNature().isAnnotation();
    }

    @Override
    public boolean isAnonymous() {
        return simpleName.startsWith("$");
    }

    @Override
    public boolean isEnclosedIn(TypeInfo typeInfo) {
        if (typeInfo == this) return true;
        if (compilationUnitOrEnclosingType.isLeft()) return false;
        return compilationUnitOrEnclosingType.getRight().isEnclosedIn(typeInfo);
    }

    @Override
    public boolean hasBeenInspected() {
        return inspection.isFinal();
    }

    @Override
    public boolean haveOnDemandInspection() {
        return inspection.haveOnDemand();
    }

    @Override
    public boolean isSynthetic() {
        return inspection.get().isSynthetic();
    }

    @Override
    public boolean isExtensible() {
        TypeNature typeNature = inspection.get().typeNature();
        if (typeNature.isClass()) {
            return inspection.get().modifiers().stream().noneMatch(m -> m.isFinal() || m.isSealed());
        }
        return typeNature.isInterface();
    }

    @Override
    public MethodInfo enclosingMethod() {
        return inspection.get().enclosingMethod();
    }

    @Override
    public boolean isSealed() {
        return inspection.get().modifiers().stream().anyMatch(TypeModifier::isSealed);
    }

    @Override
    public TypeInfo rewirePhase0(InfoMap infoMap, TypeInfo newEnclosingType) {
        assert infoMap.typeInfoNullIfAbsent(this) == null;
        TypeInfo typeInfo;
        if (compilationUnitOrEnclosingType.isLeft()) {
            typeInfo = new TypeInfoImpl(compilationUnit(), simpleName);
        } else {
            typeInfo = new TypeInfoImpl(infoMap.typeInfo(newEnclosingType), simpleName);
        }
        infoMap.put(typeInfo);
        TypeInfo.Builder builder = typeInfo.builder();
        for (TypeInfo subType : subTypes()) {
            builder.addSubType(subType.rewirePhase0(infoMap, typeInfo));
        }
        return typeInfo;
    }

    @Override
    public TypeInfo rewirePhase1(InfoMap infoMap) {
        TypeInfo typeInfo = infoMap.typeInfo(this);

        typeParameters().forEach(tp -> {
            List<AnnotationExpression> rewiredAnnotations = tp.annotations().stream()
                    .map(ae -> (AnnotationExpression) ae.rewire(infoMap)).toList();
            TypeParameter newTp = new TypeParameterImpl(tp.getIndex(), tp.simpleName(), Either.left(typeInfo));
            typeInfo.builder().addOrSetTypeParameter(newTp);
            newTp.builder()
                    .addComments(tp.comments())
                    .addAnnotations(rewiredAnnotations)
                    .setSource(tp.source())
                    .setTypeBounds(tp.typeBounds().stream().map(tb -> tb.rewire(infoMap)).toList())
                    .commit();
        });

        for (TypeInfo subType : subTypes()) {
            subType.rewirePhase1(infoMap);
        }
        for (MethodInfo constructor : constructors()) {
            MethodInfo rewiredConstructor = new MethodInfoImpl(typeInfo, constructor.methodType());
            infoMap.put(constructor, rewiredConstructor);
        }
        for (MethodInfo methodInfo : methods()) {
            MethodInfo rewiredMethod = new MethodInfoImpl(methodInfo.methodType(), methodInfo.name(), typeInfo);
            infoMap.put(methodInfo, rewiredMethod);
        }
        for (FieldInfo fieldInfo : fields()) {
            FieldInfo rewiredField = new FieldInfoImpl(fieldInfo.name(), fieldInfo.isStatic(),
                    fieldInfo.type().rewire(infoMap), typeInfo);
            infoMap.put(rewiredField);
        }

        TypeInfo.Builder builder = typeInfo.builder();
        builder.setTypeNature(typeNature())
                .setSource(source())
                .addComments(comments())
                .addAnnotations(annotations())
                .setSynthetic(isSynthetic())
                .setParentClass(parentClass() == null ? null : parentClass().rewire(infoMap))
                .setAccess(access());
        typeModifiers().forEach(builder::addTypeModifier);
        interfacesImplemented().forEach(pt -> builder.addInterfaceImplemented(pt.rewire(infoMap)));

        // do not commit!
        return typeInfo;
    }

    // all types are known; in phase 2 we do all methods, parameters, fields
    @Override
    public void rewirePhase2(InfoMap infoMap) {
        TypeInfo rewiredType = infoMap.typeInfo(this);
        assert rewiredType != this;
        TypeInfo.Builder builder = rewiredType.builder();

        for (TypeInfo subType : subTypes()) {
            subType.rewirePhase2(infoMap);
        }
        for (MethodInfo constructor : constructors()) {
            MethodInfo rewiredConstructor = infoMap.methodInfo(constructor);
            assert rewiredConstructor != constructor;
            builder.addConstructor(rewiredConstructor);
            handleMethodOrConstructor(constructor, rewiredConstructor, infoMap);
        }
        for (MethodInfo methodInfo : methods()) {
            MethodInfo rewiredMethod = infoMap.methodInfo(methodInfo);
            assert rewiredMethod != methodInfo;
            builder.addMethod(rewiredMethod);
            handleMethodOrConstructor(methodInfo, rewiredMethod, infoMap);
        }
        MethodInfo enclosingMethod = enclosingMethod();
        if (enclosingMethod != null) {
            MethodInfo rewiredEnclosing = infoMap.methodInfo(enclosingMethod);
            assert rewiredEnclosing != enclosingMethod;
            builder.setEnclosingMethod(rewiredEnclosing);
        }
        MethodInfo sam = singleAbstractMethod();
        if (sam != null) {
            MethodInfo rewiredSam = infoMap.methodInfo(sam);
            assert rewiredSam != sam;
            builder.setSingleAbstractMethod(rewiredSam);
        }
        for (FieldInfo fieldInfo : fields()) {
            FieldInfo rewiredField = infoMap.fieldInfo(fieldInfo);
            assert rewiredField != fieldInfo;
            rewiredField.builder().setSynthetic(fieldInfo.isSynthetic())
                    .setSource(fieldInfo.source())
                    .addComments(fieldInfo.comments())
                    .addAnnotations(fieldInfo.annotations())
                    .setAccess(fieldInfo.access());
            fieldInfo.modifiers().forEach(rewiredField.builder()::addFieldModifier);
            infoMap.put(rewiredField);
            builder.addField(rewiredField);
        }
        // do not commit!
    }

    private void handleMethodOrConstructor(MethodInfo methodInfo, MethodInfo rewiredMethod, InfoMap infoMap) {
        infoMap.put(methodInfo, rewiredMethod);
        MethodInfo.Builder builder = rewiredMethod.builder();

        methodInfo.typeParameters().forEach(tp -> {
            List<AnnotationExpression> rewiredAnnotations = tp.annotations().stream()
                    .map(ae -> (AnnotationExpression) ae.rewire(infoMap)).toList();
            TypeParameter newTp = new TypeParameterImpl(tp.getIndex(), tp.simpleName(), Either.right(rewiredMethod));
            builder.addTypeParameter(newTp);
            newTp.builder()
                    .addComments(tp.comments())
                    .addAnnotations(rewiredAnnotations)
                    .setSource(tp.source())
                    .setTypeBounds(tp.typeBounds().stream().map(tb -> tb.rewire(infoMap)).toList())
                    .commit();
        });
        rewireParameters(methodInfo, rewiredMethod, infoMap);
        methodInfo.methodModifiers().forEach(builder::addMethodModifier);
        builder.addComments(methodInfo.comments())
                .addComments(methodInfo.comments().stream().map(c -> c.rewire(infoMap)).toList())
                .setSource(methodInfo.source())
                .addAnnotations(methodInfo.annotations().stream()
                        .map(a -> (AnnotationExpression) a.rewire(infoMap)).toList())
                .setReturnType(methodInfo.returnType().rewire(infoMap))
                .setAccess(methodInfo.access())
                .setSynthetic(methodInfo.isSynthetic())
                .setMissingData(methodInfo.missingData());
        methodInfo.exceptionTypes().forEach(pt -> builder.addExceptionType(pt.rewire(infoMap)));
    }

    private void rewireParameters(MethodInfo methodInfo, MethodInfo rewiredMethod, InfoMap infoMap) {
        for (ParameterInfo pi : methodInfo.parameters()) {
            ParameterInfo rewiredPi = rewiredMethod.builder()
                    .addParameter(pi.name(), pi.parameterizedType().rewire(infoMap));
            rewiredPi.builder()
                    .addComments(pi.comments().stream().map(c -> c.rewire(infoMap)).toList())
                    .setSource(pi.source())
                    .addAnnotations(pi.annotations().stream()
                            .map(a -> (AnnotationExpression) a.rewire(infoMap)).toList())
                    .setIsFinal(pi.isFinal())
                    .setVarArgs(pi.isVarArgs())
                    .setSynthetic(pi.isSynthetic())
                    .commit();
            infoMap.put(pi, rewiredPi);
        }
        rewiredMethod.builder().commitParameters();
    }

    @Override
    public void rewirePhase3(InfoMap infoMap) {
        // method content, anonymous types, etc.
        for (TypeInfo subType : subTypes()) {
            subType.rewirePhase3(infoMap);
        }
        for (MethodInfo methodInfo : constructorsAndMethods()) {
            methodInfo.rewirePhase3(infoMap);
        }
        for (FieldInfo fieldInfo : fields()) {
            fieldInfo.rewirePhase3(infoMap);
        }
        TypeInfo rewiredType = infoMap.typeInfo(this);
        rewiredType.builder().commit();
    }

    @Override
    public List<TypeInfo> translate(TranslationMap translationMapIn) {
        TypeInfo direct = translationMapIn.translateTypeInfo(this);
        if (direct != this) {
            return List.of(direct);
        }

        // if there is any change, this will be the new typeInfo.
        TypeInfo typeInfo = copyAllButConstructorsMethodsFieldsSubTypesAnnotations(translationMapIn);
        ParameterizedType simpleParameterizedType = asSimpleParameterizedType();
        boolean change = !analysis().isEmpty() && translationMapIn.isClearAnalysis();

        TranslationMap.Builder tmb = new TranslationMapImpl.Builder()
                .setClearAnalysis(translationMapIn.isClearAnalysis())
                .setDelegate(translationMapIn);

        List<TypeParameter> newTypeParameters = new ArrayList<>();
        for (TypeParameter tp : typeParameters()) {
            TypeParameter newTp = tp.withOwnerVariableTypeBounds(typeInfo);
            newTypeParameters.add(newTp);
            change |= this != tp.getOwner().getLeft();
            tmb.put(new ParameterizedTypeImpl(tp, 0), new ParameterizedTypeImpl(newTp, 0));
        }

        List<FieldInfo> newFields = new ArrayList<>(2 * fields().size());

        tmb.put(simpleParameterizedType, typeInfo.asSimpleParameterizedType());
        for (FieldInfo fieldInfo : fields()) {
            FieldInfo newField = fieldInfo.withOwnerVariableBuilder(typeInfo);
            newFields.add(newField);
            tmb.put(fieldInfo, newField);
        }
        TranslationMap translationMap = tmb.build();

        for (TypeParameter tp : newTypeParameters) {
            List<ParameterizedType> newTypeBounds = tp.typeBounds()
                    .stream().map(translationMap::translateType)
                    .toList();
            tp.builder().setTypeBounds(newTypeBounds).commit();
        }
        for (FieldInfo fieldInfo : newFields) {
            Expression init = fieldInfo.initializer();
            Expression tInit = init.translate(translationMap);
            if (init != tInit) {
                change = true;
                fieldInfo.builder().setInitializer(tInit);
            }
            fieldInfo.builder().computeAccess().commit();
        }

        List<MethodInfo> newConstructors = new ArrayList<>(2 * constructors().size());
        for (MethodInfo methodInfo : constructors()) {
            List<MethodInfo> tMethod = methodInfo.translate(translationMap);
            newConstructors.addAll(tMethod);
            change |= tMethod.size() != 1 || tMethod.getFirst() != methodInfo;
        }
        List<MethodInfo> newMethods = new ArrayList<>(2 * methods().size());
        MethodInfo translatedSam = null;
        for (MethodInfo methodInfo : methods()) {
            List<MethodInfo> tMethod = methodInfo.translate(translationMap);
            newMethods.addAll(tMethod);
            change |= tMethod.size() != 1 || tMethod.getFirst() != methodInfo;
            if (methodInfo == singleAbstractMethod()) {
                translatedSam = tMethod.size() == 1 ? tMethod.getFirst() : singleAbstractMethod();
            }
        }
        List<TypeInfo> subTypeList = subTypes();
        List<TypeInfo> newSubTypes = subTypeList.stream().map(st -> st.translate(translationMap).getFirst())
                .collect(translationMap.toList(subTypeList));
        change |= newSubTypes != subTypeList;
        List<AnnotationExpression> newAnnotations = new ArrayList<>(2 * annotations().size());
        for (AnnotationExpression ae : annotations()) {
            AnnotationExpression tAe = (AnnotationExpression) ae.translate(translationMap);
            newAnnotations.add(tAe);
            change |= tAe != ae;
        }
        if (change) {
            TypeInfo.Builder builder = typeInfo.builder();
            newTypeParameters.forEach(builder::addOrSetTypeParameter);
            newConstructors.forEach(builder::addConstructor);
            newMethods.forEach(builder::addMethod);
            newSubTypes.forEach(builder::addSubType);
            newFields.forEach(builder::addField);
            builder.addAnnotations(newAnnotations)
                    .setEnclosingMethod(this.enclosingMethod())
                    .setSingleAbstractMethod(translatedSam)
                    .commit();
            if (!translationMap.isClearAnalysis()) {
                typeInfo.analysis().setAll(analysis());
            }
            return List.of(typeInfo);
        }
        return List.of(this);
    }

    private TypeInfo copyAllButConstructorsMethodsFieldsSubTypesAnnotations(TranslationMap translationMap) {
        TypeInfo typeInfo;
        if (compilationUnitOrEnclosingType.isRight()) {
            TypeInfo enclosing = compilationUnitOrEnclosingType.getRight();
            TypeInfo tEnclosing = translationMap == null ? enclosing
                    : translationMap.translateType(enclosing.asSimpleParameterizedType()).typeInfo();
            typeInfo = new TypeInfoImpl(tEnclosing, simpleName);
        } else {
            typeInfo = new TypeInfoImpl(compilationUnitOrEnclosingType.getLeft(), simpleName);
        }
        TypeInfo.Builder b = typeInfo.builder();
        b.setAccess(access());
        b.setTypeNature(typeNature());
        b.setParentClass(parentClass());
        b.setSource(source());
        b.setSynthetic(isSynthetic());
        interfacesImplemented().forEach(b::addInterfaceImplemented);
        typeModifiers().forEach(b::addTypeModifier);
        return typeInfo;
    }

    @Override
    public Element rewire(InfoMap infoMap) {
        throw new UnsupportedOperationException("Must use one of the infoMap methods");
    }

    @Override
    public boolean hasImplicitParent() {
        if (typeNature().isClass()) {
            DetailedSources detailedSources = source() == null ? null : source().detailedSources();
            if (detailedSources != null) return detailedSources.detail(DetailedSources.EXTENDS) == null;
            // fallback, we're assuming that you don't write "extends Object"...
            return parentClass() != null && parentClass().isJavaLangObject();
        }
        return true;
    }
}
