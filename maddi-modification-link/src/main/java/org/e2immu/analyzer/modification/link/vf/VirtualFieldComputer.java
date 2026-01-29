package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.VariableTranslationMap;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.VirtualFieldTranslationMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.element.Keyword;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final AtomicInteger typeCounter = new AtomicInteger();

    public static final String VF_CHAR = "§";
    public static final String VF_CONCRETE = "$";

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

    public FieldInfo newMField(TypeInfo typeInfo) {
        return newField("m", atomicBooleanPt, typeInfo);
    }

    public VirtualFields compute(TypeInfo typeInfo) {
        return compute(typeInfo.asParameterizedType(), false).virtualFields;
    }

    public record VfTm(VirtualFields virtualFields, VirtualFieldTranslationMap formalToConcrete) {
    }

    private static final VfTm NONE_NONE = new VfTm(NONE, null);

    public VfTm compute(ParameterizedType pt, boolean addTranslation) {
        int arrays = pt.arrays();
        if (pt.isTypeParameter()) {
            return typeParameter(pt, arrays);
        }
        TypeInfo typeInfo = pt.typeInfo();
        if (arrays == 0 && (typeInfo == null
                            || typeInfo.isPrimitiveExcludingVoid()
                            || typeInfo.isVoid()
                            || typeInfo.typeNature() == VIRTUAL_FIELD
                            || typeInfo.packageName().equals("java.util.function"))) {
            return NONE_NONE;
        }
        if (pt.parameters().isEmpty() && arrays == 0) {
            return typeInfoWithoutTypeParametersAndArrays(pt, typeInfo);
        }

        int extraMultiplicity = arrays + maxMultiplicityFromMethods(typeInfo) - 1;
        if (extraMultiplicity < 0) return NONE_NONE;

        // we'll need to recursively extend the current vf; they'll be the basis of our hc
        List<VfTm> parameterVfs = pt.parameters().stream()
                .map(param -> compute(param, addTranslation))
                .toList();
        FieldInfo hiddenContent;
        VirtualFieldTranslationMap fieldTm = addTranslation
                ? new VirtualFieldTranslationMapImpl(this, runtime) : null;
        if (parameterVfs.isEmpty()) {
            String baseName = "$";
            hiddenContent = newField(baseName + "s".repeat(arrays), pt, typeInfo);
        } else {
            VfTm first = parameterVfs.getFirst();
            assert !pt.parameters().isEmpty();
            if (pt.parameters().size() == 1) {
                hiddenContent = singleTypeParameter(pt, first, extraMultiplicity, typeInfo, fieldTm);
            } else {
                hiddenContent = multipleTypeParameters(pt, parameterVfs, typeInfo, extraMultiplicity, fieldTm);
            }
        }
        FieldInfo mutable = makeMutable(arrays, parameterVfs, typeInfo);
        return new VfTm(new VirtualFields(mutable, hiddenContent), addTranslation ? fieldTm : null);
    }

    private FieldInfo multipleTypeParameters(ParameterizedType pt,
                                             List<VfTm> parameterVfs,
                                             TypeInfo typeInfo,
                                             int extraMultiplicity,
                                             VirtualFieldTranslationMap fieldTm) {
        FieldInfo hiddenContent;
        List<FieldInfo> hiddenContentComponents = new ArrayList<>(pt.parameters().size());
        StringBuilder baseName = new StringBuilder();
        StringBuilder typeName = new StringBuilder();
        int j = 0;
        for (ParameterizedType param : pt.parameters()) {
            VfTm vfTm = parameterVfs.get(j);
            FieldInfo hc = vfTm == null ? null : vfTm.virtualFields.hiddenContent();
            if (hc != null && hc.type().typeParameter() != null) {
                hiddenContentComponents.add(hc);
                baseName.append(hc.simpleName());
                typeName.append(hc.type().typeParameter().simpleName())
                        .append("S".repeat(hc.type().arrays()));
            } else {
                FieldInfo fi = newField("$" + "s".repeat(param.arrays()), param, typeInfo);
                hiddenContentComponents.add(fi);
                baseName.append(fi.simpleName());
                typeName.append("$").append("S".repeat(param.arrays()));
            }
            ++j;
        }
        TypeInfo containerType = makeContainerType(typeInfo, typeName.toString(), hiddenContentComponents);
        ParameterizedType baseType = containerType.asParameterizedType();
        hiddenContent = newField(baseName + "s".repeat(extraMultiplicity),
                baseType.copyWithArrays(extraMultiplicity), typeInfo);

        if (fieldTm != null) {
            for (FieldInfo formalHiddenContent : hiddenContentHierarchy(typeInfo)) {
                TypeInfo formalContainerType = formalHiddenContent.type().typeInfo();
                int i = 0;
                for (FieldInfo field : formalContainerType.fields()) {
                    ParameterizedType outType = containerType.fields().get(i).type();
                    int arrayDiff = outType.arrays() - field.type().arrays();
                    fieldTm.put(field.type().typeParameter(), outType.copyWithArrays(arrayDiff));
                    ++i;
                }
            }
        }
        return hiddenContent;
    }

    private @Nullable FieldInfo singleTypeParameter(ParameterizedType pt,
                                                    VfTm first,
                                                    int extraMultiplicity,
                                                    TypeInfo typeInfo,
                                                    VirtualFieldTranslationMap fieldTm) {
        FieldInfo hiddenContent;
        if (first == null) {
            hiddenContent = null;
        } else {
            FieldInfo hc = first.virtualFields.hiddenContent();
            if (hc != null) {
                hiddenContent = newField(hc.name() + "s".repeat(extraMultiplicity),
                        hc.type().copyWithArrays(hc.type().arrays() + extraMultiplicity), typeInfo);
            } else {
                hiddenContent = null;
            }
        }
        if (fieldTm != null) {
            ParameterizedType p0 = pt.parameters().getFirst();
            ParameterizedType replaceBy = hiddenContent != null
                    ? hiddenContent.type()
                    : p0.copyWithArrays(p0.arrays() + extraMultiplicity);
            for (FieldInfo formalHiddenContent : hiddenContentHierarchy(typeInfo)) {
                if (formalHiddenContent != null) {
                    int arrayDiff = Math.abs(formalHiddenContent.type().arrays() - replaceBy.arrays());
                    fieldTm.put(formalHiddenContent.type().typeParameter(), replaceBy.copyWithArrays(arrayDiff));
                }
            }
        }
        return hiddenContent;
    }

    private @Nullable FieldInfo makeMutable(int arrays, List<VfTm> parameterVfs, TypeInfo typeInfo) {
        if (arrays > 0 || parameterVfs.stream().anyMatch(vf -> vf != null && vf.virtualFields.mutable() != null)) {
            return newMField(typeInfo);
        }
        Value.Immutable immutable = typeInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.MUTABLE);
        return immutable.isMutable() ? newMField(typeInfo) : null;
    }

    private @NotNull VfTm typeInfoWithoutTypeParametersAndArrays(ParameterizedType pt, TypeInfo typeInfo) {
        if (utilityClass(typeInfo)) {
            return NONE_NONE;
        }
        // normal class without type parameters
        Value.Immutable immutable = typeInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.MUTABLE);
        FieldInfo mutable = immutable.isMutable() ? newMField(typeInfo) : null;
        FieldInfo hiddenContent = newField("" + typeCounter.getAndIncrement(), pt, typeInfo);
        VirtualFields vf = new VirtualFields(mutable, hiddenContent);
        return new VfTm(vf, null);
    }

    private @NotNull VfTm typeParameter(ParameterizedType pt, int arrays) {
        VirtualFields vf;
        NamedType namedType = pt.typeParameter();
        TypeInfo typeInfo = pt.typeParameter().typeInfo();
        String baseName = namedType.simpleName().toLowerCase();

        if (arrays > 0) {
            // there'll be multiple "mutable" fields on "typeInfo", so we append the type parameter name
            FieldInfo mutable = newField("m" + namedType.simpleName(), atomicBooleanPt, typeInfo);
            String hcName = baseName + "s".repeat(arrays);
            vf = new VirtualFields(mutable, newField(hcName, pt, typeInfo));
        } else {
            // this one is always temporary; it is there as the basis of the recursion
            vf = new VirtualFields(null, newField(baseName, pt, typeInfo));
        }
        return new VfTm(vf, null);
    }

    private boolean utilityClass(TypeInfo typeInfo) {
        // TODO use own code for this, but for now:
        return typeInfo.methodStream().allMatch(MethodInfo::isStatic)
               && typeInfo.constructors().stream().allMatch(c -> c.parameters().isEmpty())
               && typeInfo.fields().stream().allMatch(FieldInfo::isStatic);
    }

    public TypeInfo makeContainerType(TypeInfo typeInfo, String typeName, List<FieldInfo> hiddenContentComponents) {
        TypeInfo newType = runtime.newTypeInfo(typeInfo, typeName);
        newType.builder()
                .setSynthetic(true)
                .setTypeNature(VIRTUAL_FIELD)
                .setParentClass(runtime.objectParameterizedType())
                .setAccess(runtime.accessPublic());
        for (FieldInfo fi : hiddenContentComponents) {
            TypeParameter tp = fi.type().typeParameter();
            int arrays = fi.type().arrays();
            FieldInfo fieldInfo = runtime.newFieldInfo(VF_CHAR + (tp == null ? "$" : tp.simpleName().toLowerCase())
                                                       + "s".repeat(arrays), false,
                    tp == null ? fi.type().copyWithArrays(arrays)
                            : runtime.newParameterizedType(tp, arrays, null), newType);
            fieldInfo.builder().addFieldModifier(runtime.fieldModifierFinal())
                    .addFieldModifier(runtime.fieldModifierPublic())
                    .setInitializer(runtime.newEmptyExpression())
                    .computeAccess().commit();
            newType.builder().addField(fieldInfo);
        }
        newType.builder().commit();
        return newType;
    }

    public static TypeInfo makeContainer(Runtime runtime, TypeInfo enclosingType, String name, List<FieldInfo> newFields) {
        TypeInfo newType = runtime.newTypeInfo(enclosingType, name);
        TypeInfo.Builder builder = newType.builder();
        builder.setTypeNature(VIRTUAL_FIELD)
                .setSynthetic(true)
                .setParentClass(runtime.objectParameterizedType())
                .setAccess(runtime.accessPublic());
        newFields.forEach(builder::addField);
        newFields.forEach(fi -> {
            if (fi.type().typeParameter() != null) builder.addOrSetTypeParameter(fi.type().typeParameter());
        });
        builder.commit();
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
        // extra check '!wrapped.equals(parameterizedType)' to avoid infinite recursion
        if (wrapped != null && !wrapped.equals(parameterizedType)) {
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

        VfTm vfTmFormal = compute(typeInfo.asParameterizedType(), false);
        FieldInfo formalHiddenContent = vfTmFormal.virtualFields.hiddenContent();
        return Stream.concat(Stream.ofNullable(formalHiddenContent), fromHigher.stream()).toList();
    }

    private VirtualFields arrayType(ParameterizedType pt) {
        NamedType namedType;
        TypeInfo typeInfo;
        String baseName;
        if (pt.typeParameter() != null) {
            namedType = pt.typeParameter();
            typeInfo = pt.typeParameter().typeInfo();
            baseName = namedType.simpleName().toLowerCase();
        } else {
            namedType = pt.typeInfo();
            typeInfo = pt.typeInfo();
            baseName = "$";
        }
        // there'll be multiple "mutable" fields on "typeInfo", so we append the type parameter name
        FieldInfo mutable = newField("m" + namedType.simpleName(), atomicBooleanPt, typeInfo);
        String hcName = baseName + "s".repeat(pt.arrays());
        FieldInfo hiddenContent = newField(hcName, pt, typeInfo);
        return new VirtualFields(mutable, hiddenContent);
    }

    public FieldInfo newField(String name, ParameterizedType type, TypeInfo owner) {
        return newField(runtime, name, type, owner);
    }

    public static FieldInfo newField(Runtime runtime, String name, ParameterizedType type, TypeInfo owner) {
        String cleanName = VF_CHAR + name.replace(VF_CHAR, "");
        return newFieldKeepName(runtime, cleanName, type, owner);
    }

    public static FieldInfo newFieldKeepName(Runtime runtime, String name, ParameterizedType type, TypeInfo owner) {
        FieldInfo fi = runtime.newFieldInfo(name, false, type, owner);
        fi.builder().setInitializer(runtime.newEmptyExpression()).commit();
        return fi;
    }

    public static final TypeNature VIRTUAL_FIELD = new TypeNature() {

        @Override
        public Keyword keyword() {
            return new Keyword() {
                @Override
                public String minimal() {
                    return "VF";
                }

                @Override
                public String write(FormattingOptions options) {
                    return "VF";
                }
            };
        }
    };

    public record M2(Variable m1, Variable m2) {
    }

    public M2 addModificationFieldEquivalence(Variable from, Variable to) {
        if (Util.needsVirtual(from.parameterizedType()) && Util.needsVirtual(to.parameterizedType())) {
            // FIXME what when one needs virtual, and the other does not? is technically possible;
            Value.Immutable immutableTo = new AnalysisHelper().typeImmutable(to.parameterizedType());
            Value.Immutable immutableFrom = new AnalysisHelper().typeImmutable(to.parameterizedType());
            Value.Immutable worst = immutableFrom.min(immutableTo);
            if (worst.isMutable()) {
                FieldInfo f1 = newMField(VariableTranslationMap.owner(runtime, from.parameterizedType()));
                FieldReference m1 = runtime.newFieldReference(f1, runtime.newVariableExpression(from), f1.type());
                FieldInfo f2 = newMField(VariableTranslationMap.owner(runtime, to.parameterizedType()));
                FieldReference m2 = runtime.newFieldReference(f2, runtime.newVariableExpression(to), f2.type());
                return new M2(m1, m2);
            }
        }
        return null;
    }
}
