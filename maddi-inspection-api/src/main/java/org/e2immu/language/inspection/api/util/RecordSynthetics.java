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

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;

import java.util.List;

public class RecordSynthetics {
    private final Runtime runtime;
    private final TypeInfo typeInfo;

    public RecordSynthetics(Runtime runtime, TypeInfo typeInfo) {
        this.runtime = runtime;
        this.typeInfo = typeInfo;
    }

    public MethodInfo createSyntheticConstructor(Source source, List<FieldInfo> fields, FieldInfo varArgsField) {
        MethodInfo cc = runtime.newConstructor(typeInfo, runtime.methodTypeSyntheticConstructor());
        Block.Builder methodBody = runtime.newBlockBuilder().setSource(source);
        Access publicAccess = runtime.accessPublic();
        This thisVar = runtime.newThis(typeInfo.asParameterizedType());
        int count = 0;
        for (FieldInfo fieldInfo : fields) {
            ParameterInfo pi = cc.builder().addParameter(fieldInfo.name(), fieldInfo.type());
            boolean varargs = fieldInfo == varArgsField;
            pi.builder().setSynthetic(true).setAccess(publicAccess).setVarArgs(varargs).commit();
            Source statementSource = runtime.newParserSource("" + count, 0, 0, 0, 0);
            VariableExpression thisVe = runtime.newVariableExpressionBuilder()
                    .setVariable(thisVar).setSource(statementSource)
                    .build();
            Assignment assignment = runtime.newAssignmentBuilder()
                    .setSource(statementSource)
                    .setValue(runtime.newVariableExpressionBuilder().setVariable(pi).setSource(statementSource).build())
                    .setTarget(runtime.newVariableExpressionBuilder()
                            .setVariable(runtime.newFieldReference(fieldInfo, thisVe, fieldInfo.type()))
                            .setSource(statementSource)
                            .build())
                    .build();
            methodBody.addStatement(runtime.newExpressionAsStatementBuilder()
                    .setExpression(assignment).setSource(statementSource).build());
            ++count;
        }
        cc.builder()
                .commitParameters()
                .addMethodModifier(runtime.methodModifierPublic())
                .setAccess(publicAccess)
                .setSynthetic(true)
                .setMethodBody(methodBody.build())
                .setSource(source)
                .setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor());
        cc.builder().commit();
        return cc;
    }

    public MethodInfo createAccessor(FieldInfo fieldInfo) {
        MethodInfo methodInfo = runtime.newMethod(fieldInfo.owner(), fieldInfo.name(),
                runtime.methodTypeMethod());
        FieldReference fr = runtime.newFieldReference(fieldInfo);
        Source source = runtime.newParserSource("0", 0, 0, 0, 0);
        ReturnStatement rs = runtime.newReturnBuilder()
                .setExpression(runtime.newVariableExpressionBuilder().setVariable(fr).setSource(source).build())
                .setSource(source).build();
        Block methodBody = runtime.newBlockBuilder().setSource(source).addStatement(rs).build();
        MethodInfo.Builder builder = methodInfo.builder();
        builder.setReturnType(fieldInfo.type())
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierPublic())
                .setSynthetic(true)
                .commitParameters()
                .setMethodBody(methodBody);
        // NOT YET COMMITTING! we cannot yet compute the overrides
        runtime.setGetSetField(methodInfo, fieldInfo, false, -1, false);
        return methodInfo;
    }

    public MethodInfo createToString() {
        MethodInfo javaLangObjectToString = runtime.objectTypeInfo().findUniqueMethod("toString", 0);
        MethodInfo methodInfo = runtime.newMethod(typeInfo, "toString", runtime.methodTypeMethod());
        methodInfo.builder()
                .setSynthetic(true)
                .setReturnType(runtime.stringParameterizedType())
                .setMethodBody(runtime.emptyBlock())
                .addOverrides(List.of(javaLangObjectToString))
                .addMethodModifier(runtime.methodModifierPublic())
                .computeAccess();
        // not commiting yet
        return methodInfo;
    }

    public MethodInfo createEquals() {
        MethodInfo javaLangObjectEquals = runtime.objectTypeInfo().findUniqueMethod("equals", 1);
        MethodInfo methodInfo = runtime.newMethod(typeInfo, "equals", runtime.methodTypeMethod());
        ParameterInfo pi = methodInfo.builder().addParameter("o", runtime.objectParameterizedType());
        pi.builder().setIsFinal(false).setVarArgs(false).commit();
        methodInfo.builder()
                .setSynthetic(true)
                .setReturnType(runtime.stringParameterizedType())
                .setMethodBody(runtime.emptyBlock())
                .addOverrides(List.of(javaLangObjectEquals))
                .addMethodModifier(runtime.methodModifierPublic())
                .computeAccess();
        // not commiting yet
        return methodInfo;
    }

    public MethodInfo createHashCode() {
        MethodInfo javaLangHashCode = runtime.objectTypeInfo().findUniqueMethod("hashCode", 0);
        MethodInfo methodInfo = runtime.newMethod(typeInfo, "hashCode", runtime.methodTypeMethod());
        methodInfo.builder()
                .setSynthetic(true)
                .setReturnType(runtime.intParameterizedType())
                .setMethodBody(runtime.emptyBlock())
                .addOverrides(List.of(javaLangHashCode))
                .addMethodModifier(runtime.methodModifierPublic())
                .computeAccess();
        // not commiting yet
        return methodInfo;
    }
}
