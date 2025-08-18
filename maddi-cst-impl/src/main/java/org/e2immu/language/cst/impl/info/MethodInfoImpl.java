package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.translate.TranslationMapImpl;
import org.e2immu.support.EventuallyFinal;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodInfoImpl extends InfoImpl implements MethodInfo {

    public enum MethodTypeEnum implements MethodType {
        CONSTRUCTOR(true), COMPACT_CONSTRUCTOR(true), SYNTHETIC_CONSTRUCTOR(true),
        SYNTHETIC_ARRAY_CONSTRUCTOR(true),
        STATIC_BLOCK(false), DEFAULT_METHOD(false), STATIC_METHOD(false),
        ABSTRACT_METHOD(false), METHOD(false);
        final boolean constructor;

        MethodTypeEnum(boolean constructor) {
            this.constructor = constructor;
        }

        @Override
        public boolean isConstructor() {
            return constructor;
        }

        @Override
        public boolean isStatic() {
            return this == STATIC_BLOCK || this == STATIC_METHOD;
        }

        @Override
        public boolean isCompactConstructor() {
            return this == COMPACT_CONSTRUCTOR;
        }

        @Override
        public boolean isAbstract() {
            return this == ABSTRACT_METHOD;
        }

        @Override
        public boolean isDefault() {
            return this == DEFAULT_METHOD;
        }
    }

    @Override
    public String info() {
        return "method";
    }

    private final TypeInfo typeInfo; // back reference, only @ContextClass after...
    private final String name;
    private final MethodInfo.MethodType methodType;
    private final EventuallyFinal<MethodInspection> inspection = new EventuallyFinal<>();

    public MethodInfoImpl(TypeInfo typeInfo) {
        this(MethodTypeEnum.CONSTRUCTOR, CONSTRUCTOR_NAME, typeInfo);
    }

    public MethodInfoImpl(TypeInfo typeInfo, MethodType methodType) {
        this(methodType, CONSTRUCTOR_NAME, typeInfo);
    }

    public MethodInfoImpl(MethodInfo.MethodType methodType,
                          String name,
                          TypeInfo typeInfo) {
        assert CONSTRUCTOR_NAME.equals(name) == methodType.isConstructor();
        this.name = name;
        this.methodType = methodType;
        this.typeInfo = typeInfo;
        inspection.setVariable(new MethodInspectionImpl.Builder(this));
    }

    public MethodInspectionImpl.Builder inspectionBuilder() {
        if (inspection.isVariable()) return (MethodInspectionImpl.Builder) inspection.get();
        throw new UnsupportedOperationException();
    }

    public boolean hasBeenCommitted() {
        return inspection.isFinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInfoImpl that)) return false;
        return fullyQualifiedName().equals(that.fullyQualifiedName())
               // note: the primitive types have no source set
               && Objects.equals(typeInfo.compilationUnit().sourceSet(), that.typeInfo.compilationUnit().sourceSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedName());
    }

    @Override
    public String toString() {
        return fullyQualifiedName();
    }

    @Override
    public MethodInfo.Builder builder() {
        if (inspection.isVariable()) return (MethodInfo.Builder) inspection.get();
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPropertyNotNull() {
        if (returnType().isPrimitiveExcludingVoid()) return true;
        return analysis().getOrDefault(PropertyImpl.NOT_NULL_METHOD, ValueImpl.NotNullImpl.NULLABLE).isAtLeastNotNull();
    }

    @Override
    public JavaDoc javaDoc() {
        return inspection.get().javaDoc();
    }

    @Override
    public boolean isPropertyNullable() {
        return analysis().getOrDefault(PropertyImpl.NOT_NULL_METHOD, ValueImpl.NotNullImpl.NULLABLE).isNullable();
    }

    public void commit(MethodInspection methodInspection) {
        try {
            inspection.setFinal(methodInspection);
        } catch (IllegalStateException ise) {
            throw new RuntimeException("Have already committed method '" + fullyQualifiedName() + "'");
        }
    }

    @Override
    public boolean isConstructor() {
        return methodType.isConstructor();
    }

    private static final Map<String, Integer> JLO_METHODS = Map.of("clone", 0, "equals", 1,
            "finalize", 0, "getClass", 0, "hashCode", 0, "notify", 0,
            "notifyAll", 0, "toString", 0, "wait", 0);

    @Override
    public boolean isOverloadOfJLOMethod() {
        int n = parameters().size();
        Integer i = JLO_METHODS.get(name);
        if (i != null) {
            if (i == n) return true;
            if ("wait".equals(name)) return i <= 2;
        }
        return false;
    }

    @Override
    public TypeInfo primaryType() {
        return typeInfo.primaryType();
    }

    @Override
    public boolean isVoid() {
        return inspection.get().returnType().isVoid();
    }

    @Override
    public int complexity() {
        Block methodBody = inspection.get().methodBody();
        if (methodBody == null || methodBody.isEmpty()) return isAbstract() ? 2 : 10;
        return methodBody.complexity();
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
    public void visit(Predicate<Element> predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new MethodPrinterImpl(this).print(qualification);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        Block methodBody = inspection.get().methodBody();
        return methodBody == null ? Stream.empty() : methodBody.variables(descendMode);
    }

    @Override
    public Stream<TypeReference> typesReferenced() {
        return typesReferenced(true);
    }

    @Override
    public Stream<TypeReference> typesReferenced(boolean includeBody) {
        Stream<TypeReference> fromReturnType = returnType().typesReferencedMadeExplicit();
        Stream<TypeReference> fromParameters = parameters().stream().flatMap(Element::typesReferenced);
        Stream<TypeReference> fromTypeParameters = typeParameters().stream()
                .flatMap(tp -> tp.typesReferenced(true, new HashSet<>()));
        Stream<TypeReference> fromAnnotations = annotations().stream().flatMap(AnnotationExpression::typesReferenced);
        Stream<TypeReference> fromExceptionTypes = exceptionTypes()
                .stream().flatMap(ParameterizedType::typesReferencedMadeExplicit);
        Stream<TypeReference> fromBody = includeBody ? methodBody().typesReferenced() : Stream.of();
        Stream<TypeReference> fromJavaDoc = javaDoc() == null ? Stream.of() : javaDoc().typesReferenced();
        return Stream.concat(fromReturnType, Stream.concat(fromParameters, Stream.concat(fromAnnotations,
                Stream.concat(fromExceptionTypes, Stream.concat(fromTypeParameters, Stream.concat(fromJavaDoc, fromBody))))));
    }

    @Override
    public boolean complexityGreaterThanCOMPLEXITY_METHOD_WITHOUT_CODE() {
        return false;
    }


    @Override
    public boolean isPostfix() {
        return inspection.get().operatorType() == MethodInspection.OperatorType.POSTFIX;
    }

    @Override
    public boolean isInfix() {
        return inspection.get().operatorType() == MethodInspection.OperatorType.INFIX;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String simpleName() {
        return name;
    }

    @Override
    public String fullyQualifiedName() {
        return inspection.get().fullyQualifiedName();
    }

    @Override
    public TypeInfo typeInfo() {
        return typeInfo;
    }

    @Override
    public boolean isDefault() {
        return methodType.isDefault();
    }

    public boolean isCompactConstructor() {
        return methodType == MethodTypeEnum.COMPACT_CONSTRUCTOR;
    }

    public boolean isSyntheticConstructor() {
        return methodType == MethodTypeEnum.SYNTHETIC_CONSTRUCTOR || methodType == MethodTypeEnum.SYNTHETIC_ARRAY_CONSTRUCTOR;
    }

    public boolean isStaticBlock() {
        return methodType == MethodTypeEnum.STATIC_BLOCK;
    }

    @Override
    public boolean isStatic() {
        return methodType.isStatic();
    }

    @Override
    public ParameterizedType returnType() {
        return inspection.get().returnType();
    }

    @Override
    public Set<MethodInfo> topOfOverloadingHierarchy() {
        Set<MethodInfo> overrides = overrides();
        if (overrides.isEmpty()) return Set.of(this);
        return overrides.stream()
                .flatMap(mi -> mi.topOfOverloadingHierarchy().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<ParameterInfo> parameters() {
        return inspection.get().parameters();
    }

    @Override
    public boolean isOverloadOf(MethodInfo methodInfo) {
        return false;
    }

    @Override
    public boolean isOverloadOfJLOEquals() {
        return parameters().size() == 1 && "equals".equals(name);
    }

    @Override
    public Set<MethodInfo> overrides() {
        return inspection.get().overrides();
    }

    @Override
    public boolean allowsInterrupts() {
        return analysis().getOrDefault(PropertyImpl.METHOD_ALLOWS_INTERRUPTS, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public boolean isPublic() {
        return inspection.get().isPublic();
    }

    @Override
    public boolean isPubliclyAccessible() {
        if (!isPublic()) return false;
        return typeInfo.isPublic();
    }

    @Override
    public boolean isSynthetic() {
        return inspection.get().isSynthetic();
    }

    @Override
    public boolean isAbstract() {
        return methodType.isAbstract();
    }

    @Override
    public boolean isNonModifying() {
        return analysis().getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public boolean isFluent() {
        return analysis().getOrDefault(PropertyImpl.FLUENT_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public boolean isIdentity() {
        return analysis().getOrDefault(PropertyImpl.IDENTITY_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public Value.CommutableData commutableData() {
        return analysis().getOrDefault(PropertyImpl.COMMUTABLE_METHODS, ValueImpl.CommutableDataImpl.NONE);
    }

    @Override
    public Value.ParameterParSeq getParallelGroups() {
        return analysis().getOrDefault(PropertyImpl.PARALLEL_PARAMETER_GROUPS, ValueImpl.ParameterParSeqImpl.EMPTY);
    }

    @Override
    public Value.FieldValue getSetField() {
        return analysis().getOrDefault(PropertyImpl.GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
    }

    @Override
    public Value.GetSetEquivalent getSetEquivalents() {
        return analysis().getOrDefault(PropertyImpl.GET_SET_EQUIVALENT, ValueImpl.GetSetEquivalentImpl.EMPTY);
    }

    @Override
    public Value.PostConditions postConditions() {
        return analysis().getOrDefault(PropertyImpl.POST_CONDITIONS_METHOD, ValueImpl.PostConditionsImpl.EMPTY);
    }

    @Override
    public Value.Precondition precondition() {
        return analysis().getOrDefault(PropertyImpl.PRECONDITION_METHOD, ValueImpl.PreconditionImpl.EMPTY);
    }

    @Override
    public List<AnnotationExpression> annotations() {
        return inspection.get().annotations();
    }

    @Override
    public Block methodBody() {
        return inspection.get().methodBody();
    }

    @Override
    public Access access() {
        return inspection.get().access();
    }

    @Override
    public List<TypeParameter> typeParameters() {
        return inspection.get().typeParameters();
    }

    @Override
    public boolean isIgnoreModification() {
        return analysis().getOrDefault(PropertyImpl.IGNORE_MODIFICATION_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public Value.IndicesOfEscapes indicesOfEscapesNotInPreOrPostConditions() {
        return analysis().getOrDefault(PropertyImpl.INDICES_OF_ESCAPE_METHOD, ValueImpl.IndicesOfEscapesImpl.EMPTY);
    }

    @Override
    public boolean isSynchronized() {
        return inspection.get().modifiers().stream().anyMatch(MethodModifier::isSynchronized);
    }

    @Override
    public Map<FieldInfo, Boolean> areOwnFieldsReadModified() {
        ValueImpl.FieldBooleanMapImpl value = analysis().getOrNull(PropertyImpl.OWN_FIELDS_READ_MODIFIED_IN_METHOD,
                ValueImpl.FieldBooleanMapImpl.class);
        return value == null ? null : value.map();
    }

    @Override
    public List<ParameterizedType> exceptionTypes() {
        return inspection.get().exceptionTypes();
    }

    @Override
    public boolean isFinal() {
        return inspection.get().modifiers().stream().anyMatch(MethodModifier::isFinal);
    }

    @Override
    public MethodType methodType() {
        return methodType;
    }

    @Override
    public ParameterizedType typeOfParameterHandleVarargs(int index) {
        int formalParams = parameters().size();
        if (index < formalParams - 1 || index < formalParams && !isVarargs()) {
            return parameters().get(index).parameterizedType();
        }
        return parameters().get(formalParams - 1).parameterizedType().copyWithOneFewerArrays();
    }

    @Override
    public boolean noReturnValue() {
        return isVoid() || isConstructor();
    }

    @Override
    public boolean explicitlyEmptyMethod() {
        if (isAbstract() || !methodBody().statements().isEmpty() || isStatic() && isSynthetic()) return false;
        // we know it's empty if we're not in an external library.
        CompilationUnit cu = typeInfo.compilationUnit();
        return cu.sourceSet() != null && !cu.sourceSet().externalLibrary();
    }

    @Override
    public Set<MethodModifier> methodModifiers() {
        return inspection.get().modifiers();
    }

    @Override
    public boolean isFactoryMethod() {
        return isStatic() && returnType().typeInfo() != null
               && returnType().typeInfo().isEnclosedIn(typeInfo);
    }

    @Override
    public void rewirePhase3(InfoMap infoMap) {
        Block rewired = methodBody() == null ? null : methodBody().rewire(infoMap);
        MethodInfo rewiredMethod = infoMap.methodInfo(this);
        rewiredMethod.builder()
                .addOverrides(overrides().stream().map(infoMap::methodInfo).toList())
                .setMethodBody(rewired).commit();
        // analysis?
    }

    @Override
    public List<MethodInfo> translate(TranslationMap translationMap) {
        List<MethodInfo> direct = translationMap.translateMethod(this);
        if (direct.size() != 1 || direct.get(0) != this) {
            return direct;
        }
        ParameterizedType tReturnType = translationMap.translateType(returnType());
        boolean change = tReturnType != returnType() || !analysis().isEmpty() && translationMap.isClearAnalysis();

        ParameterizedType ownerPt = typeInfo.asSimpleParameterizedType();
        boolean ownerChange = translationMap.translateType(ownerPt) != ownerPt;
        change |= ownerChange;

        if (!change) {
            // first test; we'll have to re-do
            translationMap.methodTranslationInfo(this, true, true);
            List<Statement> tBody = methodBody().translate(translationMap);
            translationMap.methodTranslationInfo(this, false, true);
            change = tBody.size() != 1 || tBody.get(0) != methodBody();
        }

        List<ParameterizedType> exceptionTypeList = exceptionTypes();
        List<ParameterizedType> newExceptionTypes = exceptionTypeList
                .stream().map(translationMap::translateType).collect(translationMap.toList(exceptionTypeList));
        change |= newExceptionTypes != exceptionTypeList;

        List<ParameterInfo> directVariableChanges = parameters().stream()
                .map(pi -> (ParameterInfo) translationMap.translateVariable(pi))
                .collect(translationMap.toList(parameters()));
        change |= directVariableChanges != parameters();

        List<ParameterizedType> parameterTypes = parameters().stream().map(ParameterInfo::parameterizedType).toList();
        List<ParameterizedType> tTypes = parameterTypes.stream().map(translationMap::translateType)
                .collect(translationMap.toList(parameterTypes));
        change |= parameterTypes != tTypes;

        if (change) {
            MethodInfo methodInfo = copyAllButBodyParametersReturnTypeAnnotationsExceptionTypes(translationMap);
            MethodInfo.Builder builder = methodInfo.builder();


            for (ParameterInfo dvc : directVariableChanges) {
                ParameterInfo original = parameters().get(dvc.index());
                ParameterizedType type = dvc == original ? tTypes.get(dvc.index()) : dvc.parameterizedType();
                ParameterInfo newPi = builder.addParameter(dvc.simpleName(), type);
                newPi.builder()
                        .addAnnotations(dvc.annotations())
                        .setSource(dvc.source())
                        .addComments(dvc.comments())
                        .setVarArgs(dvc.isVarArgs())
                        .setIsFinal(dvc.isFinal());
                if (!translationMap.isClearAnalysis()) {
                    newPi.analysis().setAll(original.analysis());
                }
            }
            builder.commitParameters();

            TranslationMap tmWithParameters;
            if (parameters().isEmpty()) {
                tmWithParameters = translationMap;
            } else {
                TranslationMap.Builder b = new TranslationMapImpl.Builder()
                        .setClearAnalysis(translationMap.isClearAnalysis())
                        .setDelegate(translationMap);
                for (ParameterInfo pi : builder.parameters()) {
                    b.put(parameters().get(pi.index()), pi);
                }
                tmWithParameters = b.build();
            }
            translationMap.methodTranslationInfo(this, true, false);
            List<Statement> tBody = methodBody().translate(tmWithParameters);
            translationMap.methodTranslationInfo(this, false, false);

            builder.setMethodBody((Block) tBody.get(0));

            newExceptionTypes.forEach(builder::addExceptionType);
            builder.setReturnType(tReturnType);
            builder.commit();
            if (!translationMap.isClearAnalysis()) {
                methodInfo.analysis().setAll(analysis());
            }
            return List.of(methodInfo);
        }
        return List.of(this);
    }

    private MethodInfo copyAllButBodyParametersReturnTypeAnnotationsExceptionTypes(TranslationMap translationMap) {
        TypeInfo translatedTypeInfo = translationMap == null ? typeInfo
                : translationMap.translateType(typeInfo.asSimpleParameterizedType()).typeInfo();
        MethodInfo methodInfo = new MethodInfoImpl(methodType, name, translatedTypeInfo);
        MethodInfo.Builder builder = methodInfo.builder();
        builder.setAccess(access()).setSource(source()).setSynthetic(isSynthetic());
        typeParameters()
                .stream()
                .map(tp0 -> {
                    TypeParameter tp = tp0.withOwnerVariableTypeBounds(methodInfo);
                    if (translationMap != null) {
                        List<ParameterizedType> newTypeBounds = tp.typeBounds().stream()
                                .map(translationMap::translateType)
                                .toList();
                        tp.builder().setTypeBounds(newTypeBounds);
                    }
                    tp.builder().commit();
                    return tp;
                })
                .forEach(builder::addTypeParameter);
        methodModifiers().forEach(builder::addMethodModifier);
        return methodInfo;
    }

    @Override
    public MethodInfo withMethodBody(Block newBody) {
        if (newBody == methodBody()) return this;
        TranslationMap tm = new TranslationMapImpl.Builder().put(methodBody(), newBody).build();
        return translate(tm).get(0);
    }

    @Override
    public MethodInfo withMethodType(MethodType methodType) {
        if (methodType == this.methodType) return this;
        MethodInfoImpl mi = methodType.isConstructor() ? new MethodInfoImpl(typeInfo, methodType)
                : new MethodInfoImpl(methodType, name, typeInfo);
        if (inspection.isFinal()) mi.inspection.setFinal(inspection.get());
        else mi.inspection.setVariable(inspection.get());
        mi.analysis().setAll(analysis());
        return mi;
    }

    @Override
    public MethodInfo withSynthetic(boolean synthetic) {
        if (synthetic == isSynthetic()) return this;
        if (inspection.isFinal()) {
            MethodInfoImpl mii = new MethodInfoImpl(methodType, name, typeInfo);
            mii.inspection.setFinal(inspection.get().withSynthetic(synthetic));
            mii.analysis().setAll(analysis());
            return mii;
        }
        ((MethodInspectionImpl.Builder) inspection.get()).setSynthetic(synthetic);
        return this;
    }

    @Override
    public boolean isSyntheticArrayConstructor() {
        return methodType == MethodTypeEnum.SYNTHETIC_ARRAY_CONSTRUCTOR;
    }

    @Override
    public MissingData missingData() {
        return inspection.get().missingData();
    }

    @Override
    public Element rewire(InfoMap infoMap) {
        throw new UnsupportedOperationException("Must use the infoMap.methodInfo() method");
    }
}
