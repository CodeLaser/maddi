package org.e2immu.language.inspection.api.util;

import org.e2immu.annotation.Fluent;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.method.GetSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.util.internal.util.GetSetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
this class sits a little out of place in the inspection-api package, but it is currently the only place
that has access to runtime, and can be shared between java-bytecode and java-parser.

The alternative is to put this in Factory/FactoryImpl.
 */

public class GetSetUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetSetUtil.class);
    private final Runtime runtime;

    public GetSetUtil(Runtime runtime) {
        this.runtime = runtime;
    }

    private final static String modifiedAnnotation = Modified.class.getCanonicalName();
    private final static String GET_SET_ANNOTATION = GetSet.class.getCanonicalName();


    public void createSyntheticFields(TypeInfo typeInfo) {
        TypeInfo.Builder builder = typeInfo.builder();
        builder.methods().stream().filter(MethodInfo::isAbstract).forEach(mi -> {
            String miFqn = mi.fullyQualifiedName();
            if ("java.util.List.get(int)".equals(miFqn) || "java.util.List.set(int,E)".equals(miFqn)) {
                getSet(typeInfo, mi, false, "_synthetic_list");
            } else {
                mi.annotations().forEach(ae -> {
                    if (modifiedAnnotation.equals(ae.typeInfo().fullyQualifiedName())) {
                        modifiedComponents(typeInfo, mi, ae);
                    }
                    if (GET_SET_ANNOTATION.equals(ae.typeInfo().fullyQualifiedName())) {
                        getSet(typeInfo, mi, ae);
                    }
                });
            }
        });
    }

    private void modifiedComponents(TypeInfo typeInfo, MethodInfo mi, AnnotationExpression modified) {
        String fieldName = modified.extractString("value", "");
        if (!fieldName.isBlank()) {
            FieldInfo fieldInfo = typeInfo.builder().fields().stream()
                    .filter(f -> fieldName.equals(f.name())).findFirst().orElse(null);
            FieldInfo component;
            if (fieldInfo == null) {
                LOGGER.debug("Create synthetic field for {}, named {}", mi, fieldName);
                FieldInfo syntheticField = runtime.newFieldInfo(fieldName, false,
                        runtime.objectParameterizedType(), typeInfo);
                syntheticField.builder()
                        .setSynthetic(true)
                        .setInitializer(runtime.newEmptyExpression())
                        .addFieldModifier(runtime.fieldModifierPrivate())
                        .computeAccess()
                        .commit();
                typeInfo.builder().addField(syntheticField);
                component = syntheticField;
            } else {
                component = fieldInfo;
            }
            runtime.setModificationComponent(mi, component);
        }
    }

    private void getSet(TypeInfo typeInfo, MethodInfo mi, AnnotationExpression getSet) {
        getSet(typeInfo, mi, getSet.extractBoolean("equivalent"), getSet.extractString("value", ""));
    }

    private void getSet(TypeInfo typeInfo, MethodInfo mi, boolean equivalent, String proposed) {
        if (!mi.isFactoryMethod() && !equivalent) {
            String fieldName;
            if (!proposed.isBlank()) {
                fieldName = proposed.trim();
            } else {
                fieldName = GetSetHelper.fieldName(mi.name());
            }
            FieldInfo fieldInfo = typeInfo.builder().fields().stream()
                    .filter(f -> fieldName.equals(f.name())).findFirst().orElse(null);
            FieldInfo getSetField;
            boolean setter = isSetter(mi);
            int parameterIndexOfIndex = parameterIndexOfIndex(mi, setter);
            if (fieldInfo == null) {
                LOGGER.debug("Create synthetic field for {}, named {}", mi, fieldName);
                ParameterizedType type = extractFieldType(mi, setter, parameterIndexOfIndex);
                FieldInfo syntheticField = runtime.newFieldInfo(fieldName, false, type, typeInfo);
                syntheticField.builder()
                        .setSynthetic(true)
                        .setInitializer(runtime.newEmptyExpression())
                        .addFieldModifier(runtime.fieldModifierPrivate())
                        .computeAccess()
                        .commit();
                typeInfo.builder().addField(syntheticField);
                getSetField = syntheticField;
            } else {
                getSetField = fieldInfo;
            }
            runtime.setGetSetField(mi, getSetField, setter, parameterIndexOfIndex);
        }
    }

    public static boolean isSetter(MethodInfo mi) {
        // there could be an accessor called "set()", so for that to be a setter, it must have at least one parameter
        return mi.isVoid() || isComputeFluent(mi) || mi.name().startsWith("set") && !mi.parameters().isEmpty();
    }

    public static boolean isComputeFluent(MethodInfo mi) {
        String fluentFqn = Fluent.class.getCanonicalName();
        if (mi.annotations().stream().anyMatch(ae -> fluentFqn.equals(ae.typeInfo().fullyQualifiedName()))) {
            return true;
        }
        return !mi.methodBody().isEmpty()
               && mi.methodBody().lastStatement() instanceof ReturnStatement rs
               && rs.expression() instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    public static int parameterIndexOfIndex(MethodInfo mi, boolean setter) {
        if (setter) {
            if (2 == mi.parameters().size()) {
                if (mi.parameters().getFirst().parameterizedType().isInt()) return 0;
                if (mi.parameters().get(1).parameterizedType().isInt()) return 1;
            }
            return -1;
        }
        // getter
        return mi.parameters().size() == 1 && mi.parameters().getFirst().parameterizedType().isInt() ? 0 : -1;
    }

    private static ParameterizedType extractFieldType(MethodInfo mi, boolean setter, int parameterIndexOfIndex) {
        if (mi.parameters().isEmpty()) {
            // T getT()
            assert !setter;
            return mi.returnType();
        }
        if (mi.parameters().size() == 1) {
            if (setter) {
                // void setT(T t)
                return mi.parameters().getFirst().parameterizedType();
            }
            if (mi.parameters().getFirst().parameterizedType().isInt()) {
                // T getT(int i)   INDEXED
                int a = mi.returnType().arrays();
                return mi.returnType().copyWithArrays(a + 1);
            }
            // Builder newBuilder(URI uri)
            throw new UnsupportedOperationException();
        }
        if (mi.parameters().size() == 2 && mi.parameters().get(parameterIndexOfIndex).parameterizedType().isInt()) {
            // void setObject(int i, Object o)  INDEXED
            assert setter;
            ParameterizedType p1 = mi.parameters().get(1 - parameterIndexOfIndex).parameterizedType();
            int a = p1.arrays();
            return mi.returnType().copyWithArrays(a + 1);
        }
        throw new UnsupportedOperationException();
    }
}
