/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;

import java.util.List;

public interface Resolver {
    void add(Info info,
             Info.Builder<?> infoBuilder,
             ForwardType forwardType,
             Object explicitConstructorInvocation,
             Object toResolve,
             Context newContext,
             List<Statement> recordAssignments);

    void addRecordField(FieldInfo recordField);

    void add(TypeInfo.Builder builder);

    void addAnnotationTodo(Info.Builder<?> infoBuilder,
                           TypeInfo annotationType,
                           AnnotationExpression.Builder ab,
                           int indexInAnnotationList,
                           Object annotation,
                           Context context);

    // add to the to-do list, but only for overrides
    void addRecordAccessor(MethodInfo accessor);

    void addJavadoc(Info info, Info.Builder<?> infoBuilder, Context context, JavaDoc javaDoc);

    Resolver newEmpty();

    void resolve(boolean primary);

    ParseHelper parseHelper();
}
