package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ShallowMethodLinkComputer(Runtime runtime, VirtualFieldComputer virtualFieldComputer) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShallowMethodLinkComputer.class);

    public MethodLinkedVariables go(MethodInfo methodInfo) {
        LOGGER.debug("Computing method linked variables of {}", methodInfo);

        TypeInfo typeInfo = methodInfo.typeInfo();
        VirtualFields vf = typeInfo.analysis().getOrCreate(VirtualFields.VIRTUAL_FIELDS, () ->
                virtualFieldComputer.computeOnDemand(typeInfo));
        if (vf.hiddenContent() == null) return MethodLinkedVariablesImpl.EMPTY;
        FieldReference hiddenContentFr = runtime.newFieldReference(vf.hiddenContent());
        ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);
        Links.Builder ofReturnValue = new LinksImpl.Builder(rv);
        Set<TypeParameter> typeParametersVf = correspondingTypeParameters(methodInfo.typeInfo(), vf.hiddenContent());
        Set<TypeParameter> typeParametersVfFactory = methodInfo.isFactoryMethod()
                ? convertToMethodTypeParameters(methodInfo, typeParametersVf) : null;

        // instance method, from object into return variable
        if (methodInfo.hasReturnValue() && !methodInfo.isStatic()) {
            Value.Independent independent = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.DEPENDENT);
            if (!independent.isIndependent()) {
                transfer(methodInfo.returnType(), vf, typeParametersVf, ofReturnValue, hiddenContentFr, false);
            }
        }

        List<Links> ofParameters = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            Links.Builder builder = new LinksImpl.Builder(pi);
            ParameterizedType type = pi.parameterizedType();

            Value.Independent independent = pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                    ValueImpl.IndependentImpl.DEPENDENT);
            Map<Integer, Integer> dependencies = independent.linkToParametersReturnValue();
            if (!dependencies.isEmpty()) {
                int linkLevel = dependencies.getOrDefault(-1, -1);
                // a dependence from the parameter into the return variable; we'll add it to the return variable
                // linkLevel 1 == independent HC
                if (type.typeParameter() != null && linkLevel == 1) {
                    transfer(pi.parameterizedType(), vf, typeParametersVf, ofReturnValue, pi, true);
                }
            } else if (!independent.isIndependent()) {
                if (methodInfo.isFactoryMethod()) {
                    transfer(type, vf, typeParametersVfFactory, ofReturnValue, pi, true);
                } else {
                    transfer(type, vf, typeParametersVf, builder, hiddenContentFr, false);
                }
            }
            ofParameters.add(builder.build());
        }
        return new MethodLinkedVariablesImpl(ofReturnValue.build(), ofParameters);
    }

    private void transfer(ParameterizedType type,
                          VirtualFields vf,
                          Set<TypeParameter> typeParametersVf,
                          Links.Builder builder,
                          Variable hiddenContentFr,
                          boolean reverse) {
        if (type.typeParameter() != null && typeParametersVf.contains(type.typeParameter())) {
            assert type.typeParameter().typeBounds().isEmpty() : """
                    cannot deal with type bounds at the moment; obviously, if a type bound is mutable,
                    the type parameter can be dependent""";
            int arrays = type.arrays();
            int arraysVF = vf.hiddenContent().type().arrays();
            if (arrays == arraysVF) {
                if (vf.hiddenContent().type().typeParameter() != null) {
                    LinkNature linkNature = arrays == 0 ? LinkNature.IS_IDENTICAL_TO : LinkNature.INTERSECTION_NOT_EMPTY;
                    builder.add(linkNature, hiddenContentFr);
                }
            } else if (arrays < arraysVF) {
                if (vf.hiddenContent().type().typeParameter() != null) {
                    // one element out of an array
                    LinkNature linkNature = reverse ? LinkNature.CONTAINS : LinkNature.IS_ELEMENT_OF;
                    builder.add(linkNature, hiddenContentFr);
                }
            }
        } else if (type.typeInfo() != null) {
            int arraysVF = vf.hiddenContent().type().arrays();

            Set<TypeParameter> typeParametersReturnType = type.extractTypeParameters();
            if (typeParametersReturnType.equals(typeParametersVf)) {
                int multiplicity = virtualFieldComputer.computeMultiplicity(type);
                if (multiplicity == 0 && arraysVF == 0) {
                    builder.add(LinkNature.CONTAINS, hiddenContentFr);
                } else if (multiplicity - 1 == arraysVF) {
                    builder.add(LinkNature.INTERSECTION_NOT_EMPTY, hiddenContentFr);
                }
            }
        }
    }

    /*
    The hidden content field can contain type parameters from a supertype.
    This method computes the corresponding type parameters from the current type.

    Example: VF in List is based on the virtual field in Iterable<T> -> ts[]
    In list, the corresponding type parameter is E.

     */
    private Set<TypeParameter> correspondingTypeParameters(TypeInfo descendantType, FieldInfo hiddenContent) {
        if (hiddenContent == null) return Set.of();
        if (descendantType.equals(hiddenContent.owner())) {
            return hiddenContent.type().extractTypeParameters();
        }
        Stream<ParameterizedType> parentStream = Stream.ofNullable(
                descendantType.parentClass() == null || descendantType.parentClass().isJavaLangObject()
                        ? null : descendantType.parentClass());
        Stream<ParameterizedType> superStream = Stream.concat(parentStream,
                descendantType.interfacesImplemented().stream());
        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);
        return superStream.flatMap(superType -> {
            Set<TypeParameter> fromSuper = correspondingTypeParameters(superType.typeInfo(), hiddenContent);
            var map = genericsHelper.mapInTermsOfParametersOfSubType(descendantType, superType);
            return map == null ? Stream.of()
                    : map.entrySet().stream()
                    .filter(e -> e.getValue().typeParameter() != null
                                 && fromSuper.contains(e.getValue().typeParameter()))
                    .map(e -> (TypeParameter) e.getKey());
        }).collect(Collectors.toUnmodifiableSet());
    }

    /*
     a factory method will use method type parameters, rather than type parameters bound to the type itself.
     <E> List<E> List.of(E e) -> this E is bound to List.of()...
     */
    private Set<TypeParameter> convertToMethodTypeParameters(MethodInfo methodInfo, Set<TypeParameter> typeParametersVf) {
        ParameterizedType rt = methodInfo.returnType();
        assert rt.typeInfo().equals(methodInfo.typeInfo()); // otherwise, not a factory method
        return typeParametersVf.stream().map(tp -> rt.parameters().get(tp.getIndex()).typeParameter())
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

}
