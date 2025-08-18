package org.e2immu.analyzer.modification.common.defaults;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.AnnotationExpression;

import java.util.List;

public interface AnnotationProvider {

    List<AnnotationExpression> annotations(Element element);

}
