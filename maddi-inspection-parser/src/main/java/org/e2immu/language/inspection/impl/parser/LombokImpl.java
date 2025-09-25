package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.ClassExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.Lombok;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.util.internal.util.GetSetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LombokImpl(Runtime runtime, CompiledTypesManager compiledTypesManager) implements Lombok {
    private static final String LOMBOK_DOT = "lombok.";
    private static final String EXTERN_DOT = "extern.";
    private static final Logger LOGGER = LoggerFactory.getLogger(LombokImpl.class);

    private static class DataImpl implements Data {
        private boolean addGetters;
        private boolean addSetters;
        private boolean requiredArgsConstructor;
        private boolean noArgsConstructor;
        private boolean allArgsConstructor;
        private boolean builder;

        @Override
        public boolean addGetters() {
            return addGetters;
        }

        @Override
        public boolean addSetters() {
            return addSetters;
        }

        @Override
        public boolean requiredArgsConstructor() {
            return requiredArgsConstructor;
        }

        @Override
        public boolean noArgsConstructor() {
            return noArgsConstructor;
        }

        @Override
        public boolean allArgsConstructor() {
            return allArgsConstructor;
        }

        @Override
        public boolean builder() {
            return builder;
        }
    }

    @Override
    public Data handleType(TypeInfo typeInfo) {
        DataImpl d = new DataImpl();
        Map<String, AnnotationExpression> lombokMap = new HashMap<>();
        typeInfo.builder().annotationStream()
                .filter(ae -> ae.typeInfo().fullyQualifiedName().startsWith(LOMBOK_DOT))
                .forEach(ae -> lombokMap.put(ae.typeInfo().fullyQualifiedName().substring(LOMBOK_DOT.length()), ae));
        AnnotationExpression builder = lombokMap.get("Builder");
        if (builder != null) {
            d.builder = true;
        }
        AnnotationExpression getter = lombokMap.get("Getter");
        if (getter != null) {
            d.addGetters = true;
        }
        AnnotationExpression setter = lombokMap.get("Setter");
        if (setter != null) {
            d.addSetters = true;
        }
        AnnotationExpression rac = lombokMap.get("RequiredArgsConstructor");
        if (rac != null) {
            d.requiredArgsConstructor = true;
        }
        AnnotationExpression nac = lombokMap.get("NoArgsConstructor");
        if (nac != null) {
            d.noArgsConstructor = true;
        }
        AnnotationExpression aac = lombokMap.get("AllArgsConstructor");
        if (aac != null) {
            d.allArgsConstructor = true;
        }
        AnnotationExpression data = lombokMap.get("Data");
        if (data != null) {
            d.addGetters = true;
            d.addSetters = true;
            d.requiredArgsConstructor = true;
        }
        AnnotationExpression slf4j = lombokMap.get(EXTERN_DOT + "slf4j.Slf4j");
        if (slf4j != null) {
            //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LogExample.class);
            addLogField(typeInfo, "org.slf4j.Logger", "org.slf4j.LoggerFactory",
                    "getLogger", false);
        }
        AnnotationExpression extSlf4j = lombokMap.get(EXTERN_DOT + "slf4j.XSlf4j");
        if (extSlf4j != null) {
            // private static final org.slf4j.ext.XLogger log = org.slf4j.ext.XLoggerFactory.getXLogger(LogExample.class);
            addLogField(typeInfo, "org.slf4j.ext.XLogger", "org.slf4j.ext.XLoggerFactory",
                    "getXLogger", false);
        }
        AnnotationExpression log = lombokMap.get(EXTERN_DOT + "java.Log");
        if (log != null) {
            //private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(LogExample.class.getName());
            addLogField(typeInfo, "java.util.logging.Logger", "java.util.logging.Logger",
                    "getLogger", true);
        }
        AnnotationExpression commonsLog = lombokMap.get(EXTERN_DOT + "apachecommons.CommonsLog");
        if (commonsLog != null) {
            //private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(LogExample.class);
            addLogField(typeInfo, "org.apache.commons.logging.Log", "org.apache.commons.logging.LogFactory",
                    "getLog", false);
        }
        return d;
    }

    private void addLogField(TypeInfo typeInfo, String logClass, String logFactoryClassName, String logFactoryMethodName, boolean addGetName) {
        Source source = runtime.noSource();
        SourceSet sourceSetOfRequest = typeInfo.compilationUnit().sourceSet();
        TypeInfo logType = compiledTypesManager.getOrLoad(logClass, sourceSetOfRequest);
        assert logType != null;
        TypeInfo logFactory = compiledTypesManager.getOrLoad(logFactoryClassName, sourceSetOfRequest);
        assert logFactory != null;
        TypeInfo expectedParameterType = addGetName ? runtime.stringTypeInfo() : runtime.classTypeInfo();
        ClassExpression typeClass = runtime.newClassExpressionBuilder(typeInfo.asParameterizedType()).setSource(source).build();
        MethodInfo getName = addGetName ? runtime.classTypeInfo().findUniqueMethod("getName", 0) : null;
        Expression logNameOrClass = addGetName ?
                runtime.newMethodCallBuilder()
                        .setSource(source)
                        .setMethodInfo(getName)
                        .setConcreteReturnType(runtime.stringParameterizedType())
                        .setParameterExpressions(List.of())
                        .setObject(typeClass)
                        .build() : typeClass;
        MethodInfo logFactoryMethod = logFactory.methodStream()
                .filter(mi -> mi.name().equals(logFactoryMethodName)
                              && mi.parameters().size() == 1
                              && expectedParameterType.equals(mi.parameters().getFirst().parameterizedType().typeInfo()))
                .findFirst()
                .orElseThrow();
        ParameterizedType logFactoryPt = logFactory.asParameterizedType();
        ParameterizedType logTypePt = logType.asParameterizedType();
        FieldInfo fieldInfo = runtime().newFieldInfo("log", true, logTypePt, typeInfo);
        Expression initializer = runtime.newMethodCallBuilder()
                .setSource(source)
                .setMethodInfo(logFactoryMethod)
                .setParameterExpressions(List.of(logNameOrClass))
                .setObject(runtime.newTypeExpressionBuilder().setParameterizedType(logFactoryPt)
                        .setDiamond(runtime().diamondNo()).build())
                .setConcreteReturnType(logTypePt)
                .build();
        fieldInfo.builder()
                .setSynthetic(true)
                .setSource(source)
                .setInitializer(initializer)
                .addFieldModifier(runtime.fieldModifierFinal())
                .addFieldModifier(runtime.fieldModifierPrivate())
                .addFieldModifier(runtime.fieldModifierStatic())
                .commit();
        typeInfo.builder().addField(fieldInfo);
    }

    @Override
    public void handleField(Data lombokData, FieldInfo fieldInfo) {
        AnnotationExpression getter0 = fieldInfo.builder().haveAnnotation(LOMBOK_DOT + "Getter");
        if (getter0 != null) {
            addGetter(fieldInfo);
        } else if (!fieldInfo.isStatic() && lombokData != null && lombokData.addGetters()) {
            addGetter(fieldInfo);
        }
        AnnotationExpression setter = fieldInfo.builder().haveAnnotation(LOMBOK_DOT + "Setter");
        if (setter != null) {
            addSetter(fieldInfo);
        } else if (!fieldInfo.isStatic() && lombokData != null && lombokData.addSetters()) {
            addSetter(fieldInfo);
        }
        AnnotationExpression nonNull = fieldInfo.builder().haveAnnotation(LOMBOK_DOT + "NonNull");
        if (nonNull != null) {
            runtime.setNonNullProperty(fieldInfo);
        }
    }

    @Override
    public void addConstructors(TypeInfo typeInfo, Data lombokData) {
        if (lombokData.allArgsConstructor()) {
            argsConstructor(typeInfo, false, runtime.methodModifierPublic());
        }
        if (lombokData.requiredArgsConstructor()) {
            argsConstructor(typeInfo, true, runtime.methodModifierPublic());
        }
        if (lombokData.noArgsConstructor() && typeInfo.builder().constructors().stream()
                .noneMatch(mi -> mi.parameters().isEmpty())) {
            Source source = runtime.noSource();
            MethodInfo nac = runtime.newConstructor(typeInfo);
            nac.builder().setMethodBody(runtime().emptyBlock()).addMethodModifier(runtime.methodModifierPublic());
            continueConstructor(typeInfo, nac, source);
        }
        if (lombokData.builder()) {
            createBuilder(typeInfo, lombokData);
        }
    }

    private void argsConstructor(TypeInfo typeInfo, boolean required, MethodModifier access) {
        Source source = runtime.noSource();
        MethodInfo rac = runtime.newConstructor(typeInfo);
        Block.Builder bodyBuilder = runtime().newBlockBuilder().setSource(source);
        List<ParameterizedType> types = new ArrayList<>();
        for (FieldInfo fieldInfo : typeInfo.builder().fields()) {
            if (!fieldInfo.isStatic() && canStillBeAssigned(fieldInfo, required)) {
                ParameterInfo pi = rac.builder().addParameter(fieldInfo.name(), fieldInfo.type());
                pi.builder().setSynthetic(true).setSource(source);
                Statement s = assignFieldToParameter(fieldInfo, source, pi);
                bodyBuilder.addStatement(s);
                types.add(pi.parameterizedType());
            }
        }
        if (typeInfo.constructors().stream().noneMatch(mi -> compatible(mi, types))) {
            rac.builder().setMethodBody(bodyBuilder.build()).addMethodModifier(access);
            continueConstructor(typeInfo, rac, source);
        }
    }

    private boolean compatible(MethodInfo existing, List<ParameterizedType> newTypes) {
        if (existing.parameters().size() != newTypes.size()) return false;
        int i = 0;
        for (ParameterizedType newType : newTypes) {
            ParameterizedType existingTypeErased = existing.parameters().get(i).parameterizedType().erased();
            ParameterizedType newTypeErased = newType.erased();
            if (!existingTypeErased.equals(newTypeErased)) return false;
            ++i;
        }
        return true;
    }

    private boolean canStillBeAssigned(FieldInfo fieldInfo, boolean required) {
        if (required) {
            if (hasInitializer(fieldInfo)) return false;
            if (fieldInfo.isFinal()) return true; // has no initializer
            return !fieldInfo.type().isPrimitiveExcludingVoid() && fieldInfo.isPropertyNotNull();
        }
        return !(fieldInfo.isFinal() && hasInitializer(fieldInfo));
    }

    private boolean hasInitializer(FieldInfo fieldInfo) {
        if (!fieldInfo.hasBeenInspected()) return true;// there is an initializer, to be parsed
        return fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty();
    }

    private void continueConstructor(TypeInfo typeInfo, MethodInfo nac, Source source) {
        nac.builder().commitParameters()
                .setSynthetic(true)
                .setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
                .setAccess(runtime().accessPublic())
                .setSource(source)
                .commit();
        typeInfo.builder().addConstructor(nac);
    }

    private void addGetter(FieldInfo fieldInfo) {
        String getterName = GetSetHelper.getterName(fieldInfo.name(), fieldInfo.type().isBoolean());
        TypeInfo owner = fieldInfo.owner();
        LOGGER.debug("Handle getter for {}", fieldInfo);
        if (owner.methodStream().noneMatch(mi -> getterName.equals(mi.name()) && mi.parameters().isEmpty())) {
            Source source = runtime.noSource();
            FieldReference fr = runtime.newFieldReference(fieldInfo);
            VariableExpression ve = runtime.newVariableExpressionBuilder().setSource(source)
                    .setVariable(fr).build();
            Statement returnStatement = runtime.newReturnBuilder().setSource(source).setExpression(ve).build();
            Block body = runtime.newBlockBuilder().addStatement(returnStatement).setSource(source).build();
            MethodInfo method = runtime.newMethod(owner, getterName,
                    fieldInfo.isStatic() ? runtime.methodTypeStaticMethod() : runtime.methodTypeMethod());
            method.builder()
                    .setMethodBody(body)
                    .setReturnType(fieldInfo.type())
                    .setSynthetic(true)
                    .setSource(source)
                    .commitParameters().commit();
            owner.builder().addMethod(method);

            runtime.setGetSetField(method, fieldInfo, false, -1);
        }
    }

    private void addSetter(FieldInfo fieldInfo) {
        String setterName = GetSetHelper.setterName(fieldInfo.name());
        TypeInfo owner = fieldInfo.owner();
        LOGGER.debug("Handle setter for {}", fieldInfo);
        if (owner.methodStream().noneMatch(mi -> setterName.equals(mi.name()) && mi.parameters().size() == 1)
            && canStillBeAssigned(fieldInfo, false)) {
            Source source = runtime.noSource();
            MethodInfo method = runtime.newMethod(owner, setterName,
                    fieldInfo.isStatic() ? runtime.methodTypeStaticMethod() : runtime.methodTypeMethod());
            ParameterInfo pi = method.builder().addParameter(fieldInfo.name(), fieldInfo.type());
            pi.builder().setSynthetic(true).setSource(source);
            Statement eas = assignFieldToParameter(fieldInfo, source, pi);
            Block body = runtime.newBlockBuilder().addStatement(eas).setSource(source).build();

            method.builder()
                    .setMethodBody(body)
                    .setReturnType(runtime.voidParameterizedType())
                    .setSynthetic(true)
                    .setSource(source)
                    .commitParameters().commit();
            owner.builder().addMethod(method);

            runtime.setGetSetField(method, fieldInfo, true, -1);
        }
    }

    private Statement assignFieldToParameter(FieldInfo fieldInfo, Source source, ParameterInfo pi) {
        FieldReference fr = runtime.newFieldReference(fieldInfo);
        VariableExpression veFr = runtime.newVariableExpressionBuilder().setSource(source).setVariable(fr).build();
        VariableExpression vePi = runtime.newVariableExpressionBuilder().setSource(source).setVariable(pi).build();
        Expression assignment = runtime.newAssignmentBuilder().setTarget(veFr).setValue(vePi).setSource(source).build();
        return runtime.newExpressionAsStatementBuilder().setSource(source).setExpression(assignment).build();
    }

    private void createBuilder(TypeInfo typeInfo, Data lombokData) {
        Source source = runtime.noSource();
        TypeInfo builder = runtime.newTypeInfo(typeInfo, "Builder");
        List<FieldInfo> fields = determineBuilderFields(typeInfo);

        for (FieldInfo fieldInfo : fields) {
            FieldInfo buildField = createBuilderField(builder, source, fieldInfo);
            addSetterToBuilder(builder, source, buildField);
        }
        argsConstructor(builder, true, runtime.methodModifierPrivate());
        createBuildMethod(typeInfo, builder, source);
        builder.builder()
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .setSource(source)
                .setSynthetic(true)
                .addTypeModifier(runtime.typeModifierPublic())
                .addTypeModifier(runtime.typeModifierStatic())
                .setAccess(runtime.accessPublic())
                .commit();
        typeInfo.builder().addSubType(builder);
    }

    private void addSetterToBuilder(TypeInfo builder, Source source, FieldInfo buildField) {
        String setterName = GetSetHelper.setterName(buildField.name());
        MethodInfo method = runtime.newMethod(builder, setterName, runtime.methodTypeMethod());
        ParameterInfo pi = method.builder().addParameter(buildField.name(), buildField.type());
        pi.builder().setSynthetic(true).setSource(source);
        Statement eas = assignFieldToParameter(buildField, source, pi);
        Statement returnStatement = runtime.newReturnBuilder()
                .setSource(source)
                .setExpression(runtime.newVariableExpressionBuilder().setSource(source)
                        .setVariable(runtime.newThis(builder.asParameterizedType()))
                        .build())
                .build();
        Block body = runtime.newBlockBuilder().addStatement(eas).addStatement(returnStatement).setSource(source).build();

        method.builder()
                .setMethodBody(body)
                .setReturnType(runtime.voidParameterizedType())
                .setSynthetic(true)
                .setSource(source)
                .commitParameters().commit();
        builder.builder().addMethod(method);

        runtime.setGetSetField(method, buildField, true, -1);
    }

    private FieldInfo createBuilderField(TypeInfo builder, Source source, FieldInfo fieldInfo) {
        FieldInfo builderField = runtime.newFieldInfo(fieldInfo.name(), false, fieldInfo.type(), builder);
        builderField.builder()
                .setSource(source)
                .setSynthetic(true)
                .addFieldModifier(runtime.fieldModifierPrivate())
                .setInitializer(runtime.newEmptyExpression())
                .setAccess(runtime.accessPrivate())
                .commit();
        builder.builder().addField(builderField);
        return builderField;
    }

    private List<FieldInfo> determineBuilderFields(TypeInfo typeInfo) {
        return typeInfo.builder().fields().stream()
                .filter(f -> !f.isStatic())
                .toList();
    }

    private void createBuildMethod(TypeInfo typeInfo, TypeInfo builder, Source source) {
        MethodInfo build = runtime.newMethod(builder, "build", runtime().methodTypeMethod());
        // TODO add call to constructor
        Block methodBody = runtime().emptyBlock();
        build.builder()
                .setMethodBody(methodBody)
                .setReturnType(typeInfo.asParameterizedType())
                .setSource(source)
                .setSynthetic(true)
                .addMethodModifier(runtime.methodModifierPublic())
                .setAccess(runtime.accessPublic())
                .commitParameters().commit();
        builder.builder().addMethod(build);
    }


}
