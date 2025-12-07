package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final ParameterizedType iterable;
    private final ParameterizedType map;
    private final GenericsHelper genericsHelper;

    public VirtualFieldComputer(JavaInspector javaInspector) {
        this.runtime = javaInspector.runtime();
        TypeInfo atomicBoolean = javaInspector.compiledTypesManager().getOrLoad(AtomicBoolean.class);
        this.atomicBooleanPt = atomicBoolean.asParameterizedType();
        this.iterable = javaInspector.compiledTypesManager().getOrLoad(Iterable.class).asParameterizedType();
        this.map = javaInspector.compiledTypesManager().getOrLoad(Map.class).asParameterizedType();
        this.genericsHelper = new GenericsHelperImpl(runtime);
    }

    public VirtualFields compute(TypeInfo typeInfo) {
        if ("java.util.function".equals(typeInfo.packageName())) return VirtualFields.NONE;
        Value.Immutable immutable = typeInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.MUTABLE);
        FieldInfo mutable;
        if (immutable.isMutable()) {
            mutable = runtime.newFieldInfo("$m", false, atomicBooleanPt, typeInfo);
        } else {
            mutable = null;
        }
        FieldInfo hiddenContent;
        int multiplicity = computeMultiplicity(typeInfo);
        if (multiplicity == 0 || typeInfo.typeParameters().isEmpty()) {
            hiddenContent = null;
        } else {
            ParameterizedType hcTypeWithArrays;
            String baseName;
            if (typeInfo.typeParameters().size() == 1) {
                TypeParameter typeParameter = typeInfo.typeParameters().getFirst();
                baseName = typeParameter.simpleName().toLowerCase();
                hcTypeWithArrays = runtime.newParameterizedType(typeParameter, multiplicity - 1, null);
            } else {
                TypeInfo hcType = makeRecordType(typeInfo.typeParameters());
                baseName = hcType.simpleName().toLowerCase();
                hcTypeWithArrays = runtime.newParameterizedType(hcType, multiplicity - 1);
            }
            String fieldName = baseName + ("s".repeat(multiplicity - 1));
            hiddenContent = runtime.newFieldInfo(fieldName, false, hcTypeWithArrays, typeInfo);
        }
        return new VirtualFields(mutable, hiddenContent);
    }

    private TypeInfo makeRecordType(List<TypeParameter> typeParameters) {
        throw new UnsupportedOperationException();
    }

    private int computeMultiplicity(TypeInfo typeInfo) {
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


    private int computeMultiplicity(ParameterizedType parameterizedType) {
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
        
        return 0;
    }

    private ParameterizedType wrapped(ParameterizedType parameterizedType) {
        TypeInfo typeInfo = parameterizedType.typeInfo();
        if (typeInfo != null) {
            if (typeInfo.equals(iterable.typeInfo()) && parameterizedType.parameters().size() == 1) {
                return parameterizedType.parameters().getFirst();
            }
            if (iterable.isAssignableFrom(runtime, parameterizedType)) {
                var map = genericsHelper.mapInTermsOfParametersOfSuperType(typeInfo, iterable);
                return map.entrySet().stream().findFirst().orElseThrow().getValue();
            }

        }
        return null;
    }
}
