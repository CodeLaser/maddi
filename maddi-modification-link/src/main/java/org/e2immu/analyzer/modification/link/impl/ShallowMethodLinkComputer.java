package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
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

import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.VIRTUAL_FIELD;
import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.collectTypeParametersFromVirtualField;
import static org.e2immu.analyzer.modification.prepwork.variable.LinkNature.*;

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

        boolean forceIntoReturn;
        if (methodInfo.hasReturnValue() && !methodInfo.isStatic() && hcThis != null) {
            Value.Independent independent = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.DEPENDENT);
            Map<Integer, Integer> dependencies = independent.linkToParametersReturnValue();
            int linkLevelRv = dependencies.getOrDefault(-1, -1);
            forceIntoReturn = linkLevelRv == 1; //HC from return variable to object
            if (!independent.isIndependent()) {
                transfer(ofReturnValue, returnType, null, hcThis.type(), hcThisFr, independent.isDependent(),
                        vfThis.mutable(), hcThisTps, forceIntoReturn);
            }
        } else {
            forceIntoReturn = false;
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

            // we preempt the hcReturnValue=true
            if (pi.parameterizedType().isFunctionalInterface()) {
                // *************************************************
                // parameter is functional interface
                // *************************************************

                MethodInfo sam = pi.parameterizedType().typeInfo().singleAbstractMethod();
                boolean inputHasTypeParameters = sam.parameters().stream()
                        .anyMatch(p -> p.parameterizedType().hasTypeParameters());
                // boolean isSupplier = sam.parameters().isEmpty();
                boolean outputHasTypeParameters = sam.returnType().hasTypeParameters();
                if (outputHasTypeParameters && !forceIntoReturn) {
                    // yes to Supplier<T> (result: T), no to Predicate<T> (result: boolean)
                    ParameterizedType sourceType = pi.parameterizedType().parameters().getLast().withWildcard(null);
                    Set<TypeParameter> sourceVariableTps = collectTypeParametersFromVirtualField(sourceType);
                    Set<TypeParameter> returnTypeTps = returnType.extractTypeParameters();
                    if (sourceVariableTps.equals(returnTypeTps)) {
                        // return types agree
                        transfer(ofReturnValue, returnType, null, sourceType, pi, false, null,
                                sourceVariableTps, false);
                    }
                    ofParameters.add(piBuilder.build());
                    break;
                } else if (inputHasTypeParameters && !outputHasTypeParameters && !forceIntoReturn) {
                    // Consumer<T>
                    Set<TypeParameter> sourceVariableTps = pi.parameterizedType().parameters().stream()
                            .flatMap(param -> collectTypeParametersFromVirtualField(param).stream())
                            .collect(Collectors.toUnmodifiableSet());
                    if (sourceVariableTps.equals(hcThisTps)) {
                        // from parameter into object
                        assert hcThis != null;
                        Links.Builder thisBuilder = new LinksImpl.Builder(runtime.newThis(typeInfo.asParameterizedType()));
                        transfer(thisBuilder, hcThis.type(), vfThis, pi.parameterizedType(), pi, independent.isDependent(),
                                vfThis.mutable(), hcThisTps, false);
                        ofParameters.add(thisBuilder.build());
                    }
                    break;
                } //else TODO
            }
            if (linkLevelRv == 1) {

                // *************************************************
                // explicit dependence of return variable to parameter
                // *************************************************
                Set<TypeParameter> typeParametersOfSubTo;
                Variable subTo;
                if (!methodInfo.typeParameters().isEmpty()) {
                    // TestShallow,9b
                    typeParametersOfSubTo = Stream.concat(methodInfo.typeParameters().stream(),
                            hcThisTps == null ? Stream.of() : hcThisTps.stream()).collect(Collectors.toUnmodifiableSet());
                    VirtualFields vfSubTo = virtualFieldComputer.compute(pi.parameterizedType(), false).virtualFields();
                    subTo = runtime.newFieldReference(vfSubTo.hiddenContent(), runtime.newVariableExpression(pi),
                            vfSubTo.hiddenContent().type());
                } else {
                    typeParametersOfSubTo = hcThisTps;
                    subTo = pi;
                }
                transfer(ofReturnValue, returnType, null, subTo.parameterizedType(), subTo, false, null,
                        typeParametersOfSubTo, false);
                // TODO we may have to convert hcThisTps to method parameters
            } else if (!independent.isIndependent()) {
                if (hcThis != null && !methodInfo.isStatic()) {

                    // *************************************************
                    // instance method, from parameter into object
                    // *************************************************

                    transfer(piBuilder, pi.parameterizedType(), null, hcThis.type(), hcThisFr, independent.isDependent(),
                            vfThis.mutable(), hcThisTps, false);
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
                        transfer(ofReturnValue, returnType, null, sourceVariable.parameterizedType(),
                                sourceVariable, false, null, sourceVariableTps, false);
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
                    transfer(piBuilder, pi.parameterizedType(), null, sourceHc.type(), sourceFr, false,
                            null, sourceTps, false);
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
                          VirtualFields fromVf,
                          ParameterizedType toType,
                          Variable subTo,
                          boolean dependent,
                          FieldInfo subMutable,
                          Set<TypeParameter> typeParametersOfSubTo,
                          boolean force) {
        if (fromType.typeParameter() != null && fromType.arrays() == 0) {
            assert fromType.typeParameter().typeBounds().isEmpty() : """
                    cannot deal with type bounds at the moment; obviously, if a type bound is mutable,
                    the type parameter can be dependent""";
            LinkNature linkNature = deriveLinkNature(0, toType.arrays());
            if (fromType.typeParameter().equals(toType.typeParameter())) {
                // 'to' is a type parameter, e.g. T[] ts
                // Optional.orElseGet(Supplier<T>) -> orElseGet == Λ0:supplier
                builder.add(linkNature, subTo);
            } else if (toType.typeParameter() == null) {
                // 'to' is a container (array); we'll need a slice
                FF theField = findField(fromType.typeParameter(), toType.typeInfo());
                if (theField != null) {
                    if (subTo.parameterizedType().arrays() == 0) {
                        // indexing: TestShallowPrefix,1:
                        FieldReference subSubTo = runtime.newFieldReference(theField.fieldInfo,
                                runtime.newVariableExpression(subTo), theField.fieldInfo.type());
                        builder.add(IS_IDENTICAL_TO, subSubTo);
                    } else {
                        DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subTo),
                                runtime.newInt(theField.negative()));
                        Expression scope = runtime.newVariableExpression(dv);
                        FieldReference slice = runtime.newFieldReference(theField.fieldInfo, scope, theField.fieldInfo.type());
                        builder.add(linkNature, slice);
                    }
                }
            }
        } else {
            VirtualFields vfFromType;
            if (fromType.typeInfo() != null && fromType.typeInfo().typeNature() == VIRTUAL_FIELD) {
                assert fromVf != null;
                vfFromType = fromVf;
            } else {
                vfFromType = virtualFieldComputer.compute(fromType, false).virtualFields();
            }
            if (vfFromType.hiddenContent() != null) {
                FieldReference subFrom = runtime().newFieldReference(vfFromType.hiddenContent(),
                        runtime.newVariableExpression(builder.primary()), vfFromType.hiddenContent().type());
                // concrete method type parameters
                Set<TypeParameter> typeParametersFrom =
                        collectTypeParametersFromVirtualField(vfFromType.hiddenContent().type());
                // more formal type parameters in HC
                int arraysFrom = vfFromType.hiddenContent().type().arrays();
                int arraysTo = toType.arrays();
                if (typeParametersFrom.equals(typeParametersOfSubTo) || force) {
                    boolean toIsTp = subTo.parameterizedType().isTypeParameter();
                    boolean fromIsTp = subFrom.parameterizedType().isTypeParameter();
                    if (subTo.parameterizedType().isFunctionalInterface()) {
                        // T into Stream<T> for Stream.generate(Supplier<T>)
                        builder.add(subFrom, INTERSECTION_NOT_EMPTY, subTo);
                    } else if (toIsTp && fromIsTp || !toIsTp && !fromIsTp && arraysAligned(subFrom.parameterizedType(),
                            subTo.parameterizedType())) {
                        // e.g. new ArrayList<>(Collection<> c) this.es ~ c.es
                        // e.g. Map.entrySet() entrySet.kvs ~ this.kvs
                        // e.g. TestShallowPrefix,1 oneInstance.xys>this.xy
                        LinkNature linkNature = deriveLinkNature(arraysFrom, arraysTo);
                        builder.add(subFrom, linkNature, subTo);
                    } else if (!toIsTp && !fromIsTp) {
                        // e.g. TestShallowPrefix,2 oneInstance.xsys to .xy; arrays not aligned (different!)
                        // result  oneInstance.xsys.xs>this.xy.x,oneInstance.xsys.ys>this.xy.y
                        // TODO this is very dedicated to this situation, others exist
                        for (FieldInfo fieldFrom : subFrom.parameterizedType().typeInfo().fields()) {
                            FieldInfo fieldTo;
                            if (fieldFrom.type().typeParameter() != null) {
                                FF ff = findField(fieldFrom.type().typeParameter(), subTo.parameterizedType().typeInfo());
                                fieldTo = ff == null ? null : ff.fieldInfo;
                            } else {
                                fieldTo = findField(fieldFrom.type(), subTo.parameterizedType().typeInfo());
                            }
                            if (fieldTo != null) {
                                FieldReference subSubFrom = runtime.newFieldReference(fieldFrom,
                                        runtime.newVariableExpression(subFrom), fieldFrom.type());
                                FieldReference subSubTo = runtime.newFieldReference(fieldTo,
                                        runtime.newVariableExpression(subTo), fieldTo.type());
                                builder.add(subSubFrom, CONTAINS, subSubTo);
                            }
                        }
                    } else {
                        // List.toArray()
                        if (arraysFrom == arraysTo) {
                            builder.add(subFrom, INTERSECTION_NOT_EMPTY, subTo);
                        } else {
                            throw new UnsupportedOperationException("NYI");
                        }
                    }
                } else {
                    List<TypeParameter> intersection = new ArrayList<>(typeParametersFrom.stream()
                            .sorted(Comparator.comparingInt(TypeParameter::getIndex)).toList());
                    intersection.retainAll(typeParametersOfSubTo);
                    if (intersection.size() == 1) {
                        if (arraysFrom > 0) {
                            if (arraysTo == arraysFrom) {
                                if (toType.typeParameter() != null) {
                                    FF theField = findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                                    assert theField != null;
                                    DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subFrom),
                                            runtime.newInt(theField.negative()));
                                    builder.add(dv, INTERSECTION_NOT_EMPTY, subTo);
                                } else {
                                    // slice across: TestShallow,4: keySet.ks~this.kvs[-1].k
                                    FF theField = findField(intersection.getFirst(), toType.typeInfo());
                                    assert theField != null;
                                    DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subTo),
                                            runtime.newInt(theField.negative()));
                                    Expression scope = runtime.newVariableExpression(dv);
                                    FieldReference slice = runtime.newFieldReference(theField.fieldInfo, scope,
                                            theField.fieldInfo.type());
                                    builder.add(subFrom, INTERSECTION_NOT_EMPTY, slice);
                                }
                            } else if (arraysTo == 0) {
                                // slice to a single element: TestShallowPrefix,1: oneStatic.xys[-1]>0:x
                                FF theField = findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                                assert theField != null;
                                DependentVariable dv = runtime.newDependentVariable(runtime().newVariableExpression(subFrom),
                                        runtime.newInt(theField.negative()));
                                builder.add(dv, CONTAINS, subTo);
                            }
                        } else {
                            // indexing, e.g. TestShallowPrefix,3:  oneStatic.xy.x==0:x (totalFrom 0)
                            // e.g. TestShallowPrefix,2: oneStatic.xsys.xs>0:x (totalFrom 1)
                            FF theField = findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                            assert theField != null;
                            int totalFrom = arraysFrom + theField.fieldInfo.type().arrays();
                            LinkNature linkNature = deriveLinkNature(totalFrom, arraysTo);
                            FieldReference subSubFrom = runtime.newFieldReference(theField.fieldInfo,
                                    runtime.newVariableExpression(subFrom), theField.fieldInfo.type());
                            builder.add(subSubFrom, linkNature, subTo);
                        }
                    }
                }

                if (dependent) {
                    if (vfFromType.mutable() != null && subMutable != null) {
                        FieldReference mTarget = runtime.newFieldReference(vfFromType.mutable(),
                                runtime.newVariableExpression(builder.primary()), vfFromType.mutable().type());
                        FieldReference mSource = runtime.newFieldReference(subMutable);
                        builder.add(mTarget, IS_IDENTICAL_TO, mSource);
                    } else {
                        throw new UnsupportedOperationException("Expected VF " + vfFromType + " of " +
                                                                fromType + " to have a 'mutable' field");
                    }
                }
            }
        }
    }

    // e.g. kvs, xys is aligned, ksvs is not aligned with kvs
    private boolean arraysAligned(ParameterizedType fromPt, ParameterizedType toPt) {
        List<FieldInfo> fromFields = fromPt.typeInfo().fields();
        return //fromPt.arrays() == toPt.arrays() &&
                fromFields.size() == toPt.typeInfo().fields().size() &&
                fromFields.stream().allMatch(fi ->
                        fi.type().arrays() == toPt.typeInfo().fields().get(fi.indexInType()).type().arrays());
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
        public int negative() {
            return -1 - index;
        }
    }

    private static FF findField(TypeParameter typeParameter, TypeInfo container) {
        int i = 0;
        for (FieldInfo fieldInfo : container.fields()) {
            if (typeParameter.equals(fieldInfo.type().typeParameter())) {
                return new FF(fieldInfo, i);
            }
            ++i;
        }
        return null;
    }

    private static FieldInfo findField(ParameterizedType fieldType, TypeInfo container) {
        for (FieldInfo fieldInfo : container.fields()) {
            if (fieldType.equals(fieldInfo.type())) {
                return fieldInfo;
            }
        }
        return null;
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
