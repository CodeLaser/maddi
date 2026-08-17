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

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.type.Diamond;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.support.Either;

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
