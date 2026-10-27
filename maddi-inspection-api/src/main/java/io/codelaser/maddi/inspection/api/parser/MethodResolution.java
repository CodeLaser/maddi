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
