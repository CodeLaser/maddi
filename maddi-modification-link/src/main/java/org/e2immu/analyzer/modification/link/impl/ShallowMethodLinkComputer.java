package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
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
        FieldInfo vfHc = vf.hiddenContent();
        ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);
        Links.Builder ofReturnValue = new LinksImpl.Builder(rv);
        FieldReference hiddenContentFr = vfHc == null ? null : runtime.newFieldReference(vfHc);
        Set<TypeParameter> typeParametersVf = vfHc == null ? null
                : correspondingTypeParameters(methodInfo.typeInfo(), vfHc);
        Set<TypeParameter> typeParametersVfFactory = methodInfo.isFactoryMethod() && vfHc != null
                ? convertToMethodTypeParameters(methodInfo, typeParametersVf) : null;

        // instance method, from object into return variable
        if (methodInfo.hasReturnValue() && !methodInfo.isStatic() && vfHc != null) {
            Value.Independent independent = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.DEPENDENT);
            if (!independent.isIndependent()) {
                ParameterizedType hiddenContentType = vfHc.type();
                transfer(methodInfo.returnType(), hiddenContentType, typeParametersVf, ofReturnValue, hiddenContentFr,
                        false);
                if (independent.isDependent()) {
                    ParameterizedType returnType = methodInfo.returnType();
                    assert returnType.typeInfo() != null || returnType.arrays() > 0
                            : "A type parameter cannot be dependent; a type parameter array can";
                    VirtualFields vfTarget = virtualFieldComputer.computeAllowTypeParameterArray(returnType);
                    if (vfTarget.mutable() != null) {
                        FieldReference mTarget = runtime.newFieldReference(vfTarget.mutable(),
                                runtime.newVariableExpression(rv), vfTarget.mutable().type());
                        FieldReference mSource = runtime.newFieldReference(vf.mutable());
                        ofReturnValue.add(mTarget, LinkNature.IS_IDENTICAL_TO, mSource);
                    } else {
                        LOGGER.debug("?");
                    }
                }
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
                if (linkLevel == 1) {
                    transfer(methodInfo.returnType(), pi.parameterizedType(), typeParametersVf, ofReturnValue, pi,
                            true);
                }
            } else if (!independent.isIndependent() && vfHc != null) {
                ParameterizedType hiddenContentType = vfHc.type();
                if (methodInfo.isFactoryMethod()) {
                    transfer(type, hiddenContentType, typeParametersVfFactory, ofReturnValue, pi, true);
                } else {
                    transfer(type, hiddenContentType, typeParametersVf, builder, hiddenContentFr, false);
                }
            }
            ofParameters.add(builder.build());
        }
        return new MethodLinkedVariablesImpl(ofReturnValue.build(), ofParameters);
    }

    private void transfer(ParameterizedType targetType,
                          ParameterizedType sourceType,
                          Set<TypeParameter> typeParametersVf,
                          Links.Builder builder,
                          Variable hiddenContentFr,
                          boolean reverse) {
        int arraysSource = sourceType.arrays();
        if (targetType.typeParameter() != null && typeParametersVf.contains(targetType.typeParameter())) {
            assert targetType.typeParameter().typeBounds().isEmpty() : """
                    cannot deal with type bounds at the moment; obviously, if a type bound is mutable,
                    the type parameter can be dependent""";
            int arrays = targetType.arrays();
            if (arrays == arraysSource) {
                if (sourceType.typeParameter() != null) {
                    LinkNature linkNature = arrays == 0 ? LinkNature.IS_IDENTICAL_TO : LinkNature.INTERSECTION_NOT_EMPTY;
                    builder.add(linkNature, hiddenContentFr);
                }
            } else if (arrays < arraysSource) {
                if (sourceType.typeParameter() != null) {
                    // one element out of an array
                    LinkNature linkNature = reverse ? LinkNature.CONTAINS : LinkNature.IS_ELEMENT_OF;
                    builder.add(linkNature, hiddenContentFr);
                } else {
                    // get one element out of a container array; we'll need a slice
                    FieldInfo theField = findField(List.of(targetType.typeParameter()), sourceType.typeInfo());
                    DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(hiddenContentFr),
                            runtime.newInt(-1));
                    Expression scope = runtime.newVariableExpression(dv);
                    FieldReference slice = runtime.newFieldReference(theField, scope, theField.type());
                    builder.add(LinkNature.IS_ELEMENT_OF, slice);
                }
            }
        } else if (targetType.typeInfo() != null) {
            VirtualFields vfType = virtualFieldComputer.compute(targetType.typeInfo());
            if (vfType.hiddenContent() == null) {
                return;
            }
            FieldReference linkSource = runtime().newFieldReference(vfType.hiddenContent(),
                    runtime.newVariableExpression(builder.primary()), vfType.hiddenContent().type());
            Set<TypeParameter> typeParametersReturnType = targetType.extractTypeParameters();
            if (typeParametersReturnType.equals(typeParametersVf)) {
                int multiplicity = virtualFieldComputer.computeMultiplicity(targetType.typeInfo());
                if (multiplicity - 2 >= arraysSource) {
                    // Stream<T> Optional.stream() (going from arraySource 0 to multi 2)
                    builder.add(linkSource, LinkNature.CONTAINS, hiddenContentFr);
                } else if (multiplicity - 1 == arraysSource) {
                    // List.addAll(...) target: Collection, source T[] multi 2, array source 1
                    // List.subList target List, source T[] multi 2, array source 1
                    builder.add(linkSource, LinkNature.INTERSECTION_NOT_EMPTY, hiddenContentFr);
                } else if (multiplicity <= arraysSource) {
                    // findFirst.t < this.ts in Stream (multi 1, array source 1)
                    builder.add(linkSource, LinkNature.IS_ELEMENT_OF, hiddenContentFr);
                }
            } else {
                List<TypeParameter> intersection = new ArrayList<>(typeParametersReturnType.stream()
                        .sorted(Comparator.comparingInt(TypeParameter::getIndex)).toList());
                intersection.retainAll(typeParametersVf);
                if (!intersection.isEmpty()) {
                    if (intersection.size() == typeParametersReturnType.size()) {
                        FieldInfo theField = findField(intersection, sourceType.typeInfo());
                        DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(hiddenContentFr),
                                runtime.newInt(-1));
                        Expression scope = runtime.newVariableExpression(dv);
                        FieldReference slice = runtime.newFieldReference(theField, scope, theField.type());
                        builder.add(linkSource, LinkNature.INTERSECTION_NOT_EMPTY, slice);
                    }
                }
            }
        }
    }

    private static FieldInfo findField(List<TypeParameter> typeParameters, TypeInfo container) {
        for (FieldInfo fieldInfo : container.fields()) {
            if (fieldInfo.type().typeParameter() != null && typeParameters.size() == 1
                && typeParameters.getFirst().equals(fieldInfo.type().typeParameter())) {
                return fieldInfo;
            }
        }
        throw new UnsupportedOperationException("Should be able to find a field with types " + typeParameters
                                                + " in " + container);
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
            if (hiddenContent.type().typeParameter() != null) {
                return Set.of(hiddenContent.type().typeParameter());
            }
            return hiddenContent.type().typeInfo().fields().stream()
                    .filter(f -> f.type().typeParameter() != null)
                    .map(f -> f.type().typeParameter()).collect(Collectors.toUnmodifiableSet());
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
    private Set<TypeParameter> convertToMethodTypeParameters(MethodInfo methodInfo,
                                                             Set<TypeParameter> typeParametersVf) {
        ParameterizedType rt = methodInfo.returnType();
        assert rt.typeInfo().isEnclosedIn(methodInfo.typeInfo());
        // otherwise, not a factory method. Note: Stream.builder() is an example where the return type is actually
        // enclosed, not equal. Yet the following statement still seems correct.
        return typeParametersVf.stream()
                .map(tp -> rt.parameters().get(tp.getIndex()).typeParameter())
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

}
