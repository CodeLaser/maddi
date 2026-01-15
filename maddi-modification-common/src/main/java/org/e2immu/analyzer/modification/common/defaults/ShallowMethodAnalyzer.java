/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.common.defaults;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.MessageImpl;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer.AnnotationOrigin.*;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;

/*
Important to note that, because it is used in the modification analyzer (first called by MethodModAnalyzer
then values are overwritten by the AbstractInfoAnalyzer), only non-default values are to be written to the
property-value map (analysis()).
 */
public class ShallowMethodAnalyzer extends AnnotationToProperty {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShallowMethodAnalyzer.class);
    private final AnalysisHelper analysisHelper;
    private final Map<TypeInfo, Set<TypeInfo>> hierarchyProblems = new HashMap<>();
    private final List<Message> messages = new LinkedList<>();


    public ShallowMethodAnalyzer(Runtime runtime, AnnotationProvider annotationProvider) {
        super(runtime, annotationProvider);
        this.analysisHelper = new AnalysisHelper();
    }

    public Map<Info, ShallowAnalyzer.InfoData> analyze(MethodInfo methodInfo) {
        if (methodInfo.analysis().getOrDefault(DEFAULTS_ANALYZER, FALSE).isTrue()) {
            return Map.of(); // already done
        }
        methodInfo.analysis().set(DEFAULTS_ANALYZER, TRUE);
        Map<Info, ShallowAnalyzer.InfoData> dataMap = new HashMap<>();

        boolean explicitlyEmpty = methodInfo.explicitlyEmptyMethod();

        List<AnnotationExpression> annotations = annotationProvider.annotations(methodInfo);
        Map<Property, Value> map = annotationsToMap(methodInfo, annotations);
        Map<Property, ValueOrigin> voMap = new HashMap<>();
        map.forEach((p, v) -> voMap.put(p, new ValueOrigin(v, ShallowAnalyzer.AnnotationOrigin.ANNOTATED)));

        methodPropertiesBeforeParameters(methodInfo, voMap, explicitlyEmpty);

        for (ParameterInfo parameterInfo : methodInfo.parameters()) {
            Map<Property, ValueOrigin> parameterVoMap = handleParameter(parameterInfo, voMap, explicitlyEmpty);
            ShallowAnalyzer.InfoData parameterData = new ShallowAnalyzer.InfoData(new HashMap<>());
            dataMap.put(parameterInfo, parameterData);
            parameterVoMap.forEach((p, vo) -> {
                if (!vo.value().isDefault() || vo.origin() == ANNOTATED) {
                    parameterInfo.analysis().setAllowControlledOverwrite(p, vo.value());
                }
                parameterData.put(p, vo.origin());
            });
        }

        methodPropertiesAfterParameters(methodInfo, voMap);

        ShallowAnalyzer.InfoData methodData = new ShallowAnalyzer.InfoData(new HashMap<>());
        dataMap.put(methodInfo, methodData);
        voMap.forEach((p, vo) -> {
            if (!vo.value().isDefault() || vo.origin() == ANNOTATED) {
                methodInfo.analysis().setAllowControlledOverwrite(p, vo.value());
            }
            methodData.put(p, vo.origin());
        });
        return dataMap;
    }

    private ValueOrigin computeMethodContainer(MethodInfo methodInfo) {
        ParameterizedType returnType = methodInfo.returnType();
        if (returnType.arrays() > 0 || returnType.isPrimitiveExcludingVoid() || returnType.isVoid()) {
            return FROM_TYPE_TRUE;
        }
        if (returnType.isReturnTypeOfConstructor()) {
            return FROM_TYPE_NO_VALUE;
        }
        TypeInfo bestType = returnType.bestTypeInfo();
        if (bestType == null) {
            return DEFAULT_FALSE; // unbound type parameter
        }

        // check formal return type
        Value.Bool fromReturnType = bestType.analysis().getOrDefault(CONTAINER_TYPE, FALSE);
        Value.Bool bestOfOverrides = methodInfo.overrides().stream()
                .map(mi -> mi.analysis().getOrDefault(CONTAINER_TYPE, FALSE))
                .reduce(FALSE, Value.Bool::or);
        Value.Bool formal = bestOfOverrides.or(fromReturnType);
        if (formal.isTrue()) return FROM_TYPE_TRUE;

        // check identity and parameter contract
        if (methodInfo.analysis().getOrDefault(IDENTITY_METHOD, FALSE).isTrue()) {
            ParameterInfo p0 = methodInfo.parameters().getFirst();
            return new ValueOrigin(p0.analysis().getOrDefault(CONTAINER_PARAMETER, FALSE), FROM_PARAMETER);
        }
        return DEFAULT_FALSE;
    }

    private void methodPropertiesAfterParameters(MethodInfo methodInfo, Map<Property, ValueOrigin> map) {
        ValueOrigin c = map.get(CONTAINER_METHOD);
        if (c == null) {
            map.put(CONTAINER_METHOD, computeMethodContainer(methodInfo));
        }
        ValueOrigin imm = map.get(IMMUTABLE_METHOD);
        if (imm == null) {
            map.put(IMMUTABLE_METHOD, computeMethodImmutable(methodInfo));
        }
        ValueOrigin ind = map.get(INDEPENDENT_METHOD);
        if (ind == null) {
            map.put(INDEPENDENT_METHOD, computeMethodIndependent(methodInfo, map));
        }
        ValueOrigin nn = map.get(NOT_NULL_METHOD);
        if (nn == null) {
            map.put(NOT_NULL_METHOD, computeMethodNotNull(methodInfo, map));
        }
    }

    private static final ValueOrigin NOT_NULL_FROM_METHOD = new ValueOrigin(ValueImpl.NotNullImpl.NOT_NULL,
            ShallowAnalyzer.AnnotationOrigin.FROM_METHOD);
    private static final ValueOrigin NOT_NULL_FROM_TYPE = new ValueOrigin(ValueImpl.NotNullImpl.NOT_NULL,
            ShallowAnalyzer.AnnotationOrigin.FROM_TYPE);

    private ValueOrigin computeMethodNotNull(MethodInfo methodInfo, Map<Property, ValueOrigin> map) {
        if (methodInfo.isConstructor() || methodInfo.isVoid()) {
            return NOT_NULL_FROM_METHOD;
        }
        if (methodInfo.returnType().isPrimitiveExcludingVoid()) {
            return NOT_NULL_FROM_TYPE;
        }
        ValueOrigin fluent = map.get(FLUENT_METHOD);
        if (fluent.valueAsBool().isTrue()) return NOT_NULL_FROM_METHOD;
        NotNullProperty v = ValueImpl.NotNullImpl.NULLABLE;
        for (MethodInfo mi : methodInfo.overrides()) {
            if (mi.isPublic()) {
                NotNullProperty nn = mi.analysis().getOrDefault(NOT_NULL_METHOD, ValueImpl.NotNullImpl.NULLABLE);
                v = v.max(nn);
            }
        }
        return new ValueOrigin(v, v == ValueImpl.NotNullImpl.NULLABLE ? DEFAULT : FROM_OVERRIDE);
    }

    private static final ValueOrigin MUTABLE_DEFAULT = new ValueOrigin(ValueImpl.ImmutableImpl.MUTABLE, DEFAULT);

    private ValueOrigin computeMethodImmutable(MethodInfo methodInfo) {
        ParameterizedType returnType = methodInfo.returnType();
        Immutable immutable = analysisHelper.typeImmutable(returnType);
        if (immutable == null) {
            LOGGER.warn("No immutable value for {}", returnType);
            return MUTABLE_DEFAULT;
        }
        return immutable.equals(ValueImpl.ImmutableImpl.MUTABLE) ? MUTABLE_DEFAULT : new ValueOrigin(immutable, FROM_TYPE);
    }


    private ValueOrigin computeMethodIndependent(MethodInfo methodInfo, Map<Property, ValueOrigin> map) {
        ValueOrigin returnValueIndependentVo = computeMethodIndependentReturnValue(methodInfo, map);
        Value.Independent returnValueIndependent = (Independent) returnValueIndependentVo.value();

        // typeIndependent is set by hand in AnnotatedAPI files
        Value.Independent typeIndependent = methodInfo.typeInfo().analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        Value.Independent bestOfOverrides = methodInfo.overrides().stream()
                .filter(MethodInfo::isPublic)
                .map(mi -> mi.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT))
                .reduce(DEPENDENT, Value.Independent::max);
        ShallowAnalyzer.AnnotationOrigin origin;
        Value.Independent max = returnValueIndependent.max(bestOfOverrides).max(typeIndependent);
        if (max.isDependent()) {
            origin = DEFAULT;
        } else if (typeIndependent.equals(max)) {
            origin = FROM_OWNER;
        } else if (bestOfOverrides.equals(max)) {
            origin = FROM_OVERRIDE;
        } else {
            origin = FROM_TYPE;
        }
        if (max.isIndependentHc() && methodInfo.isFactoryMethod()) {
            // at least one of the parameters must be independent HC!!
            boolean hcParam = methodInfo.parameters().stream()
                    .anyMatch(pa -> pa.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT).isIndependentHc());
            if (!hcParam) {
                LOGGER.warn("@Independent(hc=true) factory method must have at least one @Independent(hc=true) parameter");
            }
        }
        return new ValueOrigin(max, origin);
    }

    private static final ValueOrigin INDEPENDENT_FROM_METHOD = new ValueOrigin(INDEPENDENT, FROM_METHOD);
    private static final ValueOrigin INDEPENDENT_FROM_TYPE = new ValueOrigin(INDEPENDENT, FROM_TYPE);
    private static final ValueOrigin INDEPENDENT_HC_FROM_TYPE = new ValueOrigin(INDEPENDENT_HC, FROM_TYPE);
    private static final ValueOrigin DEPENDENT_DEFAULT = new ValueOrigin(DEPENDENT, DEFAULT);

    private ValueOrigin computeMethodIndependentReturnValue(MethodInfo methodInfo, Map<Property, ValueOrigin> map) {
        if (methodInfo.isConstructor() || methodInfo.isVoid()) {
            return INDEPENDENT_FROM_METHOD;
        }
        if (methodInfo.isStatic() && !methodInfo.isFactoryMethod()) {
            // if factory method, we link return value to parameters, otherwise independent by default
            return INDEPENDENT_FROM_METHOD;
        }
        ValueOrigin identity = map.get(IDENTITY_METHOD);
        ValueOrigin modified = map.get(NON_MODIFYING_METHOD);
        if (identity.valueAsBool().isTrue() && modified.valueAsBool().isFalse()) {
            return INDEPENDENT_FROM_METHOD; // @Identity + @NotModified -> must be @Independent
        }
        // from here on we're assuming the result is linked to the fields.

        ParameterizedType pt = methodInfo.returnType();
        if (pt.arrays() > 0) {
            // array type, like int[]
            return DEPENDENT_DEFAULT;
        }
        TypeInfo bestType = pt.bestTypeInfo();
        if (bestType == null || bestType.isJavaLangObject()) {
            // unbound type parameter T, or unbound with array T[], T[][]
            return INDEPENDENT_HC_FROM_TYPE;
        }
        if (bestType.isPrimitiveExcludingVoid()) {
            return INDEPENDENT_FROM_TYPE;
        }
        Value.Immutable imm = (Immutable) map.get(IMMUTABLE_METHOD).value();
        if (imm.isAtLeastImmutableHC()) {
            return new ValueOrigin(imm.toCorrespondingIndependent(), FROM_TYPE);
        }
        return DEPENDENT_DEFAULT;
    }


    private void methodPropertiesBeforeParameters(MethodInfo methodInfo,
                                                  Map<Property, ValueOrigin> map,
                                                  boolean explicitlyEmpty) {
        if (methodInfo.isConstructor()) {
            map.put(FLUENT_METHOD, DEFAULT_FALSE);
            map.put(IDENTITY_METHOD, DEFAULT_FALSE);
            map.put(NON_MODIFYING_METHOD, DEFAULT_FALSE);
            map.putIfAbsent(METHOD_ALLOWS_INTERRUPTS, DEFAULT_FALSE);
        } else {
            ValueOrigin fluent = map.get(FLUENT_METHOD);
            if (fluent != null) {
                if (fluent.valueAsBool().isTrue() && explicitlyEmpty) {
                    LOGGER.warn("Impossible! how can a method without statements be @Fluent?");
                }
            } else {
                map.put(FLUENT_METHOD, explicitlyEmpty ? DEFAULT_FALSE : computeMethodFluent(methodInfo));
            }
            ValueOrigin identity = map.get(IDENTITY_METHOD);
            if (identity != null) {
                if (identity.valueAsBool().isTrue() && explicitlyEmpty) {
                    LOGGER.warn("Impossible! how can a method without statements be @Identity?");
                }
                if (identity.valueAsBool().isTrue() && (methodInfo.parameters().isEmpty()
                                                        || !methodInfo.returnType().equals(methodInfo.parameters()
                        .getFirst().parameterizedType()))) {
                    LOGGER.warn("@Identity method must have return type identical to formal type of first parameter");
                }
            } else {
                map.put(IDENTITY_METHOD, explicitlyEmpty || methodInfo.parameters().isEmpty()
                        ? DEFAULT_FALSE : computeMethodIdentity(methodInfo));
            }

            ValueOrigin ignoreModification = map.get(IGNORE_MODIFICATION_METHOD);
            if (ignoreModification != null) {
                if (ignoreModification.valueAsBool().isTrue() && explicitlyEmpty) {
                    messages.add(MessageImpl.warn(methodInfo,
                            "Impossible! how can a method without statements need @IgnoreModifications?"));
                }
            } else {
                map.put(IGNORE_MODIFICATION_METHOD, explicitlyEmpty ? DEFAULT_FALSE
                        : computeIgnoreModificationMethod(methodInfo));
            }

            ValueOrigin nonModifying = map.get(NON_MODIFYING_METHOD);
            if ((nonModifying == null || nonModifying.valueAsBool().isFalse()) && explicitlyEmpty) {
                messages.add(MessageImpl.warn(methodInfo,
                        "Impossible! how can a method without statements be @Modified?"));
            }
            if (nonModifying == null) {
                map.put(NON_MODIFYING_METHOD, explicitlyEmpty ? FROM_METHOD_TRUE
                        : computeMethodNonModifying(methodInfo, map));
            }
            ValueOrigin allowsInterrupt = map.get(METHOD_ALLOWS_INTERRUPTS);
            if (allowsInterrupt != null) {
                if (allowsInterrupt.valueAsBool().isTrue() && explicitlyEmpty) {
                    messages.add(MessageImpl.warn(methodInfo,
                            "Impossible! how can a method without statements be @AllowInterrupt?"));
                }
            } else {
                map.put(METHOD_ALLOWS_INTERRUPTS, explicitlyEmpty ? DEFAULT_FALSE : computeAllowInterrupt(methodInfo));
            }
            ValueOrigin getSetField = map.get(GET_SET_FIELD);
            if (getSetField == null) {
                Value.FieldValue fromOverride = getSetFromOverride(methodInfo);
                if (fromOverride != null) {
                    map.put(GET_SET_FIELD, new ValueOrigin(fromOverride, FROM_OVERRIDE));
                }
            }
            // is on constructors, and factory methods, which don't get overloaded, and also with "equivalent = true"
            ValueOrigin getSetEquivalent = map.get(GET_SET_EQUIVALENT);
            if (getSetEquivalent == null) {
                Value.GetSetEquivalent fromOverride = getSetEquivalentFromOverride(methodInfo);
                if (fromOverride != null) {
                    map.put(GET_SET_EQUIVALENT, new ValueOrigin(fromOverride, FROM_OVERRIDE));
                }
            }
        }
    }

    private FieldValue getSetFromOverride(MethodInfo methodInfo) {
        for (MethodInfo override : methodInfo.overrides()) {
            FieldValue fv = override.getSetField();
            if (fv != null && fv.field() != null) return fv;
        }
        return null;
    }

    private GetSetEquivalent getSetEquivalentFromOverride(MethodInfo methodInfo) {
        for (MethodInfo override : methodInfo.overrides()) {
            GetSetEquivalent fv = override.getSetEquivalents();
            if (fv != null && !fv.isDefault()) return fv;
        }
        return null;
    }

    private Map<Property, ValueOrigin> handleParameter(ParameterInfo parameterInfo,
                                                       Map<Property, ValueOrigin> methodMap,
                                                       boolean explicitlyEmpty) {
        List<AnnotationExpression> annotations = annotationProvider.annotations(parameterInfo);
        Map<Property, Value> annotatedMap = annotationsToMap(parameterInfo, annotations);
        Map<Property, ValueOrigin> map = new HashMap<>();
        annotatedMap.forEach((p, v) -> map.put(p, new ValueOrigin(v, ShallowAnalyzer.AnnotationOrigin.ANNOTATED)));

        if (explicitlyEmpty) {
            ValueOrigin ind = map.get(INDEPENDENT_PARAMETER);
            if (ind != null && !ind.value().equals(INDEPENDENT)) {
                LOGGER.warn("Parameter {} must be independent", parameterInfo);
            }
            map.put(INDEPENDENT_PARAMETER, INDEPENDENT_FROM_METHOD);
            map.put(UNMODIFIED_PARAMETER, FROM_METHOD_TRUE);
            NotNullProperty notNullOfType = analysisHelper.notNullOfType(parameterInfo.parameterizedType());
            map.put(NOT_NULL_PARAMETER, notNullOfType.isNullable() ? NULLABLE_DEFAULT :
                    new ValueOrigin(notNullOfType, FROM_TYPE));
            map.putIfAbsent(IGNORE_MODIFICATIONS_PARAMETER, DEFAULT_FALSE);
        } else {
            ValueOrigin imm = map.get(IMMUTABLE_PARAMETER);
            if (imm == null) {
                Immutable value = analysisHelper.typeImmutable(parameterInfo.parameterizedType());
                if (value == null) {
                    LOGGER.warn("Have no @Immutable value for {}", parameterInfo.parameterizedType());
                    value = ValueImpl.ImmutableImpl.MUTABLE;
                }
                map.put(IMMUTABLE_PARAMETER, value.equals(ValueImpl.ImmutableImpl.MUTABLE) ? MUTABLE_DEFAULT
                        : new ValueOrigin(value, FROM_TYPE));
            }

            ValueOrigin ind = map.get(INDEPENDENT_PARAMETER);
            if (ind == null) {
                map.put(INDEPENDENT_PARAMETER, computeParameterIndependent(parameterInfo, methodMap, map));
            }
            ValueOrigin ign = map.get(IGNORE_MODIFICATIONS_PARAMETER);
            ValueOrigin ignComputed;
            if (ign == null) {
                ignComputed = computeParameterIgnoreModifications(parameterInfo);
                map.put(IGNORE_MODIFICATIONS_PARAMETER, ignComputed);
            } else {
                ignComputed = ign;
            }
            ValueOrigin mod = map.get(UNMODIFIED_PARAMETER);
            if (mod == null) {
                map.put(UNMODIFIED_PARAMETER, computeParameterUnmodified(parameterInfo, ignComputed));
            }
            ValueOrigin nn = map.get(NOT_NULL_PARAMETER);
            if (nn == null) {
                map.put(NOT_NULL_PARAMETER, computeParameterNotNull(parameterInfo));
            }
            ValueOrigin c = map.get(CONTAINER_PARAMETER);
            if (c == null) {
                map.put(CONTAINER_PARAMETER, computeParameterContainer(parameterInfo));
            }
        }
        return map;
    }

    private ValueOrigin computeParameterContainer(ParameterInfo parameterInfo) {
        Value.Bool container = analysisHelper.typeContainer(parameterInfo.parameterizedType());
        return container.isFalse() ? DEFAULT_FALSE : FROM_TYPE_TRUE;
    }

    private ValueOrigin computeParameterIgnoreModifications(ParameterInfo parameterInfo) {
        ParameterizedType pt = parameterInfo.parameterizedType();
        return pt.isStandardFunctionalInterface() ? FROM_TYPE_TRUE : DEFAULT_FALSE;
    }

    private ValueOrigin computeParameterNotNull(ParameterInfo parameterInfo) {
        ParameterizedType pt = parameterInfo.parameterizedType();
        if (pt.isPrimitiveExcludingVoid()) return NOT_NULL_FROM_TYPE;
        MethodInfo methodInfo = parameterInfo.methodInfo();
        NotNullProperty fromOverride = methodInfo.overrides().stream()
                .filter(MethodInfo::isPublic)
                .map(mi -> mi.parameters().get(parameterInfo.index()))
                .filter(pi -> pi.analysis().haveAnalyzedValueFor(NOT_NULL_PARAMETER))
                .map(pi -> pi.analysis().getOrDefault(NOT_NULL_PARAMETER, ValueImpl.NotNullImpl.NULLABLE))
                .reduce(ValueImpl.NotNullImpl.NULLABLE, NotNullProperty::max);
        return fromOverride.isNullable() ? NULLABLE_DEFAULT : NOT_NULL_FROM_OVERRIDE;
    }

    private ValueOrigin computeParameterUnmodified(ParameterInfo parameterInfo, ValueOrigin ign) {
        MethodInfo methodInfo = parameterInfo.methodInfo();
        if (((Value.Bool) ign.value()).isTrue()) {
            return new ValueOrigin(FALSE, ign.origin());
        }
        Value.Bool typeContainer = methodInfo.typeInfo().analysis().getOrDefault(CONTAINER_TYPE, FALSE);
        if (typeContainer.isTrue()) {
            return FROM_OWNER_TRUE;
        }
        ParameterizedType type = parameterInfo.parameterizedType();
        if (type.isPrimitiveStringClass()) {
            return FROM_TYPE_TRUE;
        }
        Value.Immutable typeImmutable = analysisHelper.typeImmutable(type);
        if (typeImmutable != null && typeImmutable.isAtLeastImmutableHC()) {
            return FROM_TYPE_TRUE;
        }
        Value.Bool override = methodInfo.overrides().stream()
                .map(mi -> mi.parameters().get(parameterInfo.index()))
                .map(pi -> pi.analysis().getOrDefault(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.NO_VALUE))
                .reduce(ValueImpl.BoolImpl.NO_VALUE, Value.Bool::or);
        if (override.isTrue()) {
            return FROM_OVERRIDE_TRUE;
        }
        return DEFAULT_FALSE;
    }

    private ValueOrigin computeParameterIndependent(ParameterInfo parameterInfo,
                                                    Map<Property, ValueOrigin> methodMap,
                                                    Map<Property, ValueOrigin> map) {
        ParameterizedType type = parameterInfo.parameterizedType();
        Value.Immutable immutable = (Immutable) map.get(PropertyImpl.IMMUTABLE_PARAMETER).value();
        MethodInfo methodInfo = parameterInfo.methodInfo();

        if (type.isPrimitiveExcludingVoid() || immutable.isImmutable()) {
            return INDEPENDENT_FROM_TYPE;
        }


        Value.Bool nonModifyingMethod = methodMap.get(PropertyImpl.NON_MODIFYING_METHOD).valueAsBool();
        if (nonModifyingMethod.isTrue() && !methodInfo.isFactoryMethod()) {
            return INDEPENDENT_FROM_METHOD;
        }

        // note that an unbound type parameter is by default @Dependent, not @Independent1!!
        Value.Independent independentFromImmutable = immutable.toCorrespondingIndependent();
        TypeInfo ownerType = parameterInfo.methodInfo().typeInfo();
        Value.Independent independentType = ownerType.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        Value.Independent override = methodInfo.overrides().stream()
                .filter(MethodInfo::isPublic)
                .map(mi -> mi.parameters().get(parameterInfo.index()))
                .filter(pi -> pi.analysis().haveAnalyzedValueFor(INDEPENDENT_PARAMETER))
                .map(pi -> pi.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT))
                .reduce(DEPENDENT, Value.Independent::max);
        Value.Independent max = override.max(independentType.max(independentFromImmutable));
        if (max.isDependent()) return DEPENDENT_DEFAULT;
        if (!max.equals(override)) return new ValueOrigin(max, FROM_TYPE);
        return new ValueOrigin(max, FROM_OVERRIDE);
    }


    private ValueOrigin computeMethodNonModifying(MethodInfo methodInfo, Map<Property, ValueOrigin> map) {
        if (methodInfo.isConstructor()) return DEFAULT_FALSE; // almost by default, constructors modify the fields
        ValueOrigin fluent = map.get(FLUENT_METHOD);
        if (fluent != null && fluent.valueAsBool().isTrue()) return DEFAULT_FALSE; // modifying--what else would it do?

        Value.Immutable typeImmutable = methodInfo.typeInfo().analysis().getOrDefault(IMMUTABLE_TYPE,
                ValueImpl.ImmutableImpl.MUTABLE);
        if (typeImmutable.isAtLeastImmutableHC()) return FROM_TYPE_TRUE;

        return commonBooleanFromOverride(NON_MODIFYING_METHOD, methodInfo);
    }

    private ValueOrigin computeMethodFluent(MethodInfo methodInfo) {
        if (methodInfo.returnType().typeInfo() != methodInfo.typeInfo()) return DEFAULT_FALSE;
        return commonBooleanFromOverride(FLUENT_METHOD, methodInfo);
    }

    private ValueOrigin computeMethodIdentity(MethodInfo methodInfo) {
        return commonBooleanFromOverride(IDENTITY_METHOD, methodInfo);
    }

    private ValueOrigin computeIgnoreModificationMethod(MethodInfo methodInfo) {
        return commonBooleanFromOverride(IGNORE_MODIFICATION_METHOD, methodInfo);
    }

    private ValueOrigin computeAllowInterrupt(MethodInfo methodInfo) {
        return commonBooleanFromOverride(METHOD_ALLOWS_INTERRUPTS, methodInfo);
    }

    private ValueOrigin commonBooleanFromOverride(Property property, MethodInfo methodInfo) {
        Value.Bool v = FALSE;
        for (MethodInfo mi : methodInfo.overrides()) {
            if (mi.isPublic()) {
                Value.Bool o = mi.analysis().getOrNull(property, ValueImpl.BoolImpl.class);
                if (o != null) {
                    v = v.or(o);
                }
            }
        }
        return v.isFalse() ? DEFAULT_FALSE : FROM_OVERRIDE_TRUE;
    }

    public List<Message> messages() {
        return messages;
    }
}
