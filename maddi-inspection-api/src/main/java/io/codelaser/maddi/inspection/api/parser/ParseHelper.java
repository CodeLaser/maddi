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

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.Statement;

import java.util.List;

public interface ParseHelper {

    List<AnnotationExpression.KV> parseAnnotationExpression(TypeInfo annotationType, Object annotation, Context context);

    Expression parseExpression(Context context, String index, ForwardType forward, Object expression);

    JavaDoc.Tag parseJavaDocReferenceInTag(Context context, Info info, JavaDoc.Tag tag);

    void resolveMethodInto(MethodInfo.Builder methodInfoBuilder, Context context, ForwardType forwardType,
                           Object eci, Object expression, List<Statement> recordAssignments);
}
