package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeModifier;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.api.info.TypeParameter;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface TypeInspection extends Inspection {

    List<FieldInfo> fields();

    Set<TypeInfo> superTypesExcludingJavaLangObject();

    List<TypeParameter> typeParameters();

    boolean isAbstract();

    Stream<MethodInfo> methodStream();

    List<MethodInfo> constructors();

    TypeNature typeNature();

    /**
     * only returns null for JLO, primitives.
     *
     * @return the parent of this type
     */
    ParameterizedType parentClass();

    List<ParameterizedType> interfacesImplemented();

    MethodInfo singleAbstractMethod();

    List<TypeInfo> subTypes();

    Set<TypeModifier> modifiers();

    boolean fieldsAccessedInRestOfPrimaryType();

    MethodInfo enclosingMethod();

    List<TypeInfo> permittedWhenSealed();

    int anonymousTypes();

    boolean isFinal();
}
