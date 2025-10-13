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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.FieldReference;

import java.util.List;

class RecordSynthetics {
    private final Runtime runtime;
    private final TypeInfo typeInfo;

    RecordSynthetics(Runtime runtime, TypeInfo typeInfo) {
        this.runtime = runtime;
        this.typeInfo = typeInfo;
    }

    MethodInfo createSyntheticConstructor(Source source, List<ParseTypeDeclaration.RecordField> recordFields) {
        MethodInfo cc = runtime.newConstructor(typeInfo, runtime.methodTypeSyntheticConstructor());
        Block.Builder methodBody = runtime.newBlockBuilder().setSource(source);
        Access publicAccess = runtime.accessPublic();
        int count = 0;
        for (ParseTypeDeclaration.RecordField rf : recordFields) {
            FieldInfo fieldInfo = rf.fieldInfo();
            ParameterInfo pi = cc.builder().addParameter(fieldInfo.name(), fieldInfo.type());
            pi.builder().setSynthetic(true).setAccess(publicAccess).setVarArgs(rf.varargs()).commit();
            Source fieldSource = fieldInfo.source();
            Source statementSource = runtime.newParserSource("" + count, fieldSource.beginLine(),
                    fieldSource.beginPos(), fieldSource.endLine(), fieldSource.endPos());
            Assignment assignment = runtime.newAssignmentBuilder()
                    .setSource(statementSource)
                    .setValue(runtime.newVariableExpressionBuilder().setVariable(pi).setSource(statementSource).build())
                    .setTarget(runtime.newVariableExpressionBuilder()
                            .setVariable(runtime.newFieldReference(fieldInfo))
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

    List<MethodInfo> createAccessors(List<ParseTypeDeclaration.RecordField> recordFields) {
        return recordFields.stream().map(this::createAccessor).toList();
    }

    private MethodInfo createAccessor(ParseTypeDeclaration.RecordField rf) {
        FieldInfo fieldInfo = rf.fieldInfo();
        MethodInfo methodInfo = runtime.newMethod(fieldInfo.owner(), fieldInfo.name(),
                runtime.methodTypeMethod());
        FieldReference fr = runtime.newFieldReference(fieldInfo);
        Source fieldSource = fieldInfo.source();
        Source source = runtime.newParserSource("0", fieldSource.beginLine(),
                fieldSource.beginPos(), fieldSource.endLine(), fieldSource.endPos());
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
        runtime.setGetSetField(methodInfo, fieldInfo, false, -1);
        return methodInfo;
    }
}
