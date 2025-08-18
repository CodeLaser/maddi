package org.e2immu.language.cst.api.runtime;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.stream.Stream;

public interface Types {

    TypeInfo getFullyQualified(String name, boolean complain);

    default TypeInfo getFullyQualified(Class<?> clazz, boolean complain) {
        return getFullyQualified(clazz.getCanonicalName(), complain);
    }

    TypeInfo syntheticFunctionalType(int inputParameters, boolean hasReturnValue);

    // separate from getFullyQualified, as these have been preloaded

    AnnotationExpression e2immuAnnotation(String fullyQualifiedName);

    Stream<AnnotationExpression> e2immuAnnotations();

    String e2aAbsent();

    String e2aContract();

    String e2aContent();

    String e2aImplied();

    String e2aHiddenContent();

    String e2aValue();

    String e2aPar();

    String e2aSeq();

    String e2aMulti();

    String e2aAfter();

    String e2aBefore();

    String e2aConstruction();

    String e2aInconclusive();

    String e2aHcParameters();
}
