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

package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;

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
