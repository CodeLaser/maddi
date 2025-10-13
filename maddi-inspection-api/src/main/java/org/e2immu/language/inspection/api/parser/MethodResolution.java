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

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.support.Either;

import java.util.List;
import java.util.Set;

public interface MethodResolution {
    record Count(int parameters, boolean isVoid) {
    }

    GenericsHelper genericsHelper();

    /*
    used for method call erasure,
     */
    Set<ParameterizedType> computeScope(Context context, String index,
                                        String methodName, Object unparsedScope, List<Object> unparsedArguments);

    Expression resolveConstructor(Context context, List<Comment> comments, Source source, String index,
                                  ParameterizedType formalType,
                                  ParameterizedType expectedConcreteType,
                                  Diamond diamond,
                                  Object unparsedObject,
                                  Source unparsedObjectSource,
                                  List<Object> unparsedArguments,
                                  List<ParameterizedType> methodTypeArguments,
                                  boolean complain,
                                  boolean useObjectForUndefinedTypeParameters);

    Expression resolveMethod(Context context,
                             List<Comment> comments,
                             Source source,
                             Source sourceOfName,
                             String index,
                             ForwardType forwardType,
                             String methodName,
                             Object unparsedObject,
                             Source unparsedObjectSource,
                             List<ParameterizedType> methodTypeArguments,
                             DetailedSources.Builder typeArgumentsDetailedSources,
                             List<Object> unparsedArguments);

    Expression resolveMethodReference(Context context, List<Comment> comments, Source source, String index,
                                      ForwardType forwardType,
                                      Expression scope, String methodName);

    Either<Set<Count>, Expression> computeMethodReferenceErasureCounts(Context context, List<Comment> comments, Source source,
                                                                       Expression scope, String methodName);
}
