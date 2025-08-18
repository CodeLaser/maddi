package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

public interface HierarchyHelper {


    IsMyself isMyself(TypeInfo me, ParameterizedType type);

    IsMyself isMyself(TypeInfo me, TypeInfo bestType);

    boolean nonStaticallyEnclosingTypesContains(TypeInfo me, TypeInfo target);

    boolean parentalHierarchyContains(TypeInfo me, TypeInfo target);

    TypeInfo recursivelyImplements(TypeInfo enclosingType, String s);

    interface IsMyself {

        boolean isNo();

        boolean isYes();

        boolean inSamePrimaryType();

        boolean toFalse(Property property);
    }

}
