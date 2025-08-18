package org.e2immu.language.cst.api.info;

import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.element.Keyword;

public interface MethodModifier {

   default boolean isAccessModifier() {
       return isPublic() || isPrivate() || isProtected();
   }

    boolean isPublic();

    boolean isPrivate();

    boolean isProtected();

    boolean isAbstract();

    boolean isDefault();

    boolean isSynchronized();

    boolean isFinal();

    boolean isStatic();

    Keyword keyword();
}
