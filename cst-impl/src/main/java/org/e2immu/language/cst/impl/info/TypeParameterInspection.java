package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.Access;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public interface TypeParameterInspection extends Inspection {
    List<ParameterizedType> typeBounds();

    boolean typeBoundsAreSet();

    @Override
    default Access access() {
        throw new UnsupportedOperationException("There is no access for type parameters");
    }

    @Override
    default JavaDoc javaDoc() {
        throw new UnsupportedOperationException("There are no javadocs for type parameters");
    }
}
