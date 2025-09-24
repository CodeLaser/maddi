package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.ClassExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
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
    }

    @Override
    public Data handleType(TypeInfo typeInfo) {
        DataImpl d = new DataImpl();
        Map<String, AnnotationExpression> lombokMap = new HashMap<>();
        typeInfo.builder().annotationStream()
                .filter(ae -> ae.typeInfo().fullyQualifiedName().startsWith(LOMBOK_DOT))
                .forEach(ae -> lombokMap.put(ae.typeInfo().fullyQualifiedName().substring(LOMBOK_DOT.length()), ae));
        AnnotationExpression getter0 = lombokMap.get("Getter");
        if (getter0 != null) {
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
    }

    @Override
    public void addConstructors(TypeInfo typeInfo, Data lombokData) {
        if (lombokData.requiredArgsConstructor()) {

        }
        if (lombokData.noArgsConstructor() && typeInfo.builder().constructors().stream()
                .noneMatch(mi -> mi.parameters().isEmpty())) {
            Source source = runtime.noSource();
            MethodInfo nac = runtime.newConstructor(typeInfo);
            nac.builder().setMethodBody(runtime().emptyBlock())
                    .commitParameters()
                    .setSynthetic(true)
                    .setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
                    .setAccess(runtime().accessPublic())
                    .addMethodModifier(runtime.methodModifierPublic())
                    .setSource(source)
                    .commit();
            typeInfo.builder().addConstructor(nac);
        }
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
        }
    }

    private void addSetter(FieldInfo fieldInfo) {
        String setterName = GetSetHelper.setterName(fieldInfo.name());
        TypeInfo owner = fieldInfo.owner();
        LOGGER.debug("Handle setter for {}", fieldInfo);
        if (owner.methodStream().noneMatch(mi -> setterName.equals(mi.name()) && mi.parameters().size() == 1)) {
            Source source = runtime.noSource();
            MethodInfo method = runtime.newMethod(owner, setterName,
                    fieldInfo.isStatic() ? runtime.methodTypeStaticMethod() : runtime.methodTypeMethod());
            ParameterInfo pi = method.builder().addParameter(fieldInfo.name(), fieldInfo.type());
            FieldReference fr = runtime.newFieldReference(fieldInfo);
            VariableExpression veFr = runtime.newVariableExpressionBuilder().setSource(source).setVariable(fr).build();
            VariableExpression vePi = runtime.newVariableExpressionBuilder().setSource(source).setVariable(pi).build();
            Expression assignment = runtime.newAssignmentBuilder().setTarget(veFr).setValue(vePi).setSource(source).build();
            Statement eas = runtime.newExpressionAsStatementBuilder().setSource(source).setExpression(assignment).build();
            Block body = runtime.newBlockBuilder().addStatement(eas).setSource(source).build();

            method.builder()
                    .setMethodBody(body)
                    .setReturnType(runtime.voidParameterizedType())
                    .setSynthetic(true)
                    .setSource(source)
                    .commitParameters().commit();
            owner.builder().addMethod(method);
        }
    }
}
