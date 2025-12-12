package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.vf.VirtualFields.NONE;

/*
Given an interface, or a class in the JDK for which we do not want to parse the sources (shallow analysis),
construct virtual fields (actually make them, but don't attach them to the type) that can be used for type- and
modification linking.

Rules:

Functional interface in java.util.function: no fields, by definition
Comparable<X>: no fields , by computation (hidden content, but never retrieved)

Modifiable types: a field called $m of type AtomicBoolean (could have been any type, but AtomicBoolean is modifiable
and a boolean seems the right value: if I've been modified, then you're modified.)

Types with type parameters, representing the hidden content:
- we make a distinction between types with one type parameter, and types with multiple type parameters
- we must determine if the parameters ever occur in a "multiple" situation (Optional<X> does not, List<X> does).
  We'll use arrays to indicate the level of "multiplicity", as in List<List<X>> is level 2, corresponding to X[][]
- types like Comparable have multiplicity 0, they never return hidden content.

One type parameter, called E: single: E e, multiple one: E[] es, multiple two: E[][] ess, etc.
Multiple type parameters: create a container record for any combination of the type parameters by using their names,
and wrap all of them in the container containing all. E.g. for type parameters X and Y, make
    XY(X x, Y y); single: XY xy; multiple one: XY[] xys, multiple two: XY[][] xyss, etc.
For three type parameters TSV, we'll get
    TSV(T t, S s, V v, TS ts, SV sv, TV tv); single TSV tsv, multiple one TSV[] tsvs, multiple two TSV[][] tsvss, ...

A multiple one of TS combinations in a multiple one TSV will be expressed by tsvs[-1].ts
The index -1 will be used to indicate "slicing".
 */
public class VirtualFieldComputer {
    private final ParameterizedType atomicBooleanPt;
    private final Runtime runtime;
    private final GenericsHelper genericsHelper;
    private final Set<TypeInfo> multi2;

    public VirtualFieldComputer(JavaInspector javaInspector) {
        this.runtime = javaInspector.runtime();
        TypeInfo atomicBoolean = javaInspector.compiledTypesManager().getOrLoad(AtomicBoolean.class);
        this.atomicBooleanPt = atomicBoolean.asParameterizedType();
        ParameterizedType iterable = javaInspector.compiledTypesManager().getOrLoad(Iterable.class).asParameterizedType();
        ParameterizedType iterator = javaInspector.compiledTypesManager().getOrLoad(Iterator.class).asParameterizedType();
        this.genericsHelper = new GenericsHelperImpl(runtime);
        multi2 = Set.of(iterable.typeInfo(), iterator.typeInfo());
    }

    // ----- computation of "temporary" virtual fields

    public VirtualFields compute(TypeInfo typeInfo) {
        return computeAllowTypeParameterArray(typeInfo.asParameterizedType(), false).virtualFields;
    }

    public record VfTm(VirtualFields virtualFields, TranslationMap formalToConcrete) {
    }

    private static final VfTm NONE_NONE = new VfTm(NONE, null);

    public VfTm computeAllowTypeParameterArray(ParameterizedType pt, boolean addTranslation) {
        if (pt.arrays() > 0) {
            return new VfTm(arrayType(pt), null);
        }
        if (pt.isTypeParameter()) {
            if (notRecursive(pt.typeParameter())) {
                // this one is always temporary; it is there as the basis of the recursion
                VirtualFields vf = new VirtualFields(null,
                        newField(pt.typeParameter().simpleName().toLowerCase(), pt, pt.typeParameter().typeInfo()));
                return new VfTm(vf, null);
            }
            return NONE_NONE;
        }
        if (pt.parameters().isEmpty()) {
            return NONE_NONE;
        }
        TypeInfo typeInfo = pt.typeInfo();
        if (typeInfo.packageName().equals("java.util.function")) {
            return NONE_NONE;
        }
        int extraMultiplicity = maxMultiplicityFromMethods(typeInfo) - 1;
        if (extraMultiplicity < 0) return NONE_NONE;

        // we'll need to recursively extend the current vf; they'll be the basis of our hc
        List<VfTm> parameterVfs = pt.parameters().stream()
                .map(param -> computeAllowTypeParameterArray(param, addTranslation))
                .filter(vftm -> vftm.virtualFields != NONE)
                .toList();
        FieldInfo mutable;
        if (parameterVfs.stream().anyMatch(vf -> vf.virtualFields.mutable() != null)) {
            mutable = newField("$m", atomicBooleanPt, pt.typeInfo());
        } else {
            Value.Immutable immutable = typeInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE,
                    ValueImpl.ImmutableImpl.MUTABLE);
            mutable = immutable.isMutable() ? newField("$m", atomicBooleanPt, typeInfo)
                    : null;
        }
        FieldInfo hiddenContent;
        FieldTranslationMap fieldTm = addTranslation ? new FieldTranslationMap(runtime) : null;

        if (parameterVfs.isEmpty()) {
            hiddenContent = null; // nothing here, and nothing in the parameters
        } else if (parameterVfs.size() == 1) {
            VirtualFields inner = parameterVfs.getFirst().virtualFields;
            FieldInfo hc = inner.hiddenContent();
            if (hc != null) {
                hiddenContent = newField(hc.name() + "s".repeat(extraMultiplicity),
                        hc.type().copyWithArrays(hc.type().arrays() + extraMultiplicity), pt.typeInfo());

                // translation from formal to concrete hidden content variable
                if (addTranslation) {
                    for (FieldInfo formalHiddenContent : hiddenContentHierarchy(typeInfo)) {
                        fieldTm.put(formalHiddenContent.type().typeParameter(), hiddenContent.type().typeParameter());
                    }
                }
            } else {
                hiddenContent = null;
            }
        } else if (parameterVfs.stream().allMatch(vf -> vf.virtualFields.hiddenContent() != null
                                                        && vf.virtualFields.hiddenContent().type().isTypeParameter())) {
            TypeInfo containerType = makeContainerType(typeInfo, parameterVfs.stream()
                    .map(vf -> vf.virtualFields.hiddenContent()).toList());
            ParameterizedType baseType = containerType.asParameterizedType();
            String baseName = parameterVfs.stream()
                    .map(vf -> vf.virtualFields.hiddenContent().name()).collect(Collectors.joining());
            hiddenContent = newField(baseName + "s".repeat(extraMultiplicity),
                    baseType.copyWithArrays(extraMultiplicity), pt.typeInfo());

            if (addTranslation) {
                for (FieldInfo formalHiddenContent : hiddenContentHierarchy(typeInfo)) {
                    TypeInfo formalContainerType = formalHiddenContent.type().typeInfo();
                    int i = 0;
                    for (FieldInfo field : formalContainerType.fields()) {
                        fieldTm.put(field.type().typeParameter(), containerType.fields().get(i).type().typeParameter());
                        ++i;
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("NYI");
        }

        return new VfTm(new VirtualFields(mutable, hiddenContent), addTranslation ? fieldTm : null);
    }


    private static boolean notRecursive(TypeParameter tp) {
        assert tp.typeBoundsAreSet();
        return tp.typeBounds().stream().noneMatch(pt -> pt.extractTypeParameters().contains(tp));
    }

    private TypeInfo makeContainerType(TypeInfo typeInfo, List<FieldInfo> hiddenContentComponents) {
        String name = hiddenContentComponents.stream()
                .map(fi -> fi.type().typeParameter().simpleName() + "S".repeat(fi.type().arrays()))
                .collect(Collectors.joining());
        TypeInfo newType = runtime.newTypeInfo(typeInfo, name);
        newType.builder().setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .setAccess(runtime.accessPublic());
        for (FieldInfo fi : hiddenContentComponents) {
            TypeParameter tp = fi.type().typeParameter();
            int arrays = fi.type().arrays();
            FieldInfo fieldInfo = runtime.newFieldInfo(tp.simpleName().toLowerCase()
                                                       + "s".repeat(arrays), false,
                    runtime.newParameterizedType(tp, arrays, null), newType);
            fieldInfo.builder().addFieldModifier(runtime.fieldModifierFinal())
                    .addFieldModifier(runtime.fieldModifierPublic())
                    .setInitializer(runtime.newEmptyExpression())
                    .computeAccess().commit();
            newType.builder().addField(fieldInfo);
        }
        newType.builder().commit();
        return newType;
    }

    // package for testing
    int maxMultiplicityFromMethods(TypeInfo typeInfo) {
        // base for many computations
        if (multi2.contains(typeInfo)) return 2;
        int multiplicity = 0;
        for (MethodInfo methodInfo : typeInfo.constructorsAndMethods()) {
            ParameterizedType returnType = methodInfo.returnType();
            int m = computeMultiplicity(returnType, true);
            multiplicity = Math.max(multiplicity, m);
        }
        return multiplicity;
    }

    private int computeMultiplicity(ParameterizedType parameterizedType, boolean ignoreMethodParameters) {
        int arrays = parameterizedType.arrays();
        if (arrays > 0) {
            int withoutArrays = computeMultiplicity(parameterizedType.copyWithoutArrays(), ignoreMethodParameters);
            return withoutArrays == 0 ? 0 : withoutArrays + arrays;
        }
        if (parameterizedType.typeParameter() != null) {
            return parameterizedType.typeParameter().isMethodTypeParameter() && ignoreMethodParameters ? 0 : 1;
        }
        ParameterizedType wrapped = wrapped(parameterizedType);
        if (wrapped != null) {
            int m = computeMultiplicity(wrapped, ignoreMethodParameters);
            return m == 0 ? 0 : m + 1;
        }
        return hasTypeParametersTestVirtualFields(parameterizedType, new HashSet<>()) ? 1 : 0;
    }

    private boolean hasTypeParametersTestVirtualFields(ParameterizedType pt, Set<ParameterizedType> visited) {
        if (pt.isTypeParameter()) return true;
        TypeInfo typeInfo = pt.typeInfo();
        if (typeInfo == null) return false;
        if (!typeInfo.typeParameters().isEmpty()) return true;
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (visited.add(fieldInfo.type()) && hasTypeParametersTestVirtualFields(fieldInfo.type(), visited))
                return true;
        }
        return false;
    }


    public static Set<TypeParameter> collectTypeParametersFromVirtualField(ParameterizedType virtualFieldPt) {
        return collectTypeParametersFromVirtualField(virtualFieldPt, new HashSet<>());
    }

    private static Set<TypeParameter> collectTypeParametersFromVirtualField(ParameterizedType pt,
                                                                            Set<ParameterizedType> visited) {
        if (pt.isTypeParameter()) return Set.of(pt.typeParameter());
        TypeInfo typeInfo = pt.typeInfo();
        if (typeInfo == null) return Set.of();
        Set<TypeParameter> collect = new HashSet<>();
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (visited.add(fieldInfo.type())) {
                Set<TypeParameter> recursive = collectTypeParametersFromVirtualField(fieldInfo.type(), visited);
                collect.addAll(recursive);
            }
        }
        return collect;
    }

    private ParameterizedType wrapped(ParameterizedType parameterizedType) {
        if (parameterizedType.isPrimitiveStringClass() || parameterizedType.isVoid()) {
            return null; // saves some more complex tests, frequent!
        }
        TypeInfo typeInfo = parameterizedType.typeInfo();
        if (typeInfo != null) {
            for (TypeInfo m2 : multi2) {
                if (typeInfo.equals(m2.typeInfo()) && parameterizedType.parameters().size() == 1) {
                    return parameterizedType.parameters().getFirst();
                }
                ParameterizedType m2Pt = m2.asParameterizedType();
                if (m2Pt.isAssignableFrom(runtime, parameterizedType)) {
                    var map = genericsHelper.mapInTermsOfParametersOfSuperType(typeInfo, m2Pt);
                    return map.entrySet().stream().findFirst().orElseThrow().getValue();
                }
            }
        }
        return null;
    }


    private boolean sameSetOfTypeParameters(List<TypeParameter> typeParameters, ParameterizedType pt) {
        return pt.extractTypeParameters().equals(typeParameters.stream().collect(Collectors.toUnmodifiableSet()));
    }

    private List<FieldInfo> hiddenContentHierarchy(TypeInfo typeInfo) {
        Stream<ParameterizedType> parentStream = Stream.ofNullable(
                typeInfo.parentClass() == null || typeInfo.parentClass().isJavaLangObject() ? null : typeInfo.parentClass());
        Stream<ParameterizedType> superStream = Stream.concat(parentStream, typeInfo.interfacesImplemented().stream());
        List<FieldInfo> fromHigher = superStream
                .filter(pt -> sameSetOfTypeParameters(typeInfo.typeParameters(), pt))
                .map(ParameterizedType::typeInfo)
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(ti -> hiddenContentHierarchy(ti).stream())
                .toList();

        VfTm vfTmFormal = computeAllowTypeParameterArray(typeInfo.asParameterizedType(), false);
        FieldInfo formalHiddenContent = vfTmFormal.virtualFields.hiddenContent();
        return Stream.concat(Stream.of(formalHiddenContent), fromHigher.stream()).toList();
    }

    private VirtualFields arrayType(ParameterizedType pt) {
        NamedType namedType;
        TypeInfo typeInfo;
        if (pt.typeParameter() != null) {
            namedType = pt.typeParameter();
            typeInfo = pt.typeParameter().typeInfo();
        } else {
            namedType = pt.typeInfo();
            typeInfo = pt.typeInfo();
        }
        // there'll be multiple "mutable" fields on "typeInfo", so we append the type parameter name
        FieldInfo mutable = newField("$m" + namedType.simpleName(), atomicBooleanPt, typeInfo);
        String hcName = namedType.simpleName().toLowerCase() + "s".repeat(pt.arrays());
        FieldInfo hiddenContent = newField(hcName, pt, typeInfo);
        return new VirtualFields(mutable, hiddenContent);
    }

    private FieldInfo newField(String name, ParameterizedType type, TypeInfo owner) {
        FieldInfo fi = runtime.newFieldInfo(name, false, type, owner);
        fi.builder().setInitializer(runtime.newEmptyExpression()).commit();
        return fi;
    }
}
