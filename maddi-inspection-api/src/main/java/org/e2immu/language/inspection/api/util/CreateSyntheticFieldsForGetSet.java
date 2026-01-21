/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.api.util;

import org.e2immu.annotation.Fluent;
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
import org.e2immu.util.internal.util.GetSetNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
this class sits a little out of place in the inspection-api package, but it is currently the only place
that has access to runtime, and can be shared between java-bytecode and java-parser.

The alternative is to put this in Factory/FactoryImpl.
 */

public record CreateSyntheticFieldsForGetSet(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateSyntheticFieldsForGetSet.class);

    private final static String GET_SET_ANNOTATION = GetSet.class.getCanonicalName();

    public void createSyntheticFields(TypeInfo typeInfo) {
        TypeInfo.Builder builder = typeInfo.builder();
        builder.methods().stream().filter(MethodInfo::isAbstract).forEach(mi -> {
            mi.annotations().forEach(ae -> {
                if (GET_SET_ANNOTATION.equals(ae.typeInfo().fullyQualifiedName())) {
                    getSet(typeInfo, mi, ae);
                }
            });
        });
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
                fieldName = GetSetNames.fieldName(mi.name());
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
            boolean list = isList(mi);
            runtime.setGetSetField(mi, getSetField, setter, parameterIndexOfIndex, list);
        }
    }

    public static final String LIST_GET = "java.util.List.get(int)";
    public static final String LIST_SET = "java.util.List.set(int,E)";

    public static boolean isList(MethodInfo methodInfo) {
        return overrideOf(methodInfo, LIST_GET) || overrideOf(methodInfo, LIST_SET);
    }

    public static boolean overrideOf(MethodInfo methodInfo, String fqn) {
        if (fqn.equals(methodInfo.fullyQualifiedName())) return true;
        return methodInfo.overrides().stream().anyMatch(mi -> fqn.equals(mi.fullyQualifiedName()));
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
