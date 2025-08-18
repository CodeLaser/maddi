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
