package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
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

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.VIRTUAL_FIELD;
import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.collectTypeParametersFromVirtualField;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.GET_SET_FIELD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_METHOD;

/*
rules for ⊆ instead of ≡, ~

- the return value of a function can have its hidden content ⊆ the hidden content of its source.
  This is because the hidden content must come from the source.
- a varargs parameter can have its hidden content ⊆ the destination. This is an extension of the use of multiple
  single elements.
- a new object can have its hidden content ⊆ the constructor parameters
- the input of a lambda function/consumer is ⊆ this

These rules mean that we still use ~ for the 'normal' transfer of hidden content from parameter to/from this.

The ⊆ / ⊇ links must change into a ~ after a modification on such an object.
Their main advantage is that ∈ followed by ⊆ is still ∈.
Their natural application is a stream, where e.g. the filter input is part of the filter source,
the filter output again part of the input, etc. Many streams reduce the amount of hidden content from a source.
 */
public record ShallowMethodLinkComputer(Runtime runtime, VirtualFieldComputer virtualFieldComputer) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShallowMethodLinkComputer.class);

    public MethodLinkedVariables go(MethodInfo methodInfo) {
        LOGGER.debug("Computing method linked variables of {}", methodInfo);
        ParameterizedType returnType = methodInfo.returnType();
        TypeInfo typeInfo = methodInfo.typeInfo();
        ReturnVariableImpl rv = new ReturnVariableImpl(methodInfo);

        Value.FieldValue fv = methodInfo.analysis().getOrDefault(GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
        boolean abstractGetSet = fv.field() != null && methodInfo.isAbstract()
                                 // TODO better way of checking "non-standard get/set"
                                 && methodInfo.annotations().stream().anyMatch(ae -> "GetSet".equals(ae.typeInfo().simpleName()));
        if (abstractGetSet) {
            if (fv.setter()) {
                return abstractSetter(methodInfo, fv, rv);
            }
            return abstractGetter(methodInfo, fv, rv);
        }

        // virtual fields of the default source = the object ~ "this"
        VirtualFields vfThis = virtualFieldComputer.compute(typeInfo.asParameterizedType(),
                false).virtualFields();
        FieldInfo hcThis = vfThis.hiddenContent();
        FieldReference hcThisFr = hcThis == null ? null : runtime.newFieldReference(hcThis);
        Set<TypeParameter> hcThisTps = hcThis == null ? null : correspondingTypeParameters(typeInfo, hcThis);
        Links.Builder ofReturnValue = new LinksImpl.Builder(rv);

        // *************************************************
        // instance method, from return variable to object
        // *************************************************

        boolean forceIntoReturn;
        if (methodInfo.hasReturnValue() && !methodInfo.isStatic() && hcThis != null) {
            Value.Independent independent = methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD,
                    ValueImpl.IndependentImpl.DEPENDENT);
            Map<Integer, Integer> dependencies = independent.linkToParametersReturnValue();
            int linkLevelRv = dependencies.getOrDefault(-1, -1);
            forceIntoReturn = linkLevelRv == 1; //HC from return variable to object
            if (!independent.isIndependent()) {
                transfer(ofReturnValue, returnType, null, hcThis.type(), hcThisFr, independent,
                        vfThis.mutable(), hcThisTps, forceIntoReturn, IS_SUBSET_OF, false);
            }
        } else {
            forceIntoReturn = false;
        }

        List<Links> ofParameters = new ArrayList<>(methodInfo.parameters().size());
        Set<Variable> modified = new HashSet<>();
        if (methodInfo.isModifying() && !methodInfo.isIgnoreModification() && !methodInfo.isFinalizer()) {
            modified.add(runtime.newThis(typeInfo.asParameterizedType()));
        }

        for (ParameterInfo pi : methodInfo.parameters()) {
            if (pi.isModified() && !pi.isIgnoreModifications()) {
                Value.Immutable immutable = new AnalysisHelper().typeImmutable(typeInfo, pi.parameterizedType());
                if (immutable.isMutable()) {
                    modified.add(pi);
                }
            }
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
                    ParameterizedType sourceType = findReturnType(pi.parameterizedType());
                    Set<TypeParameter> sourceVariableTps = collectTypeParametersFromVirtualField(sourceType);
                    Set<TypeParameter> returnTypeTps = returnType.extractTypeParameters();
                    if (sourceVariableTps.equals(returnTypeTps)) {
                        // return types agree
                        transfer(ofReturnValue, returnType, null, sourceType, pi, null, null,
                                sourceVariableTps, false, IS_SUBSET_OF, false);
                    }
                    ofParameters.add(piBuilder.build());
                    continue;
                } else if (inputHasTypeParameters && !outputHasTypeParameters && !forceIntoReturn) {
                    // Consumer<T>
                    Set<TypeParameter> sourceVariableTps = pi.parameterizedType().parameters().stream()
                            .flatMap(param -> collectTypeParametersFromVirtualField(param).stream())
                            .collect(Collectors.toUnmodifiableSet());
                    if (sourceVariableTps.equals(hcThisTps)) {
                        // from parameter into object
                        assert hcThis != null;
                        Links.Builder thisBuilder = new LinksImpl.Builder(runtime.newThis(typeInfo.asParameterizedType()));
                        transfer(thisBuilder, hcThis.type(), vfThis, pi.parameterizedType(), pi, independent,
                                vfThis.mutable(), hcThisTps, false, IS_SUPERSET_OF, true);
                        ofParameters.add(thisBuilder.build());
                    } else {
                        ofParameters.add(piBuilder.build());
                    }
                    continue;
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
                    VirtualFields vfSubTo = virtualFieldComputer.compute(pi.parameterizedType(),
                            false).virtualFields();
                    subTo = runtime.newFieldReference(vfSubTo.hiddenContent(), runtime.newVariableExpression(pi),
                            vfSubTo.hiddenContent().type());
                } else {
                    typeParametersOfSubTo = hcThisTps;
                    subTo = pi;
                }
                transfer(ofReturnValue, returnType, null, subTo.parameterizedType(), subTo,
                        null, null, typeParametersOfSubTo, false, IS_SUBSET_OF,
                        false);
                // TODO we may have to convert hcThisTps to method parameters
            } else if (!independent.isIndependent()) {
                if (hcThis != null && !methodInfo.isStatic()) {

                    // *************************************************
                    // instance method, from parameter into object
                    // *************************************************

                    transfer(piBuilder, pi.parameterizedType(), null, hcThis.type(), hcThisFr,
                            independent,
                            vfThis.mutable(), hcThisTps, false,
                            methodInfo.isConstructor() ? IS_SUPERSET_OF : SHARES_ELEMENTS, true);
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
                                sourceVariable, null, null, sourceVariableTps, false,
                                IS_SUBSET_OF, false);
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
                    VirtualFields sourceVfs = virtualFieldComputer.compute(source.parameterizedType(),
                            false).virtualFields();
                    Set<TypeParameter> sourceTps = correspondingTypeParameters(source.parameterizedType().typeInfo(),
                            sourceVfs.hiddenContent()).stream()
                            .map(tp -> formalToConcrete(tp, source.parameterizedType()))
                            .collect(Collectors.toUnmodifiableSet());
                    FieldInfo sourceHc = sourceVfs.hiddenContent();
                    Expression scope = runtime.newVariableExpression(source);
                    FieldReference sourceFr = runtime.newFieldReference(sourceHc, scope, sourceHc.type());
                    transfer(piBuilder, pi.parameterizedType(), null, sourceHc.type(), sourceFr, null,
                            null, sourceTps, false, IS_SUBSET_OF, false);
                }
            }
            ofParameters.add(piBuilder.build());
        }

        // dedicated code for the @Identity annotation; from parameter directly into return value
        if (methodInfo.isIdentity()) {
            ofReturnValue.add(IS_ASSIGNED_FROM, methodInfo.parameters().getFirst());
        }

        return new MethodLinkedVariablesImpl(ofReturnValue.build(), ofParameters, Set.copyOf(modified));
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
                          Value.Independent independent,
                          FieldInfo subMutable,
                          Set<TypeParameter> typeParametersOfSubTo,
                          boolean force,
                          LinkNature subShareSuper,
                          boolean reverseIsAssignedFrom) {
        FieldInfo vfFromTypeMutable;
        if (fromType.typeParameter() != null && fromType.arrays() == 0) {
            LinkNature linkNature = deriveLinkNature(0, toType.arrays(), subShareSuper, reverseIsAssignedFrom);
            if (fromType.typeParameter().equals(toType.typeParameter())) {
                // 'to' is a type parameter, e.g. T[] ts
                // Optional.orElseGet(Supplier<T>) -> orElseGet == Λ0:supplier
                builder.add(linkNature, subTo);
            } else if (toType.typeParameter() == null) {
                // 'to' is a container (array); we'll need a slice
                SliceFactory.FF theField = SliceFactory.findField(fromType.typeParameter(), toType.typeInfo());
                if (theField != null) {
                    if (subTo.parameterizedType().arrays() == 0) {
                        // indexing: TestShallowPrefix,1:
                        FieldReference subSubTo = runtime.newFieldReference(theField.fieldInfo(),
                                runtime.newVariableExpression(subTo), theField.fieldInfo().type());
                        builder.add(IS_ASSIGNED_TO, subSubTo);
                    } else {
                        DependentVariable slice = SliceFactory.create(runtime, subTo, theField.negative(),
                                theField.fieldInfo());
                        builder.add(linkNature, slice);
                    }
                }
            }
            TypeInfo bestType = fromType.bestTypeInfo();
            if (bestType != null) {
                VirtualFields vf = virtualFieldComputer.compute(bestType);
                vfFromTypeMutable = vf.mutable();
            } else {
                vfFromTypeMutable = null;
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
                        builder.add(subFrom, subShareSuper, subTo);
                    } else if (toIsTp && fromIsTp || !toIsTp && !fromIsTp && arraysAligned(subFrom.parameterizedType(),
                            subTo.parameterizedType())) {
                        // e.g. new ArrayList<>(Collection<> c) this.es ~ c.es
                        // e.g. Map.entrySet() entrySet.kvs ~ this.kvs
                        // e.g. TestShallowPrefix,1 oneInstance.xys>this.xy
                        LinkNature linkNature = deriveLinkNature(arraysFrom, arraysTo, subShareSuper, reverseIsAssignedFrom);
                        builder.add(subFrom, linkNature, subTo);
                    } else if (!toIsTp && !fromIsTp) {
                        // e.g. TestShallowPrefix,2 oneInstance.xsys to .xy; arrays not aligned (different!)
                        // result  oneInstance.xsys.xs>this.xy.x,oneInstance.xsys.ys>this.xy.y
                        // TODO this is very dedicated to this situation, others exist
                        for (FieldInfo fieldFrom : subFrom.parameterizedType().typeInfo().fields()) {
                            if (Util.virtual(fieldFrom)) {
                                FieldInfo fieldTo;
                                if (fieldFrom.type().typeParameter() != null) {
                                    SliceFactory.FF ff = SliceFactory.findField(fieldFrom.type().typeParameter(),
                                            subTo.parameterizedType().typeInfo());
                                    fieldTo = ff == null ? null : ff.fieldInfo();
                                } else {
                                    fieldTo = findField(fieldFrom.type(), subTo.parameterizedType().typeInfo());
                                }
                                if (fieldTo != null) {
                                    FieldReference subSubFrom = runtime.newFieldReference(fieldFrom,
                                            runtime.newVariableExpression(subFrom), fieldFrom.type());
                                    FieldReference subSubTo = runtime.newFieldReference(fieldTo,
                                            runtime.newVariableExpression(subTo), fieldTo.type());
                                    builder.add(subSubFrom, CONTAINS_AS_MEMBER, subSubTo);
                                }
                            }
                        }
                    } else {
                        // List.toArray()
                        if (arraysFrom == arraysTo) {
                            builder.add(subFrom, IS_SUBSET_OF, subTo);
                        } // no idea what to do, see TestShallow,12
                    }
                } else {
                    List<TypeParameter> intersection = new ArrayList<>(typeParametersFrom.stream()
                            .sorted(Comparator.comparingInt(TypeParameter::getIndex)).toList());
                    intersection.retainAll(typeParametersOfSubTo);
                    if (intersection.size() == 1) {
                        if (arraysFrom > 0) {
                            if (arraysTo == arraysFrom) {
                                if (toType.typeParameter() != null) {
                                    SliceFactory.FF theField = SliceFactory.findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                                    assert theField != null;
                                    DependentVariable dv = SliceFactory.create(runtime, subFrom, theField.negative(),
                                            theField.fieldInfo());
                                    builder.add(dv, IS_SUBSET_OF, subTo);
                                } else {
                                    // slice across: TestShallow,4: keySet.ks~this.kvs[-1].k
                                    SliceFactory.FF theField = SliceFactory.findField(intersection.getFirst(), toType.typeInfo());
                                    assert theField != null;
                                    DependentVariable slice = SliceFactory.create(runtime, subTo, theField.negative(),
                                            theField.fieldInfo());
                                    builder.add(subFrom, IS_SUBSET_OF, slice);
                                }
                            } else if (arraysTo == 0) {
                                // slice to a single element: TestShallowPrefix,1: oneStatic.xys[-1]>0:x
                                SliceFactory.FF theField = SliceFactory.findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                                assert theField != null;
                                DependentVariable slice = SliceFactory.create(runtime, subFrom, theField.negative(),
                                        theField.fieldInfo());
                                builder.add(slice, CONTAINS_AS_MEMBER, subTo);
                            }
                        } else {
                            // indexing, e.g. TestShallowPrefix,3:  oneStatic.xy.x==0:x (totalFrom 0)
                            // e.g. TestShallowPrefix,2: oneStatic.xsys.xs>0:x (totalFrom 1)
                            SliceFactory.FF theField = SliceFactory.findField(intersection.getFirst(), subFrom.parameterizedType().typeInfo());
                            assert theField != null;
                            int totalFrom = arraysFrom + theField.fieldInfo().type().arrays();
                            LinkNature linkNature = deriveLinkNature(totalFrom, arraysTo, subShareSuper, reverseIsAssignedFrom);
                            FieldReference subSubFrom = runtime.newFieldReference(theField.fieldInfo(),
                                    runtime.newVariableExpression(subFrom), theField.fieldInfo().type());
                            builder.add(subSubFrom, linkNature, subTo);
                        }
                    }
                }
                vfFromTypeMutable = vfFromType.mutable();
            } else {
                vfFromTypeMutable = null;
            }
        }
        if (independent != null
            && (independent.isDependent() || !independent.dependentMethods().isEmpty())
            && vfFromTypeMutable != null
            && subMutable != null) {
            FieldReference mTarget = runtime.newFieldReference(vfFromTypeMutable,
                    runtime.newVariableExpression(builder.primary()), vfFromTypeMutable.type());
            FieldReference mSource = runtime.newFieldReference(subMutable);
            LinkNature linkNature = LinkNatureImpl.makeIdenticalTo(independent.dependentMethods());
            builder.add(mTarget, linkNature, mSource);
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

    private static LinkNature deriveLinkNature(int arrays,
                                               int arraysSource,
                                               LinkNature subShareSuper,
                                               boolean reverseIsAssignedFrom) {
        if (arrays == arraysSource) {
            return arrays == 0 ? (reverseIsAssignedFrom ? IS_ASSIGNED_TO : IS_ASSIGNED_FROM) : subShareSuper;
        }
        if (arrays < arraysSource) {
            return IS_ELEMENT_OF;
        }
        return CONTAINS_AS_MEMBER;
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

    // see TestStaticValuesRecord,7: @GetSet("variables") Object variable(int i);
    // we can't really use the virtual field computer, because this is a "fake" getter/setter

    private MethodLinkedVariables abstractGetter(MethodInfo methodInfo, Value.FieldValue fv, ReturnVariableImpl rv) {
        LinksImpl.Builder builder = new LinksImpl.Builder(rv);
        if (methodInfo.parameters().isEmpty()) {
            // rv ≡ this.§t  (where §t is a virtual field representing an object of type 'baseType')
            FieldReference t = runtime.newFieldReference(fv.field());
            builder.add(IS_ASSIGNED_FROM, t);
        } else if (methodInfo.parameters().size() == 1) {
            // rv ∈ this.§ts (where §ts is a virtual field representing an array of 'baseType' elements)
            FieldReference ts = runtime.newFieldReference(fv.field());
            builder.add(IS_ELEMENT_OF, ts);
            DependentVariable dv = runtime.newDependentVariable(runtime.newVariableExpression(ts),
                    runtime.newVariableExpression(methodInfo.parameters().getFirst()));
            builder.add(IS_ASSIGNED_FROM, dv);
        }
        return new MethodLinkedVariablesImpl(builder.build(), methodInfo.parameters().isEmpty()
                ? List.of() : List.of(LinksImpl.EMPTY), Set.of());
    }

    private MethodLinkedVariables abstractSetter(MethodInfo methodInfo,
                                                 Value.FieldValue fv,
                                                 ReturnVariable rv) {
        ParameterInfo piValue = methodInfo.parameters().get(fv.parameterIndexOfValue());
        ParameterInfo piIndex;
        LinksImpl.Builder builder = new LinksImpl.Builder(piValue);
        FieldReference fr;
        if (methodInfo.parameters().size() == 1) {
            builder.add(IS_ASSIGNED_TO, runtime.newFieldReference(fv.field()));
            piIndex = null;
            fr = null;
        } else {
            assert methodInfo.parameters().size() == 2;
            assert fv.hasIndex();
            fr = runtime.newFieldReference(fv.field());
            piIndex = methodInfo.parameters().get(fv.parameterIndexOfIndex());
            VariableExpression fieldVe = runtime.newVariableExpression(fr);
            builder.add(IS_ASSIGNED_TO, runtime.newDependentVariable(fieldVe, runtime.newVariableExpression(piIndex)));
            builder.add(IS_ELEMENT_OF, fr);
        }
        Links returnLinks;
        if (methodInfo.isFluent()) {
            LinksImpl.Builder fluentBuilder = new LinksImpl.Builder(rv)
                    .add(IS_ASSIGNED_FROM, runtime.newThis(methodInfo.typeInfo().asSimpleParameterizedType()));
            if (methodInfo.parameters().size() == 1) {
                returnLinks = fluentBuilder
                        .add(runtime.newFieldReference(fv.field(), runtime.newVariableExpression(rv),
                                fv.field().type()), IS_ASSIGNED_FROM, piValue)
                        .build();
            } else {
                VariableExpression fieldVe = runtime.newVariableExpression(runtime.newFieldReference(fv.field(),
                        runtime.newVariableExpression(rv), fv.field().type()));
                returnLinks = fluentBuilder
                        .add(runtime.newDependentVariable(fieldVe, runtime.newVariableExpression(piIndex)),
                                IS_ASSIGNED_FROM, piValue)
                        .build();
            }
        } else {
            returnLinks = LinksImpl.EMPTY;
        }
        List<Links> paramList;
        Set<Variable> modified;
        This thisVar = runtime().newThis(methodInfo.typeInfo().asParameterizedType());
        if (methodInfo.parameters().size() == 1) {
            paramList = List.of(builder.build());
            modified = Set.of(thisVar);
        } else if (fv.parameterIndexOfIndex() == 0) {
            paramList = List.of(LinksImpl.EMPTY, builder.build());
            assert fr != null;
            modified = Set.of(thisVar, fr);
        } else {
            paramList = List.of(builder.build(), LinksImpl.EMPTY);
            assert fr != null;
            modified = Set.of(thisVar, fr);
        }
        return new MethodLinkedVariablesImpl(returnLinks, paramList, modified);
    }

    // TestShallowFunctional,2, case where the interface is not one of the java.util.function.* interfaces
    private ParameterizedType findReturnType(ParameterizedType fiType) {
        TypeInfo best = fiType.bestTypeInfo();
        MethodInfo sam = best.singleAbstractMethod();
        assert sam != null : "Not a functional interface if it hasn't got a SAM";
        Map<NamedType, ParameterizedType> map;
        if (sam.typeInfo() == best) {
            map = fiType.initialTypeParameterMap();
        } else {
            map = new GenericsHelperImpl(runtime).mapInTermsOfParametersOfSuperType(best, sam.typeInfo().asParameterizedType());
        }
        return sam.returnType().withWildcard(null).applyTranslation(runtime, map).withWildcard(null);
    }
}
