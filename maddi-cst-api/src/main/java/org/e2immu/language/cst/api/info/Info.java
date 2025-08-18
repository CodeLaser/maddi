package org.e2immu.language.cst.api.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;

public interface Info extends Element {

    String info();

    Access access();

    CompilationUnit compilationUnit();

    String simpleName();

    String fullyQualifiedName();

    boolean isSynthetic();

    TypeInfo typeInfo();

    boolean hasBeenAnalyzed();

    JavaDoc javaDoc();

    List<? extends Info> translate(TranslationMap translationMap);

    interface Builder<B extends Builder<?>> extends Element.Builder<B> {
        @Fluent
        B setAccess(Access access);

        @Fluent
        B setAnnotationExpression(int index, AnnotationExpression annotationExpression);

        @Fluent
        B setSynthetic(boolean synthetic);

        boolean hasBeenCommitted();

        // once all the modifiers have been set
        @Fluent
        B computeAccess();

        @Fluent
        B setJavaDoc(JavaDoc javaDoc);

        void commit();
    }

}
