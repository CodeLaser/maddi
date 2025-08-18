package org.e2immu.language.cst.impl.type;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.runtime.PredefinedWithoutParameterizedType;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.output.QualificationImpl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParameterizedTypeImpl implements ParameterizedType {
    public static final ParameterizedType NULL_CONSTANT = new ParameterizedTypeImpl();
    public static final ParameterizedType RETURN_TYPE_OF_CONSTRUCTOR = new ParameterizedTypeImpl();
    public static final ParameterizedType NO_TYPE_GIVEN_IN_LAMBDA = new ParameterizedTypeImpl();
    public static final ParameterizedType WILDCARD_PARAMETERIZED_TYPE = new ParameterizedTypeImpl(WildcardEnum.UNBOUND);
    public static final ParameterizedType TYPE_OF_EMPTY_EXPRESSION = new ParameterizedTypeImpl();

    private final TypeParameter typeParameter;
    private final TypeInfo typeInfo;
    private final int arrays;
    private final Wildcard wildcard;
    private final List<ParameterizedType> parameters;

    public ParameterizedTypeImpl(TypeParameter typeParameter, int arrays) {
        this(null, typeParameter, List.of(), arrays, null);
    }

    public ParameterizedTypeImpl(TypeInfo typeInfo) {
        this(typeInfo, null, List.of(), 0, null);
    }

    public ParameterizedTypeImpl(TypeInfo typeInfo, int arrays) {
        this(typeInfo, null, List.of(), arrays, null);
    }

    public ParameterizedTypeImpl(TypeInfo typeInfo, List<ParameterizedType> parameters) {
        this(typeInfo, null, parameters, 0, null);
    }

    public ParameterizedTypeImpl(TypeParameter typeParameter, Wildcard wildcard) {
        this(null, typeParameter, List.of(), 0, wildcard);
    }

    public ParameterizedTypeImpl(TypeInfo typeInfo, Wildcard wildcard) {
        this(typeInfo, null, List.of(), 0, wildcard);
    }

    public ParameterizedTypeImpl(Wildcard wildcard) {
        this(null, null, List.of(), 0, wildcard);
    }

    public ParameterizedTypeImpl() {
        this(null, null, List.of(), 0, null);
    }

    public ParameterizedTypeImpl(TypeInfo typeInfo,
                                 TypeParameter typeParameter,
                                 List<ParameterizedType> parameters,
                                 int arrays,
                                 Wildcard wildcard) {
        this.typeParameter = typeParameter;
        this.typeInfo = typeInfo;
        this.arrays = arrays;
        this.wildcard = wildcard;
        this.parameters = parameters;
        assert parameters.stream().noneMatch(ParameterizedType::isPrimitiveExcludingVoid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterizedTypeImpl that = (ParameterizedTypeImpl) o;
        return arrays == that.arrays
               && Objects.equals(typeParameter, that.typeParameter)
               && Objects.equals(typeInfo, that.typeInfo)
               && Objects.equals(wildcard, that.wildcard)
               && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeParameter, typeInfo, arrays, wildcard, parameters);
    }

    public OutputBuilder print(Qualification qualification) {
        return ParameterizedTypePrinter.print(qualification, this,
                false, DiamondEnum.SHOW_ALL, false);
    }

    @Override
    public Wildcard wildcard() {
        return wildcard;
    }

    @Override
    public TypeParameter typeParameter() {
        return typeParameter;
    }

    @Override
    public TypeInfo typeInfo() {
        return typeInfo;
    }

    @Override
    public int arrays() {
        return arrays;
    }

    @Override
    public List<ParameterizedType> parameters() {
        return parameters;
    }

    @Override
    public String fullyQualifiedName() {
        return printForMethodFQN(false, DiamondEnum.SHOW_ALL);
    }

    public OutputBuilder print(Qualification qualification, boolean varArgs, Diamond diamond) {
        return ParameterizedTypePrinter.print(qualification, this, varArgs, diamond, false);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        if (typeInfo != null) {
            Stream<Element.TypeReference> s1 = Stream.of(new ElementImpl.TypeReference(typeInfo, false));
            return Stream.concat(s1, parameters.stream().flatMap(ParameterizedType::typesReferenced));
        }
        if (typeParameter != null) {
            return typeParameter.typesReferenced(false, new HashSet<>());
        }
        return Stream.of();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(boolean explicit, Set<TypeParameter> visited) {
        if (typeInfo != null) {
            Stream<Element.TypeReference> s1 = Stream.of(new ElementImpl.TypeReference(typeInfo, explicit));
            return Stream.concat(s1, parameters.stream()
                    .flatMap(pt -> pt.typesReferenced(explicit, visited)));
        }
        if (typeParameter != null) {
            return typeParameter.typesReferenced(false, visited);
        }
        return Stream.of();
    }

    @Override
    public Stream<ParameterizedType> components() {
        return Stream.concat(Stream.of(this), parameters.stream().flatMap(ParameterizedType::components));
    }

    @Override
    public Stream<Element.TypeReference> typesReferencedMadeExplicit() {
        return typesReferenced().map(Element.TypeReference::typeInfo).filter(Objects::nonNull)
                .map(ti -> new ElementImpl.TypeReference(ti, true));
    }

    @Override
    public ParameterizedType erased() {
        if (arrays == 0 && wildcard == null && parameters.isEmpty()) return this;
        return new ParameterizedTypeImpl(typeInfo, typeParameter, List.of(), arrays, null);
    }

    @Override
    public Set<TypeParameter> extractTypeParameters() {
        if (typeParameter != null) return Set.of(typeParameter);
        if (typeInfo != null) {
            return parameters.stream().flatMap(p -> p.extractTypeParameters().stream()).collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    @Override
    public ParameterizedType copyWithArrays(int arrays) {
        // the following check is important to maintain object '==' for the static types like NULL_CONSTANT
        if (arrays == this.arrays) return this;
        assert arrays >= 0;
        return new ParameterizedTypeImpl(typeInfo, typeParameter, parameters, arrays, wildcard);
    }

    @Override
    public ParameterizedType copyWithOneFewerArrays() {
        assert arrays > 0;
        return new ParameterizedTypeImpl(typeInfo, typeParameter, parameters, arrays - 1, wildcard);
    }

    @Override
    public ParameterizedType ensureBoxed(PredefinedWithoutParameterizedType predefined) {
        if (isPrimitiveExcludingVoid() || isVoid()) {
            return toBoxed(predefined).asSimpleParameterizedType();
        }
        return this;
    }

    @Override
    public ParameterizedType copyWithFewerArrays(int n) {
        assert arrays >= n;
        return new ParameterizedTypeImpl(typeInfo, typeParameter, parameters, arrays - n, wildcard);
    }

    @Override
    public ParameterizedType copyWithoutArrays() {
        return new ParameterizedTypeImpl(typeInfo, typeParameter, parameters, 0, wildcard);
    }

    @Override
    public boolean isBoolean() {
        return arrays == 0 && typeInfo != null && typeInfo.isBoolean();
    }

    @Override
    public boolean isTypeParameter() {
        return typeParameter != null;
    }

    @Override
    public boolean isVoidOrJavaLangVoid() {
        if (isVoid()) return true;
        return arrays == 0 && typeInfo != null && typeInfo.isJavaLangVoid();
    }

    @Override
    public boolean isBooleanOrBoxedBoolean() {
        if (arrays != 0) return false;
        return typeInfo != null && (typeInfo.isBoolean() || typeInfo.isBoxedBoolean());
    }

    @Override
    public boolean isVoid() {
        if (this == TYPE_OF_EMPTY_EXPRESSION) return true;
        return arrays == 0 && typeInfo != null && typeInfo.isVoid();
    }

    @Override
    public boolean isPrimitiveExcludingVoid() {
        return arrays == 0 && typeInfo != null && typeInfo.isPrimitiveExcludingVoid();
    }

    @Override
    public boolean isPrimitiveStringClass() {
        return arrays == 0 && typeInfo != null
               && (typeInfo.isPrimitiveExcludingVoid() || typeInfo.isJavaLangString() || typeInfo.isJavaLangClass());
    }

    @Override
    public boolean isJavaLangString() {
        return arrays == 0 && typeInfo != null && typeInfo.isJavaLangString();
    }

    @Override
    public boolean isInt() {
        return arrays == 0 && typeInfo != null && typeInfo.isInt();
    }

    @Override
    public boolean isByte() {
        return arrays == 0 && typeInfo != null && typeInfo.isByte();
    }

    @Override
    public boolean isLong() {
        return arrays == 0 && typeInfo != null && typeInfo.isLong();
    }

    @Override
    public boolean isShort() {
        return arrays == 0 && typeInfo != null && typeInfo.isShort();
    }

    @Override
    public boolean isDouble() {
        return arrays == 0 && typeInfo != null && typeInfo.isDouble();
    }

    @Override
    public Boolean isFloat() {
        return arrays == 0 && typeInfo != null && typeInfo.isFloat();
    }

    @Override
    public boolean isJavaLangObject() {
        return arrays == 0 && typeInfo != null && typeInfo.isJavaLangObject();
    }

    @Override
    public boolean isFunctionalInterface() {
        if (typeInfo == null || arrays > 0) return false;
        return typeInfo.isFunctionalInterface();
    }

    @Override
    public boolean isBoxedExcludingVoid() {
        return arrays == 0 && typeInfo != null && typeInfo.isBoxedExcludingVoid();
    }

    @Override
    public boolean isWILDCARD_PARAMETERIZED_TYPE() {
        return this == WILDCARD_PARAMETERIZED_TYPE;
    }

    @Override
    public boolean isReturnTypeOfConstructor() {
        return this == RETURN_TYPE_OF_CONSTRUCTOR;
    }

    @Override
    public boolean isNumeric() {
        return arrays == 0 && typeInfo != null && typeInfo.isNumeric();
    }

    @Override
    public boolean isUnboundTypeParameter() {
        return arrays == 0
               && typeParameter != null
               && (typeParameter.typeBounds().isEmpty()
                   || typeParameter.typeBounds().size() == 1 && typeParameter.typeBounds().getFirst().isJavaLangObject());
    }

    @Override
    public boolean isAssignableFrom(Predefined runtime, ParameterizedType other) {
        return new IsAssignableFrom(runtime, this, other).execute();
    }

    @Override
    public int numericIsAssignableFrom(Predefined runtime, ParameterizedType other) {
        return new IsAssignableFrom(runtime, this, other).execute(false, false,
                IsAssignableFrom.Mode.COVARIANT);
    }

    @Override
    public String detailedString() {
        return printForMethodFQN(false, DiamondEnum.SHOW_ALL);
    }

    @Override
    public String simpleString() {
        return print(QualificationImpl.SIMPLE_NAMES, false, DiamondEnum.SHOW_ALL).toString();
    }

    @Override
    public TypeInfo toBoxed(PredefinedWithoutParameterizedType runtime) {
        return runtime.boxed(typeInfo);
    }

    public ParameterizedType ensureBoxed(Predefined runtime) {
        if (isPrimitiveExcludingVoid() || isVoid()) {
            return toBoxed(runtime).asSimpleParameterizedType();
        }
        return this;
    }

    @Override
    public String printForMethodFQN(boolean varArgs, Diamond diamond) {
        return ParameterizedTypePrinter.print(QualificationImpl.FULLY_QUALIFIED_NAMES,
                this, varArgs, diamond, false, true).toString();
    }

    @Override
    public boolean equalsIgnoreArrays(ParameterizedType other) {
        return Objects.equals(typeInfo, other.typeInfo())
               && Objects.equals(typeParameter, other.typeParameter())
               && Objects.equals(parameters, other.parameters())
               && Objects.equals(wildcard, other.wildcard());
    }

    @Override
    public boolean isTypeOfNullConstant() {
        return this == NULL_CONSTANT;
    }

    @Override
    public boolean isUnboundWildcard() {
        return typeInfo == null && typeParameter == null && wildcard != null && wildcard.isUnbound();
    }

    @Override
    public ParameterizedType concreteSuperType(ParameterizedType superType) {
        TypeInfo bestType = bestTypeInfo();
        if (bestType == null) {
            assert superType.isJavaLangObject();
            return superType;
        }
        if (bestType == superType.typeInfo()) {
            // if we start with Iterable<String>, and we're aiming for Iterable<E>, then
            // Iterable<String> is the right answer
            return this;
        }
        ParameterizedType parentClass = bestType.parentClass();
        if (parentClass != null && !parentClass.isJavaLangObject()) {
            if (parentClass.typeInfo() == superType.typeInfo()) {
                return concreteDirectSuperType(parentClass);
            }
            /* do a recursion, but accept that we may return null
            we must call concreteSuperType on a concrete version of the parentClass
            */
            ParameterizedType res = parentClass.concreteSuperType(superType);
            if (res != null) {
                return concreteDirectSuperType(res);
            }
        }
        for (ParameterizedType interfaceType : bestType.interfacesImplemented()) {
            if (interfaceType.typeInfo() == superType.typeInfo()) {
                return concreteDirectSuperType(interfaceType);
            }
            // similar to parent
            ParameterizedType res = interfaceType.concreteSuperType(superType);
            if (res != null) {
                return concreteDirectSuperType(res);
            }
        }
        return null;
    }

    @Override
    public ParameterizedType concreteDirectSuperType(ParameterizedType parentType) {
        if (parentType.parameters().isEmpty()) return parentType;

        Map<NamedType, ParameterizedType> map = initialTypeParameterMap();
        ParameterizedType formalType = parentType.typeInfo().asParameterizedType();
        List<ParameterizedType> newParameters = new ArrayList<>(formalType.parameters().size());
        int i = 0;
        for (ParameterizedType param : formalType.parameters()) {
            ParameterizedType formalInParentType = parentType.parameters().get(i);
            ParameterizedType result;
            if (formalInParentType.typeInfo() != null) {
                result = formalInParentType;
            } else if (formalInParentType.typeParameter() != null) {
                result = map.get(formalInParentType.typeParameter());
            } else {
                result = param;
            }
            newParameters.add(result == null ? param : result);
            i++;
        }
        return new ParameterizedTypeImpl(parentType.typeInfo(), List.copyOf(newParameters));
    }

    @Override
    public Map<NamedType, ParameterizedType> initialTypeParameterMap() {
        if (!isType()) return Map.of();
        if (parameters.isEmpty()) return Map.of();
        return initialTypeParameterMap(new HashSet<>());
    }

    private Map<NamedType, ParameterizedType> initialTypeParameterMap(Set<TypeInfo> visited) {
        if (!isType()) return Map.of();
        visited.add(typeInfo);
        if (parameters.isEmpty()) return Map.of();
        ParameterizedType originalType = typeInfo.asParameterizedType();
        int i = 0;
        // linkedHashMap to maintain an order for testing
        Map<NamedType, ParameterizedType> map = new LinkedHashMap<>();
        for (ParameterizedType parameter : originalType.parameters()) {
            ParameterizedType recursive;
            if (parameter.isTypeParameter()) {
                ParameterizedType pt = parameters.get(i);
                if (pt != null && pt.isUnboundWildcard() && !parameter.typeParameter().typeBounds().isEmpty()) {
                    // replace '?' by '? extends X', with 'X' the first type bound, see TypeParameter_3
                    // but never do this for JLO (see e.g. issues described in MethodCall_73)
                    TypeInfo bound = parameter.typeParameter().typeBounds().getFirst().typeInfo();
                    if (bound.isJavaLangObject()) {
                        recursive = WILDCARD_PARAMETERIZED_TYPE;
                    } else {
                        recursive = new ParameterizedTypeImpl(bound, WildcardEnum.EXTENDS);
                    }
                } else {
                    recursive = pt;
                }
                map.put(parameter.typeParameter(), recursive);
            } else if (parameter.isType()) {
                recursive = parameter;
            } else throw new UnsupportedOperationException();
            if (recursive != null && recursive.isType() && !visited.contains(recursive.typeInfo())) {
                Map<NamedType, ParameterizedType> recursiveMap = ((ParameterizedTypeImpl) recursive)
                        .initialTypeParameterMap(visited);
                map.putAll(recursiveMap);
            }
            i++;
        }
        return map;
    }

    @Override
    public String toString() {
        return (typeInfo != null ? "Type " : typeParameter != null ? "Type param " : "") + detailedString();
    }

    @Override
    public TypeInfo bestTypeInfo() {
        if (typeInfo != null) return typeInfo;
        if (typeParameter != null) {
            if (wildcard != null && wildcard.isExtends() && parameters.size() == 1) {
                return parameters.getFirst().bestTypeInfo();
            }
            TypeParameter definition;
            if (typeParameter.getOwner() != null) {
                if (typeParameter.getOwner().isLeft()) {
                    TypeInfo owner = typeParameter.getOwner().getLeft();
                    definition = owner.typeParameters().get(typeParameter.getIndex());
                } else {
                    MethodInfo owner = typeParameter.getOwner().getRight();
                    definition = owner.typeParameters().get(typeParameter.getIndex());
                }
                if (!definition.typeBounds().isEmpty()) {
                    // IMPROVE should be a joint type
                    return definition.typeBounds().getFirst().typeInfo();
                }
            } // else: in JFocus, we can temporarily have no owner during type generalization
        }
        return null;
    }

    @Override
    public ParameterizedType mostSpecific(Predefined runtime, TypeInfo primaryType, ParameterizedType other) {
        if (equals(other) || other == null) return this;
        if (isType() && typeInfo.isVoid()) return other;
        if (other.isType() && other.typeInfo().isVoid()) return this;
        if (isUnboundWildcard()) return other;
        if (other.isUnboundWildcard()) return this;
        if (isTypeParameter()) {
            if (other.isTypeParameter()) {
                // a type parameter in the primary type has priority over another one
                // IMPROVE change this to a hierarchy rather than primary vs other
                if (primaryType.equals(other.typeParameter().primaryType())) return other;
                return this;
            }
            return other;
        }
        if (other.isTypeParameter() && !isTypeParameter()) return this;

        if (isBoxedExcludingVoid() && other.isPrimitiveExcludingVoid()) return other;
        if (other.isBoxedExcludingVoid() && isPrimitiveExcludingVoid()) return this;

        if (isAssignableFrom(runtime, other)) {
            return other;
        }
        return this;
    }

    /*

    /*
    HashMap<K, V> implements Map<K, V>
    Given Map<K, V>, go from abstract to concrete (HM:K to Map:K, HM:V to Map:V)
    */
    @Override
    public Map<NamedType, ParameterizedType> forwardTypeParameterMap() {
        if (!isType()) return Map.of();
        if (parameters.isEmpty()) return Map.of();
        ParameterizedType originalType = typeInfo.asParameterizedType(); // Map:K, Map:V
        assert originalType.parameters().size() == parameters.size();
        int i = 0;
        // linkedHashMap to maintain an order for testing
        Map<NamedType, ParameterizedType> map = new LinkedHashMap<>();
        for (ParameterizedType parameter : originalType.parameters()) {
            ParameterizedType p = parameters.get(i);
            if (p.isTypeParameter()) {
                map.put(p.typeParameter(), parameter);
            }
            i++;
        }
        return map;
    }

    /**
     * IMPORTANT: code copied from MethodTypeParameterMap
     *
     * @param translate the map to be applied on the type parameters of this
     * @return a newly created ParameterizedType
     */
    @Override
    public ParameterizedType applyTranslation(PredefinedWithoutParameterizedType predefined,
                                              Map<NamedType, ParameterizedType> translate) {
        return applyTranslation(predefined, translate, new HashSet<>());
    }

    /*
    Mono<R> is to be translated
    the translation map maps R -> Function<Object, ? extends Mono<? extends R>>
     */
    private ParameterizedType applyTranslation(PredefinedWithoutParameterizedType primitives,
                                               Map<NamedType, ParameterizedType> translate,
                                               Set<ParameterizedType> alreadyTranslated) {
        if (translate.isEmpty()) return this;
        ParameterizedType pt = this;
        if (pt.isTypeParameter()) {
            boolean add = false;
            while (pt.isTypeParameter() && translate.containsKey(pt.typeParameter())) {
                ParameterizedType newPt = translate.get(pt.typeParameter());
                if (newPt.equals(pt) || newPt.isTypeParameter() && pt.typeParameter().equals(newPt.typeParameter()))
                    break;
                pt = newPt;
                add = true;
            }
            // we want to add this.arrays only once, and only when there was a translation (MethodCall_61)
            if (add) {
                pt = pt.copyWithArrays(pt.arrays() + arrays);
            }
        }
        // see MethodCall_60,_61,_62
        final ParameterizedType stablePt = pt;
        if (stablePt.parameters().isEmpty()) return stablePt;
        List<ParameterizedType> recursivelyMappedParameters = stablePt.parameters().stream()
                .map(x -> {
                    if (x == stablePt || x == this) return stablePt;
                    if (alreadyTranslated.contains(x)) return WILDCARD_PARAMETERIZED_TYPE;
                    Set<ParameterizedType> addToAlreadyTranslated = Stream.concat(Stream.of(x),
                            alreadyTranslated.stream()).collect(Collectors.toUnmodifiableSet());
                    return ((ParameterizedTypeImpl) x).applyTranslation(primitives, translate, addToAlreadyTranslated);
                })
                .map(x -> x.ensureBoxed(primitives))
                .collect(Collectors.toList());
        if (stablePt.typeInfo() == null) {
            throw new UnsupportedOperationException("? input " + stablePt + " has no type");
        }
        return new ParameterizedTypeImpl(stablePt.typeInfo(), null, recursivelyMappedParameters, arrays, wildcard);
    }

    @Override
    public ParameterizedType rewire(InfoMap infoMap, Map<TypeParameter, TypeParameter> rewiredTypeParameters) {
        if (typeInfo != null) {
            TypeInfo rewired = infoMap.typeInfo(typeInfo);
            List<ParameterizedType> rewiredParameters = parameters.stream()
                    .map(pt -> pt.rewire(infoMap, rewiredTypeParameters)).toList();
            return new ParameterizedTypeImpl(rewired, null, rewiredParameters, arrays, wildcard);
        }
        if (typeParameter != null) {
            assert parameters.isEmpty();
            TypeParameter rewiredTp = Objects.requireNonNullElseGet(rewiredTypeParameters.get(typeParameter),
                    () -> (TypeParameter) typeParameter.rewire(infoMap));
            return new ParameterizedTypeImpl(null, rewiredTp, List.of(), arrays, wildcard);
        }
        return this;
    }

    @Override
    public boolean isMathematicallyInteger() {
        return arrays == 0 && typeInfo != null && (
                typeInfo.isByte() || typeInfo.isBoxedByte() ||
                typeInfo.isShort() || typeInfo.isBoxedShort() ||
                typeInfo.isInt() || typeInfo.isInteger() ||
                typeInfo.isLong() || typeInfo.isBoxedLong() ||
                typeInfo.isChar() || typeInfo.isCharacter());
    }

    @Override
    public ParameterizedType withWildcard(Wildcard wildcard) {
        return new ParameterizedTypeImpl(typeInfo, typeParameter, parameters, arrays, wildcard);
    }

    @Override
    public ParameterizedType withParameters(List<ParameterizedType> parameters) {
        return new ParameterizedTypeImpl(typeInfo, typeParameter, parameters, arrays, wildcard);
    }

    @Override
    public boolean isNoTypeGivenInLambda() {
        return this == NO_TYPE_GIVEN_IN_LAMBDA;
    }


    // initially implemented to ensure that a NULL type doesn't overwrite a valid type, see Lambda_17
    @Override
    public ParameterizedType bestDefined(ParameterizedType other) {
        if (typeInfo != null && other.typeInfo() == null) return this;
        if (other.typeInfo() != null && typeInfo == null) return other;
        if (typeInfo != null) {
            return this; // TODO? should we go recursively?
        }
        if (typeParameter != null && other.typeParameter() == null) return this;
        if (typeParameter == null && other.typeParameter() != null) return other;
        return this;// doesn't matter anymore
    }

    /*
  Let A, B be a type rather than a type parameter.
  A -> A
  A<T extends B> -> A<B>
  T extends A -> return A
  T extends Comparable<? super T> -> return Comparable, remove the T
  T === T extends Object -> return JLO

  when there are multiple type bounds,
   */
    public List<ParameterizedType> replaceByTypeBounds() {
        return replaceByTypeBounds(new HashSet<>());
    }

    /*
    MethodCall_32,_36,46,_47 show why this method is relatively complicated
     */
    private List<ParameterizedType> replaceByTypeBounds(Set<TypeParameter> remove) {
        if (typeInfo != null) {
            if (parameters.isEmpty()) {
                return List.of(this);
            }
            // the translation collector detects !=; returns 'parameters 'when there are no differences in the list
            List<ParameterizedType> updatedParameters = parameters.stream()
                    .filter(pt -> pt.typeParameter() == null || !remove.contains(pt.typeParameter()))
                    .flatMap(p -> ((ParameterizedTypeImpl) p).replaceByTypeBounds(remove).stream())
                    .collect(TranslationMap.staticToList(parameters));
            if (updatedParameters == parameters) {
                return List.of(this);
            }
            return List.of(new ParameterizedTypeImpl(typeInfo, updatedParameters));
        }
        if (typeParameter != null) {
            List<ParameterizedType> typeBounds = typeParameter.typeBounds();
            if (typeBounds.isEmpty()) {
                // a type parameter without type bounds; we can keep that one; IsAssignableFrom can deal with it
                return List.of(this);
            }
            Set<TypeParameter> newRemove = Stream.concat(remove.stream(), Stream.of(typeParameter))
                    .collect(Collectors.toUnmodifiableSet());
            return typeBounds
                    .stream().filter(pt -> pt.typeParameter() == null || !remove.contains(pt.typeParameter()))
                    .flatMap(p -> ((ParameterizedTypeImpl) p).replaceByTypeBounds(newRemove).stream())
                    .map(p -> p.copyWithArrays(arrays))
                    .collect(TranslationMap.staticToList(typeBounds));
        }
        return List.of(this);
    }

    @Override
    public boolean hasTypeParameters() {
        if (typeParameter != null) return true;
        if (typeInfo != null) {
            return parameters.stream().anyMatch(ParameterizedType::hasTypeParameters);
        }
        return false;
    }

    @Override
    public ParameterizedType replaceTypeParameter(TypeParameter oldTp, TypeParameter newPt) {
        if (oldTp.equals(typeParameter)) return new ParameterizedTypeImpl(newPt, arrays);
        if (!parameters.isEmpty()) {
            return withParameters(parameters.stream().map(pt -> pt.replaceTypeParameter(oldTp, newPt)).toList());
        }
        return this;
    }

    @Override
    public boolean isJavaUtilList() {
        TypeInfo bestType = bestTypeInfo();
        return bestType != null
               && ("java.util.List".equals(bestType.fullyQualifiedName()) || bestType.superTypesExcludingJavaLangObject().stream()
                .anyMatch(ti -> "java.util.List".equals(ti.fullyQualifiedName())));
    }

    @Override
    public Map<NamedType, ParameterizedType> formalToConcrete(ParameterizedType concrete) {
        Map<NamedType, ParameterizedType> res = new HashMap<>();
        formalToConcrete(this, concrete, res);
        return res;
    }

    private static void formalToConcrete(ParameterizedType formal, ParameterizedType concrete, Map<NamedType, ParameterizedType> res) {
        if (formal.typeParameter() != null && !formal.equals(concrete)) {
            // see TestMethodCall7,7, method2 for an example where both are present, and bestDefined needs to intervene
            assert formal.typeParameter() != null;
            assert concrete != null;
            res.merge(formal.typeParameter(), concrete, ParameterizedType::bestDefined);
        } else {
            int i = 0;
            for (ParameterizedType parameter : formal.parameters()) {
                if (i >= concrete.parameters().size()) break;
                formalToConcrete(parameter, concrete.parameters().get(i), res);
                ++i;
            }
        }
    }

    @Override
    public boolean typeBoundsAreSet(TypeParameter self) {
        if (typeParameter != null && !typeParameter.equals(self)) {
            return typeParameter.typeBoundsAreSet();
        }
        for (ParameterizedType param : parameters) {
            if (!param.typeBoundsAreSet(self)) return false;
        }
        return true;
    }
}
