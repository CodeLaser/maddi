package org.e2immu.language.cst.api.element;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public interface DetailedSources {
    // used to grab the closing parenthesis of the record field list,
    // the closing parenthesis of any method declaration's form parameter list
    Object END_OF_PARAMETER_LIST = new Object();
    // marker for the "extends" keyword, see TypeInfo.hasImplicitParent()
    Object EXTENDS = new Object();

    Source detail(Object object);

    // use for typeInfo objects when the detailed sources contain parameterized types, where the same typeInfo object
    // can occur multiple times
    @NotNull
    List<Source> details(Object object);

    DetailedSources merge(DetailedSources other);

    // parameterized types use this as the copyWithoutArrays
    // in this way, we can have the sources of the type without the []
    // see ParseType
    Object associatedObject(Object object);

    DetailedSources withSources(Object o, List<Source> sources);

    interface Builder {

        Builder addAll(DetailedSources detailedSources);

        Builder copy();

        Object getAssociated(Object pt);

        Builder put(Object object, Source source);

        DetailedSources build();

        // associated object
        Builder putWithArrayToWithoutArray(ParameterizedType withArray, ParameterizedType withoutArray);

        // associated object
        record TypeInfoSource(TypeInfo typeInfo, Source source) {}
        Builder putTypeQualification(TypeInfo typeInfo, List<TypeInfoSource> associatedList);
    }
}
