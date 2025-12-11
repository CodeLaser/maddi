package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Given an interface, or a class in the JDK for which we do not want to parse the sources (shallow analysis),
construct virtual fields (actually make them, but don't attach them to the type) that can be used for type- and
modification linking.

Rules:

Functional interface in java.util.function: no fields, by definition
Comparator<X>: no fields , by computation (hidden content, but never retrieved)

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

    public VirtualFields computeAllowTypeParameterArray(ParameterizedType pt) {
        if (pt.arrays() > 0) {
            return arrayType(pt);
        }
        return compute(pt.typeInfo());
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
        FieldInfo mutable = runtime.newFieldInfo("$m" + namedType.simpleName(),
                false, atomicBooleanPt, typeInfo);
        String hcName = namedType.simpleName().toLowerCase() + "s";
        FieldInfo hiddenContent = runtime.newFieldInfo(hcName, false, pt, typeInfo);
        return new VirtualFields(mutable, hiddenContent);
    }

    public VirtualFields compute(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrCreate(VirtualFields.VIRTUAL_FIELDS, () -> computeOnDemand(typeInfo));
    }

    public VirtualFields computeOnDemand(TypeInfo typeInfo) {
        List<VirtualFields> virtualFieldsOfSuperTypes = typeInfo.recursiveSuperTypeStream().map(this::compute).toList();

        if ("java.util.function".equals(typeInfo.packageName())) return VirtualFields.NONE;
        Value.Immutable immutable = typeInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.MUTABLE);
        FieldInfo mutable;
        if (immutable.isMutable()) {
            FieldInfo inSuperTypes = virtualFieldsOfSuperTypes.stream().filter(vf -> vf.mutable() != null)
                    .findFirst().map(VirtualFields::mutable).orElse(null);
            if (inSuperTypes != null) {
                mutable = inSuperTypes;
            } else {
                mutable = runtime.newFieldInfo("$m", false, atomicBooleanPt, typeInfo);
            }
        } else {
            mutable = null;
        }
        FieldInfo hiddenContent;
        int multiplicity = maxMultiplicityFromMethods(typeInfo);
        if (multiplicity == 0 || typeInfo.typeParameters().isEmpty()) {
            hiddenContent = null;
        } else {
            FieldInfo inSuperType = copyFromSuperType(typeInfo);
            if (inSuperType != null) {
                hiddenContent = inSuperType;
            } else {
                ParameterizedType hcTypeWithArrays;
                String baseName;
                List<TypeParameter> filteredTypeParameters = typeInfo.typeParameters().stream()
                        .filter(VirtualFieldComputer::notRecursive)
                        .toList();
                if (filteredTypeParameters.size() == 1) {
                    TypeParameter typeParameter = filteredTypeParameters.getFirst();
                    baseName = typeParameter.simpleName().toLowerCase();
                    hcTypeWithArrays = runtime.newParameterizedType(typeParameter, multiplicity - 1, null);
                } else {
                    TypeInfo hcType = makeContainerType(typeInfo, filteredTypeParameters);
                    baseName = hcType.simpleName().toLowerCase();
                    hcTypeWithArrays = runtime.newParameterizedType(hcType, multiplicity - 1);
                }
                String fieldName = baseName + ("s".repeat(multiplicity - 1));
                hiddenContent = runtime.newFieldInfo(fieldName, false, hcTypeWithArrays, typeInfo);
            }
        }
        return new VirtualFields(mutable, hiddenContent);
    }

    private static boolean notRecursive(TypeParameter tp) {
        assert tp.typeBoundsAreSet();
        return tp.typeBounds().stream().noneMatch(pt -> pt.extractTypeParameters().contains(tp));
    }

    private FieldInfo copyFromSuperType(TypeInfo typeInfo) {
        Stream<ParameterizedType> parentStream = Stream.ofNullable(
                typeInfo.parentClass() == null || typeInfo.parentClass().isJavaLangObject() ? null : typeInfo.parentClass());
        Stream<ParameterizedType> superStream = Stream.concat(parentStream, typeInfo.interfacesImplemented().stream());
        return superStream.filter(pt -> sameSetOfTypeParameters(typeInfo.typeParameters(), pt))
                .findFirst()
                .map(pt -> computeOnDemand(pt.typeInfo()).hiddenContent())
                .orElse(null);
    }

    private boolean sameSetOfTypeParameters(List<TypeParameter> typeParameters, ParameterizedType pt) {
        return pt.extractTypeParameters().equals(typeParameters.stream().collect(Collectors.toUnmodifiableSet()));
    }

    private TypeInfo makeContainerType(TypeInfo typeInfo, List<TypeParameter> filteredTypeParameters) {
        String name = filteredTypeParameters.stream().map(TypeParameter::simpleName).collect(Collectors.joining());
        TypeInfo newType = runtime.newTypeInfo(typeInfo, name);
        newType.builder().setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .setAccess(runtime.accessPublic());
        for (TypeParameter tp : filteredTypeParameters) {
            FieldInfo fieldInfo = runtime.newFieldInfo(tp.simpleName().toLowerCase(), false,
                    runtime.newParameterizedType(tp, 0, null), newType);
            fieldInfo.builder().addFieldModifier(runtime.fieldModifierFinal())
                    .addFieldModifier(runtime.fieldModifierPublic())
                    .setInitializer(runtime.newEmptyExpression())
                    .computeAccess().commit();
            newType.builder().addField(fieldInfo);
        }
        newType.builder().commit();
        return newType;
    }

    public int maxMultiplicityFromMethods(TypeInfo typeInfo) {
        // base for many computations
        if (multi2.contains(typeInfo)) return 2;
        int multiplicity = 0;
        for (MethodInfo methodInfo : typeInfo.constructorsAndMethods()) {
            ParameterizedType returnType = methodInfo.returnType();
            int m = computeMultiplicity(returnType);
            multiplicity = Math.max(multiplicity, m);
            for (ParameterInfo pi : methodInfo.parameters()) {
                int mpi = computeMultiplicity(pi.parameterizedType());
                multiplicity = Math.max(multiplicity, mpi);
            }
        }
        return multiplicity;
    }

    public int computeMultiplicity(ParameterizedType parameterizedType) {
        int arrays = parameterizedType.arrays();
        if (arrays > 0) {
            int withoutArrays = computeMultiplicity(parameterizedType.copyWithoutArrays());
            return withoutArrays == 0 ? 0 : withoutArrays + arrays;
        }
        if (parameterizedType.typeParameter() != null) {
            return parameterizedType.typeParameter().isMethodTypeParameter() ? 0 : 1;
        }
        ParameterizedType wrapped = wrapped(parameterizedType);
        if (wrapped != null) {
            int m = computeMultiplicity(wrapped);
            return m == 0 ? 0 : m + 1;
        }
        Set<TypeParameter> typeParameters = parameterizedType.extractTypeParameters();

        return typeParameters.isEmpty() ? 0 : 1;
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
}
