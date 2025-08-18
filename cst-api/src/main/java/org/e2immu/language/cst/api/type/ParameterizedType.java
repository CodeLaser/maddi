package org.e2immu.language.cst.api.type;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.runtime.PredefinedWithoutParameterizedType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface ParameterizedType {

    ParameterizedType applyTranslation(PredefinedWithoutParameterizedType predefined,
                                       Map<NamedType, ParameterizedType> translate);

    ParameterizedType rewire(InfoMap infoMap, Map<TypeParameter, TypeParameter> rewiredTypeParameters);

    default ParameterizedType rewire(InfoMap infoMap) {
        return rewire(infoMap, Map.of());
    }

    int arrays();

    // initially implemented to ensure that a NULL type doesn't overwrite a valid type, see Lambda_17
    ParameterizedType bestDefined(ParameterizedType other);
    // the different components

    /**
     * @return null in case of an unbound type parameter, in which case "JLO" can be understood.
     */
    TypeInfo bestTypeInfo();

    ParameterizedType concreteDirectSuperType(ParameterizedType interfaceImplemented);

    ParameterizedType concreteSuperType(ParameterizedType superType);

    ParameterizedType copyWithArrays(int arrays);

    ParameterizedType copyWithFewerArrays(int n);

    ParameterizedType copyWithOneFewerArrays();

    ParameterizedType copyWithoutArrays();

    // print

    String fullyQualifiedName();

    String detailedString();

    boolean isByte();

    boolean isDouble();

    Boolean isFloat();

    boolean isJavaUtilList();

    boolean isLong();

    boolean isShort();

    // Qualification: SimpleNames, vararg: false, Diamond: ShowAll
    String simpleString();

    OutputBuilder print(Qualification qualification, boolean varArgs, Diamond diamond);

    String printForMethodFQN(boolean varArgs, Diamond diamond);

    //

    ParameterizedType ensureBoxed(Predefined runtime);

    // conversions

    ParameterizedType ensureBoxed(PredefinedWithoutParameterizedType predefined);

    boolean equalsIgnoreArrays(ParameterizedType other);

    ParameterizedType erased();

    Set<TypeParameter> extractTypeParameters();

    /*
    HashMap<K, V> implements Map<K, V>
    Given Map<K, V>, go from abstract to concrete (HM:K to Map:K, HM:V to Map:V)
    */
    Map<NamedType, ParameterizedType> forwardTypeParameterMap();

    /*
       Given a concrete type (List<String>) make a map from the type's abstract parameters to its concrete ones (E -> String)

       If the abstract type contains self-references, we cannot recurse, because their type parameters have the same name...
       With visited, the method returns K=Integer, V=Map<Integer,String> when presented with Map<Integer,Map<Integer,String>>,
       without visited, it would recurse and return K=Integer, V=String
       */
    Map<NamedType, ParameterizedType> initialTypeParameterMap();

    // simple checks

    // TODO consider moving this to "runtime"
    boolean isAssignableFrom(Predefined runtime, ParameterizedType other);

    int numericIsAssignableFrom(Predefined runtime, ParameterizedType other);

    boolean isBoolean();

    boolean isBooleanOrBoxedBoolean();

    boolean isBoxedExcludingVoid();

    boolean isFunctionalInterface();

    boolean isInt();

    boolean isJavaLangObject();

    boolean isJavaLangString();

    boolean isMathematicallyInteger();

    boolean isNoTypeGivenInLambda();

    boolean isNumeric();

    boolean isPrimitiveExcludingVoid();

    boolean isPrimitiveStringClass();

    boolean isReturnTypeOfConstructor();

    default boolean isType() {
        return typeInfo() != null;
    }

    boolean isTypeOfNullConstant();

    boolean isTypeParameter();

    boolean isUnboundTypeParameter();

    boolean isUnboundWildcard();

    boolean isVoid();

    boolean isVoidOrJavaLangVoid();

    boolean isWILDCARD_PARAMETERIZED_TYPE();

    ParameterizedType mostSpecific(Predefined runtime,
                                   TypeInfo primaryType,
                                   ParameterizedType other);

    List<ParameterizedType> parameters();

    List<ParameterizedType> replaceByTypeBounds();

    TypeInfo toBoxed(PredefinedWithoutParameterizedType runtime);

    TypeInfo typeInfo();

    TypeParameter typeParameter();

    default NamedType namedType() {
        TypeInfo typeInfo = typeInfo();
        return typeInfo == null ? typeParameter() : typeInfo;
    }

    Stream<ParameterizedType> components();

    Stream<Element.TypeReference> typesReferenced();

    Stream<Element.TypeReference> typesReferenced(boolean explicit, Set<TypeParameter> visited);

    Stream<Element.TypeReference> typesReferencedMadeExplicit();

    Wildcard wildcard();

    ParameterizedType withParameters(List<ParameterizedType> parameterizedTypes);

    ParameterizedType withWildcard(Wildcard wildcard);

    boolean hasTypeParameters();

    ParameterizedType replaceTypeParameter(TypeParameter oldTp, TypeParameter newPt);

    Map<NamedType, ParameterizedType> formalToConcrete(ParameterizedType forwardedReturnType);

    boolean typeBoundsAreSet(TypeParameter self);

}
