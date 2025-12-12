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
import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.collectTypeParametersFromVirtualField;

public record ShallowMethodLinkComputer(Runtime runtime, VirtualFieldComputer virtualFieldComputer) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShallowMethodLinkComputer.class);

    public MethodLinkedVariables go(MethodInfo methodInfo) {
        LOGGER.debug("Computing method linked variables of {}", methodInfo);
        ParameterizedType returnType = methodInfo.returnType();
        TypeInfo typeInfo = methodInfo.typeInfo();
        ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);
        Links.Builder ofReturnValue = new LinksImpl.Builder(rv);

        // virtual fields of the default source = the object ~ "this"
        VirtualFields vfThis = virtualFieldComputer.compute(typeInfo.asParameterizedType(),
                false).virtualFields();
        FieldInfo hcThis = vfThis.hiddenContent();
        FieldReference hcThisFr = hcThis == null ? null : runtime.newFieldReference(hcThis);
        Set<TypeParameter> hcThisTps = hcThis == null ? null : correspondingTypeParameters(typeInfo, hcThis);

        // *************************************************
        // instance method, from return variable to object
        // *************************************************

        if (methodInfo.hasReturnValue() && !methodInfo.isStatic() && hcThis != null) {
            Value.Independent independent = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.DEPENDENT);
            if (!independent.isIndependent()) {
                transfer(ofReturnValue, returnType, hcThis.type(), hcThisFr, independent.isDependent(),
                        vfThis.mutable(), hcThisTps);
            }
        }

        List<Links> ofParameters = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            Links.Builder piBuilder = new LinksImpl.Builder(pi);

            Value.Independent independent = pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                    ValueImpl.IndependentImpl.DEPENDENT);
            Map<Integer, Integer> dependencies = independent.linkToParametersReturnValue();
            int linkLevelRv = dependencies.getOrDefault(-1, -1);
            // a dependence from the parameter into the return variable; we'll add it to the return variable
            // linkLevel 1 == independent HC
            if (linkLevelRv == 1) {

                // *************************************************
                // explicit dependence of return variable to parameter
                // *************************************************

                transfer(ofReturnValue, returnType, pi.parameterizedType(), pi, false, null,
                        hcThisTps);
                // TODO we may have to convert hcThisTps to method parameters
            } else if (!independent.isIndependent()) {
                if (hcThis != null && !methodInfo.isStatic()) {

                    // *************************************************
                    // instance method, from parameter into object
                    // *************************************************

                    transfer(piBuilder, pi.parameterizedType(), hcThis.type(), hcThisFr, independent.isDependent(),
                            vfThis.mutable(), hcThisTps);
                } else {
                    // *************************************************
                    // static method, from return variable into parameter
                    // *************************************************

                    Variable sourceVariable;
                    if (pi.parameterizedType().typeParameter() != null && pi.parameterizedType().arrays() == 0) {
                        // no $m, no virtual fields... simple type parameter
                        sourceVariable = pi;
                    } else {
                        VirtualFields vfSource = virtualFieldComputer
                                .compute(pi.parameterizedType(), false).virtualFields();
                        if (vfSource.hiddenContent() != null) {
                            sourceVariable = runtime.newFieldReference(vfSource.hiddenContent(),
                                    runtime.newVariableExpression(pi), vfSource.hiddenContent().type());
                        } else {
                            sourceVariable = null; // bail out, there will be no link
                        }
                    }
                    if (sourceVariable != null) {
                        Set<TypeParameter> sourceVariableTps
                                = collectTypeParametersFromVirtualField(sourceVariable.parameterizedType());
                        transfer(ofReturnValue, returnType, sourceVariable.parameterizedType(),
                                sourceVariable, false, null, sourceVariableTps);
                    }
                }
            }

            // *************************************************
            // links between parameters
            // *************************************************

            for (int i = 0; i < pi.index(); ++i) {
                int linkLevelPi = crosslinkInIndependentProperty(methodInfo, pi, i);
                // a dependence from the parameter into another parameter; we'll add it here
                // linkLevel 1 == independent HC.
                // Example: Collections.addAll(...), param 0 (source) -> param 1 (target), at link level 1
                // see TestShallow,7,8
                if (linkLevelPi == 1) {
                    ParameterInfo source = methodInfo.parameters().get(i);
                    VirtualFields sourceVfs = virtualFieldComputer.compute(source.parameterizedType(), false).virtualFields();
                    Set<TypeParameter> sourceTps = correspondingTypeParameters(source.parameterizedType().typeInfo(),
                            sourceVfs.hiddenContent()).stream()
                            .map(tp -> formalToConcrete(tp, source.parameterizedType()))
                            .collect(Collectors.toUnmodifiableSet());
                    FieldInfo sourceHc = sourceVfs.hiddenContent();
                    Expression scope = runtime.newVariableExpression(source);
                    FieldReference sourceFr = runtime.newFieldReference(sourceHc, scope, sourceHc.type());
                    transfer(piBuilder, pi.parameterizedType(), sourceHc.type(), sourceFr, false,
                            null, sourceTps);
                }
            }
            ofParameters.add(piBuilder.build());
        }
        return new MethodLinkedVariablesImpl(ofReturnValue.build(), ofParameters);
    }

    private TypeParameter formalToConcrete(TypeParameter formal, ParameterizedType parameterizedType) {
        return parameterizedType.parameters().get(formal.getIndex()).typeParameter();
    }

    // important: the sourceType must be virtual fields/type parameters;
    // the virtual fields of the target type get computed as required
    private void transfer(Links.Builder builder,
                          ParameterizedType fromType,
                          ParameterizedType toType,
                          Variable subTo,
                          boolean dependent,
                          FieldInfo subMutable,
                          Set<TypeParameter> typeParametersOfSubTo) {
        if (fromType.typeParameter() != null && fromType.arrays() == 0) {
            assert fromType.typeParameter().typeBounds().isEmpty() : """
                    cannot deal with type bounds at the moment; obviously, if a type bound is mutable,
                    the type parameter can be dependent""";
            LinkNature linkNature = deriveLinkNature(0, toType.arrays());
            if (fromType.typeParameter().equals(toType.typeParameter())) {
                // 'to' is a type parameter, e.g. T[] ts
                builder.add(linkNature, subTo);
            } else if (toType.typeParameter() == null) {
                // 'to' is a container (array); we'll need a slice
                FF theField = findField(fromType.typeParameter(), toType.typeInfo());
                if (subTo.parameterizedType().arrays() == 0) {
                    // indexing: TestShallowPrefix,1:
                    FieldReference subSubTo = runtime.newFieldReference(theField.fieldInfo,
                            runtime.newVariableExpression(subTo), theField.fieldInfo.type());
                    builder.add(IS_IDENTICAL_TO, subSubTo);
                } else {
                    DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subTo),
                            runtime.newInt(-1 - theField.index));
                    Expression scope = runtime.newVariableExpression(dv);
                    FieldReference slice = runtime.newFieldReference(theField.fieldInfo, scope, theField.fieldInfo.type());
                    builder.add(linkNature, slice);
                }
            }
        } else {
            VirtualFields vfFromType = virtualFieldComputer.compute(fromType, false).virtualFields();
            if (vfFromType.hiddenContent() != null) {
                FieldReference subFrom = runtime().newFieldReference(vfFromType.hiddenContent(),
                        runtime.newVariableExpression(builder.primary()), vfFromType.hiddenContent().type());
                // concrete method type parameters
                Set<TypeParameter> typeParametersFrom = fromType.extractTypeParameters();
                // more formal type parameters in HC
                assert typeParametersFrom.equals(collectTypeParametersFromVirtualField(vfFromType.hiddenContent().type()));
                int arraysFrom = vfFromType.hiddenContent().type().arrays();
                int arraysTo = toType.arrays();
                if (typeParametersFrom.equals(typeParametersOfSubTo)) {
                    LinkNature linkNature = deriveLinkNature(arraysFrom, arraysTo);
                    builder.add(subFrom, linkNature, subTo);
                } else {
                    List<TypeParameter> intersection = new ArrayList<>(typeParametersFrom.stream()
                            .sorted(Comparator.comparingInt(TypeParameter::getIndex)).toList());
                    intersection.retainAll(typeParametersOfSubTo);
                    if (intersection.size() == 1) {
                        if (arraysFrom > 0) {
                            if (arraysTo == arraysFrom) {
                                if (toType.typeParameter() != null) {
                                    FF theField = findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                                    DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subFrom),
                                            runtime.newInt(-1 - theField.index));
                                    builder.add(dv, INTERSECTION_NOT_EMPTY, subTo);
                                } else {
                                    // slice across: TestShallow,4: keySet.ks~this.kvs[-1].k
                                    FF theField = findField(intersection.getFirst(), toType.typeInfo());
                                    DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subTo),
                                            runtime.newInt(-1 - theField.index));
                                    Expression scope = runtime.newVariableExpression(dv);
                                    FieldReference slice = runtime.newFieldReference(theField.fieldInfo, scope, theField.fieldInfo.type());
                                    builder.add(subFrom, INTERSECTION_NOT_EMPTY, slice);
                                }
                            } else if (arraysTo == 0) {
                                // slice to a single element: TestShallowPrefix,1: oneStatic.xys[-1]>0:x
                                FF theField = findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                                DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subFrom),
                                        runtime.newInt(-1 - theField.index));
                                builder.add(dv, CONTAINS, subTo);
                            }
                        } else {
                            // indexing: TestShallowPrefix,2: oneStatic.xsys.xs>0:x
                            FF theField = findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                            FieldReference subSubFrom = runtime.newFieldReference(theField.fieldInfo,
                                    runtime.newVariableExpression(subFrom), theField.fieldInfo.type());
                            builder.add(subSubFrom, CONTAINS, subTo);
                        }
                    }
                }

                if (dependent) {
                    if (vfFromType.mutable() != null) {
                        FieldReference mTarget = runtime.newFieldReference(vfFromType.mutable(),
                                runtime.newVariableExpression(builder.primary()), vfFromType.mutable().type());
                        FieldReference mSource = runtime.newFieldReference(subMutable);
                        builder.add(mTarget, IS_IDENTICAL_TO, mSource);
                    } else {
                        throw new UnsupportedOperationException("?");
                    }
                }
            }
        }
    }

    private static LinkNature deriveLinkNature(int arrays, int arraysSource) {
        if (arrays == arraysSource) {
            return arrays == 0 ? IS_IDENTICAL_TO : INTERSECTION_NOT_EMPTY;
        }
        if (arrays < arraysSource) {
            return IS_ELEMENT_OF;
        }
        return CONTAINS;
    }

    private record FF(FieldInfo fieldInfo, int index) {
    }

    private static FF findField(TypeParameter typeParameter, TypeInfo container) {
        int i = 0;
        for (FieldInfo fieldInfo : container.fields()) {
            if (typeParameter.equals(fieldInfo.type().typeParameter())) {
                return new FF(fieldInfo, i);
            }
            ++i;
        }
        throw new UnsupportedOperationException("Should be able to find a field with type " + typeParameter
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

    private static int crosslinkInIndependentProperty(MethodInfo methodInfo, ParameterInfo pi, int i) {
        ParameterInfo piPrev = methodInfo.parameters().get(i);
        Value.Independent independentPrev = piPrev.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.DEPENDENT);
        Map<Integer, Integer> dependenciesPrev = independentPrev.linkToParametersReturnValue();
        return dependenciesPrev.getOrDefault(pi.index(), -1);
    }

}
