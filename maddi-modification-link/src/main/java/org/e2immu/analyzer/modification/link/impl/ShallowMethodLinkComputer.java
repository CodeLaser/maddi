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

import static org.e2immu.analyzer.modification.link.LinkNature.*;

public record ShallowMethodLinkComputer(Runtime runtime, VirtualFieldComputer virtualFieldComputer) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShallowMethodLinkComputer.class);

    public MethodLinkedVariables go(MethodInfo methodInfo) {
        LOGGER.debug("Computing method linked variables of {}", methodInfo);
        ParameterizedType returnType = methodInfo.returnType();
        TypeInfo typeInfo = methodInfo.typeInfo();
        ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);
        Links.Builder ofReturnValue = new LinksImpl.Builder(rv);

        // virtual fields of the default source = the object ~ "this"
        VirtualFields virtualFields = typeInfo.analysis()
                .getOrCreate(VirtualFields.VIRTUAL_FIELDS, () -> virtualFieldComputer.computeOnDemand(typeInfo));
        FieldInfo hiddenContent = virtualFields.hiddenContent();
        FieldReference hiddenContentFr = hiddenContent == null ? null : runtime.newFieldReference(hiddenContent);
        Set<TypeParameter> hiddenContentTps = hiddenContent == null ? null : correspondingTypeParameters(typeInfo,
                hiddenContent);

        // instance method, from object into return variable
        if (methodInfo.hasReturnValue() && !methodInfo.isStatic() && hiddenContent != null) {
            Value.Independent independent = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.DEPENDENT);
            if (!independent.isIndependent()) {
                transfer(ofReturnValue, returnType, hiddenContent.type(), hiddenContentFr, hiddenContentTps,
                        false);
                if (independent.isDependent()) {
                    assert returnType.typeInfo() != null || returnType.arrays() > 0
                            : "A type parameter cannot be dependent; a type parameter array can";
                    VirtualFields vfTarget = virtualFieldComputer.computeAllowTypeParameterArray(returnType);
                    if (vfTarget.mutable() != null) {
                        FieldReference mTarget = runtime.newFieldReference(vfTarget.mutable(),
                                runtime.newVariableExpression(rv), vfTarget.mutable().type());
                        FieldReference mSource = runtime.newFieldReference(virtualFields.mutable());
                        ofReturnValue.add(mTarget, IS_IDENTICAL_TO, mSource);
                    } else {
                        throw new UnsupportedOperationException("?");
                    }
                }
            }
        }

        List<Links> ofParameters = new ArrayList<>(methodInfo.parameters().size());
        Set<TypeParameter> hiddenContentMethodTypeParameters = methodInfo.isFactoryMethod() && hiddenContent != null
                ? convertToMethodTypeParameters(methodInfo, hiddenContentTps) : null;

        for (ParameterInfo pi : methodInfo.parameters()) {
            Links.Builder piBuilder = new LinksImpl.Builder(pi);

            Value.Independent independent = pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                    ValueImpl.IndependentImpl.DEPENDENT);
            Map<Integer, Integer> dependencies = independent.linkToParametersReturnValue();
            int linkLevelRv = dependencies.getOrDefault(-1, -1);
            // a dependence from the parameter into the return variable; we'll add it to the return variable
            // linkLevel 1 == independent HC
            if (linkLevelRv == 1) {
                transfer(ofReturnValue, returnType, pi.parameterizedType(), pi, hiddenContentTps,
                        true);
            } else if (!independent.isIndependent()) {
                if (hiddenContent != null) {
                    if (methodInfo.isFactoryMethod()) {
                        transfer(ofReturnValue, returnType, pi.parameterizedType(), pi,
                                hiddenContentMethodTypeParameters, true);
                    } else {
                        transfer(piBuilder, pi.parameterizedType(), hiddenContent.type(), hiddenContentFr, hiddenContentTps,
                                false);
                    }
                } else {
                    Variable sourceVariable;
                    if (pi.parameterizedType().typeParameter() != null && pi.parameterizedType().arrays() == 0) {
                        // no $m, no virtual fields... simple type parameter
                        sourceVariable = pi;
                    } else {
                        VirtualFields vfSource = virtualFieldComputer.computeAllowTypeParameterArray(pi.parameterizedType());
                        if (vfSource.hiddenContent() != null) {
                            sourceVariable = runtime.newFieldReference(vfSource.hiddenContent(),
                                    runtime.newVariableExpression(pi), vfSource.hiddenContent().type());
                        } else {
                            sourceVariable = null; // bail out, there will be no link
                        }
                    }
                    if (sourceVariable != null) {
                        if (returnType.typeParameter() != null && returnType.arrays() == 0) {
                            // there is no real "type" to be found
                            transfer(ofReturnValue, returnType, sourceVariable.parameterizedType(),
                                    sourceVariable, Set.of(returnType.typeParameter()), false);
                        } else {
                            VirtualFields vfTarget = virtualFieldComputer.computeAllowTypeParameterArray(returnType);
                            if (vfTarget.hiddenContent() != null) {
                                FieldReference vfTargetFr = runtime.newFieldReference(vfTarget.hiddenContent(),
                                        runtime.newVariableExpression(rv), vfTarget.hiddenContent().type());
                                // FIXME type parameters...
                                Set<TypeParameter> returnTypeTypeParameters = vfTarget.hiddenContent().type().extractTypeParameters();
                                transfer(ofReturnValue, vfTargetFr.parameterizedType(), sourceVariable.parameterizedType(),
                                        sourceVariable, returnTypeTypeParameters, false);
                            }
                        }
                    }
                }
            }
            // links to other parameters
            for (int i = 0; i < pi.index(); ++i) {
                int linkLevelPi = crosslinkInIndependentProperty(methodInfo, pi, i);
                // a dependence from the parameter into another parameter; we'll add it here
                // linkLevel 1 == independent HC.
                // Example: Collections.addAll(...), param 0 (source) -> param 1 (target), at link level 1
                // see TestShallow,7,8
                if (linkLevelPi == 1) {
                    ParameterInfo source = methodInfo.parameters().get(i);
                    VirtualFields sourceVfs = virtualFieldComputer.computeAllowTypeParameterArray(source.parameterizedType());
                    Set<TypeParameter> sourceTps = correspondingTypeParameters(source.parameterizedType().typeInfo(),
                            sourceVfs.hiddenContent()).stream()
                            .map(tp -> formalToConcrete(tp, source.parameterizedType()))
                            .collect(Collectors.toUnmodifiableSet());
                    FieldInfo sourceHc = sourceVfs.hiddenContent();
                    Expression scope = runtime.newVariableExpression(source);
                    FieldReference sourceFr = runtime.newFieldReference(sourceHc, scope, sourceHc.type());
                    transfer(piBuilder, pi.parameterizedType(), sourceHc.type(), sourceFr, sourceTps, false);
                }
            }
            ofParameters.add(piBuilder.build());
        }
        return new MethodLinkedVariablesImpl(ofReturnValue.build(), ofParameters);
    }

    private TypeParameter formalToConcrete(TypeParameter formal, ParameterizedType parameterizedType) {
        return parameterizedType.parameters().get(formal.getIndex()).typeParameter();
    }

    // important: the sourceType must be virtual fields/type parameters; the virtual fields of the target type get computed
    // when necessary
    private void transfer(Links.Builder targetBuilder,
                          ParameterizedType targetType,
                          ParameterizedType sourceType,
                          Variable sourceField,
                          Set<TypeParameter> typeParametersOfSourceField,
                          boolean reverse) {
        if (targetType.typeParameter() != null && typeParametersOfSourceField.contains(targetType.typeParameter())) {
            assert targetType.typeParameter().typeBounds().isEmpty() : """
                    cannot deal with type bounds at the moment; obviously, if a type bound is mutable,
                    the type parameter can be dependent""";
            int arrays = targetType.arrays();
            LinkNature linkNature = deriveLinkNature(arrays, sourceType.arrays(), reverse);
            if (sourceType.typeParameter() != null) {
                targetBuilder.add(linkNature, sourceField);
            } else {
                // get one element out of a container array; we'll need a slice
                FieldInfo theField = findField(List.of(targetType.typeParameter()), sourceType.typeInfo());
                DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(sourceField),
                        runtime.newInt(-1));
                Expression scope = runtime.newVariableExpression(dv);
                FieldReference slice = runtime.newFieldReference(theField, scope, theField.type());
                targetBuilder.add(linkNature, slice);

            }
        } else if (targetType.typeInfo() != null) {
            VirtualFields vfType = virtualFieldComputer.compute(targetType.typeInfo());
            if (vfType.hiddenContent() == null) {
                return;
            }
            FieldReference linkSource = runtime().newFieldReference(vfType.hiddenContent(),
                    runtime.newVariableExpression(targetBuilder.primary()), vfType.hiddenContent().type());
            // FIXME check extractTypeParameters -> ?
            Set<TypeParameter> typeParametersReturnType = targetType.extractTypeParameters();
            if (typeParametersReturnType.equals(typeParametersOfSourceField)) {
                int arraysSource = sourceType.arrays();
                int multiplicity = virtualFieldComputer.maxMultiplicityFromMethods(targetType.typeInfo());
                if (multiplicity - 2 >= arraysSource) {
                    // Stream<T> Optional.stream() (going from arraySource 0 to multi 2)
                    targetBuilder.add(linkSource, CONTAINS, sourceField);
                } else if (multiplicity - 1 == arraysSource) {
                    // List.addAll(...) target: Collection, source T[] multi 2, array source 1
                    // List.subList target List, source T[] multi 2, array source 1
                    targetBuilder.add(linkSource, multiplicity == 1 ? IS_IDENTICAL_TO : INTERSECTION_NOT_EMPTY, sourceField);
                } else if (multiplicity <= arraysSource) {
                    // findFirst.t < this.ts in Stream (multi 1, array source 1)
                    targetBuilder.add(linkSource, IS_ELEMENT_OF, sourceField);
                }
            } else {
                List<TypeParameter> intersection = new ArrayList<>(typeParametersReturnType.stream()
                        .sorted(Comparator.comparingInt(TypeParameter::getIndex)).toList());
                intersection.retainAll(typeParametersOfSourceField);
                if (!intersection.isEmpty()) {
                    if (intersection.size() == typeParametersReturnType.size()) {
                        FieldInfo theField = findField(intersection, sourceType.typeInfo());
                        DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(sourceField),
                                runtime.newInt(-1));
                        Expression scope = runtime.newVariableExpression(dv);
                        FieldReference slice = runtime.newFieldReference(theField, scope, theField.type());
                        targetBuilder.add(linkSource, INTERSECTION_NOT_EMPTY, slice);
                    }
                }
            }
        }
    }

    private static LinkNature deriveLinkNature(int arrays, int arraysSource, boolean reverse) {
        if (arrays == arraysSource) {
            return arrays == 0 ? IS_IDENTICAL_TO : INTERSECTION_NOT_EMPTY;
        }
        if (arrays < arraysSource) {
            return reverse ? CONTAINS : IS_ELEMENT_OF;
        }
        return reverse ? IS_ELEMENT_OF : CONTAINS;
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


    private static int crosslinkInIndependentProperty(MethodInfo methodInfo, ParameterInfo pi, int i) {
        ParameterInfo piPrev = methodInfo.parameters().get(i);
        Value.Independent independentPrev = piPrev.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.DEPENDENT);
        Map<Integer, Integer> dependenciesPrev = independentPrev.linkToParametersReturnValue();
        return dependenciesPrev.getOrDefault(pi.index(), -1);
    }

}
