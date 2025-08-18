package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.util.ParSeq;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class ValueImpl implements Value {

    private static final Map<Class<? extends Value>, BiFunction<Codec.DI, Codec.EncodedValue, Value>> decoderMap = new HashMap<>();

    public static BiFunction<Codec.DI, Codec.EncodedValue, Value> decoder(Class<? extends Value> clazz) {
        return decoderMap.get(clazz);
    }

    public static class BoolImpl implements Value.Bool {
        public static final Bool NO_VALUE = new BoolImpl(-1);
        public static final Bool FALSE = new BoolImpl(0);
        public static final Bool TRUE = new BoolImpl(1);
        private final int value;

        private BoolImpl(int value) {
            this.value = value;
        }

        public static Value.Bool from(int i) {
            return switch (i) {
                case -1 -> BoolImpl.NO_VALUE;
                case 0 -> BoolImpl.FALSE;
                case 1 -> BoolImpl.TRUE;
                default -> throw new UnsupportedOperationException();
            };
        }

        public static Value.Bool from(boolean b) {
            return b ? TRUE : FALSE;
        }

        @Override
        public boolean isDefault() {
            return value == 0;
        }

        @Override
        public boolean isFalse() {
            return value == 0;
        }

        @Override
        public boolean hasAValue() {
            return value >= 0;
        }

        @Override
        public Bool or(Bool bool) {
            if (this == NO_VALUE) return bool;
            if (bool == NO_VALUE) return this;
            return value == 1 ? this : bool;
        }

        @Override
        public boolean isTrue() {
            return value == 1;
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            if (value == 0) return null; // no need to encode 0
            return codec.encodeInt(context, value);
        }

        @Override
        public int compareTo(Value o) {
            if (o instanceof BoolImpl b) {
                return value - b.value;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return switch (value) {
                case -1 -> "<no value>";
                case 0 -> "false";
                case 1 -> "true";
                default -> throw new UnsupportedOperationException();
            };
        }

        @Override
        public boolean overwriteAllowed(Value newValue) {
            return value < ((BoolImpl) newValue).value;
        }
    }

    static {
        decoderMap.put(BoolImpl.class, (di, encodedValue) -> BoolImpl.from(di.codec().decodeInt(di.context(), encodedValue)));
    }

    public record MessageImpl(String message) implements Value.Message {
        public static final Message EMPTY = new MessageImpl("");

        @Override
        public boolean isDefault() {
            return message.isBlank();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            if (message.isEmpty()) return null;
            return codec.encodeString(context, message);
        }

        @Override
        public boolean isEmpty() {
            return message.isEmpty();
        }
    }

    public record ParameterParSeqImpl(ParSeq<ParameterInfo> parSeq) implements Value.ParameterParSeq {
        public static ParameterParSeqImpl EMPTY = new ValueImpl.ParameterParSeqImpl(new ParSeq<>() {
            @Override
            public boolean containsParallels() {
                return false;
            }

            @Override
            public <X> List<X> sortParallels(List<X> items, Comparator<X> comparator) {
                return items;
            }
        });

        @Override
        public boolean isDefault() {
            return this == EMPTY;
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            throw new UnsupportedOperationException();
        }
    }

    public record CommutableDataImpl(String seq, String par, String multi) implements CommutableData {
        public static final CommutableData NONE = new ValueImpl.CommutableDataImpl(null, null, null);
        public static final CommutableData BLANK = new ValueImpl.CommutableDataImpl("", "", "");

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            return codec.encodeList(context, List.of(codec.encodeString(context, seq), codec.encodeString(context, par),
                    codec.encodeString(context, multi)));
        }

        @Override
        public boolean isNone() {
            return NONE == this;
        }

        @Override
        public boolean isDefault() {
            return NONE.equals(this);
        }
    }

    static {
        decoderMap.put(CommutableDataImpl.class, (di, encodedValue) -> {
            List<Codec.EncodedValue> list = di.codec().decodeList(di.context(), encodedValue);
            return new CommutableDataImpl(di.codec().decodeString(di.context(), list.getFirst()),
                    di.codec().decodeString(di.context(), list.get(1)),
                    di.codec().decodeString(di.context(), list.get(2)));
        });
    }

    public static class ImmutableImpl implements Immutable {
        private final int value;

        private ImmutableImpl(int value) {
            this.value = value;
        }

        // NO_VALUE for 'null' constant
        public static final ImmutableImpl NO_VALUE = new ImmutableImpl(-1);
        public static final ImmutableImpl MUTABLE = new ImmutableImpl(0);
        public static final ImmutableImpl FINAL_FIELDS = new ImmutableImpl(1);
        public static final ImmutableImpl IMMUTABLE_HC = new ImmutableImpl(2);
        public static final ImmutableImpl IMMUTABLE = new ImmutableImpl(3);

        public static Value.Immutable from(int level) {
            return switch (level) {
                case -1 -> NO_VALUE;
                case 0 -> MUTABLE;
                case 1 -> FINAL_FIELDS;
                case 2 -> IMMUTABLE_HC;
                case 3 -> IMMUTABLE;
                default -> throw new UnsupportedOperationException();
            };
        }

        @Override
        public boolean isDefault() {
            return value == 0;
        }

        @Override
        public boolean isAtLeastImmutableHC() {
            return value >= 2;
        }

        @Override
        public boolean isImmutableHC() {
            return value == 2;
        }

        @Override
        public boolean isImmutable() {
            return value == 3;
        }

        @Override
        public boolean isMutable() {
            return value == 0 || value == 1;
        }

        @Override
        public boolean isFinalFields() {
            return value == 1;
        }

        @Override
        public Immutable max(Immutable other) {
            if (other == null) return this;
            assert this != NO_VALUE && other != NO_VALUE;
            int otherValue = ((ImmutableImpl) other).value;
            if (value >= otherValue) return this;
            return other;
        }

        @Override
        public Immutable min(Immutable other) {
            if (other == null) return this;
            assert this != NO_VALUE && other != NO_VALUE;
            int otherValue = ((ImmutableImpl) other).value;
            if (value <= otherValue) return this;
            return other;
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            if (value <= 0) return null;
            return codec.encodeInt(context, value);
        }

        @Override
        public int compareTo(Value o) {
            if (o instanceof ImmutableImpl i) {
                return value - i.value;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public Independent toCorrespondingIndependent() {
            return switch (value) {
                case 0, 1 -> IndependentImpl.DEPENDENT;
                case 2 -> IndependentImpl.INDEPENDENT_HC;
                case 3 -> IndependentImpl.INDEPENDENT;
                default -> throw new UnsupportedOperationException();
            };
        }

        @Override
        public String toString() {
            return switch (value) {
                case -1 -> "<no immutable value>";
                case 0 -> "@Mutable";
                case 1 -> "@FinalFields";
                case 2 -> "@Immutable(hc=true)";
                case 3 -> "@Immutable";
                default -> throw new UnsupportedOperationException();
            };
        }

        @Override
        public boolean overwriteAllowed(Value newValue) {
            return value < ((ImmutableImpl) newValue).value;
        }
    }

    static {
        decoderMap.put(ImmutableImpl.class, (di, encodedValue) ->
                ImmutableImpl.from(di.codec().decodeInt(di.context(), encodedValue)));
    }

    public static final int INDEX_RETURN_VALUE = -1;

    public static class IndependentImpl implements Independent {
        private final int value;
        private final Map<Integer, Integer> linkToParametersReturnValue;

        private IndependentImpl(int value) {
            this.value = value;
            this.linkToParametersReturnValue = Map.of();
        }

        // public: testing in cst-io
        public IndependentImpl(int value, Map<Integer, Integer> linkToParametersReturnValue) {
            this.value = value;
            this.linkToParametersReturnValue = linkToParametersReturnValue;
        }

        /*
        The map is a summary of all that happens at this parameter.
         */
        public static Map<Integer, Integer> makeMap(int[] dependentParameters,
                                                    int[] hcParameters,
                                                    Boolean dependentReturnValue,
                                                    Boolean hcReturnValue) {
            Map<Integer, Integer> map = new HashMap<>();
            if (hcReturnValue != null && hcReturnValue) map.put(-1, 1);
            if (dependentReturnValue != null && dependentReturnValue) map.put(-1, 0);
            if (dependentParameters != null) {
                for (int i : dependentParameters) map.put(i, 0);
            }
            if (hcParameters != null) {
                for (int i : hcParameters) map.put(i, 1);
            }
            return Map.copyOf(map);
        }

        @Override
        public Map<Integer, Integer> linkToParametersReturnValue() {
            return linkToParametersReturnValue;
        }

        public static final Independent INDEPENDENT_DELAYED = new IndependentImpl(-1);
        public static final Independent DEPENDENT = new IndependentImpl(0);
        public static final Independent INDEPENDENT_HC = new IndependentImpl(1);
        public static final Independent INDEPENDENT = new IndependentImpl(2);

        public static Independent from(int level) {
            return switch (level) {
                case -1 -> INDEPENDENT_DELAYED;
                case 0 -> DEPENDENT;
                case 1 -> INDEPENDENT_HC;
                case 2 -> INDEPENDENT;
                default -> throw new UnsupportedOperationException();
            };
        }

        @Override
        public boolean isDefault() {
            return value == 0 && linkToParametersReturnValue.isEmpty();
        }

        @Override
        public boolean isAtLeastIndependentHc() {
            return value > 0;
        }

        @Override
        public boolean isDependent() {
            return value == 0;
        }

        @Override
        public boolean isIndependent() {
            return value == 2;
        }

        @Override
        public boolean isIndependentHc() {
            return value == 1;
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            if (linkToParametersReturnValue.isEmpty()) {
                if (value == 0) return null;
                return codec.encodeInt(context, value);
            }
            Map<Codec.EncodedValue, Codec.EncodedValue> encodedMap = linkToParametersReturnValue.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(e -> codec.encodeString(context, "" + e.getKey()),
                            e -> codec.encodeInt(context, e.getValue())));
            return codec.encodeList(context, List.of(codec.encodeInt(context, value), codec.encodeMap(context, encodedMap)));
        }

        @Override
        public int compareTo(Value o) {
            if (o instanceof IndependentImpl i) {
                return value - i.value;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public Independent min(Independent other) {
            if (other == null) return this;
            int otherValue = ((IndependentImpl) other).value;
            return value <= otherValue ? this : other;
        }

        @Override
        public Independent max(Independent other) {
            if (other == null) return this;
            int otherValue = ((IndependentImpl) other).value;
            return value >= otherValue ? this : other;
        }

        @Override
        public String toString() {
            String linkString = computeLinkString();
            return switch (value) {
                case -1 -> "<independent delayed>";
                case 0 -> "@Dependent" + (linkString.isEmpty() ? "" : "(" + linkString + ")");
                case 1 -> "@Independent(hc=true" + (linkString.isEmpty() ? "" : ", " + linkString) + ")";
                case 2 -> "@Independent" + (linkString.isEmpty() ? "" : "(" + linkString + ")");
                default -> throw new UnsupportedOperationException();
            };
        }

        private String computeLinkString() {
            List<String> list = new ArrayList<>();
            List<String> hcList = new ArrayList<>();
            List<String> depList = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : linkToParametersReturnValue.entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getKey)).toList()) {
                int index = e.getKey();
                int independentLevel = e.getValue();
                if (e.getKey() == INDEX_RETURN_VALUE) {
                    if (0 == independentLevel) list.add("dependentReturnValue=true");
                    else if (1 == independentLevel) list.add("hcReturnValue=true");
                } else if (0 == independentLevel) {
                    depList.add(Integer.toString(index));
                } else if (1 == independentLevel) {
                    hcList.add(Integer.toString(index));
                }
            }
            if (!depList.isEmpty()) {
                list.add("dependentParameters={" + String.join(", ", depList) + "}");
            }
            if (!hcList.isEmpty()) {
                list.add("hcParameters={" + String.join(", ", hcList) + "}");
            }
            return String.join(", ", list);
        }

        @Override
        public boolean overwriteAllowed(Value newValue) {
            return value < ((IndependentImpl) newValue).value;
        }
    }

    public static Independent decodeIndependentImpl(Codec codec, Codec.Context context, Codec.EncodedValue encodedValue) {
        if (codec.isList(encodedValue)) {
            List<Codec.EncodedValue> encodedList = codec.decodeList(context, encodedValue);
            int value = codec.decodeInt(context, encodedList.getFirst());
            Map<Codec.EncodedValue, Codec.EncodedValue> encodedMap = codec.decodeMap(context, encodedList.get(1));
            Map<Integer, Integer> map = encodedMap.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    e -> Integer.parseInt(codec.decodeString(context, e.getKey())),
                    e -> codec.decodeInt(context, e.getValue())));
            return new IndependentImpl(value, map);
        }
        return IndependentImpl.from(codec.decodeInt(context, encodedValue));
    }

    static {
        decoderMap.put(IndependentImpl.class, (di, ev) -> decodeIndependentImpl(di.codec(), di.context(), ev));
    }

    public record GetSetValueImpl(FieldInfo field, boolean setter, int parameterIndexOfIndex) implements FieldValue {
        public static final FieldValue EMPTY = new GetSetValueImpl(null, false, -1);

        @Override
        public boolean isDefault() {
            return equals(EMPTY);
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            List<Codec.EncodedValue> list = new ArrayList<>();
            list.add(codec.encodeInfoInContext(context, field, "" + codec.fieldIndex(field)));
            list.add(codec.encodeBoolean(context, setter));
            if (parameterIndexOfIndex >= 0) {
                list.add(codec.encodeInt(context, parameterIndexOfIndex));
            }
            return codec.encodeList(context, list);
        }

        @Override
        public Variable createVariable(Runtime runtime, Expression object, Expression indexOrNull) {
            assert object != null;
            FieldReference fieldReference = runtime.newFieldReference(field, object, field.type());
            if (indexOrNull == null) {
                // straight: objects
                return fieldReference;
            }
            // indexing: objects[i]
            VariableExpression arrayExpression = runtime.newVariableExpressionBuilder()
                    .setVariable(fieldReference)
                    .setSource(object.source())
                    .build();
            return runtime.newDependentVariable(arrayExpression, indexOrNull);
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            return new GetSetValueImpl(infoMap.fieldInfo(field), setter, parameterIndexOfIndex);
        }
    }

    static {
        decoderMap.put(GetSetValueImpl.class, (di, encodedValue) -> {
            Codec codec = di.codec();
            Codec.Context context = di.context();
            List<Codec.EncodedValue> list = codec.decodeList(context, encodedValue);
            FieldInfo field = codec.decodeFieldInfo(context.currentType(), list.getFirst());
            boolean setter = codec.decodeBoolean(context, list.get(1));
            int index = list.size() == 2 ? -1 : codec.decodeInt(context, list.get(2));
            return new GetSetValueImpl(field, setter, index);
        });
    }

    public record FieldBooleanMapImpl(Map<FieldInfo, Boolean> map) implements FieldBooleanMap {
        public static final FieldBooleanMap EMPTY = new FieldBooleanMapImpl(Map.of());

        @Override
        public boolean isDefault() {
            return map.isEmpty();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            Map<Codec.EncodedValue, Codec.EncodedValue> encodedMap = map.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            e -> codec.encodeInfoInContext(context, e.getKey(), "" + codec.fieldIndex(e.getKey())),
                            e -> codec.encodeBoolean(context, e.getValue())));
            return codec.encodeMap(context, encodedMap);
        }

        @Override
        public String toString() {
            return map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).sorted()
                    .collect(Collectors.joining(", "));
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            return new FieldBooleanMapImpl(map.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    e -> infoMap.fieldInfo(e.getKey()), Map.Entry::getValue)));
        }
    }

    static {
        decoderMap.put(FieldBooleanMapImpl.class, (di, encodedValue) -> {
            Map<FieldInfo, Boolean> decodedMap = di.codec().decodeMap(di.context(), encodedValue)
                    .entrySet().stream().collect(Collectors.toUnmodifiableMap(
                            e -> di.codec().decodeFieldInfo(di.context().currentType(), e.getKey()),
                            e -> di.codec().decodeBoolean(di.context(), e.getValue())));
            return new FieldBooleanMapImpl(decodedMap);
        });
    }

    public record VariableBooleanMapImpl(Map<Variable, Boolean> map) implements VariableBooleanMap {
        public static final VariableBooleanMap EMPTY = new VariableBooleanMapImpl(Map.of());

        @Override
        public boolean isDefault() {
            return map.isEmpty();
        }

        // we allow the removal of elements, or the change from true to false
        @Override
        public boolean overwriteAllowed(Value newValue) {
            VariableBooleanMap other = (VariableBooleanMap) newValue;
            return map.entrySet().stream().allMatch(e -> {
                Boolean newB = other.map().get(e.getKey());
                return newB == null || !newB || e.getValue();
            });
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            if(map.isEmpty()) return null;
            Map<Codec.EncodedValue, Codec.EncodedValue> encodedMap = map.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(e -> codec.encodeVariable(context, e.getKey()),
                            e -> codec.encodeBoolean(context, e.getValue())));
            return codec.encodeMapAsList(context, encodedMap);
        }

        @Override
        public String toString() {
            return map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).sorted()
                    .collect(Collectors.joining(", "));
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            return new VariableBooleanMapImpl(map.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    e -> e.getKey().rewire(infoMap), Map.Entry::getValue)));
        }
    }

    static {
        decoderMap.put(VariableBooleanMapImpl.class, (di, encodedValue) -> {
            Map<Variable, Boolean> decodedMap = di.codec().decodeMapAsList(di.context(), encodedValue)
                    .entrySet().stream().collect(Collectors.toUnmodifiableMap(
                            e -> di.codec().decodeVariable(di.context(), e.getKey()),
                            e -> di.codec().decodeBoolean(di.context(), e.getValue())));
            return new VariableBooleanMapImpl(decodedMap);
        });
    }

    public record AssignedToFieldImpl(Set<FieldInfo> fields) implements AssignedToField {
        public static final AssignedToField EMPTY = new AssignedToFieldImpl(Set.of());

        @Override
        public boolean isDefault() {
            return fields.isEmpty();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            Set<Codec.EncodedValue> set = fields.stream().map(fi -> codec.encodeInfoInContext(context, fi,
                    "" + codec.fieldIndex(fi))).collect(Collectors.toUnmodifiableSet());
            return codec.encodeSet(context, set);
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            return new AssignedToFieldImpl(fields.stream().map(infoMap::fieldInfo)
                    .collect(Collectors.toUnmodifiableSet()));
        }
    }

    static {
        decoderMap.put(AssignedToFieldImpl.class, (di, encodedValue) -> {
            Set<FieldInfo> decodedSet = di.codec().decodeSet(di.context(), encodedValue).stream()
                    .map(e -> di.codec().decodeFieldInfo(di.context().currentType(), e))
                    .collect(Collectors.toUnmodifiableSet());
            return new AssignedToFieldImpl(decodedSet);
        });
    }

    public record PostConditionsImpl(Map<String, Expression> byIndex) implements PostConditions {
        public static final PostConditions EMPTY = new PostConditionsImpl(Map.of());

        @Override
        public boolean isDefault() {
            return byIndex.isEmpty();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            Map<Codec.EncodedValue, Codec.EncodedValue> encodedMap = byIndex.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            e -> codec.encodeString(context, e.getKey()),
                            e -> codec.encodeExpression(context, e.getValue())));
            return codec.encodeMap(context, encodedMap);
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            return new PostConditionsImpl(byIndex.entrySet().stream().collect(Collectors
                    .toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().rewire(infoMap))));
        }
    }

    static {
        decoderMap.put(PostConditionsImpl.class, (di, encodedValue) -> {
            Map<String, Expression> decodeMap = di.codec().decodeMap(di.context(), encodedValue).entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            e -> di.codec().decodeString(di.context(), e.getKey()),
                            e -> di.codec().decodeExpression(di.context(), e.getValue())));
            return new PostConditionsImpl(decodeMap);
        });
    }

    public record PreconditionImpl(Expression expression) implements Precondition {
        public static final Precondition EMPTY = new PreconditionImpl(null);

        @Override
        public boolean isDefault() {
            return expression == null;
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            return codec.encodeExpression(context, expression);
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            return new PreconditionImpl(expression.rewire(infoMap));
        }
    }

    static {
        decoderMap.put(PreconditionImpl.class, (di, encodedValue) ->
                new PreconditionImpl(di.codec().decodeExpression(di.context(), encodedValue)));
    }

    public record IndicesOfEscapesImpl(Set<String> indices) implements IndicesOfEscapes {
        public static final IndicesOfEscapes EMPTY = new IndicesOfEscapesImpl(Set.of());

        @Override
        public boolean isDefault() {
            return indices.isEmpty();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            Set<Codec.EncodedValue> set = indices.stream()
                    .map(string -> codec.encodeString(context, string))
                    .collect(Collectors.toUnmodifiableSet());
            return codec.encodeSet(context, set);
        }
    }

    static {
        decoderMap.put(IndicesOfEscapesImpl.class, (di, encodedValue) -> {
            Set<String> indices = di.codec().decodeSet(di.context(), encodedValue).stream()
                    .map(e -> di.codec().decodeString(di.context(), e))
                    .collect(Collectors.toUnmodifiableSet());
            return new IndicesOfEscapesImpl(indices);
        });
    }

    public record GetSetEquivalentImpl(Set<ParameterInfo> convertToGetSet,
                                       MethodInfo methodWithoutParameters) implements GetSetEquivalent {
        public static final GetSetEquivalent EMPTY = new GetSetEquivalentImpl(Set.of(), null);

        @Override
        public boolean isDefault() {
            return equals(EMPTY);
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            Set<Codec.EncodedValue> set = convertToGetSet.stream()
                    .map(v -> codec.encodeVariable(context, v))
                    .collect(Collectors.toUnmodifiableSet());
            return codec.encodeList(context, List.of(codec.encodeSet(context, set),
                    codec.encodeInfoOutOfContext(context, methodWithoutParameters)));
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            return new GetSetEquivalentImpl(convertToGetSet.stream().map(infoMap::parameterInfo)
                    .collect(Collectors.toUnmodifiableSet()), infoMap.methodInfo(methodWithoutParameters));
        }
    }

    static {
        decoderMap.put(GetSetEquivalentImpl.class, (di, encodedValue) -> {
            List<Codec.EncodedValue> list = di.codec().decodeList(di.context(), encodedValue);
            Set<ParameterInfo> set = di.codec().decodeSet(di.context(), list.getFirst()).stream()
                    .map(e -> (ParameterInfo) di.codec().decodeVariable(di.context(), e))
                    .collect(Collectors.toUnmodifiableSet());
            return new GetSetEquivalentImpl(set, (MethodInfo) di.codec().decodeInfoOutOfContext(di.context(), list.get(1)));
        });
    }


    public record NotNullImpl(int value) implements NotNull {
        public static final NotNull NO_VALUE = new NotNullImpl(-1);
        public static final NotNull NULLABLE = new NotNullImpl(0);
        public static final NotNull NOT_NULL = new NotNullImpl(1);
        public static final NotNull CONTENT_NOT_NULL = new NotNullImpl(2);

        public static Value from(int level) {
            return switch (level) {
                case -1 -> NO_VALUE;
                case 0 -> NULLABLE;
                case 1 -> NOT_NULL;
                case 2 -> CONTENT_NOT_NULL;
                default -> throw new UnsupportedOperationException();
            };
        }

        @Override
        public boolean isDefault() {
            return value == 0;
        }

        @Override
        public boolean isAtLeastNotNull() {
            return value >= 1;
        }

        @Override
        public boolean isNullable() {
            return value == 0;
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            if (value <= 0) return null;
            return codec.encodeInt(context, value);
        }

        @Override
        public int compareTo(Value o) {
            if (o instanceof NotNullImpl(int value1)) {
                return value - value1;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public NotNull max(NotNull other) {
            int v = ((NotNullImpl) other).value;
            return value >= v ? this : other;
        }
    }

    static {
        decoderMap.put(NotNullImpl.class, (di, encodedValue) ->
                NotNullImpl.from(di.codec().decodeInt(di.context(), encodedValue)));
    }

    public record SetOfStringsImpl(Set<String> set) implements SetOfStrings {
        public static final SetOfStrings EMPTY_SET = new SetOfStringsImpl(Set.of());

        @Override
        public boolean isDefault() {
            return set.isEmpty();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            List<Codec.EncodedValue> encodedValues = set.stream().sorted()
                    .map(s -> codec.encodeString(context, s)).toList();
            return codec.encodeList(context, encodedValues);
        }

        public static SetOfStrings from(Codec codec, Codec.Context context, Codec.EncodedValue encodedList) {
            List<Codec.EncodedValue> encodedValues = codec.decodeList(context, encodedList);
            Set<String> set = encodedValues.stream().map(e -> codec.decodeString(context, e))
                    .collect(Collectors.toUnmodifiableSet());
            return new SetOfStringsImpl(set);
        }
    }

    static {
        decoderMap.put(SetOfStringsImpl.class, (di, ev) -> SetOfStringsImpl.from(di.codec(), di.context(), ev));
    }

    public record SetOfInfoImpl(Set<? extends Info> infoSet) implements SetOfInfo {

        @Override
        public boolean isDefault() {
            return infoSet.isEmpty();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            List<Codec.EncodedValue> encodedValues = infoSet.stream()
                    .sorted(Comparator.comparing(Info::fullyQualifiedName))
                    .map(info -> {
                        int index;
                        if (info instanceof TypeInfo) index = -1;
                        else if (info instanceof MethodInfo methodInfo) {
                            if (methodInfo.isConstructor()) {
                                index = codec.constructorIndex(methodInfo);
                            } else {
                                index = codec.methodIndex(methodInfo);
                            }
                        } else if (info instanceof FieldInfo fieldInfo) index = codec.fieldIndex(fieldInfo);
                        else throw new UnsupportedOperationException();
                        return codec.encodeInfoInContext(context, info, "" + index);
                    }).toList();
            return codec.encodeList(context, encodedValues);
        }

        public static SetOfInfo from(Codec codec, Codec.Context context, Codec.EncodedValue encodedList) {
            List<Codec.EncodedValue> encodedValues = codec.decodeList(context, encodedList);
            Set<Info> set = encodedValues.stream().map(e -> codec.decodeInfoInContext(context, e))
                    .collect(Collectors.toUnmodifiableSet());
            return new SetOfInfoImpl(set);
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            throw new UnsupportedOperationException("NYI");
        }
    }

    static {
        decoderMap.put(SetOfInfoImpl.class, (di, ev) -> SetOfInfoImpl.from(di.codec(), di.context(), ev));
    }

    public record SetOfTypeInfoImpl(Set<TypeInfo> typeInfoSet) implements SetOfTypeInfo {
        public static final SetOfTypeInfo EMPTY = new SetOfTypeInfoImpl(Set.of());

        @Override
        public boolean isDefault() {
            return typeInfoSet.isEmpty();
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            List<Codec.EncodedValue> encodedValues = typeInfoSet.stream()
                    .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName))
                    .map(info -> codec.encodeInfoInContext(context, info, "")).toList();
            return codec.encodeList(context, encodedValues);
        }

        public static SetOfTypeInfo from(Codec codec, Codec.Context context, Codec.EncodedValue encodedList) {
            List<Codec.EncodedValue> encodedValues = codec.decodeList(context, encodedList);
            Set<TypeInfo> set = encodedValues.stream().map(e -> (TypeInfo) codec.decodeInfoInContext(context, e))
                    .collect(Collectors.toUnmodifiableSet());
            return new SetOfTypeInfoImpl(set);
        }

        @Override
        public Value rewire(InfoMap infoMap) {
            throw new UnsupportedOperationException("NYI");
        }
    }

    static {
        decoderMap.put(SetOfTypeInfoImpl.class, (di, ev) -> SetOfTypeInfoImpl.from(di.codec(), di.context(), ev));
    }
}
