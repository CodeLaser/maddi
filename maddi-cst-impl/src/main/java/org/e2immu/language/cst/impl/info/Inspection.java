package org.e2immu.language.cst.impl.info;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.Access;

import java.util.List;

public interface Inspection {

    Access access();

    default boolean accessNotYetComputed() {
        return access() == null;
    }

    List<Comment> comments();

    Source source();

    boolean isSynthetic();

    default boolean isPublic() {
        return access().isPublic();
    }

    default boolean isPrivate() {
        return access().isPrivate();
    }

    default boolean isProtected() {
        return access().isProtected();
    }

    default boolean isPackagePrivate() {
        return access().isPackage();
    }

    @NotNull(content = true)
    List<AnnotationExpression> annotations();

    JavaDoc javaDoc();
}
